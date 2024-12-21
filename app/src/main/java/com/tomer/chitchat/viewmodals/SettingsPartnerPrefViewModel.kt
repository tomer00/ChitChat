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
import com.tomer.chitchat.utils.qrProvider.GradModel
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

    private val _sharedContent = MutableLiveData<List<Pair<Boolean, File>>>(listOf())
    val sharedContent: LiveData<List<Pair<Boolean, File>>> = _sharedContent

    //endregion STATES

    //region THEME DATA

    val rvDoodle = listOf(
        1 to RenderModel(1f, Color.GREEN, GradModel(120, Color.parseColor("#a6c0fe"), Color.parseColor("#f68084"))),
        2 to RenderModel(1f, Color.GREEN, GradModel(45, Color.parseColor("#cf5e0c"), Color.parseColor("#ee0979"))),
        3 to RenderModel(1f, Color.GREEN, GradModel(90, Color.parseColor("#642b73"), Color.parseColor("#c6426e"))),
        4 to RenderModel(1f, Color.GREEN, GradModel(45, Color.parseColor("#cb356b"), Color.parseColor("#bd3f32"))),
        5 to RenderModel(1f, Color.GREEN, GradModel(125, Color.parseColor("#283c86"), Color.parseColor("#45a247"))),
        6 to RenderModel(1f, Color.GREEN, GradModel(100, Color.parseColor("#4b639f"), Color.parseColor("#5baeb4"))),
        7 to RenderModel(1f, Color.GREEN, GradModel(15, Color.parseColor("#cf5e0c"), Color.parseColor("#ee0979"))),
        8 to RenderModel(1f, Color.GREEN, GradModel(45, Color.parseColor("#0f3443"), Color.parseColor("#34e89e"))),
        9 to RenderModel(1f, Color.GREEN, GradModel(125, Color.parseColor("#935096"), Color.parseColor("#4568dc"))),
        10 to RenderModel(1f, Color.GREEN, GradModel(95, Color.parseColor("#cf5e0c"), Color.parseColor("#ee0979"))),
        11 to RenderModel(1f, Color.GREEN, GradModel(25, Color.parseColor("#fefdcd"), Color.parseColor("#a3e6ff"))),
        12 to RenderModel(1f, Color.GREEN, GradModel(220, Color.parseColor("#bdc3c7"), Color.parseColor("#2c3e50"))),
    )

    val rvAccent = listOf(
        RenderModel(1f, Color.parseColor("#005feb")),
        RenderModel(1f, Color.parseColor("#28a359")),
        RenderModel(1f, Color.GREEN, GradModel(100, Color.parseColor("#6573f8"), Color.parseColor("#a751a8"))),
        RenderModel(1f, Color.parseColor("#8366cc")),
        RenderModel(1f, Color.GREEN, GradModel(30, Color.parseColor("#2f0743"), Color.parseColor("#41295a"))),
        RenderModel(1f, Color.GREEN, GradModel(45, Color.parseColor("#358669"), Color.parseColor("#4d8f51"))),
        RenderModel(1f, Color.parseColor("#d28036")),
        RenderModel(1f, Color.GREEN, GradModel(100, Color.parseColor("#4b639f"), Color.parseColor("#5baeb4"))),
    )

    val rvBgRenders = listOf(
        RenderModel(1f, Color.GREEN, GradModel(120, Color.parseColor("#a6c0fe"), Color.parseColor("#f68084"))),
        RenderModel(1f, Color.GREEN, GradModel(45, Color.parseColor("#cf5e0c"), Color.parseColor("#ee0979"))),
        RenderModel(1f, Color.GREEN, GradModel(90, Color.parseColor("#642b73"), Color.parseColor("#c6426e"))),
        RenderModel(1f, Color.parseColor("#c678dd")),
        RenderModel(1f, Color.GREEN, GradModel(125, Color.parseColor("#283c86"), Color.parseColor("#45a247"))),
        RenderModel(1f, Color.GREEN, GradModel(15, Color.parseColor("#cf5e0c"), Color.parseColor("#ee0979"))),
        RenderModel(1f, Color.GREEN, GradModel(45, Color.parseColor("#0f3443"), Color.parseColor("#34e89e"))),
        RenderModel(1f, Color.GREEN, GradModel(80, Color.parseColor("#eacda3"), Color.parseColor("#d6ae7b"))),
        RenderModel(1f, Color.GREEN, GradModel(95, Color.parseColor("#cf5e0c"), Color.parseColor("#ee0979"))),
        RenderModel(1f, Color.GREEN, GradModel(45, Color.parseColor("#cb356b"), Color.parseColor("#bd3f32"))),
        RenderModel(1f, Color.GREEN, GradModel(125, Color.parseColor("#935096"), Color.parseColor("#4568dc"))),
        RenderModel(1f, Color.GREEN, GradModel(100, Color.parseColor("#4b639f"), Color.parseColor("#5baeb4"))),
        RenderModel(1f, Color.parseColor("#d28036")),
        RenderModel(1f, Color.GREEN, GradModel(25, Color.parseColor("#fefdcd"), Color.parseColor("#a3e6ff"))),
        RenderModel(1f, Color.GREEN, GradModel(220, Color.parseColor("#bdc3c7"), Color.parseColor("#2c3e50"))),
        RenderModel(1f, Color.GREEN, GradModel(125, Color.parseColor("#f7ff00"), Color.parseColor("#db36a4"))),
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
                val listFiles = mutableListOf<Pair<Boolean, File>>()
                repoMsgs.getMsgsOfUser(phone).filter { it.msgType == MsgMediaType.GIF || it.msgType == MsgMediaType.IMAGE }.forEach {
                    repoStorage.getFileFromFolder(it.msgType, it.mediaFileName ?: "")?.let { file -> listFiles.add((it.msgType == MsgMediaType.GIF) to file) }
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

    fun setBackGround(assetNo: Int, renderModel: RenderModel?) {
        isChanged = true
        val pref = _partnerPref.value ?: return
        pref.backgroundAssetNo = assetNo
        if (renderModel != null)
            pref.background = renderModel
        viewModelScope.launch {
            repoPersons.insertPersonPref(pref)
            _partnerPref.postValue(pref)
            _transparency.postValue(1f)
        }
    }

    fun setBackGroundIndex(index: Int) {
        isChanged = true
        val pref = _partnerPref.value ?: return
        pref.background = rvBgRenders.getOrNull(index) ?: RenderModel(1f, Color.parseColor("#005feb"))
        viewModelScope.launch {
            repoPersons.insertPersonPref(pref)
            _partnerPref.postValue(pref)
            _transparency.postValue(1f)
        }
    }

    fun setAccent(index: Int) {
        isChanged = true
        val pref = _partnerPref.value ?: return
        pref.accent = rvAccent.getOrNull(index) ?: RenderModel(1f, Color.parseColor("#005feb"))
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
