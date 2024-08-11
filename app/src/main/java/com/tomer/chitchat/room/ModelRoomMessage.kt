package com.tomer.chitchat.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tomer.chitchat.modals.states.MsgStatus

@Entity(tableName = "messages")
data class ModelRoomMessage(
    @PrimaryKey
    val id: Long,
    val replyId: Long,
    val partnerId: String,
    var msgText: String,
    val repText: String,

    var msgStatus: MsgStatus,
    val msgType: MsgMediaType,
    val replyType: MsgMediaType,

    var mediaFileName: String?,
    val replyMediaFileName: String?,

    val isSent: Boolean,
    val isRep: Boolean,

    val timeMillis: Long
)