package com.tomer.chitchat.repo.impl

import com.tomer.chitchat.modals.states.MsgStatus
import com.tomer.chitchat.repo.RepoMessages
import com.tomer.chitchat.room.Dao
import com.tomer.chitchat.room.ModelRoomMessage
import javax.inject.Inject

class RepoMessagesImpl @Inject constructor(
    private val dao: Dao
) : RepoMessages {

    override suspend fun addMsg(modal: ModelRoomMessage) {
        if (dao.getFromID(modal.id) == null)
            dao.insertMsg(modal)
    }

    override suspend fun getMsg(id: Long): ModelRoomMessage? = dao.getByUser(id).getOrNull(0)
    override suspend fun getMsgsOfUser(partnerId: String) = dao.getByUser(partnerId)

    override suspend fun updateMsg(tempId: Long, newId: Long) {
        dao.updateMsgSent(tempId, newId)
    }

    override suspend fun updateMsgReceived(msgId: Long, status: MsgStatus) {
        val data = dao.getFromID(msgId) ?: return
        dao.deleteFromId(msgId)
        data.msgStatus = status
        dao.insertMsg(data)
    }

    override suspend fun getMsgFromFileName(fileName: String) = dao.getByFileName(fileName).getOrNull(0)
}