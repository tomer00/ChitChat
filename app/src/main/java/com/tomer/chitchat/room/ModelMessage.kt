package com.tomer.chitchat.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tomer.chitchat.modals.MessageData
import com.tomer.chitchat.modals.MsgType

@Entity(tableName = "messages")
data class ModelMessage(
    @PrimaryKey
    val id: Long,
    val partner: Long,
    val msg: MessageData,
    val type: MsgType,
    val isSentToPartner: Boolean,
    val isReceivedByServer: Boolean
){

}