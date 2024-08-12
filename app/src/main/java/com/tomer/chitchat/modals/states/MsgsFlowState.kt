package com.tomer.chitchat.modals.states

import com.tomer.chitchat.utils.Utils
import java.io.File

sealed class MsgsFlowState(
    val fromUser: String,
    val data: UiMsgModal? = null,
    val msgId: Long? = null,
    val oldId: Long? = null,
    val type: FlowType,
    val fileGif: File? = null

) {
    data class ChatMessageFlowState(val modelMessage: UiMsgModal, val fromUserC: String) :
        MsgsFlowState(type = FlowType.MSG, data = modelMessage, fromUser = fromUserC)

    data class IOFlowState(val id: Long, val ioType: FlowType, val fromUserC: String, val msgData: UiMsgModal? = null) :
        MsgsFlowState(type = ioType, msgId = id, fromUser = fromUserC, data = msgData)

    data class MsgStatusFlowState(val newId: Long, val oId: Long, val ioType: FlowType, val fromUserC: String) :
        MsgsFlowState(type = ioType, msgId = newId, oldId = oId, fromUser = fromUserC)

    data class PartnerEventsFlowState(val ioType: FlowType, val fromUserC: String, val millis: Long? = null) :
        MsgsFlowState(type = ioType, fromUser = fromUserC, msgId = millis)

    data class ChangeGif(val file: File? = null, val modelMessage: UiMsgModal? = null, val typeF: FlowType = FlowType.CHANGE_GIF) :
        MsgsFlowState(fromUser = Utils.currentPartner?.partnerId.toString(), type = typeF, fileGif = file, data = modelMessage)

}

