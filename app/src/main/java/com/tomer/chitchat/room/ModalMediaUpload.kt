package com.tomer.chitchat.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media")
data class ModalMediaUpload(
    @PrimaryKey
    val uri: String,
    val localFileName: String
)