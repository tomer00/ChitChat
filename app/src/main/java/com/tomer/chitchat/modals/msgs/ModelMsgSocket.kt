package com.tomer.chitchat.modals.msgs

import com.tomer.chitchat.room.MsgMediaType

data class ModelMsgSocket(
    val replyId:Long,
    val msgData:String,
    val replyData:String,
    val msgType:MsgMediaType,
    val replyMsgType:MsgMediaType,
    val isReply:Boolean
)
