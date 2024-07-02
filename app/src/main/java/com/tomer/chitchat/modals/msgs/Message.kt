package com.tomer.chitchat.modals.msgs

abstract class Message(
    private val msg:String
) {
    override fun toString() = msg
}

data class Acknowledge(
    val msgId:String
):Message(
    "*-ACK-*$msgId"
)

class Typing:Message("*-TYP-*")

class NoTyping:Message("*N-TYP*")

// <(10)toPhone>  <(7)MSG_TYPE>
// <DATA>  { <(12) TEMPID><ENC DATA> }
data class ChatMessage(
    val msgId:String,
    val msg:String
):Message(
    "*-MSG-*$msgId$msg"
)