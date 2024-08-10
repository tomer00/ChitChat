package com.tomer.chitchat.modals.msgs

import com.tomer.chitchat.room.MsgMediaType

data class ModelMsgSocket(
    val replyId: Long,
    val msgData: String,
    val replyData: String,
    val msgType: MsgMediaType,
    val replyMsgType: MsgMediaType,
    val isReply: Boolean,
    val timeMillis: Long,
    val replyMediaFileName: String?,
    val mediaFileName: String?,
) {
    class Builder {
        private var replyId: Long = -1
        private var time: Long = 0
        private var msgData: String = ""
        private var replyData: String = ""
        private var mediaName: String? = null
        private var replyMediaName: String? = null
        private var msgType: MsgMediaType = MsgMediaType.TEXT
        private var replyMsgType: MsgMediaType = MsgMediaType.TEXT
        private var isReply: Boolean = false

        fun replyId(replyId: Long) = apply { this.replyId = replyId }
        fun msgData(msgData: String) = apply { this.msgData = msgData }
        fun replyData(replyData: String) = apply { this.replyData = replyData }
        fun mediaFileName(fileName: String?) = apply { this.mediaName = fileName }
        fun replyMediaFileName(fileName: String?) = apply { this.replyMediaName = fileName }
        fun msgType(msgType: MsgMediaType) = apply { this.msgType = msgType }
        fun replyMsgType(replyMsgType: MsgMediaType) = apply { this.replyMsgType = replyMsgType }
        fun isReply(isReply: Boolean) = apply { this.isReply = isReply }
        fun setTimeMillis(time: Long) = apply { this.time = time }

        fun build() = ModelMsgSocket(
            replyId = replyId,
            msgData = msgData,
            replyData = replyData,
            mediaFileName = mediaName,
            replyMediaFileName = replyMediaName,
            msgType = msgType,
            replyMsgType = replyMsgType,
            isReply = isReply,
            timeMillis = time
        )
    }
}
