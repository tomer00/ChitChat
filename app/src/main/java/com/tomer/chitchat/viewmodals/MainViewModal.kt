package com.tomer.chitchat.viewmodals

import android.util.Log
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomer.chitchat.assets.RepoAssets
import com.tomer.chitchat.crypto.CipherUtils
import com.tomer.chitchat.crypto.CryptoService
import com.tomer.chitchat.modals.msgs.NewConnection
import com.tomer.chitchat.modals.rv.PersonModel
import com.tomer.chitchat.modals.states.FlowType
import com.tomer.chitchat.modals.states.MsgStatus
import com.tomer.chitchat.modals.states.MsgsFlowState
import com.tomer.chitchat.repo.RepoMessages
import com.tomer.chitchat.repo.RepoPersons
import com.tomer.chitchat.repo.RepoRelations
import com.tomer.chitchat.repo.RepoStorage
import com.tomer.chitchat.repo.RepoUtils
import com.tomer.chitchat.retro.Api
import com.tomer.chitchat.room.ModelRoomPersonRelation
import com.tomer.chitchat.room.ModelRoomPersons
import com.tomer.chitchat.room.MsgMediaType
import com.tomer.chitchat.utils.ConversionUtils
import com.tomer.chitchat.utils.EmojisHashingUtils
import com.tomer.chitchat.utils.Utils
import com.tomer.chitchat.utils.WebSocketHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigInteger
import javax.inject.Inject


