package com.tomer.chitchat.viewmodals

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.tomer.chitchat.assets.RepoAssets
import com.tomer.chitchat.modals.msgs.ModelMsgSocket
import com.tomer.chitchat.modals.states.FlowType
import com.tomer.chitchat.modals.states.MsgsFlowState
import com.tomer.chitchat.modals.states.UiMsgModal
import com.tomer.chitchat.modals.states.UiMsgModalBuilder
import com.tomer.chitchat.repo.RepoMedia
import com.tomer.chitchat.repo.RepoMessages
import com.tomer.chitchat.repo.RepoStorage
import com.tomer.chitchat.retro.Api
import com.tomer.chitchat.room.ModalMediaUpload
import com.tomer.chitchat.room.ModelRoomMessageBuilder
import com.tomer.chitchat.room.MsgMediaType
import com.tomer.chitchat.utils.ConversionUtils
import com.tomer.chitchat.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine


@HiltViewModel
class AssetsViewModel @Inject constructor(
    private val repoAssets: RepoAssets,
    private val repoStorage: RepoStorage,
    private val retro: Api,
    private val repoMedia: RepoMedia,
    private val repoMsgs: RepoMessages,
) : ViewModel() {

    val flowEvents = MutableSharedFlow<MsgsFlowState>()
    val defBmp = getBmpFromDrawable()

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(20000)
                val l = repoAssets.getRandomJson()
                flowEvents.emit(MsgsFlowState.ChangeGif(null, l, phone = Utils.currentPartner?.partnerId.toString()))
            }
        }
    }

    private fun getBmpFromDrawable(): Bitmap {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawText("Chit Chat", 0f, 0f, Paint().apply {
            color = Color.CYAN
            textSize = 68f
        })
        return bitmap
    }

    //region BIG JSON

    fun showJsonViaFlow(nameJson: String, fromUser: String = Utils.currentPartner?.partnerId.toString()) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val r = repoAssets.getLottieJson(nameJson) ?: return@withContext
                val builder = UiMsgModalBuilder()
                builder.setMsg(r)
                builder.mediaFileName(nameJson)
                flowEvents.emit(MsgsFlowState.ChangeGif(null, builder.build(), FlowType.SHOW_BIG_JSON, fromUser))
            }
        }
    }

    fun showGoogleJsonViaFlow(nameJson: String, fromUser: String = Utils.currentPartner?.partnerId.toString()) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val r = repoAssets.getGoogleLottieJson(nameJson) ?: return@withContext
                val builder = UiMsgModalBuilder()
                builder.setMsg(r)
                builder.mediaFileName(nameJson)
                flowEvents.emit(MsgsFlowState.ChangeGif(null, builder.build(), FlowType.SHOW_BIG_JSON, fromUser))
            }
        }
    }

    fun showGifViaFlow(nameGif: String, fromUser: String = Utils.currentPartner?.partnerId.toString()) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val r = repoAssets.getGifFile(nameGif) ?: return@withContext
                flowEvents.emit(MsgsFlowState.ChangeGif(r, typeF = FlowType.SHOW_BIG_GIF, phone = fromUser))
            }
        }
    }

    fun showTeleGifViaFlow(nameGif: String, fromUser: String = Utils.currentPartner?.partnerId.toString()) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val r = repoAssets.getGifTelemoji(ConversionUtils.encode(nameGif)) ?: return@withContext
                flowEvents.emit(MsgsFlowState.ChangeGif(r, typeF = FlowType.SHOW_BIG_GIF, phone = fromUser))
            }
        }
    }

    fun getGifNow() {
        viewModelScope.launch {
            val l = repoAssets.getRandomJson()
            flowEvents.emit(MsgsFlowState.ChangeGif(null, l, phone = Utils.currentPartner?.partnerId.toString()))
        }
    }

    //endregion BIG JSON

    //region COMMUNICATION

    fun uploadRetry(msg: UiMsgModal, partner: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val oldMsg = repoMsgs.getMsg(msg.id) ?: return@withContext
                val uri = repoMedia.getUriFromFileName(msg.mediaFileName!!) ?: return@withContext

                var link = retro.checkForUpload(Utils.myPhone + uri).body()
                if (link.isNullOrEmpty() || link == "false") {
                    val bytes = repoStorage.getBytesFromFolder(msg.msgType, msg.mediaFileName) ?: return@withContext
                    link = uploadToServer(bytes, msg.msgType.name, Utils.myPhone + uri, msg.mediaFileName)
                }

                if (link == null) {
                    flowEvents.emit(MsgsFlowState.IOFlowState(msg.id, FlowType.UPLOAD_FAILS, partner))
                    return@withContext
                }

                oldMsg.msgText = "$link,-,${oldMsg.msgText.split(",-,")[1]}"
                val builder = UiMsgModalBuilder()
                    .id(oldMsg.id)
                    .replyId(oldMsg.replyId)
                    .setMsg(oldMsg.msgText)
                    .setRep(oldMsg.repText)
                    .mediaFileName(oldMsg.mediaFileName)
                    .replyMediaFileName(oldMsg.replyMediaFileName)
                    .isReply(oldMsg.isRep)
                    .msgType(oldMsg.msgType)
                    .isProg(false)
                    .replyType(oldMsg.replyType)
                flowEvents.emit(MsgsFlowState.IOFlowState(oldMsg.timeMillis, FlowType.UPLOAD_SUCCESS, partner, builder.build()))
                repoMsgs.addMsg(oldMsg)
            }
        }
    }

    private fun Uri.getName(context: Context): String {
        try {
            val returnCursor = context.contentResolver.query(this, null, null, null, null)
            val nameIndex = returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            returnCursor.moveToFirst()
            val fileName = returnCursor.getString(nameIndex)
            returnCursor.close()
            return fileName
        } catch (e: Exception) {
            return ""
        }
    }


    fun uploadFile(
        toUser: String,
        mediaType: MsgMediaType,
        file: Uri, con: Context,
        msg: ModelMsgSocket,
        tempId: Long,
        repBytes: ByteArray?,
        created: (flow: MsgsFlowState) -> Unit
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {

                suspend fun handleUploaing(fileBytes: ByteArray, thumbBytes: ByteArray, fileNa: String? = null) {
                    val builder = ModelRoomMessageBuilder()
                    builder.id(tempId)
                    builder.replyId(msg.replyId)
                    builder.setPartner(toUser)
                    builder.repText(msg.replyData)
                    builder.msgType(msg.msgType)
                    builder.replyType(msg.replyMsgType)
                    builder.isSent(true)
                    builder.setBytes(fileBytes)
                    builder.isRep(msg.isReply)
                    builder.setTimeMillis(msg.timeMillis)
                    builder.setTimeText(ConversionUtils.millisToTimeText(msg.timeMillis))
                    builder.replyMediaFileName(msg.replyMediaFileName)
                    builder.setRepBytes(repBytes)

                    val roomMsg = builder.build()
                    builder.msgText("Uploading,-,${ConversionUtils.byteArrToBase64(thumbBytes)}")

                    val fileName = fileNa ?: when (mediaType) {
                        MsgMediaType.TEXT, MsgMediaType.EMOJI -> ""
                        MsgMediaType.IMAGE -> getImageName(msg.timeMillis)
                        MsgMediaType.GIF -> getGifName(msg.timeMillis)
                        MsgMediaType.FILE -> {
                            getDocumentName(msg.timeMillis)
                        }

                        MsgMediaType.VIDEO -> {
                            getVideoName(msg.timeMillis)
                        }
                    }
                    builder.mediaFileName(fileName)
                    //sending Callback to activity to display img and loading
                    created(MsgsFlowState.ChatMessageFlowState(builder.buildUI(), roomMsg.partnerId, true))

                    repoStorage.saveBytesToFolder(msg.msgType, fileName, fileBytes)
                    repoMsgs.addMsg(builder.build())

                    Log.d("TAG--", "uploadFile: ${file.encodedPath}")
                    repoMedia.saveMedia(ModalMediaUpload(file.encodedPath.toString(), fileName))

                    var link = retro.checkForUpload(Utils.myPhone + file.encodedPath).body()
                    if (link.isNullOrEmpty() || link == "false")
                        link = uploadToServer(fileBytes, mediaType.name, Utils.myPhone + file.encodedPath, fileName)

                    if (link == null) {
                        flowEvents.emit(MsgsFlowState.IOFlowState(tempId, FlowType.UPLOAD_FAILS, toUser))
                        return
                    }

                    builder.msgText("$link,-,${ConversionUtils.byteArrToBase64(thumbBytes)}")
                    flowEvents.emit(MsgsFlowState.IOFlowState(msg.timeMillis, FlowType.UPLOAD_SUCCESS, toUser, builder.buildUI()))
                    repoMsgs.addMsg(builder.build())
                }

                when (mediaType) {
                    MsgMediaType.TEXT, MsgMediaType.EMOJI -> {}
                    MsgMediaType.IMAGE -> {

                        val baos = ByteArrayOutputStream()
                        val baosT = ByteArrayOutputStream()

                        val previousFile = repoMedia.getFileNameFromUri(uri = file.encodedPath.toString())

                        val isFilePresent = repoStorage.isPresent(previousFile.toString(), mediaType)
                        if (previousFile == null || !isFilePresent) {
                            val bmp = getBmpUsingGlide(file, con) ?: return@withContext
                            val bmpThumb = getThumbBmpUsingGlide(file, con, bmp) ?: return@withContext

                            bmp.compress(Bitmap.CompressFormat.WEBP, 80, baos)
                            bmpThumb.compress(Bitmap.CompressFormat.WEBP, 20, baosT)
                            handleUploaing(baos.toByteArray(), baosT.toByteArray())
                        } else {
                            baos.write(repoStorage.getBytesFromFolder(mediaType, previousFile))
                            try {
                                val oldMsg = repoMsgs.getMsgFromFileName(previousFile)
                                baosT.write(ConversionUtils.base64ToByteArr(oldMsg!!.msgText.split(",-,")[1]))
                                handleUploaing(baos.toByteArray(), baosT.toByteArray(), oldMsg.mediaFileName)
                            } catch (e: Exception) {
                                val bmpThumb = getThumbBmpUsingGlide(file, con) ?: defBmp
                                bmpThumb.compress(Bitmap.CompressFormat.WEBP, 20, baosT)
                                handleUploaing(baos.toByteArray(), baosT.toByteArray())
                            }
                        }

                    }

                    MsgMediaType.GIF -> {

                        val baos = ByteArrayOutputStream()
                        val baosT = ByteArrayOutputStream()

                        val previousFile = repoMedia.getFileNameFromUri(uri = file.encodedPath.toString())
                        if (repoStorage.isPresent(previousFile.toString(), mediaType)) {
                            try {
                                baos.write(repoStorage.getBytesFromFolder(mediaType, previousFile!!))
                                val oldMsg = repoMsgs.getMsgFromFileName(previousFile)
                                baosT.write(ConversionUtils.base64ToByteArr(oldMsg!!.msgText.split(",-,")[1]))
                                handleUploaing(baos.toByteArray(), baosT.toByteArray(), oldMsg.mediaFileName)
                            } catch (e: Exception) {
                                val bmpThumb = getGifThumbBmpUsingGlide(file, con) ?: defBmp
                                bmpThumb.compress(Bitmap.CompressFormat.WEBP, 20, baosT)
                                handleUploaing(baos.toByteArray(), baosT.toByteArray())
                            }
                        } else {
                            try {
                                con.contentResolver.openInputStream(file).use {
                                    it?.copyTo(baos)
                                }
                            } catch (_: Exception) {

                            }
                            val bmpThumb = getGifThumbBmpUsingGlide(file, con) ?: defBmp
                            bmpThumb.compress(Bitmap.CompressFormat.WEBP, 20, baosT)
                            handleUploaing(baos.toByteArray(), baosT.toByteArray())
                        }

                    }

                    MsgMediaType.FILE -> {}
                    MsgMediaType.VIDEO -> {}
                }
            }
        }
    }

    fun downLoadFile(url: String, fileName: String, type: MsgMediaType, id: Long, fromUser: String, onLoadBytes: (ByteArray) -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {

                if (repoStorage.isPresent(fileName, type)) {
                    val byes = repoStorage.getBytesFromFolder(type, fileName)
                    if (byes == null)
                        flowEvents.emit(MsgsFlowState.IOFlowState(id, FlowType.DOWNLOAD_FAILS, fromUser))
                    else onLoadBytes(byes)
                    return@withContext
                }

                val byes = repoAssets.downLoadBytes(url)
                if (byes == null)
                    flowEvents.emit(MsgsFlowState.IOFlowState(id, FlowType.DOWNLOAD_FAILS, fromUser))
                else {
                    repoStorage.saveBytesToFolder(type, fileName, byes)
                    val build = UiMsgModalBuilder()
                    build.bytes(byes)
                    flowEvents.emit(MsgsFlowState.IOFlowState(id, FlowType.DOWNLOAD_SUCCESS, fromUser, build.build()))
                }
            }
        }
    }

    fun getBytesOfFile(type: MsgMediaType, fileName: String) =
        repoStorage.getBytesFromFolder(type, fileName)


    //endregion COMMUNICATION

    //region UTILS

    private fun getImageName(millis: Long) = "IMG_${ConversionUtils.millisToFullDateText(millis)}.webp"
    private fun getVideoName(millis: Long) = "VID_${ConversionUtils.millisToFullDateText(millis)}"
    private fun getDocumentName(millis: Long) = "DOC_${ConversionUtils.millisToFullDateText(millis)}"
    private fun getGifName(millis: Long) = "GIF_${ConversionUtils.millisToFullDateText(millis)}.gif"
    //endregion UTILS

    private suspend fun uploadToServer(baos: ByteArray, type: String, uri: String, fileName: String): String? {
        Log.d("TAG--", "uploadToServer: Actuallly uploading $fileName  -- $uri  -- $type")
        val reqBody = baos
            .toRequestBody(
                "multipart/form-data".toMediaTypeOrNull(),
                0, baos.size
            )
        return try {
            val res = retro.uploadMedia(
                MultipartBody.Part.createFormData(
                    "file", fileName, reqBody
                ), type, uri
            )
            if (res.isSuccessful) res.body() else null
        } catch (e: Exception) {
            null
        }
    }


    //region THUMB UTILS


    private suspend fun getBmpUsingGlide(uri: Uri, con: Context): Bitmap? {
        return suspendCoroutine { continuation ->
            Glide.with(con)
                .asBitmap()
                .load(uri)
                .into(
                    object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            continuation.resumeWith(Result.success(resource))
                        }

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            super.onLoadFailed(errorDrawable)
                            continuation.resumeWith(Result.success(null))
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                        }
                    }
                )
        }
    }

    private suspend fun getThumbBmpUsingGlide(uri: Uri, con: Context, bmp: Bitmap? = null): Bitmap? {
        return suspendCoroutine { continuation ->
            Glide.with(con)
                .asBitmap()
                .load(bmp ?: uri)
                .override(12)
                .into(
                    object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            continuation.resumeWith(Result.success(resource))
                        }

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            super.onLoadFailed(errorDrawable)
                            continuation.resumeWith(Result.success(null))
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                        }
                    }
                )
        }
    }

    private suspend fun getGifThumbBmpUsingGlide(uri: Uri, con: Context): Bitmap? {
        return suspendCoroutine { continuation ->
            Glide.with(con)
                .asBitmap()
                .load(uri)
                .apply(
                    RequestOptions().frame(0)
                )
                .override(12)
                .into(
                    object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            continuation.resumeWith(Result.success(resource))
                        }
                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            super.onLoadFailed(errorDrawable)
                            continuation.resumeWith(Result.success(null))
                        }
                        override fun onLoadCleared(placeholder: Drawable?) {
                        }
                    }
                )
        }
    }

    private fun createVideoThumbnail(context: Context, uri: Uri): Bitmap? {
        val filePath = getPathFromUri(context, uri)
        return filePath?.let {
            ThumbnailUtils.createVideoThumbnail(it, MediaStore.Images.Thumbnails.MINI_KIND)
        }
    }

    private fun getPathFromUri(context: Context, uri: Uri): String? {
        var filePath: String? = null
        val projection = arrayOf(MediaStore.Video.Media.DATA)
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                filePath = it.getString(columnIndex)
            }
        }
        return filePath
    }

    //endregion THUMB UTILS

}
