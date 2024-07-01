package com.tomer.chitchat.room

import com.tomer.chitchat.modals.states.MsgStatus
import com.tomer.chitchat.modals.states.UiMsgModal

class ModelRoomMessageBuilder {
    private var id: Long = 0
    private var replyId: Long = 0
    private var toUser: Long = 0
    private var fromUser: Long = 0
    private var msgText: String = ""
    private var repText: String = ""
    private var msgStatus: MsgStatus = MsgStatus.SENDING
    private var msgType: MsgMediaType = MsgMediaType.TEXT
    private var replyType: MsgMediaType = MsgMediaType.TEXT
    private var isSent: Boolean = false
    private var isRep: Boolean = false

    private var bytes: ByteArray? = null
    private var repBytes: ByteArray?=null

    fun id(id: Long) = apply { this.id = id }
    fun replyId(id: Long) = apply { this.replyId = id }
    fun toUser(toUser: Long) = apply { this.toUser = toUser }
    fun fromUser(fromUser: Long) = apply { this.fromUser = fromUser }
    fun msgText(msgText: String) = apply { this.msgText = msgText }
    fun repText(repText: String) = apply { this.repText = repText }
    fun msgStatus(msgStatus: MsgStatus) = apply { this.msgStatus = msgStatus }
    fun msgType(msgType: MsgMediaType) = apply { this.msgType = msgType }
    fun replyType(replyType: MsgMediaType) = apply { this.replyType = replyType }
    fun isSent(isSent: Boolean) = apply { this.isSent = isSent }
    fun isRep(isRep: Boolean) = apply { this.isRep = isRep }
    fun setBytes(bytes: ByteArray) = apply { this.bytes = bytes }
    fun setRepBytes(repBytes: ByteArray) = apply { this.repBytes = repBytes }

    fun build() = ModelRoomMessage(
        id = id,
        replyId=replyId,
        toUser = toUser,
        fromUser = fromUser,
        msgText = msgText,
        repText = repText,
        msgStatus = msgStatus,
        msgType = msgType,
        replyType = replyType,
        isSent = isSent,
        isRep = isRep
    )
    fun buildUI() = UiMsgModal(
        id = id,
        replyId = replyId,
        status = msgStatus,
        msg = msgText,
        rep = repText,
        isSent = isSent,
        isReply = isRep,
        msgType = msgType,
        replyType = replyType,
        isDone = false,
        isDDone = false,
        isRetry = false,
        bytes = bytes,
        repBytes = repBytes
    )
}