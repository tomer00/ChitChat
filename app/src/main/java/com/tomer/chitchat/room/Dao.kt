package com.tomer.chitchat.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface Dao {

    @Query("update messages set id=:newId  where id=:tempId")
    fun updateMsgSent(tempId: Long, newId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMsg(msg: ModelRoomMessage)

    @Query("delete from messages where id=:id")
    suspend fun deleteFromId(id:Long)

    @Query("select * from messages where id=:id")
    suspend fun getFromID(id: Long)
}
