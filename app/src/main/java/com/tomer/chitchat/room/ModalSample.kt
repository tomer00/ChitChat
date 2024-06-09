package com.tomer.chitchat.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "code")
data class ModalSample(
    @PrimaryKey
    val f:String
)
