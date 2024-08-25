package com.tomer.chitchat.utils

import android.util.Log
import com.google.gson.Gson
import com.tomer.chitchat.crypto.CryptoService
import com.tomer.chitchat.modals.msgs.ModelMsgSocket
import com.tomer.chitchat.modals.states.FlowType
import com.tomer.chitchat.modals.states.MsgStatus
import com.tomer.chitchat.modals.states.MsgsFlowState
import com.tomer.chitchat.modals.states.UiMsgModalBuilder
import com.tomer.chitchat.notifications.NotificationService
import com.tomer.chitchat.repo.RepoMessages
import com.tomer.chitchat.repo.RepoPersons
import com.tomer.chitchat.repo.RepoRelations
import com.tomer.chitchat.repo.RepoStorage
import com.tomer.chitchat.room.ModelRoomMessageBuilder
import com.tomer.chitchat.room.ModelRoomPersonRelation
import com.tomer.chitchat.room.ModelRoomPersons
import com.tomer.chitchat.room.MsgMediaType
import java.util.stream.Collectors

class MessageHandler(
    private val gson: Gson,
    private val repoMsg: RepoMessages,
    private val repoPersons: RepoPersons,
    private val crypto: CryptoService,
    private val notiService: NotificationService,
    private val repoRelation: RepoRelations,
    private val repoStorage: RepoStorage,
    private val callBack: (msg: MsgsFlowState) -> Unit
) {
    //<(10)fromUser><(7)MSG_TYPE><DATA>
    @Throws(Exception::class)
    suspend fun handelMsg(text: String) {
        if (text.isEmpty() || text == "PONG") return
        Log.d("TAG--", "handelMsg: $text")
        val fromUser = text.substring(0, 10).intern()
        val msgType = text.substring(10, 17).intern()
        when (msgType) {

            //<(10)fromUser><(7)MSG_TYPE><RTT<DATA>>
            "*-MSG-*" -> {
                val actualDecData = crypto.decString(fromUser, ConversionUtils.decode(text.substring(29)))
                if (actualDecData == null) {
                    callBack(MsgsFlowState.PartnerEventsFlowState(FlowType.SEND_NEW_CONNECTION_REQUEST, fromUser))
                    return
                }
                try {
                    val mod = gson.fromJson(actualDecData, ModelMsgSocket::class.java)
                    val currentId = ConversionUtils.fromBase64(text.substring(17, 29))
                    callBack(MsgsFlowState.IOFlowState(currentId, FlowType.SEND_PR, fromUser))
                    handleMsgCombine(fromUser, currentId, mod, true)
                } catch (e: Exception) {
                    Log.e("TAG--", "handelMsg: ", e)
                }
            }

            "*ACK-SR" -> {
                val tempId = ConversionUtils.fromBase64(text.substring(17, 29))
                val newId = text.substring(29).toLong()
                repoMsg.updateMsg(tempId, newId)
                repoMsg.updateMsgReceived(newId, MsgStatus.SENT_TO_SERVER)
                callBack(MsgsFlowState.MsgStatusFlowState(newId, tempId, FlowType.SERVER_REC, fromUser))

                val per = repoPersons.getPersonByPhone(fromUser) ?: return
                if (per.lastMsgId == tempId)
                    ModelRoomPersons(
                        phoneNo = per.phoneNo,
                        name = per.name,
                        mediaType = per.mediaType,
                        lastMsg = per.lastMsg,
                        lastMsgId = newId,
                        timeMillis = per.timeMillis,
                        unReadCount = per.unReadCount,
                        lastSeenMillis = per.lastSeenMillis,
                        isSent = true,
                        msgStatus = MsgStatus.SENT_TO_SERVER
                    ).also { repoPersons.insertPerson(it) }
            }

            "*ACK-PR" -> {
                val idL = text.substring(17).toLong()
                repoMsg.updateMsgReceived(idL, MsgStatus.RECEIVED)
                callBack(MsgsFlowState.MsgStatusFlowState(idL, idL, FlowType.PARTNER_REC, fromUser))

                val per = repoPersons.getPersonByPhone(fromUser) ?: return
                if (per.lastMsgId == idL)
                    ModelRoomPersons(
                        phoneNo = per.phoneNo,
                        name = per.name,
                        mediaType = per.mediaType,
                        lastMsg = per.lastMsg,
                        lastMsgId = idL,
                        timeMillis = per.timeMillis,
                        unReadCount = per.unReadCount,
                        lastSeenMillis = per.lastSeenMillis,
                        isSent = true,
                        msgStatus = MsgStatus.RECEIVED
                    ).also { repoPersons.insertPerson(it) }
            }

            "*-TYP-*" -> callBack(MsgsFlowState.PartnerEventsFlowState(FlowType.TYPING, fromUser))
            "*N-TYP*" -> callBack(MsgsFlowState.PartnerEventsFlowState(FlowType.NO_TYPING, fromUser))

            "*-ONL-*" -> {
                val per = repoPersons.getPersonByPhone(fromUser) ?: return
                callBack(MsgsFlowState.PartnerEventsFlowState(FlowType.ONLINE, fromUser))
                per.lastSeenMillis = -1L
                repoPersons.insertPerson(per)
            }

            "*OFF-L*" -> {
                try {
                    val per = repoPersons.getPersonByPhone(fromUser) ?: throw Exception()
                    per.lastSeenMillis = text.substring(17).toLong()
                    callBack(MsgsFlowState.PartnerEventsFlowState(FlowType.OFFLINE, fromUser, per.lastSeenMillis))
                    repoPersons.insertPerson(per)
                } catch (_: Exception) {
                    callBack(MsgsFlowState.PartnerEventsFlowState(FlowType.OFFLINE, fromUser, System.currentTimeMillis()))
                }
            }

            "*P-STA*" -> {
                try {
                    val per = repoPersons.getPersonByPhone(fromUser) ?: return
                    per.lastSeenMillis = text.substring(17).toLong()
                    if (per.lastSeenMillis == -1L) callBack(MsgsFlowState.PartnerEventsFlowState(FlowType.ONLINE, fromUser))
                    else callBack(MsgsFlowState.PartnerEventsFlowState(FlowType.OFFLINE, fromUser, per.lastSeenMillis))
                    repoPersons.insertPerson(per)
                } catch (_: Exception) {
                }
            }

            "*MSG-B*" -> {
                val msgs = text.substring(17).split(",-,")
                    .parallelStream()
                    .map { item ->
                        val id = ConversionUtils.fromBase64(item.substring(0, 12))
                        val actualDecData = crypto.decString(fromUser, ConversionUtils.decode(item.substring(12)))
                        Pair(id, gson.fromJson(actualDecData, ModelMsgSocket::class.java))
                    }.collect(Collectors.toList())
                var isNeedToSendNewConn = false
                for (i in msgs) {
                    if (i.second == null) {
                        isNeedToSendNewConn = true
                        break
                    }
                }
                if (isNeedToSendNewConn) {
                    callBack(MsgsFlowState.PartnerEventsFlowState(FlowType.SEND_NEW_CONNECTION_REQUEST, fromUser))
                    val sb = StringBuilder()
                    msgs.forEach {
                        sb.append(it.first)
                        sb.append(',')
                    }
                    val uiB = UiMsgModalBuilder()
                    sb.deleteCharAt(sb.length - 1)
                    uiB.setMsg(sb.toString())
                    callBack(MsgsFlowState.IOFlowState(0L, FlowType.SEND_BULK_REC, fromUser, uiB.build()))
                    return
                }

                msgs.sortWith { o1, o2 -> o1.second.timeMillis.compareTo(o2.second.timeMillis) }
                val sb = StringBuilder()
                msgs.forEach {
                    sb.append(it.first)
                    sb.append(',')
                }
                val uiB = UiMsgModalBuilder()
                sb.deleteCharAt(sb.length - 1)
                uiB.setMsg(sb.toString())
                callBack(MsgsFlowState.IOFlowState(0L, FlowType.SEND_BULK_REC, fromUser, uiB.build()))

                for (i in 0..msgs.size - 2) handleMsgCombine(fromUser, msgs[i].first, msgs[i].second, false)
                handleMsgCombine(fromUser, msgs[msgs.lastIndex].first, msgs[msgs.lastIndex].second, true)
                Log.d("TAG--", "Handeling BULK MSg: $msgs")
            }

            "ACK-PRB" -> {
                val per = repoPersons.getPersonByPhone(fromUser) ?: return

                text.substring(17).split(",")
                    .forEach {
                        val idL = it.toLong()
                        repoMsg.updateMsgReceived(idL, MsgStatus.RECEIVED)
                        callBack(MsgsFlowState.MsgStatusFlowState(idL, idL, FlowType.PARTNER_REC, fromUser))

                        if (per.lastMsgId == idL)
                            ModelRoomPersons(
                                phoneNo = per.phoneNo,
                                name = per.name,
                                mediaType = per.mediaType,
                                lastMsg = per.lastMsg,
                                lastMsgId = idL,
                                timeMillis = per.timeMillis,
                                unReadCount = per.unReadCount,
                                lastSeenMillis = per.lastSeenMillis,
                                isSent = true,
                                msgStatus = MsgStatus.RECEIVED
                            ).also { p -> repoPersons.insertPerson(p) }
                    }
            }

            "*-NEW-*" -> {
                val sts = text.substring(17).split(",-,".toRegex(), 2)
                crypto.updateKeyAndGenerateFullKey(sts[0], fromUser)
                notiService.showNewUserNotification(fromUser, sts[1])
                repoRelation.saveRelation(
                    ModelRoomPersonRelation(
                        fromUser, sts[1], isConnSent = false, isAccepted = false, isRejected = false
                    )
                )
                repoPersons.insertPerson(
                    ModelRoomPersons(
                        fromUser, sts[1],
                        MsgMediaType.TEXT, "Sent you connection request...", -1L,
                        System.currentTimeMillis(),
                        lastSeenMillis = System.currentTimeMillis(),
                        isSent = false,
                        msgStatus = MsgStatus.RECEIVED
                    )
                )
                callBack(MsgsFlowState.PartnerEventsFlowState(FlowType.INCOMING_NEW_CONNECTION_REQUEST, fromUser, System.currentTimeMillis()))
            }

            "*F-ACC*" -> {
                if (repoPersons.getPersonByPhone(fromUser) == null) {
                    callBack(MsgsFlowState.PartnerEventsFlowState(FlowType.SEND_NEW_CONNECTION_REQUEST, fromUser))
                    return
                }

                callBack(MsgsFlowState.PartnerEventsFlowState(FlowType.REQ_ACCEPTED, fromUser))
                val sts = text.substring(17).split(",-,".toRegex(), 2)
                crypto.updateKeyAndGenerateFullKey(sts[0], fromUser)
                repoRelation.saveRelation(
                    ModelRoomPersonRelation(
                        fromUser, sts[1], isConnSent = true, isAccepted = true, isRejected = false
                    )
                )
                repoPersons.insertPerson(
                    ModelRoomPersons(
                        fromUser, sts[1],
                        MsgMediaType.TEXT, "Request accepted...", -1L,
                        System.currentTimeMillis(),
                        lastSeenMillis = System.currentTimeMillis(),
                        isSent = false,
                        msgStatus = MsgStatus.RECEIVED
                    )
                )
            }

            "*F-REJ*" -> {
                callBack(MsgsFlowState.PartnerEventsFlowState(FlowType.REQ_REJECTED, fromUser))
                val oldPer = repoPersons.getPersonByPhone(fromUser)
                val name = repoRelation.getRelation(fromUser)?.partnerName ?: fromUser
                if (oldPer != null)
                    ModelRoomPersons(
                        fromUser, name,
                        MsgMediaType.TEXT, "Request rejected...", -1L,
                        System.currentTimeMillis(),
                        lastSeenMillis = oldPer.lastSeenMillis,
                        isSent = false,
                        msgStatus = MsgStatus.RECEIVED
                    ).also { repoPersons.insertPerson(it) }
                repoRelation.saveRelation(
                    ModelRoomPersonRelation(
                        fromUser, name, isConnSent = true, isAccepted = false, isRejected = true
                    )
                )
            }
        }
    }

    private suspend fun handleMsgCombine(fromUser: String, id: Long, mod: ModelMsgSocket, isLast: Boolean) {
        val builderRoom = ModelRoomMessageBuilder()
        try {
            Log.d("TAG--", "handelMsg: $mod")
            builderRoom
                .id(id)
                .isRep(mod.isReply)
                .msgType(mod.msgType)
                .setPartner(fromUser)
                .msgStatus(MsgStatus.RECEIVED)
                .mediaFileName(mod.mediaFileName)
                .replyMediaFileName(mod.replyMediaFileName)
                .isSent(false)
                .setTimeMillis(mod.timeMillis)
                .setTimeText(ConversionUtils.millisToTimeText(mod.timeMillis))
                .mediaSize(mod.mediaFileSize)

            if (mod.isReply) {
                builderRoom
                    .replyId(mod.replyId)
                    .repText(mod.replyData)
                    .replyType(mod.replyMsgType)

                if (mod.replyData.isNotEmpty())
                    try {
                        when (mod.replyMsgType) {
                            MsgMediaType.IMAGE, MsgMediaType.GIF -> {
                                builderRoom.setRepBytes(
                                    repoStorage.getBytesFromFolder(mod.replyMsgType, mod.replyMediaFileName ?: "def")
                                        ?: ConversionUtils.base64ToByteArr(mod.replyData.split(",-,")[1])
                                )
                            }

                            MsgMediaType.VIDEO -> builderRoom.setRepBytes(
                                ConversionUtils.base64ToByteArr(mod.replyData.split(",-,")[1])
                            )

                            else -> {}

                        }
                    } catch (_: Exception) {

                    }

            }
            val roomMsg = builderRoom
                .msgText(mod.msgData)
                .build()
            repoMsg.addMsg(roomMsg)

            when (mod.msgType) {
                MsgMediaType.IMAGE, MsgMediaType.GIF, MsgMediaType.VIDEO -> {
                    val sts = mod.msgData.split(",-,")
                    try {
                        builderRoom.msgText(mod.msgData)
                            .setBytes(ConversionUtils.base64ToByteArr(sts[1]))
                    } catch (_: Exception) {
                    }
                }
                MsgMediaType.FILE -> builderRoom.setBytes(ByteArray(2))
                else -> {}
            }

            if (!isLast) {
                callBack(MsgsFlowState.ChatMessageFlowState(builderRoom.buildUI(), fromUser, false))
                return
            }

            val lastMsg: String = when (mod.msgType) {
                MsgMediaType.TEXT, MsgMediaType.EMOJI -> mod.msgData
                MsgMediaType.IMAGE, MsgMediaType.GIF, MsgMediaType.VIDEO, MsgMediaType.FILE -> mod.mediaFileName ?: mod.msgType.name
            }
            val per = repoPersons.getPersonByPhone(fromUser) ?: ModelRoomPersons(
                phoneNo = fromUser,
                name = repoRelation.getRelation(fromUser)?.partnerName ?: fromUser,
                mediaType = mod.msgType,
                lastMsg = lastMsg,
                lastMsgId = id,
                timeMillis = mod.timeMillis,
                unReadCount = 0,
                lastSeenMillis = -1L,
                isSent = false,
                msgStatus = MsgStatus.RECEIVED
            )

            ModelRoomPersons(
                phoneNo = per.phoneNo,
                name = per.name,
                mediaType = mod.msgType,
                lastMsg = lastMsg,
                lastMsgId = id,
                timeMillis = mod.timeMillis,
                unReadCount = per.unReadCount + 1,
                lastSeenMillis = per.lastSeenMillis,
                isSent = false,
                msgStatus = MsgStatus.RECEIVED
            ).also { repoPersons.insertPerson(it) }
            callBack(MsgsFlowState.ChatMessageFlowState(builderRoom.buildUI(), fromUser, true))
        } catch (e: Exception) {
            Log.e("TAG--", "handelMsg: ", e)
        }
    }
}
