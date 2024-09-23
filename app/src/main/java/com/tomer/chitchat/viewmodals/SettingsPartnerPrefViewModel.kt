package com.tomer.chitchat.viewmodals

import android.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomer.chitchat.modals.prefs.MyPrefs
import com.tomer.chitchat.modals.prefs.PartnerPrefBuilder
import com.tomer.chitchat.repo.RepoMessages
import com.tomer.chitchat.repo.RepoPersons
import com.tomer.chitchat.repo.RepoStorage
import com.tomer.chitchat.repo.RepoUtils
import com.tomer.chitchat.room.ModelPartnerPref
import com.tomer.chitchat.room.MsgMediaType
import com.tomer.chitchat.utils.qrProvider.RenderModel
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

    var isChanged = false

    //region STATES

    private val _partnerPref = MutableLiveData<ModelPartnerPref>()
    val partnerPref: LiveData<ModelPartnerPref> = _partnerPref

    private val _transparency = MutableLiveData(1f)
    val transparency: LiveData<Float> = _transparency

    private val _sharedContent = MutableLiveData<List<File>>(listOf())
    val sharedContent: LiveData<List<File>> = _sharedContent

    //endregion STATES

    //region THEME DATA

    val rvDoodle = listOf(
        1 to RenderModel(1f, Color.GREEN),
        2 to RenderModel(1f, Color.GREEN),
        3 to RenderModel(1f, Color.GREEN),
        4 to RenderModel(1f, Color.GREEN),
        5 to RenderModel(1f, Color.GREEN),
        6 to RenderModel(1f, Color.GREEN),
        7 to RenderModel(1f, Color.GREEN),
        8 to RenderModel(1f, Color.GREEN),
        9 to RenderModel(1f, Color.GREEN),
        10 to RenderModel(1f, Color.GREEN),
    )

    val rvAccent = listOf(
        RenderModel(1f, Color.GREEN),
        RenderModel(1f, Color.RED),
        RenderModel(1f, Color.DKGRAY),
        RenderModel(1f, Color.YELLOW),
        RenderModel(1f, Color.GREEN),
        RenderModel(1f, Color.BLUE),
        RenderModel(1f, Color.GREEN),
        RenderModel(1f, Color.GREEN),
        RenderModel(1f, Color.GREEN),
        RenderModel(1f, Color.GREEN),
    )

    //endregion THEME DATA

    val myPref: MyPrefs = repoUtils.getPrefs()
    var phone = ""

    fun loadPref(phone: String) {
        this.phone = phone
        viewModelScope.launch {
            val pref = repoPersons.getPersonPref(phone) ?: PartnerPrefBuilder(phone).build()
            _partnerPref.postValue(pref)
            _transparency.postValue(pref.background.alpha)
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

    fun setDimming(value: Float) {
        isChanged = true
        val pref = _partnerPref.value ?: return
        pref.background.alpha = value
        _transparency.postValue(value)
        viewModelScope.launch {
            repoPersons.insertPersonPref(pref)
        }
    }

    fun setBackGround(assetNo: Int) {
        isChanged = true
        val pref = _partnerPref.value ?: return
        pref.backgroundAssetNo = assetNo
        viewModelScope.launch {
            repoPersons.insertPersonPref(pref)
            _partnerPref.postValue(pref)
        }
    }

    fun setAccent(index: Int) {
        isChanged = true
        val pref = _partnerPref.value ?: return
        pref.accent = rvAccent.getOrNull(index) ?: RenderModel(1f, Color.RED)
        viewModelScope.launch {
            repoPersons.insertPersonPref(pref)
            _partnerPref.postValue(pref)
        }
    }

    fun setNotification(show: Boolean) {
        viewModelScope.launch {
            _partnerPref.value?.let {
                it.notificationAllowed = show
                repoPersons.insertPersonPref(it)
            }
        }
    }

    fun setChatLock(locked: Boolean) {
        viewModelScope.launch {
            _partnerPref.value?.let {
                it.chatLocked = locked
                repoPersons.insertPersonPref(it)
            }
        }
    }

}