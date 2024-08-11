package com.tomer.chitchat.utils

import android.util.Log
import com.google.gson.Gson
import com.tomer.chitchat.crypto.CryptoService
import com.tomer.chitchat.modals.msgs.ModelMsgSocket
import com.tomer.chitchat.modals.states.FlowType
import com.tomer.chitchat.modals.states.MsgStatus
import com.tomer.chitchat.modals.states.MsgsFlowState
import com.tomer.chitchat.notifications.NotificationService
import com.tomer.chitchat.repo.RepoMessages
import com.tomer.chitchat.repo.RepoPersons
import com.tomer.chitchat.repo.RepoRelations
import com.tomer.chitchat.repo.RepoStorage
import com.tomer.chitchat.room.ModelRoomMessageBuilder
import com.tomer.chitchat.room.ModelRoomPersonRelation
import com.tomer.chitchat.room.ModelRoomPersons
import com.tomer.chitchat.room.MsgMediaType

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
        val fromUser = text.substring(0, 10)
        val msgType = text.substring(10, 17)
        when (msgType) {

            //<(10)fromUser><(7)MSG_TYPE><RTT<DATA>>
            "*-MSG-*" -> {
                val builderRoom = ModelRoomMessageBuilder()
                val actualDecData = crypto.decString(fromUser, ConversionUtils.decode(text.substring(29)))
                if (actualDecData == null) {
                    callBack(MsgsFlowState.PartnerEventsFlowState(FlowType.SEND_NEW_CONNECTION_REQUEST, fromUser))
                    return
                }
                try {
                    val mod = gson.fromJson(actualDecData, ModelMsgSocket::class.java)
                    Log.d("TAG--", "handelMsg: $mod")
                    builderRoom
                        .id(ConversionUtils.fromBase64(text.substring(17, 29)))
                        .isRep(mod.isReply)
                        .msgType(mod.msgType)
                        .setPartner(fromUser)
                        .msgStatus(MsgStatus.RECEIVED)
                        .mediaFileName(mod.mediaFileName)
                        .replyMediaFileName(mod.replyMediaFileName)
                        .isSent(false)
                        .setTimeMillis(mod.timeMillis)
                        .setTimeText(ConversionUtils.millisToTimeText(mod.timeMillis))

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
                                        repoStorage.getBytesOfVideoThumb(mod.replyMediaFileName ?: "def")
                                            ?: ConversionUtils.base64ToByteArr(mod.replyData.split(",-,")[1])
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

                        else -> {}
                    }

                    callBack(MsgsFlowState.ChatMessageFlowState(builderRoom.buildUI(), fromUser))

                    val per = repoPersons.getPersonByPhone(fromUser) ?: return
                    val lastMsg: String = when (mod.msgType) {
                        MsgMediaType.TEXT, MsgMediaType.FILE -> mod.msgData
                        MsgMediaType.IMAGE -> "IMAGE"
                        MsgMediaType.GIF -> "GIF"
                        MsgMediaType.VIDEO -> "VIDEO"

                    }
                    ModelRoomPersons(
                        phoneNo = per.phoneNo,
                        name = per.name,
                        mediaType = mod.msgType,
                        lastMsg = lastMsg,
                        timeMillis = mod.timeMillis,
                        unReadCount = per.unReadCount + 1
                    ).also { repoPersons.insertPerson(it) }

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
            }

            "*ACK-PR" -> {
                val id = text.substring(17)
                val idL = id.toLong()
                repoMsg.updateMsgReceived(idL, MsgStatus.RECEIVED)
                callBack(MsgsFlowState.MsgStatusFlowState(idL, idL, FlowType.PARTNER_REC, fromUser))

            }

            "*-TYP-*" -> callBack(MsgsFlowState.PartnerEventsFlowState(FlowType.TYPING, fromUser))
            "*N-TYP*" -> callBack(MsgsFlowState.PartnerEventsFlowState(FlowType.NO_TYPING, fromUser))

            "*-ONL-*" -> callBack(MsgsFlowState.PartnerEventsFlowState(FlowType.ONLINE, fromUser))
            "*OFF-L*" -> {
                try {
                    val per = repoPersons.getPersonByPhone(fromUser)
                    per!!.timeMillis = text.substring(17).toLong()
                    callBack(MsgsFlowState.PartnerEventsFlowState(FlowType.OFFLINE, fromUser, per.timeMillis))
                } catch (_: Exception) {
                    callBack(MsgsFlowState.PartnerEventsFlowState(FlowType.OFFLINE, fromUser, System.currentTimeMillis()))
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
                        MsgMediaType.TEXT, "",
                        System.currentTimeMillis()
                    )
                )
            }

            "*F-ACC*" -> {
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
                        MsgMediaType.TEXT, "",
                        System.currentTimeMillis()
                    )
                )
                callBack(MsgsFlowState.PartnerEventsFlowState(FlowType.ACCEPT_REQ, fromUser))
            }

            "*F-REJ*" -> {
                repoRelation.saveRelation(
                    ModelRoomPersonRelation(
                        fromUser, fromUser, isConnSent = true, isAccepted = false, isRejected = true
                    )
                )
                callBack(MsgsFlowState.PartnerEventsFlowState(FlowType.REJECT_REQ, fromUser))
            }
        }
    }
}