package com.tomer.chitchat.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "relation")
data class ModelRoomPersonRelation(
    @PrimaryKey
    val partnerId : String,
    var isConnSent : Boolean,
    var isAccepted : Boolean,
    var isRejected : Boolean
)