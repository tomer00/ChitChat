package com.tomer.chitchat.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tomer.chitchat.modals.states.MsgStatus

@Entity(tableName = "persons")
data class ModelRoomPersons(
    @PrimaryKey
    val phoneNo: String,
    val name: String,
    val mediaType: MsgMediaType,
    val lastMsg: String,
    val lastMsgId: Long,
    var timeMillis: Long,
    var unReadCount: Int = 0,
    var lastSeenMillis: Long,
    val isSent: Boolean,
    var msgStatus: MsgStatus
)
