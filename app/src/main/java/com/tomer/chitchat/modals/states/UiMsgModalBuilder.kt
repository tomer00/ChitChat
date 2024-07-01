package com.tomer.chitchat.modals.states

import com.tomer.chitchat.room.MsgMediaType

class UiMsgModalBuilder {
    private var id: Long = 0
    private var replyId: Long = 0
    private var status: MsgStatus = MsgStatus.SENDING
    private var msg: String = ""
    private var rep: String = ""
    private var isSent: Boolean = false
    private var isReply: Boolean = false
    private var msgType: MsgMediaType = MsgMediaType.TEXT
    private var replyType: MsgMediaType = MsgMediaType.TEXT
    private var isDone: Boolean = false
    private var isDDone: Boolean = false
    private var isRetry: Boolean = false
    private var bytes: ByteArray? = null
    private var repBytes: ByteArray? = null

    fun id(id: Long) = apply { this.id = id }
    fun replyId(replyId: Long) = apply { this.replyId = replyId }fun status(status: MsgStatus) = apply { this.status = status }
    fun setMsg(msg: String) = apply { this.msg = msg }
    fun setRep(rep: String) = apply { this.rep = rep }
    fun isSent(isSent: Boolean) = apply { this.isSent = isSent }
    fun isReply(isReply: Boolean) = apply { this.isReply = isReply }
    fun msgType(msgType: MsgMediaType) = apply { this.msgType = msgType }
    fun replyType(replyType: MsgMediaType) = apply { this.replyType = replyType }
    fun isDone(isDone: Boolean) = apply { this.isDone = isDone }
    fun isDDone(isDDone: Boolean) = apply { this.isDDone = isDDone }
    fun isRetry(isRetry: Boolean) = apply { this.isRetry = isRetry }
    fun bytes(bytes: ByteArray?) = apply { this.bytes = bytes }
    fun repBytes(repBytes: ByteArray?) = apply { this.repBytes = repBytes }

    fun build()= UiMsgModal(
        id = id,
        replyId = replyId,
        status = status,
        msg = msg,
        rep = rep,
        isSent = isSent,
        isReply = isReply,
        msgType = msgType,
        replyType = replyType,
        isDone = isDone,
        isDDone = isDDone,
        isRetry = isRetry,
        bytes = bytes,
        repBytes = repBytes
    )
}