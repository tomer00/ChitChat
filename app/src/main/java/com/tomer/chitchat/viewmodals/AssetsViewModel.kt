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
import com.tomer.chitchat.utils.NetworkUtils
import com.tomer.chitchat.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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
    val defBmpBytes = getBmpFromDrawable()

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(20000)
                val l = repoAssets.getRandomJson()
                flowEvents.emit(MsgsFlowState.ChangeGif(null, l, phone = Utils.currentPartner?.partnerId.toString()))
            }
        }
    }

    private fun getBmpFromDrawable(): ByteArray {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawText("Chit Chat", 0f, 0f, Paint().apply {
            color = Color.CYAN
            textSize = 68f
        })
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.WEBP, 20, baos)
        return baos.toByteArray()
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

    fun setTypingJson() {
        viewModelScope.launch {
            val builder = UiMsgModalBuilder()
            builder.setMsg(
                "{\"v\":\"5.5.8\",\"fr\":60,\"ip\":0,\"op\":110,\"w\":144,\"h\":105,\"nm\":\"typing indicator\",\"ddd\":0,\"assets\":[],\"layers\":[{\"ddd\":0,\"ind\":1,\"ty\":3,\"nm\":\"Ã¢Â\u0096Â½ Dots\",\"sr\":1,\"ks\":{\"o\":{\"a\":0,\"k\":100,\"ix\":11},\"r\":{\"a\":0,\"k\":0,\"ix\":10},\"p\":{\"a\":0,\"k\":[72,51,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[36,9,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100,100],\"ix\":6}},\"ao\":0,\"ip\":0,\"op\":110,\"st\":0,\"bm\":0},{\"ddd\":0,\"ind\":2,\"ty\":4,\"nm\":\"dot 1\",\"parent\":1,\"sr\":1,\"ks\":{\"o\":{\"a\":0,\"k\":100,\"ix\":11},\"r\":{\"a\":0,\"k\":0,\"ix\":10},\"p\":{\"a\":1,\"k\":[{\"i\":{\"x\":0.667,\"y\":1},\"o\":{\"x\":0.333,\"y\":0},\"t\":0,\"s\":[9,9,0],\"to\":[0,0,0],\"ti\":[0,0,0]},{\"i\":{\"x\":0.667,\"y\":1},\"o\":{\"x\":0.333,\"y\":0},\"t\":3.053,\"s\":[9,10,0],\"to\":[0,0,0],\"ti\":[0,0,0]},{\"i\":{\"x\":0.667,\"y\":1},\"o\":{\"x\":0.333,\"y\":0},\"t\":15.273,\"s\":[9,-9,0],\"to\":[0,0,0],\"ti\":[0,0,0]},{\"i\":{\"x\":0.667,\"y\":1},\"o\":{\"x\":0.333,\"y\":0},\"t\":25.174,\"s\":[9,12,0],\"to\":[0,0,0],\"ti\":[0,0,0]},{\"t\":32.744140625,\"s\":[9,9,0]}],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100,100],\"ix\":6}},\"ao\":0,\"shapes\":[{\"ty\":\"gr\",\"it\":[{\"d\":1,\"ty\":\"el\",\"s\":{\"a\":0,\"k\":[6,6],\"ix\":2},\"p\":{\"a\":0,\"k\":[0,0],\"ix\":3},\"nm\":\"Ellipse Path 1\",\"mn\":\"ADBE Vector Shape - Ellipse\",\"hd\":false},{\"ty\":\"fl\",\"c\":{\"a\":1,\"k\":[{\"i\":{\"x\":[0.472],\"y\":[1]},\"o\":{\"x\":[1],\"y\":[0]},\"t\":0,\"s\":[0.690196078431,0.725490196078,0.752941176471,1]},{\"i\":{\"x\":[0.49],\"y\":[1]},\"o\":{\"x\":[1],\"y\":[0]},\"t\":15.273,\"s\":[0.349019616842,0.392156869173,0.427450984716,1]},{\"t\":32.072265625,\"s\":[0.690196078431,0.725490196078,0.752941176471,1]}],\"ix\":4},\"o\":{\"a\":0,\"k\":100,\"ix\":5},\"r\":1,\"bm\":0,\"nm\":\"Fill 1\",\"mn\":\"ADBE Vector Graphic - Fill\",\"hd\":false},{\"ty\":\"tr\",\"p\":{\"a\":0,\"k\":[0,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[300,300],\"ix\":3},\"r\":{\"a\":0,\"k\":0,\"ix\":6},\"o\":{\"a\":0,\"k\":100,\"ix\":7},\"sk\":{\"a\":0,\"k\":0,\"ix\":4},\"sa\":{\"a\":0,\"k\":0,\"ix\":5},\"nm\":\"Transform\"}],\"nm\":\"dot 1\",\"np\":2,\"cix\":2,\"bm\":0,\"ix\":1,\"mn\":\"ADBE Vector Group\",\"hd\":false}],\"ip\":0,\"op\":110,\"st\":0,\"bm\":0},{\"ddd\":0,\"ind\":3,\"ty\":4,\"nm\":\"dot 2\",\"parent\":1,\"sr\":1,\"ks\":{\"o\":{\"a\":0,\"k\":100,\"ix\":11},\"r\":{\"a\":0,\"k\":0,\"ix\":10},\"p\":{\"a\":1,\"k\":[{\"i\":{\"x\":0.667,\"y\":1},\"o\":{\"x\":0.333,\"y\":0},\"t\":10.691,\"s\":[36,9,0],\"to\":[0,0,0],\"ti\":[0,0,0]},{\"i\":{\"x\":0.667,\"y\":1},\"o\":{\"x\":0.333,\"y\":0},\"t\":15.273,\"s\":[36,10,0],\"to\":[0,0,0],\"ti\":[0,0,0]},{\"i\":{\"x\":0.667,\"y\":1},\"o\":{\"x\":0.333,\"y\":0},\"t\":25.963,\"s\":[36,-9,0],\"to\":[0,0,0],\"ti\":[0,0,0]},{\"i\":{\"x\":0.667,\"y\":1},\"o\":{\"x\":0.333,\"y\":0},\"t\":36.984,\"s\":[36,14.326,0],\"to\":[0,0,0],\"ti\":[0,0,0]},{\"t\":43.97265625,\"s\":[36,9,0]}],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100,100],\"ix\":6}},\"ao\":0,\"shapes\":[{\"ty\":\"gr\",\"it\":[{\"d\":1,\"ty\":\"el\",\"s\":{\"a\":0,\"k\":[6,6],\"ix\":2},\"p\":{\"a\":0,\"k\":[0,0],\"ix\":3},\"nm\":\"Ellipse Path 1\",\"mn\":\"ADBE Vector Shape - Ellipse\",\"hd\":false},{\"ty\":\"fl\",\"c\":{\"a\":1,\"k\":[{\"i\":{\"x\":[0.472],\"y\":[1]},\"o\":{\"x\":[1],\"y\":[0]},\"t\":10.691,\"s\":[0.690196078431,0.725490196078,0.752941176471,1]},{\"i\":{\"x\":[0.49],\"y\":[1]},\"o\":{\"x\":[1],\"y\":[0]},\"t\":25.963,\"s\":[0.349019616842,0.392156869173,0.427450984716,1]},{\"t\":42.763671875,\"s\":[0.690196078431,0.725490196078,0.752941176471,1]}],\"ix\":4},\"o\":{\"a\":0,\"k\":100,\"ix\":5},\"r\":1,\"bm\":0,\"nm\":\"Fill 1\",\"mn\":\"ADBE Vector Graphic - Fill\",\"hd\":false},{\"ty\":\"tr\",\"p\":{\"a\":0,\"k\":[0,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[300,300],\"ix\":3},\"r\":{\"a\":0,\"k\":0,\"ix\":6},\"o\":{\"a\":0,\"k\":100,\"ix\":7},\"sk\":{\"a\":0,\"k\":0,\"ix\":4},\"sa\":{\"a\":0,\"k\":0,\"ix\":5},\"nm\":\"Transform\"}],\"nm\":\"dot 2\",\"np\":2,\"cix\":2,\"bm\":0,\"ix\":1,\"mn\":\"ADBE Vector Group\",\"hd\":false}],\"ip\":0,\"op\":110,\"st\":0,\"bm\":0},{\"ddd\":0,\"ind\":4,\"ty\":4,\"nm\":\"dot 3\",\"parent\":1,\"sr\":1,\"ks\":{\"o\":{\"a\":0,\"k\":100,\"ix\":11},\"r\":{\"a\":0,\"k\":0,\"ix\":10},\"p\":{\"a\":1,\"k\":[{\"i\":{\"x\":0.667,\"y\":1},\"o\":{\"x\":0.333,\"y\":0},\"t\":21.383,\"s\":[63,9,0],\"to\":[0,0,0],\"ti\":[0,0,0]},{\"i\":{\"x\":0.667,\"y\":1},\"o\":{\"x\":0.333,\"y\":0},\"t\":25.963,\"s\":[63,10,0],\"to\":[0,0,0],\"ti\":[0,0,0]},{\"i\":{\"x\":0.667,\"y\":1},\"o\":{\"x\":0.333,\"y\":0},\"t\":38.184,\"s\":[63,-9,0],\"to\":[0,0,0],\"ti\":[0,0,0]},{\"i\":{\"x\":0.667,\"y\":1},\"o\":{\"x\":0.333,\"y\":0},\"t\":49.594,\"s\":[63,13.139,0],\"to\":[0,0,0],\"ti\":[0,0,0]},{\"t\":56,\"s\":[63,9,0]}],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[100,100,100],\"ix\":6}},\"ao\":0,\"shapes\":[{\"ty\":\"gr\",\"it\":[{\"d\":1,\"ty\":\"el\",\"s\":{\"a\":0,\"k\":[6,6],\"ix\":2},\"p\":{\"a\":0,\"k\":[0,0],\"ix\":3},\"nm\":\"Ellipse Path 1\",\"mn\":\"ADBE Vector Shape - Ellipse\",\"hd\":false},{\"ty\":\"fl\",\"c\":{\"a\":1,\"k\":[{\"i\":{\"x\":[0.472],\"y\":[1]},\"o\":{\"x\":[1],\"y\":[0]},\"t\":21.383,\"s\":[0.690196078431,0.725490196078,0.752941176471,1]},{\"i\":{\"x\":[0.49],\"y\":[1]},\"o\":{\"x\":[1],\"y\":[0]},\"t\":36.656,\"s\":[0.349019616842,0.392156869173,0.427450984716,1]},{\"t\":53.45703125,\"s\":[0.690196078431,0.725490196078,0.752941176471,1]}],\"ix\":4},\"o\":{\"a\":0,\"k\":100,\"ix\":5},\"r\":1,\"bm\":0,\"nm\":\"Fill 1\",\"mn\":\"ADBE Vector Graphic - Fill\",\"hd\":false},{\"ty\":\"tr\",\"p\":{\"a\":0,\"k\":[0,0],\"ix\":2},\"a\":{\"a\":0,\"k\":[0,0],\"ix\":1},\"s\":{\"a\":0,\"k\":[300,300],\"ix\":3},\"r\":{\"a\":0,\"k\":0,\"ix\":6},\"o\":{\"a\":0,\"k\":100,\"ix\":7},\"sk\":{\"a\":0,\"k\":0,\"ix\":4},\"sa\":{\"a\":0,\"k\":0,\"ix\":5},\"nm\":\"Transform\"}],\"nm\":\"dot 3\",\"np\":2,\"cix\":2,\"bm\":0,\"ix\":1,\"mn\":\"ADBE Vector Group\",\"hd\":false}],\"ip\":0,\"op\":110,\"st\":0,\"bm\":0}],\"markers\":[]}"
            )
            builder.mediaFileName("typing")
            flowEvents.emit(MsgsFlowState.ChangeGif(null, builder.build(), FlowType.SET_TYPING_GIF, Utils.currentPartner?.partnerId.toString()))
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

                suspend fun handleUploaing(fileBytes: ByteArray, thumbB: ByteArray?, fileNa: String? = null) {
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
                    builder.mediaSize(Utils.humanReadableSize(fileBytes.size))

                    val roomMsg = builder.build()
                    builder.msgText("Uploading")

                    val fileName = fileNa ?: when (mediaType) {
                        MsgMediaType.TEXT, MsgMediaType.EMOJI -> ""
                        MsgMediaType.IMAGE -> getImageName(msg.timeMillis)
                        MsgMediaType.GIF -> getGifName(msg.timeMillis)
                        MsgMediaType.FILE -> file.getName(con)
                        MsgMediaType.VIDEO -> getVideoName(msg.timeMillis)
                    }
                    builder.mediaFileName(fileName)
                    //sending Callback to activity to display img and loading
                    created(MsgsFlowState.ChatMessageFlowState(builder.buildUI(), roomMsg.partnerId, true))

                    repoStorage.saveBytesToFolder(msg.msgType, fileName, fileBytes)
                    repoMsgs.addMsg(builder.build())

                    repoMedia.saveMedia(ModalMediaUpload(file.encodedPath.toString(), fileName))

                    val thumbBytes = ByteArrayOutputStream()

                    val thumb = async(Dispatchers.IO) {
                        if (thumbB != null && thumbB.size > 10) {
                            thumbBytes.write(thumbB)
                            return@async
                        }
                        when (msg.msgType) {
                            MsgMediaType.TEXT, MsgMediaType.EMOJI, MsgMediaType.FILE -> {}
                            MsgMediaType.IMAGE -> getThumbBmpUsingGlide(fileBytes, con)?.compress(Bitmap.CompressFormat.WEBP, 12, thumbBytes)
                            MsgMediaType.GIF -> getGifThumbBmpUsingGlide(file, con)?.compress(Bitmap.CompressFormat.WEBP, 12, thumbBytes)
                            MsgMediaType.VIDEO -> {}
                        }
                    }

                    var link = retro.checkForUpload(Utils.myPhone + file.encodedPath).body().also { Log.d("TAG--", "handleUploaing: $it") }
                    if (link.isNullOrEmpty() || link == "false")
                        link = uploadToServer(fileBytes, mediaType.name, Utils.myPhone + file.encodedPath, fileName)

                    thumb.await()
                    if (thumbBytes.size() < 10) thumbBytes.write(defBmpBytes)
                    if (link == null) {
                        flowEvents.emit(MsgsFlowState.IOFlowState(tempId, FlowType.UPLOAD_FAILS, toUser))
                        if (msg.msgType != MsgMediaType.FILE)
                            builder.msgText("Uploading,-,${ConversionUtils.byteArrToBase64(thumbBytes.toByteArray())}")
                        repoMsgs.addMsg(builder.build())
                        return
                    }
                    builder.msgText(link)
                    if (msg.msgType != MsgMediaType.FILE)
                        builder.msgText("$link,-,${ConversionUtils.byteArrToBase64(thumbBytes.toByteArray())}")
                    flowEvents.emit(MsgsFlowState.IOFlowState(msg.timeMillis, FlowType.UPLOAD_SUCCESS, toUser, builder.buildUI()))
                    repoMsgs.addMsg(builder.build())
                }

                when (mediaType) {
                    MsgMediaType.TEXT, MsgMediaType.EMOJI -> {}
                    MsgMediaType.IMAGE -> {

                        val baos = ByteArrayOutputStream()

                        val previousFile = repoMedia.getFileNameFromUri(uri = file.encodedPath.toString())

                        val isFilePresent = repoStorage.isPresent(previousFile.toString(), mediaType)
                        if (previousFile == null || !isFilePresent) {
                            val bmp = getBmpUsingGlide(file, con) ?: return@withContext
                            bmp.compress(Bitmap.CompressFormat.WEBP, 80, baos)
                            handleUploaing(baos.toByteArray(), null)
                        } else {
                            val baosT = ByteArrayOutputStream()
                            baos.write(repoStorage.getBytesFromFolder(mediaType, previousFile))
                            try {
                                val oldMsg = repoMsgs.getMsgFromFileName(previousFile)
                                baosT.write(ConversionUtils.base64ToByteArr(oldMsg!!.msgText.split(",-,")[1]))
                                handleUploaing(baos.toByteArray(), baosT.toByteArray(), oldMsg.mediaFileName)
                            } catch (e: Exception) {
                                handleUploaing(baos.toByteArray(), null)
                            }
                        }

                    }

                    MsgMediaType.GIF -> {

                        val baos = ByteArrayOutputStream()

                        val previousFile = repoMedia.getFileNameFromUri(uri = file.encodedPath.toString())
                        if (repoStorage.isPresent(previousFile.toString(), mediaType)) {
                            try {
                                val baosT = ByteArrayOutputStream()
                                baos.write(repoStorage.getBytesFromFolder(mediaType, previousFile.toString()))
                                val oldMsg = repoMsgs.getMsgFromFileName(previousFile.toString())
                                baosT.write(ConversionUtils.base64ToByteArr(oldMsg!!.msgText.split(",-,")[1]))
                                handleUploaing(baos.toByteArray(), baosT.toByteArray(), oldMsg.mediaFileName)
                            } catch (e: Exception) {
                                handleUploaing(baos.toByteArray(), null)
                            }
                        } else {
                            try {
                                con.contentResolver.openInputStream(file).use {
                                    it?.copyTo(baos)
                                }
                            } catch (_: Exception) {

                            }
                            handleUploaing(baos.toByteArray(), null)
                        }
                    }

                    MsgMediaType.FILE -> {
                        val baos = ByteArrayOutputStream()
                        try {
                            con.contentResolver.openInputStream(file).use {
                                it?.copyTo(baos)
                            }
                        } catch (_: Exception) {

                        }
                        handleUploaing(baos.toByteArray(), null)
                    }

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

                val byes = NetworkUtils.downloadBytes(url)
                if (byes == null)
                    flowEvents.emit(MsgsFlowState.IOFlowState(id, FlowType.DOWNLOAD_FAILS, fromUser))
                else {
                    repoStorage.saveBytesToFolder(type, fileName, byes)
                    val build = UiMsgModalBuilder()
                    build.msgType(type)
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
    private fun getGifName(millis: Long) = "GIF_${ConversionUtils.millisToFullDateText(millis)}.gif"
    //endregion UTILS

    //region mediaView

    fun openFile(fileName: String) {
        val file = repoStorage.getFileFromFolder(MsgMediaType.FILE, fileName) ?: return
        viewModelScope.launch { flowEvents.emit(MsgsFlowState.OpenFileFlowState(file, FlowType.OPEN_FILE, 0L)) }
    }

    fun openImage(fileName: String, isGif: Boolean, msgId: Long) {
        val file = repoStorage.getFileFromFolder(if (fileName.startsWith("GIF")) MsgMediaType.GIF else MsgMediaType.IMAGE, fileName) ?: return
        viewModelScope.launch {
            val msgTime = repoMsgs.getMsg(msgId)?.timeMillis ?: -1L
            flowEvents.emit(MsgsFlowState.OpenFileFlowState(file, if (isGif) FlowType.OPEN_GIF else FlowType.OPEN_IMAGE, msgTime))
        }
    }

    //endregion mediaView

    private suspend fun uploadToServer(baos: ByteArray, type: String, uri: String, fileName: String): String? {
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
        } catch (_: Exception) {
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

    private suspend fun getThumbBmpUsingGlide(bytes: ByteArray, con: Context): Bitmap? {
        return suspendCoroutine { continuation ->
            Glide.with(con)
                .asBitmap()
                .load(bytes)
                .override(24)
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
                .override(28)
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
