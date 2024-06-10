package com.tomer.chitchat.modals

data class MessageData(
    val data: String,
    val replyMsgId: Long?,
    val isReply: Boolean,
    val isImage:Boolean
){

}
