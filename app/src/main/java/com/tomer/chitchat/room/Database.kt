package com.tomer.chitchat.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tomer.chitchat.room.Dao
import com.tomer.chitchat.room.ModalSample

@Database(entities = [ModalSample::class], version = 1, exportSchema = false)
abstract class Database :RoomDatabase() {
    abstract fun channelDao(): Dao
}