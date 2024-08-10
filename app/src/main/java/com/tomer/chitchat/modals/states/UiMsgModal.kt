package com.tomer.chitchat.modals.states

import com.tomer.chitchat.room.MsgMediaType

class UiMsgModal constructor(
    var id: Long,
    val replyId: Long,
    var status: MsgStatus,
    val msg: String,
    val rep: String,
    val mediaFileName: String?,
    val replyMediaFileName: String?,
    val timeText: String,
    var isSent: Boolean,
    val isReply: Boolean,
    val msgType: MsgMediaType,
    val replyType: MsgMediaType,
    var isUploaded: Boolean,
    var isDownloaded: Boolean,
    var isProg: Boolean,
    var bytes: ByteArray?,
    val repBytes: ByteArray?,
    val isEmojiOnly: Boolean
)