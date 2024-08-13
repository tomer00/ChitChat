package com.tomer.chitchat.viewmodals

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomer.chitchat.assets.RepoAssets
import com.tomer.chitchat.modals.rv.PersonModel
import com.tomer.chitchat.repo.RepoMessages
import com.tomer.chitchat.repo.RepoPersons
import com.tomer.chitchat.repo.RepoStorage
import com.tomer.chitchat.room.ModelRoomPersons
import com.tomer.chitchat.room.MsgMediaType
import com.tomer.chitchat.utils.ConversionUtils
import com.tomer.chitchat.utils.EmojisHashingUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MainViewModal @Inject constructor(
    private val repoPersons: RepoPersons,
    private val repoStorage: RepoStorage,
    private val repoAssets: RepoAssets,
    private val repoMsg: RepoMessages,
) : ViewModel() {

    private val _persons = MutableLiveData<List<PersonModel>>()
    val persons: LiveData<List<PersonModel>> = _persons

    private suspend fun ModelRoomPersons.toUi(oldList: List<PersonModel>): PersonModel {
        val builder = PersonModel.Builder()
        builder.lastMsgId(lastMsgId)
        builder.name(name)
        builder.phoneNumber(phoneNo)
        builder.messageMediaType(mediaType)
        builder.lastDate(ConversionUtils.millisToTimeText(timeMillis))
        builder.lastMessage(lastMsg)
        builder.unreadCount(unReadCount)
        builder.isOnline(lastSeenMillis == -1L)


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
                builder.jsonText(repoAssets.getLottieJson(nameGoogleJson) ?: "")
                builder.jsonName(nameGoogleJson)
                return builder.build()
            }

            val nameJson = EmojisHashingUtils.jHash[ConversionUtils.encode(lastMsg)]
            if (!nameJson.isNullOrEmpty()) {
                builder.jsonText(repoAssets.getLottieJson(nameJson) ?: "")
                builder.jsonName(nameJson)
                return builder.build()
            }

            val nameGif = EmojisHashingUtils.gHash[ConversionUtils.encode(lastMsg)]
            if (!nameGif.isNullOrEmpty()) {
                builder.lastMessageFile(repoAssets.getGifFile(nameGif))
                return builder.build()
            }

            val nameTeleGif = EmojisHashingUtils.teleHash[ConversionUtils.encode(lastMsg)]
            if (!nameTeleGif.isNullOrEmpty()) {
                builder.lastMessageFile(repoAssets.getGifFile(nameTeleGif))
                return builder.build()
            }

        } else if (mediaType == MsgMediaType.IMAGE || mediaType == MsgMediaType.GIF) {
            val msg = repoMsg.getMsg(lastMsgId) ?: return builder.messageMediaType(MsgMediaType.TEXT).build()
            val file = repoStorage.getFileFromFolder(mediaType, msg.mediaFileName.toString())
            if (file == null)
                builder.jsonText(msg.msgText.split(",-,")[1].also { Log.d("TAG--", "toUi: $it" ) })
            else builder.lastMessageFile(file)

        }

        return builder.build()
    }

    fun loadPersons(oldList: List<PersonModel>) {
        viewModelScope.launch {
            val per = repoPersons.getAllPersons().map { it.toUi(oldList) }
            Log.d("TAG--", "loadPersons: $per")
            _persons.postValue(per)
        }
    }
}