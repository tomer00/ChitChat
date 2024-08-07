package com.tomer.chitchat.repo

import com.tomer.chitchat.modals.rv.PersonModel
import com.tomer.chitchat.repo.impl.RepoPersonRoom
import com.tomer.chitchat.room.ModelRoomPersons
import java.util.Optional

interface RepoPersons {

    suspend fun getAllPersons():List<PersonModel>
    suspend fun insertPerson(personRoom: ModelRoomPersons)
    suspend fun deletePerson(personRoom: ModelRoomPersons)
    suspend fun updatePerson(personRoom: ModelRoomPersons)
    suspend fun getPersonByPhone(phoneNo:String) : ModelRoomPersons?

}