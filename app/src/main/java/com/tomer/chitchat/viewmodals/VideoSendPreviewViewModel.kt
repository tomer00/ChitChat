package com.tomer.chitchat.viewmodals

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.PlayerMessage
import com.otaliastudios.transcoder.TranscoderListener
import com.tomer.chitchat.repo.RepoPersons
import com.tomer.chitchat.repo.RepoStorage
import com.tomer.chitchat.room.ModelPartnerPref
import com.tomer.chitchat.room.MsgMediaType
import com.tomer.chitchat.utils.ConversionUtils
import com.tomer.chitchat.utils.VideoDetails
import com.tomer.chitchat.utils.compressAndTrimDeepMedia
import com.tomer.chitchat.utils.estimateFileSize
import com.tomer.chitchat.utils.extract10Frames
import com.tomer.chitchat.utils.getAllPossibleDetails
import com.tomer.chitchat.utils.timeTextFromMs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class VideoSendPreviewViewModel
@Inject constructor(
    @param:ApplicationContext val appContext: Context,
    private val repo: RepoPersons,
    private val repoStorage: RepoStorage,
) : ViewModel() {

    val exoPlayer = ExoPlayer.Builder(appContext).build()

    val flowFramesThumb = MutableSharedFlow<Pair<Int, Bitmap>>()

    val partnerName = MutableStateFlow<ModelPartnerPref?>(null)
    val showPlayButton = MutableStateFlow(true)
    val metaData = MutableStateFlow<VideoDetails?>(null)

    val videoFinalTime = MutableStateFlow("")
    val videoTrimTime = MutableStateFlow("")
    val isMute = MutableStateFlow(false)
    val seekBars = MutableStateFlow(Triple(0f, 0f, 1f))

    val encodeProg = MutableStateFlow(-1f)
    val finishActivity = MutableStateFlow(false)


    private var uri: Uri? = null
    private var seekBarSyncJob = viewModelScope.launch { }

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) {
                    seekBarSyncJob.cancel()
                    seekBarSyncJob = createSeekBarSyncJob()
                } else seekBarSyncJob.cancel()
                viewModelScope.launch {
                    showPlayButton.emit(!isPlaying)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_READY) {
                    viewModelScope.launch {
                        if (videoFinalTime.value.isEmpty()) {
                            val duration = exoPlayer.duration.timeTextFromMs()
                            videoFinalTime.emit(
                                "$duration • ${
                                    estimateFileSize(
                                        0L,
                                        exoPlayer.duration,
                                        isMute.value.not()
                                    )
                                }"
                            )
                        }
                    }
                }
            }
        })
    }

    fun toggleMuteButton() {
        viewModelScope.launch {
            isMute.emit(isMute.value.not())
            val startMs = (exoPlayer.duration * seekBars.value.first).toLong()
            val endMs = (exoPlayer.duration * seekBars.value.third).toLong()

            val estimatedSize = estimateFileSize(
                startMs,
                endMs,
                isMute.value.not()
            )

            videoFinalTime.emit("${endMs - startMs} • $estimatedSize")
        }
    }

    // Keep a reference to the last message so we can cancel it if the user moves the handle again
    private var currentStopMessage: PlayerMessage? = null

    fun onSeekBarChange(start: Float, end: Float) {
        val totalVideoDuration = exoPlayer.duration

        if (totalVideoDuration <= 0) return

        val startMs = (totalVideoDuration * start).toLong()
        val endMs = (totalVideoDuration * end).toLong()

        // 1. Cancel the old trap (from previous slider movement)
        currentStopMessage?.cancel()

        // 2. Define a helper to schedule the loop "trap"
        fun scheduleLoop() {
            currentStopMessage = exoPlayer.createMessage { _, _ ->
                // A. Loop back to start
                exoPlayer.seekTo(startMs)

                // B. IMPORTANT: Re-arm the trap for the next run!
                scheduleLoop()
            }
                .setLooper(Looper.getMainLooper())
                .setPosition(endMs)
                .setDeleteAfterDelivery(true)
                .send()
        }

        // 3. Set the first trap
        scheduleLoop()

        // 4. Seek to start and ensure playing
        exoPlayer.seekTo(startMs)

        // 5. Update UI
        viewModelScope.launch {
            val trimDurationStr = (endMs - startMs).timeTextFromMs()
            val estimatedSize = estimateFileSize(startMs, endMs, isMute.value.not())

            videoFinalTime.emit("$trimDurationStr • $estimatedSize")
            videoTrimTime.emit("${startMs.timeTextFromMs()} -- ${endMs.timeTextFromMs()}")
        }
    }

    fun processSelectedUri(uri: Uri, partnerPhone: String) {
        extract10Frames(uri, appContext) { i, bmp ->
            viewModelScope.launch {
                flowFramesThumb.emit(i to bmp)
            }
        }
        if (this.uri != null) return
        this.uri = uri
        viewModelScope.launch {
            val p = repo.getPersonPref(phoneNo = partnerPhone)
            partnerName.emit(p)
        }
        viewModelScope.launch {
            val data = getAllPossibleDetails(appContext, uri)
            metaData.emit(data)
        }
        viewModelScope.launch {
            exoPlayer.setMediaItem(MediaItem.fromUri(uri))
            exoPlayer.prepare()
        }
    }

    fun finalProcessOfVideo() {
        if (uri == null) return
        if (encodeProg.value >= 0f) return
        viewModelScope.launch {
            encodeProg.emit(0f)
        }
        val listener = object : TranscoderListener {
            override fun onTranscodeProgress(prog: Double) {
                viewModelScope.launch {
                    encodeProg.emit(prog.toFloat())
                }
            }

            override fun onTranscodeCompleted(p0: Int) {
                viewModelScope.launch {
                    finishActivity.emit(true)
                }
            }

            override fun onTranscodeCanceled() {
            }

            override fun onTranscodeFailed(p0: Throwable) {
            }
        }
        val videoName = getVideoName(System.currentTimeMillis())
        repoStorage.saveBytesToFolder(MsgMediaType.VIDEO, videoName, ByteArray(0))
        val fileVideo = repoStorage.getFileFromFolder(MsgMediaType.VIDEO, videoName)!!
        if (seekBars.value.first == 0f && seekBars.value.third == 1f)
            compressAndTrimDeepMedia(
                appContext,
                uri!!,
                fileVideo.absolutePath,
                listener,
                !isMute.value,
            )
        else compressAndTrimDeepMedia(
            appContext,
            uri!!,
            fileVideo.absolutePath,
            listener,
            !isMute.value,
            seekBars.value.first
                .times(exoPlayer.duration).toLong(),
            seekBars.value.third
                .times(exoPlayer.duration).toLong()
        )
    }

    private fun createSeekBarSyncJob(): Job {
        return viewModelScope.launch {
            while (isActive) {
                val currentPosition = exoPlayer.currentPosition
                val totalDuration = exoPlayer.duration

                if (totalDuration > 0) {
                    val progress = currentPosition.toFloat() / totalDuration
                    // Assuming seekBars Triple is (start, current, end)
                    seekBars.emit(Triple(seekBars.value.first, progress, seekBars.value.third))
                }
                delay(100)
            }
        }
    }

    private fun getVideoName(millis: Long) =
        "VID_${ConversionUtils.millisToFullDateText(millis)}.mp4"

    override fun onCleared() {
        super.onCleared()
        exoPlayer.pause()
        exoPlayer.release()
        seekBarSyncJob.cancel()
        Log.d("TAG--", "onCleared: ")
    }
}