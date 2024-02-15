package com.example.mvvm.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.mvvm.room.Dao
import com.example.mvvm.room.ModalSample

@Database(entities = [ModalSample::class], version = 1, exportSchema = false)
abstract class Database :RoomDatabase() {
    abstract fun channelDao(): Dao
}