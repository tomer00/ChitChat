package com.tomer.chitchat.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tomer.chitchat.modals.rv.PersonModel
import com.tomer.chitchat.utils.ConversionUtils

@Entity(tableName = "persons")
data class ModelRoomPersons(
    @PrimaryKey
    val phoneNo: String,
    val name: String,
    val mediaType: MsgMediaType,
    val lastMsg: String,
    var timeMillis: Long,
    var unReadCount: Int = 0,
    var lastSeenMillis: Long
) {
    fun toPersonModel() = PersonModel(phoneNo, name, mediaType, ConversionUtils.millisToTimeText(timeMillis), lastMsg, false, unReadCount, lastSeenMillis == -1L)
}