@HiltViewModel
class MainViewModal @Inject constructor(
    private val repoUtils: RepoUtils,
    private val repoPersons: RepoPersons,
    private val repoStorage: RepoStorage,
    private val repoAssets: RepoAssets,
    private val repoMsg: RepoMessages,
    private val retro: Api,
    private val repoRelations: RepoRelations,
    private val cryptoService: CryptoService,
    private val webSocket: WebSocketHandler
) : ViewModel() {

    //region WEBSOCK FLOW

    fun closeWebSocket() {
        try {
            webSocket.closeConnection()
        } catch (_: Exception) {
        }
    }

    fun connectNew(phone: String, openNextActivity: Boolean, mandatoryConnect: Boolean = true) {
        if (!mandatoryConnect) {
            viewModelScope.launch {
                if (!phone.isDigitsOnly()) return@launch
                val oldRel = repoRelations.getRelation(phone)
                if (oldRel == null) connectNew(phone, openNextActivity, true)
                else {
                    val oldPersons = repoPersons.getPersonByPhone(phone)
                    if (oldPersons == null)
                        ModelRoomPersons(
                            phone, oldRel.partnerName,
                            MsgMediaType.TEXT, "", -1L,
                            System.currentTimeMillis(),
                            lastSeenMillis = System.currentTimeMillis(),
                            isSent = false,
                            msgStatus = MsgStatus.RECEIVED
                        ).apply { repoPersons.insertPerson(this) }
                    if (openNextActivity)
                        flowMsgs.emit(MsgsFlowState.PartnerEventsFlowState(FlowType.OPEN_NEW_CONNECTION_ACTIVITY, phone))
                }
            }
            return
        }
        viewModelScope.launch {
            if (!phone.isDigitsOnly()) return@launch
            var oldRel = repoRelations.getRelation(phone)
            val relation = ModelRoomPersonRelation(phone, oldRel?.partnerName ?: phone, isConnSent = true, isAccepted = false, isRejected = false)
            Utils.currentPartner = relation
            cryptoService.setCurrentPartner(phone)

            genKeyAndSendNotification(relation)
            if (openNextActivity)
                flowMsgs.emit(MsgsFlowState.PartnerEventsFlowState(FlowType.OPEN_NEW_CONNECTION_ACTIVITY, phone))
            else flowMsgs.emit(MsgsFlowState.PartnerEventsFlowState(FlowType.INCOMING_NEW_CONNECTION_REQUEST, phone))

            //getting Name from server
            oldRel = relation
            val name = if (oldRel.partnerName == oldRel.partnerId) {
                try {
                    retro.getName(phone).body() ?: phone
                } catch (e: Exception) {
                    phone
                }
            } else oldRel.partnerName
            val olRel = repoRelations.getRelation(phone)
            if (olRel != null) {
                olRel.partnerName = name
                repoRelations.saveRelation(olRel)
            }
            ModelRoomPersons(
                phone, name,
                MsgMediaType.TEXT, "Connection request sent...", -1L,
                System.currentTimeMillis(),
                lastSeenMillis = System.currentTimeMillis(),
                isSent = false,
                msgStatus = MsgStatus.RECEIVED
            ).apply { repoPersons.insertPerson(this) }
        }
    }

    private fun genKeyAndSendNotification(relation: ModelRoomPersonRelation) {
        relation.isRejected = false
        repoRelations.saveRelation(relation)
        val key = cryptoService.checkForKeyAndGenerateIfNot(relation.partnerId)
        webSocket.sendMessage(
            "${relation.partnerId}${
                NewConnection(
                    CipherUtils.G.modPow(
                        BigInteger(key.tempKeyMy, 16),
                        CipherUtils.P
                    ).toString(16)
                )
            }"
        )
    }

    val flowMsgs = MutableSharedFlow<MsgsFlowState>()

    init {
        webSocket.openConnection(repoUtils.getToken())
        viewModelScope.launch {
            webSocket.flowMsgs.collectLatest { msg ->
                flowMsgs.emit(msg)
            }
        }
    }

    //endregion WEBSOCK FLOW

    private val _persons = MutableLiveData<List<PersonModel>>()
    val persons: LiveData<List<PersonModel>> = _persons

    //region SELECTION HANDLING

    private val _headMenu = MutableLiveData<Boolean>()
    val headMenu: LiveData<Boolean> = _headMenu

    private val _selCount = MutableLiveData(0)
    val selCount: LiveData<Int> = _selCount

    private val selectedPhoneNos = mutableListOf<String>()

    fun addDelNo(phoneNo: String): Boolean {
        val i = selectedPhoneNos.indexOfFirst { phoneNo == it }
        return if (i == -1) {
            selectedPhoneNos.add(phoneNo)
            true
        } else {
            selectedPhoneNos.removeIf { phoneNo == it }
            false
        }.also {
            _selCount.postValue(selectedPhoneNos.size)
            if (selectedPhoneNos.isEmpty()) {
                if (headMenu.value != false)
                    _headMenu.postValue(false)
            } else
                if (headMenu.value != true)
                    _headMenu.postValue(true)
        }
    }

    fun delSelected(isDel: Boolean, oldList: List<PersonModel>) {
        if (isDel) {
            viewModelScope.launch {
                selectedPhoneNos.forEach {
                    repoPersons.deletePersonById(it)
                }
                loadPersons(oldList)
                withContext(Dispatchers.IO) {
                    selectedPhoneNos.forEach { no ->
                        val msgs = repoMsg.getMsgsOfUser(no)
                        msgs.forEach { msg ->
                            repoStorage.deleteFile(msg.mediaFileName, msg.msgType)
                        }
                        repoMsg.deleteAllByUser(no)
                    }
                }
            }
        }

        selectedPhoneNos.clear()
        _selCount.postValue(0)
        _headMenu.postValue(false)

    }

    //endregion SELECTION HANDLING

    //region LOAD PERSON DATA
    private suspend fun ModelRoomPersons.toUi(
        oldList: List<PersonModel>,
        needTobeDownload: MutableList<Pair<Int, String>>,
    ): PersonModel {
        val builder = PersonModel.Builder()
        builder.lastMsgId(lastMsgId)
        builder.name(name)
        builder.phoneNumber(phoneNo)
        builder.messageMediaType(mediaType)
        builder.lastDate(ConversionUtils.getRelativeTime(timeMillis))
        builder.lastMessage(lastMsg)
        builder.unreadCount(unReadCount)
        builder.isSelected(false)
        builder.isOnline(lastSeenMillis == -1L)
        builder.isSent(isSent)
        builder.msgStatus(msgStatus)


        val prevSel = oldList.find { it.phoneNo == phoneNo }
        if (prevSel != null) builder.isSelected(prevSel.isSelected)

        val old = oldList.find { it.lastMsgId == lastMsgId }
        if (old != null) {
            builder.lastMessageFile(old.fileGifImg)
            builder.jsonText(old.jsonText)
            builder.jsonName(old.jsonName)
            return builder.build()
        }

        if (mediaType == MsgMediaType.EMOJI) {

            val nameGoogleJson = EmojisHashingUtils.googleJHash[ConversionUtils.encode(lastMsg)]
            if (!nameGoogleJson.isNullOrEmpty()) {
                builder.jsonText(repoAssets.getLottieJson(nameGoogleJson, true).also {
                    if (it == null) needTobeDownload.add(Pair(0, nameGoogleJson))
                } ?: "")
                builder.jsonName(nameGoogleJson)
                return builder.build()
            }

            val nameJson = EmojisHashingUtils.jHash[ConversionUtils.encode(lastMsg)]
            if (!nameJson.isNullOrEmpty()) {
                builder.jsonText(repoAssets.getLottieJson(nameJson, true).also {
                    if (it == null) needTobeDownload.add(Pair(1, nameJson))
                } ?: "")
                builder.jsonName(nameJson)
                return builder.build()
            }

            val nameGif = EmojisHashingUtils.gHash[ConversionUtils.encode(lastMsg)]
            if (!nameGif.isNullOrEmpty()) {
                builder.lastMessageFile(repoAssets.getGifFile(nameGif, true).also {
                    if (it == null) needTobeDownload.add(Pair(2, nameGif))
                })
                return builder.build()
            }

            val nameTeleGif = EmojisHashingUtils.teleHash[ConversionUtils.encode(lastMsg)]
            if (!nameTeleGif.isNullOrEmpty()) {
                builder.lastMessageFile(repoAssets.getGifFile(ConversionUtils.encode(nameTeleGif), true).also {
                    if (it == null) needTobeDownload.add(Pair(3, ConversionUtils.encode(nameTeleGif)))
                })
                return builder.build()
            }

        } else if (mediaType == MsgMediaType.IMAGE || mediaType == MsgMediaType.GIF) {
            val msg = repoMsg.getMsg(lastMsgId) ?: return builder.messageMediaType(MsgMediaType.TEXT).build()
            val file = repoStorage.getFileFromFolder(mediaType, msg.mediaFileName.toString())
            if (file == null)
                builder.jsonText(msg.msgText.split(",-,")[1])
            else builder.lastMessageFile(file)

        }

        return builder.build()
    }

    fun loadPersons(oldList: List<PersonModel>) {
        viewModelScope.launch {
            val needTobeDownload = mutableListOf<Pair<Int, String>>()
            val per = repoPersons.getAllPersons().map { it.toUi(oldList, needTobeDownload) }
            Log.d("TAG--", "loadPersons: $per")
            _persons.postValue(per)
            for (i in needTobeDownload) {
                async {
                    when (i.first) {
                        0 -> repoAssets.getGoogleLottieJson(i.second)
                        1 -> repoAssets.getLottieJson(i.second)
                        2 -> repoAssets.getGifFile(i.second)
                        else -> repoAssets.getGifTelemoji(i.second)
                    }
                }
            }
        }
    }
    //endregion LOAD PERSON DATA
}