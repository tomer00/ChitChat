package com.example.mvvm.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "code")
data class ModalSample(
    @PrimaryKey
    val f:String
)
