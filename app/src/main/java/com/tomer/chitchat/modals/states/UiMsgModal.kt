package com.tomer.chitchat.modals.states

import com.tomer.chitchat.room.MsgMediaType

class UiMsgModal constructor(
    val id:Long,
    val replyId:Long,
    val status: MsgStatus,
    val msg: String,
    val rep: String,
    val isSent: Boolean,
    val isReply: Boolean,
    val msgType: MsgMediaType,
    val replyType: MsgMediaType,
    val isDone: Boolean,
    val isDDone: Boolean,
    val isRetry: Boolean,
    val bytes: ByteArray?,
    val repBytes: ByteArray?
)