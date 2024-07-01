package com.tomer.chitchat.modals.states

sealed class MsgsState(
    val data: UiMsgModal? = null,
    val msgId: Long? = null,
    val type: FlowType
) {
    data class ChatMessageState(val modelMessage: UiMsgModal) : MsgsState(type = FlowType.MSG, data = modelMessage)
    data class IOState(val id: Long, val ioType: FlowType) : MsgsState(type = ioType, msgId = id)
}

