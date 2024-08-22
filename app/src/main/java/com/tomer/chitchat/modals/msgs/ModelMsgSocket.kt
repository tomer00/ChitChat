package com.tomer.chitchat.modals.msgs

import com.google.gson.annotations.SerializedName
import com.tomer.chitchat.room.MsgMediaType

data class ModelMsgSocket(
    @SerializedName("reply_id") val replyId: Long,
    @SerializedName("msg_data") val msgData: String,
    @SerializedName("reply_data") val replyData: String,
    @SerializedName("msg_type") val msgType: MsgMediaType,
    @SerializedName("reply_msg_type") val replyMsgType: MsgMediaType,
    @SerializedName("is_reply") val isReply: Boolean,
    @SerializedName("time_millis") val timeMillis: Long,
    @SerializedName("reply_media_file_name") val replyMediaFileName: String?,
    @SerializedName("media_file_name") val mediaFileName: String?,
    @SerializedName("media_file_size") val mediaFileSize: String,
) {
    class Builder {
        private var replyId: Long = -1
        private var time: Long = System.currentTimeMillis()
        private var msgData: String = ""
        private var replyData: String = ""
        private var mediaName: String? = null
        private var replyMediaName: String? = null
        private var msgType: MsgMediaType = MsgMediaType.TEXT
        private var replyMsgType: MsgMediaType = MsgMediaType.TEXT
        private var isReply: Boolean = false
        private var mediaSize: String = ""

        fun replyId(replyId: Long) = apply { this.replyId = replyId }
        fun msgData(msgData: String) = apply { this.msgData = msgData }
        fun replyData(replyData: String) = apply { this.replyData = replyData }
        fun mediaFileName(fileName: String?) = apply { this.mediaName = fileName }
        fun replyMediaFileName(fileName: String?) = apply { this.replyMediaName = fileName }
        fun msgType(msgType: MsgMediaType) = apply { this.msgType = msgType }
        fun replyMsgType(replyMsgType: MsgMediaType) = apply { this.replyMsgType = replyMsgType }
        fun isReply(isReply: Boolean) = apply { this.isReply = isReply }
        fun setTimeMillis(time: Long) = apply { this.time = time }
        fun mediaSize(mediaSize: String) = apply { this.mediaSize = mediaSize }

        fun build() = ModelMsgSocket(
            replyId = replyId,
            msgData = msgData,
            replyData = replyData,
            mediaFileName = mediaName,
            replyMediaFileName = replyMediaName,
            msgType = msgType,
            replyMsgType = replyMsgType,
            isReply = isReply,
            timeMillis = time,
            mediaFileSize = mediaSize
        )
    }
}
