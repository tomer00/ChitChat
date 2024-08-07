package com.tomer.chitchat.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tomer.chitchat.crypto.CryptoKey

@Dao
interface Dao {

    //region TABLE MESSAGES


    @Query("update messages set id=:newId  where id=:tempId")
    fun updateMsgSent(tempId: Long, newId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMsg(msg: ModelRoomMessage)

    @Query("delete from messages where id=:id")
    fun deleteFromId(id:Long)

    @Query("select * from messages where id=:id")
    fun getFromID(id: Long) : ModelRoomMessage?

    @Query("select * from messages where partnerId=:partnerId order by timeMillis DESC")
    fun getByUser(partnerId: String) : List<ModelRoomMessage>

    @Query("select * from messages where id=:msgId")
    fun getByUser(msgId: Long) : List<ModelRoomMessage>

    //endregion TABLE MESSAGES

    //region TABLE PERSONS

    @Query("select * from persons order by timeMillis DESC")
    fun getMainViewList() : List<ModelRoomPersons>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPerson(person: ModelRoomPersons)

    @Query("delete from persons where phoneNo=:phoneNo")
    fun deletePerson(phoneNo: String)

    @Query("select * from persons where phoneNo=:phoneNo")
    fun getPerson(phoneNo: String) : ModelRoomPersons?

    //endregion TABLE PERSONS

    //region TABLE KEYS

    @Query("select * from keystore where partnerUserPhone=:phoneNo")
    fun getKey(phoneNo: String) : List<CryptoKey>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertKey(person: CryptoKey)

    @Query("delete from keystore where partnerUserPhone=:phoneNo")
    fun deleteKey(phoneNo: String)

    //endregion TABLE KEYS

    // region Relations

    @Query("select * from relation where partnerId=:phoneNo")
    fun getRelation(phoneNo: String) : List<ModelRoomPersonRelation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRelation(person: ModelRoomPersonRelation)

    //endregion Relations
}
