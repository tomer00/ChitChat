package com.tomer.chitchat.viewmodals

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomer.chitchat.modals.prefs.MyPrefs
import com.tomer.chitchat.repo.RepoMessages
import com.tomer.chitchat.repo.RepoPersons
import com.tomer.chitchat.repo.RepoStorage
import com.tomer.chitchat.repo.RepoUtils
import com.tomer.chitchat.room.ModelPartnerPref
import com.tomer.chitchat.room.MsgMediaType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsPartnerPrefViewModel @Inject constructor(
    private val repoMsgs: RepoMessages,
    private val repoStorage: RepoStorage,
    private val repoPersons: RepoPersons,
    repoUtils: RepoUtils,
) : ViewModel() {

    //region STATES

    private val _myPrefs = MutableLiveData<ModelPartnerPref>()
    val myPrefs: LiveData<ModelPartnerPref> = _myPrefs

    private val _sharedContent = MutableLiveData<List<File>>(listOf())
    val sharedContent: LiveData<List<File>> = _sharedContent

    //endregion STATES

    val myPref: MyPrefs = repoUtils.getPrefs()

    fun loadPref(phone: String) {
        viewModelScope.launch {
            val pref = repoPersons.getPersonPref(phone) ?: return@launch
            _myPrefs.postValue(pref)
        }
        if (_sharedContent.value?.isEmpty() == true)
            viewModelScope.launch {
                val listFiles = mutableListOf<File>()
                repoMsgs.getMsgsOfUser(phone).filter { it.msgType == MsgMediaType.GIF || it.msgType == MsgMediaType.IMAGE }.forEach {
                    repoStorage.getFileFromFolder(it.msgType, it.mediaFileName ?: "")?.let { file -> listFiles.add(file) }
                }
                _sharedContent.postValue(listFiles)
            }
    }

    fun setNotification(show: Boolean) {
        viewModelScope.launch {
            _myPrefs.value?.let {
                it.notificationAllowed = show
                repoPersons.insertPersonPref(it)
            }
        }
    }

    fun setChatLock(locked: Boolean) {
        viewModelScope.launch {
            _myPrefs.value?.let {
                it.chatLocked = locked
                repoPersons.insertPersonPref(it)
            }
        }
    }

}