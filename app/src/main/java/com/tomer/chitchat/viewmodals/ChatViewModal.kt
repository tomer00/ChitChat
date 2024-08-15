package com.tomer.chitchat.viewmodals

import android.util.Log
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.tomer.chitchat.crypto.CipherUtils
import com.tomer.chitchat.crypto.CryptoService
import com.tomer.chitchat.modals.msgs.AcceptConnection
import com.tomer.chitchat.modals.msgs.BulkReceived
import com.tomer.chitchat.modals.msgs.ChatMessage
import com.tomer.chitchat.modals.msgs.Message
import com.tomer.chitchat.modals.msgs.ModelMsgSocket
import com.tomer.chitchat.modals.msgs.NewConnection
import com.tomer.chitchat.modals.msgs.RejectConnection
import com.tomer.chitchat.modals.states.FlowType
import com.tomer.chitchat.modals.states.MsgStatus
import com.tomer.chitchat.modals.states.MsgsFlowState
import com.tomer.chitchat.modals.states.UiMsgModal
import com.tomer.chitchat.modals.states.UiMsgModalBuilder
import com.tomer.chitchat.notifications.NotificationService
import com.tomer.chitchat.repo.RepoMessages
import com.tomer.chitchat.repo.RepoPersons
import com.tomer.chitchat.repo.RepoRelations
import com.tomer.chitchat.repo.RepoStorage
import com.tomer.chitchat.repo.RepoUtils
import com.tomer.chitchat.retro.Api
import com.tomer.chitchat.room.ModelRoomMessage
import com.tomer.chitchat.room.ModelRoomMessageBuilder
import com.tomer.chitchat.room.ModelRoomPersonRelation
import com.tomer.chitchat.room.ModelRoomPersons
import com.tomer.chitchat.room.MsgMediaType
import com.tomer.chitchat.utils.ConversionUtils
import com.tomer.chitchat.utils.MessageHandler
import com.tomer.chitchat.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.math.BigInteger
import java.util.LinkedList
import javax.inject.Inject

