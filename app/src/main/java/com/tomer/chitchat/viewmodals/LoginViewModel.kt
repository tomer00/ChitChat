package com.tomer.chitchat.viewmodals

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.messaging.FirebaseMessaging
import com.tomer.chitchat.repo.RepoUtils
import com.tomer.chitchat.retro.Api
import com.tomer.chitchat.utils.ConversionUtils
import com.tomer.chitchat.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val retro: Api,
    private val repo: RepoUtils
) : ViewModel() {

    private val _phone = MutableLiveData<String>()
    val phone: LiveData<String> = _phone

    private val _reSend = MutableLiveData<Boolean>()
    val reSend: LiveData<Boolean> = _reSend

    private val _sendOtp = MutableLiveData<Boolean>()
    val sendOtp: LiveData<Boolean> = _sendOtp

    private val _codeSend = MutableLiveData<Boolean>()
    val codeSend: LiveData<Boolean> = _codeSend

    private val _loginProg = MutableLiveData<Boolean>()
    val loginProg: LiveData<Boolean> = _loginProg

    private val _showSelGallery = MutableLiveData<Boolean>()
    val showSelGallery: LiveData<Boolean> = _showSelGallery

    private val _done = MutableLiveData<Boolean>()
    val done: LiveData<Boolean> = _done

    private val _canAuth = MutableLiveData<Boolean>()
    val canAuth: LiveData<Boolean> = _canAuth

    private val _selectedImg = MutableLiveData<Uri>()
    val selectedImg: LiveData<Uri> = _selectedImg

    private val _resendOtpButton = MutableLiveData(false)
    val resendOtpButton: LiveData<Boolean> = _resendOtpButton

    private val _resendOtpTimer = MutableLiveData(40)
    val resendOtpTimer: LiveData<Int> = _resendOtpTimer

    private val _currentFrag = MutableLiveData(1)
    val currentFrag: LiveData<Int> = _currentFrag

    val flowToasts = MutableSharedFlow<String>()

    var name = ""
    var otpTemp = ""
    private var jobTimer: Job = viewModelScope.launch { ; }

    init {
        viewModelScope.launch {
            val cA = try {
                retro.canAuth().body().equals("true")
            } catch (e: Exception) {
                false
            }
            _canAuth.postValue(cA)
        }
    }

    fun setOtp(otp: String) {
        signInWithPhoneAuthCredential(otp)
        _loginProg.value = true
    }

    fun showGallery(bool: Boolean) = _showSelGallery.postValue(bool)
    fun imgPicked(uri: Uri) = _selectedImg.postValue(uri)

    private suspend fun uploadImage(bytes: ByteArray) {
        val reqBody = bytes
            .toRequestBody(
                "multipart/form-data".toMediaTypeOrNull(),
                0, bytes.size
            )
        try {
            retro.uploadProfileImage(
                MultipartBody.Part.createFormData(
                    "file", Utils.myPhone, reqBody
                )
            )
        } catch (_: Exception) {
        }
    }

    fun setReSend(reSend: Boolean) {
        _reSend.value = reSend
        _loginProg.value = true
    }
    fun setSendOtp(sendOtp: Boolean) {
        _sendOtp.value = sendOtp
    }

    fun setCurrentFrag(no: Int) {
        _currentFrag.postValue(no)
    }

    fun setPhone(phone: String, viaClick: Boolean) {
        _phone.value = phone
        if (viaClick) _sendOtp.postValue(true)
    }

    lateinit var resendingToken: PhoneAuthProvider.ForceResendingToken
    var verificationId: String = ""
    val phoneCallback by lazy {
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onCodeSent(p0: String, p1: PhoneAuthProvider.ForceResendingToken) {
                super.onCodeSent(p0, p1)
                resendingToken = p1
                verificationId = p0
                _codeSend.value = true
                _loginProg.value = false
                jobTimer.cancel()
                jobTimer = createNewCountDownJob()
                _reSend.postValue(false)
            }

            override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                _currentFrag.postValue(3)
                jobTimer.cancel()
                _reSend.postValue(false)
            }

            override fun onVerificationFailed(p0: FirebaseException) {
                Log.e("TAG--", "onVerificationFailed: ", p0)
                _codeSend.value = false
                _loginProg.value = false
                viewModelScope.launch { flowToasts.emit("Something went wrong...") }
                _reSend.postValue(false)
            }
        }
    }

    private var currentRemTime = 40
    private fun createNewCountDownJob(): Job {
        currentRemTime = 40
        _resendOtpButton.postValue(false)
        return viewModelScope.launch {
            while (currentRemTime != 0) {
                delay(1000)
                _resendOtpTimer.postValue(--currentRemTime)
            }
            _resendOtpButton.postValue(true)
            _resendOtpTimer.postValue(40)
        }
    }

    fun login(name: String, bmp: Bitmap?) {
        _loginProg.postValue(true)
        repo.saveName(name)
        Utils.myName = name
        viewModelScope.launch {
            val res1 = async {
                retro.updateName(name)
            }
            if (bmp == null) {
                res1.await()
                _loginProg.value = false
                _done.postValue(true)
                return@launch
            }
            val res2 = async {
                uploadImage(ConversionUtils.convertToWebp(bmp))
            }
            res2.await()
            res1.await()
            _loginProg.postValue(false)
            _done.postValue(true)
        }
    }


    private fun signInWithPhoneAuthCredential(otp: String) {
        FirebaseAuth.getInstance().signInWithCredential(PhoneAuthProvider.getCredential(verificationId, otp))
            .addOnFailureListener {
                viewModelScope.launch { flowToasts.emit("Please Provide Valid OTP") }
                _loginProg.postValue(false)
            }
            .addOnCompleteListener { task: Task<AuthResult> ->
                if (task.isSuccessful) {
                    viewModelScope.launch {
                        withContext(Dispatchers.IO) {
                            val firebaseAuthToken = async { getFirebaseAuthToken() }
                            val firebaseNotificationToken = async { getFirebaseNotificationToken() }
                            try {
                                val token = retro.getLoginToken(firebaseAuthToken.await(), firebaseNotificationToken.await())
                                if (token.isSuccessful) {
                                    token.body()?.let {
                                        repo.saveToken(it.token)
                                        name = it.name
                                    }
                                    _currentFrag.postValue(3)
                                    jobTimer.cancel()
                                    _loginProg.postValue(false)
                                } else {
                                    _loginProg.postValue(false)
                                }
                            } catch (e: Exception) {
                                Log.e("TAG--", "signInWithPhoneAuthCredential: ", e)
                                _loginProg.postValue(false)
                            }
                        }
                    }
                }
            }
    }

    private suspend fun getFirebaseAuthToken(): String {
        return suspendCoroutine { continuation ->
            FirebaseAuth.getInstance().currentUser!!.getIdToken(true)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful)
                        continuation.resumeWith(Result.success(task.result.token!!))
                    else continuation.resumeWith(Result.failure(task.exception!!))
                }

        }
    }

    private suspend fun getFirebaseNotificationToken(): String {
        return suspendCoroutine { continuation ->
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (task.isSuccessful)
                        continuation.resumeWith(Result.success(task.result))
                    else continuation.resumeWith(Result.failure(task.exception!!))
                }

        }
    }
}