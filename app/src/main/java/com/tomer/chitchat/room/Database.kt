package com.tomer.chitchat.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tomer.chitchat.room.Dao

@Database(entities = [ModelRoomMessage::class], version = 1, exportSchema = false)
abstract class Database :RoomDatabase() {
    abstract fun messageDao(): Dao
}