@HiltViewModel
class ChatViewModal @Inject constructor(
    private val repoUtils: RepoUtils,
    private val repoMsgs: RepoMessages,
    private val repoStorage: RepoStorage,
    private val repoPersons: RepoPersons,
    private val gson: Gson,
    private val retro: Api,
    private val notificationService: NotificationService,
    private val repoRelations: RepoRelations,
    private val cryptoService: CryptoService,
) : ViewModel() {

    private var webSocket: WebSocket? = null
    private var token = ""


    private fun tryReconnectAfter2Sec() {
        if (token.isNotEmpty())
            viewModelScope.launch {
                delay(2000)
                Log.d("TAG--", "tryReconnectAfter2Sec: ")
                this@ChatViewModal.webSocket = openWebSocket(token)
            }
    }

    fun closeWebSocket() {
        try {
            webSocket!!.close(1001, "Closed for activity close")
        } catch (_: Exception) {
        }
    }

    private val webSocketListener = object : WebSocketListener() {
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            Log.d("TAG--", "onClosed: $code $reason")
            if (code != 1001)
                tryReconnectAfter2Sec()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            tryReconnectAfter2Sec()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            viewModelScope.launch {
                try {
                    msgHandler.handelMsg(text)
                } catch (e: Exception) {
                    Log.e("TAG--", "onMessage: ", e)
                }
            }
        }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d("TAG--", "onOpen: ")
            sendPendingMsgs()
        }
    }

    private fun sendPendingMsgs() {
        val pendingMsgs = chatMsgs.filter {
            it.status == MsgStatus.SENDING
        }.map { it.id }

        viewModelScope.launch {
            pendingMsgs.reversed().forEach {
                val msgRoom = repoMsgs.getMsg(it)
                Log.d("TAG--", "Sending peding: ${msgRoom?.msgText}")
                if (msgRoom != null && !msgRoom.msgText.startsWith('U')) {

                    val socketMsgBuilder = ModelMsgSocket.Builder()

                    socketMsgBuilder.replyId(msgRoom.replyId)
                    socketMsgBuilder.msgData(msgRoom.msgText)
                    socketMsgBuilder.replyData(msgRoom.repText)
                    socketMsgBuilder.msgType(msgRoom.msgType)
                    socketMsgBuilder.replyMsgType(msgRoom.replyType)
                    socketMsgBuilder.isReply(msgRoom.isRep)
                    socketMsgBuilder.setTimeMillis(System.currentTimeMillis())
                    socketMsgBuilder.replyMediaFileName(msgRoom.replyMediaFileName)
                    socketMsgBuilder.mediaFileName(msgRoom.mediaFileName)

                    try {
                        val encMsg = cryptoService.encString(gson.toJson(socketMsgBuilder.build())) ?: return@launch
                        val actualMsg = "${Utils.currentPartner?.partnerId ?: "0000000000"}${
                            ChatMessage(
                                ConversionUtils.toBase64(it),
                                ConversionUtils.encode(encMsg)
                            )
                        }"
                        sendViaSocket(actualMsg)
                    } catch (_: Exception) {
                    }
                }

            }
        }
    }

    val flowMsgs = MutableSharedFlow<MsgsFlowState>()

    val chatMsgs = LinkedList<UiMsgModal>()

    var isChatActivityVisible = false

    private val msgHandler = MessageHandler(gson, repoMsgs, repoPersons, cryptoService, notificationService, repoRelations, repoStorage) { msg ->

        if (Utils.currentPartner?.partnerId.toString() == msg.fromUser) {
            if (!isChatActivityVisible && msg.type == FlowType.MSG)
                notificationService.showNewMessageNotification(msg.data, msg.fromUser)

            if (msg.type == FlowType.SERVER_REC) {
                for (i in chatMsgs.indices) {
                    if (chatMsgs[i].id == msg.oldId!!) {
                        chatMsgs[i].id = msg.msgId!!
                        break
                    }
                }
            }
            viewModelScope.launch {
                flowMsgs.emit(msg)
                if (msg.type == FlowType.SEND_PR)
                    sendViaSocket("${msg.fromUser}*ACK-PR${msg.msgId}")
                else if (msg.type == FlowType.SEND_BULK_REC)
                    sendViaSocket(BulkReceived(msg.fromUser, msg.data?.msg ?: "").toString())
            }
            return@MessageHandler
        }

        if (msg.type == FlowType.MSG) {
            notificationService.showNewMessageNotification(msg.data, msg.fromUser)
        } else if (msg.type == FlowType.SEND_PR)
            viewModelScope.launch {
                sendViaSocket("${msg.fromUser}*ACK-PR${msg.msgId}")
            }
        else if (msg.type == FlowType.SEND_BULK_REC)
            sendViaSocket(BulkReceived(msg.fromUser, msg.data?.msg ?: "").toString())

        viewModelScope.launch { flowMsgs.emit(msg) }
    }


    init {
        token = repoUtils.getToken()
        webSocket = openWebSocket(token)
        Utils.myName = repoUtils.getName()

        viewModelScope.launch {
            while (isActive) {
                delay(150000)
                if (webSocket != null) {
                    webSocket?.send("PING")
                } else continue
            }
        }
    }

    private fun sendViaSocket(data: String) {
        try {
            webSocket?.send(data)
        } catch (_: Exception) {
        }
    }

    //<(10)toPhone><(7)MSG_TYPE><DATA>
    fun sendMsg(msg: Message) {
        sendViaSocket("${Utils.currentPartner?.partnerId}$msg")
    }

    fun sendChatMsg(msg: ModelMsgSocket, replyBytes: ByteArray?) {
        viewModelScope.launch {
            val builder = ModelRoomMessageBuilder()
            builder.id(repoUtils.getTempId())
            builder.replyId(msg.replyId)
            builder.setPartner(Utils.currentPartner?.partnerId ?: "0000000000")
            builder.msgText(msg.msgData)
            builder.repText(msg.replyData)
            builder.msgType(msg.msgType)
            builder.replyType(msg.replyMsgType)
            builder.isSent(true)
            builder.isRep(msg.isReply)
            builder.setTimeMillis(msg.timeMillis)
            builder.setTimeText(ConversionUtils.millisToTimeText(msg.timeMillis))
            builder.mediaFileName(msg.mediaFileName)
            builder.replyMediaFileName(msg.replyMediaFileName)
            builder.setRepBytes(replyBytes)

            val roomMsg = builder.build()
            repoMsgs.addMsg(roomMsg)
            try {
                val encMsg = cryptoService.encString(gson.toJson(msg)) ?: return@launch
                val actualMsg = "${Utils.currentPartner?.partnerId ?: "0000000000"}${
                    ChatMessage(
                        ConversionUtils.toBase64(roomMsg.id),
                        ConversionUtils.encode(encMsg)
                    )
                }"
                sendViaSocket(actualMsg)
                try {
                    flowMsgs.emit(MsgsFlowState.ChatMessageFlowState(builder.buildUI(), roomMsg.partnerId))
                    repoMsgs.addMsg(builder.build())
                    updatePersonModel(msg, builder.build().id)
                } catch (_: Exception) {
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun openWebSocket(token: String): WebSocket? {
        return try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(Utils.WEBSOCKET_LINK)
                .addHeader("Authorization", "Bearer $token")
                .build()
            client.newWebSocket(request, webSocketListener)
        } catch (e: Exception) {
            null
        }
    }

    fun getTempId() = repoUtils.getTempId()

    fun sendMediaUploaded(msg: ModelMsgSocket, id: Long, toUser: String) {
        val encMsg = cryptoService.encString(gson.toJson(msg)) ?: return
        val actualMsg = "$toUser${
            ChatMessage(
                ConversionUtils.toBase64(id),
                ConversionUtils.encode(encMsg)
            )
        }"
        sendViaSocket(actualMsg)
    }

    //region ACTIVITY COMM

    fun showLastSeen() {
        viewModelScope.launch {
            val per = repoPersons.getPersonByPhone(Utils.currentPartner?.partnerId.toString()) ?: return@launch
            delay(40)
            if (per.lastSeenMillis == -1L) flowMsgs.emit(MsgsFlowState.PartnerEventsFlowState(FlowType.ONLINE, Utils.currentPartner!!.partnerId))
            else flowMsgs.emit(MsgsFlowState.MsgStatusFlowState(per.lastSeenMillis, 0L, FlowType.OFFLINE, Utils.currentPartner!!.partnerId))
        }
    }

    fun updatePersonModel(mod: ModelMsgSocket, tempId: Long) {
        viewModelScope.launch {
            val per = repoPersons.getPersonByPhone(Utils.currentPartner!!.partnerId) ?: return@launch
            val lastMsg: String = when (mod.msgType) {
                MsgMediaType.TEXT, MsgMediaType.EMOJI -> mod.msgData
                MsgMediaType.IMAGE, MsgMediaType.GIF, MsgMediaType.VIDEO, MsgMediaType.FILE -> mod.mediaFileName ?: mod.msgType.name
            }
            ModelRoomPersons(
                phoneNo = per.phoneNo,
                name = per.name,
                mediaType = mod.msgType,
                lastMsg = lastMsg,
                lastMsgId = tempId,
                timeMillis = mod.timeMillis,
                unReadCount = per.unReadCount,
                lastSeenMillis = per.lastSeenMillis
            ).also { repoPersons.insertPerson(it) }
        }
    }

    private fun ModelRoomMessage.toUI(): UiMsgModal {
        val builder = UiMsgModalBuilder()
            .id(id)
            .replyId(replyId)
            .status(msgStatus)
            .setMsg(msgText)
            .setRep(repText)
            .mediaFileName(mediaFileName)
            .replyMediaFileName(replyMediaFileName)
            .setTimeText(ConversionUtils.millisToTimeText(timeMillis))
            .isSent(isSent)
            .isReply(isRep)
            .msgType(msgType)
            .isProg(false)
            .replyType(replyType)
            .isUploaded(msgText.startsWith("http"))
            .isDownloaded(
                if (mediaFileName != null)
                    repoStorage.isPresent(
                        mediaFileName!!,
                        msgType
                    )
                else true
            )

        when (msgType) {
            MsgMediaType.IMAGE, MsgMediaType.GIF -> {
                builder.bytes(
                    repoStorage.getBytesFromFolder(msgType, mediaFileName ?: "def")
                        ?: ConversionUtils.base64ToByteArr(msgText.split(",-,")[1])
                )
            }

            MsgMediaType.VIDEO -> builder.bytes(
                repoStorage.getBytesOfVideoThumb(mediaFileName ?: "def")
                    ?: ConversionUtils.base64ToByteArr(msgText.split(",-,")[1])
            )

            else -> {}
        }

        try {
            if (isRep)
                when (replyType) {
                    MsgMediaType.IMAGE, MsgMediaType.GIF -> {
                        builder.repBytes(
                            repoStorage.getBytesFromFolder(replyType, replyMediaFileName ?: "def")
                                ?: ConversionUtils.base64ToByteArr(repText.split(",-,")[1])
                        )
                    }

                    MsgMediaType.VIDEO -> builder.repBytes(
                        repoStorage.getBytesOfVideoThumb(replyMediaFileName ?: "def")
                            ?: ConversionUtils.base64ToByteArr(repText.split(",-,")[1])
                    )

                    else -> {}
                }
        } catch (e: Exception) {
            Log.e("TAG--", "toUI: ", e)
        }

        return builder.build()
    }

    fun openChat(phone: String) {
        Log.d("TAG--", "openChat: $phone")
        Utils.currentPartner = repoRelations.getRelation(phone)
        cryptoService.setCurrentPartner(phone)
        viewModelScope.launch {
            try {
                chatMsgs.clear()

                val roomMsgs = repoMsgs.getMsgsOfUser(Utils.currentPartner?.partnerId!!)
                val arrUI = Array<UiMsgModal?>(roomMsgs.size) { _ -> null }

                val job = viewModelScope.launch {
                    val defList = mutableListOf<Deferred<Unit>>()
                    withContext(Dispatchers.IO) {
                        for (i in roomMsgs.indices) {
                            defList.add(async { arrUI[i] = roomMsgs[i].toUI() })
                        }
                        for (i in defList.indices) defList[i].join()
                    }
                }
                job.join()
                for (a in arrUI) if (a != null) chatMsgs.add(a)

                flowMsgs.emit(MsgsFlowState.IOFlowState(0L, FlowType.RELOAD_RV, phone))

                repoPersons.getPersonByPhone(phone)?.also {
                    it.unReadCount = 0
                    repoPersons.insertPerson(it)
                }
                if (Utils.currentPartner!!.isAccepted)
                    sendPendingMsgs()
            } catch (e: Exception) {
                Log.e("TAG--", ": ", e)
            }
        }
    }

    fun connectNew(phone: String, openNextActivity: Boolean) {
        if (!phone.isDigitsOnly()) return
        val relation = ModelRoomPersonRelation(phone, phone, isConnSent = true, isAccepted = false, isRejected = false)
        Utils.currentPartner = relation
        cryptoService.setCurrentPartner(phone)
        viewModelScope.launch {
            val per = repoPersons.getPersonByPhone(phone)
            if (per == null) {
                repoPersons.insertPerson(
                    ModelRoomPersons(
                        phone, "",
                        MsgMediaType.TEXT, "", -1L,
                        System.currentTimeMillis(),
                        lastSeenMillis = System.currentTimeMillis()
                    )
                )
            }
            genKeyAndSendNotification(relation)
            if (openNextActivity)
                flowMsgs.emit(MsgsFlowState.PartnerEventsFlowState(FlowType.OPEN_NEW_CONNECTION_ACTIVITY, phone))
            else flowMsgs.emit(MsgsFlowState.PartnerEventsFlowState(FlowType.INCOMING_NEW_CONNECTION_REQUEST, phone))

            //getting Name from server
            val name = try {
                retro.getName(phone).body() ?: return@launch
            } catch (e: Exception) {
                return@launch
            }
            val oldPer = repoPersons.getPersonByPhone(phone) ?: return@launch

            repoPersons.insertPerson(
                ModelRoomPersons(
                    phone, name,
                    oldPer.mediaType, oldPer.lastMsg,
                    oldPer.lastMsgId,
                    oldPer.timeMillis,
                    lastSeenMillis = oldPer.lastSeenMillis
                )
            )
        }
    }

    fun genKeyAndSendNotification(relation: ModelRoomPersonRelation) {
        relation.isRejected = false
        repoRelations.saveRelation(relation)
        val key = cryptoService.checkForKeyAndGenerateIfNot(relation.partnerId)
        sendViaSocket(
            "${relation.partnerId}${
                NewConnection(
                    CipherUtils.G.modPow(
                        BigInteger(key.tempKeyMy, 16),
                        CipherUtils.P
                    ).toString(16)
                )
            }"
        )
    }

    fun acceptConnection(accepted: Boolean) {
        if (accepted)
            sendMsg(
                AcceptConnection(
                    CipherUtils.G.modPow(
                        BigInteger(cryptoService.checkForKeyAndGenerateIfNot(Utils.currentPartner!!.partnerId).tempKeyMy, 16), CipherUtils.P
                    ).toString(16)
                )
            )
        else sendMsg(RejectConnection())

        viewModelScope.launch {
            val relation = Utils.currentPartner!!
            if (accepted)
                relation.isAccepted = true
            else {
                relation.isConnSent = false
                relation.isAccepted = false
                relation.isRejected = true
            }
            repoRelations.saveRelation(relation)
            Utils.currentPartner = relation
        }

    }

    //endregion ACTIVITY COMM
}