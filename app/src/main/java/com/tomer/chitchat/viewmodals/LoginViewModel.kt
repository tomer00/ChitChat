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
import com.tomer.chitchat.repo.RepoUtils
import com.tomer.chitchat.retro.Api
import com.tomer.chitchat.utils.ConversionUtils
import com.tomer.chitchat.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class LoginViewModel @Inject constructor(private val retro: Api, private val repo: RepoUtils) : ViewModel() {

    private val _phone = MutableLiveData<String>()
    val phone: LiveData<String> = _phone

    private val _reSend = MutableLiveData<Boolean>()
    val reSend: LiveData<Boolean> = _reSend

    private val _codeSend = MutableLiveData<Boolean>()
    val codeSend: LiveData<Boolean> = _codeSend

    private val _loginProg = MutableLiveData<Boolean>()
    val loginProg: LiveData<Boolean> = _loginProg

    private val _loggedIn = MutableLiveData<Boolean>()
    val loggedIn: LiveData<Boolean> = _loggedIn

    private val _showSelGallery = MutableLiveData<Boolean>()
    val showSelGallery: LiveData<Boolean> = _showSelGallery

    private val _done = MutableLiveData<Boolean>()
    val done: LiveData<Boolean> = _done

    private val _selectedImg = MutableLiveData<Uri>()
    val selectedImg: LiveData<Uri> = _selectedImg

    var name = ""

    fun setOtp(otp: String) {
        signInWithPhoneAuthCredential(otp)
        _loginProg.value = true
    }

    fun showGallery() = _showSelGallery.postValue(true)
    fun imgPicked(uri: Uri) = _selectedImg.postValue(uri)

    suspend fun uploadImage(bytes: ByteArray) {
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
        }catch (e:Exception){
            Log.e("TAG--", "uploadImage: ", e)
        }
    }

    fun setReSend(reSend: Boolean) {
        _reSend.value = reSend
    }

    fun setPhone(phone: String) {
        _phone.value = phone
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
            }

            override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                _loggedIn.postValue(true)
            }

            override fun onVerificationFailed(p0: FirebaseException) {
                _codeSend.value = false
                _loginProg.value = false
            }
        }
    }

    fun askForVariousPermissions() {
        
    }

    fun login(name: String, bmp: Bitmap?) {
        _loginProg.postValue(true)
        viewModelScope.launch {
            val res1 = async {
                retro.updateName(name)
            }
            if (bmp == null) {
                _loginProg.value = false
                _done.postValue(true)
                return@launch
            }
            val res2 = async {
                uploadImage(ConversionUtils.convertToWebp(bmp))
            }
            res2.await()
            res1.await()
            _loginProg.value = false
            _done.postValue(true)
        }
    }

    private fun signInWithPhoneAuthCredential(otp: String) {
        FirebaseAuth.getInstance().signInWithCredential(PhoneAuthProvider.getCredential(verificationId, otp))
            .addOnCompleteListener { task: Task<AuthResult> ->
                if (task.isSuccessful) {
                    viewModelScope.launch {
                        withContext(Dispatchers.IO) {
                            val firebaseToken = getFirebaseToken()
                            try {
                                Log.d("TAG--", "Firesse: $firebaseToken")
                                val token = retro.getLoginToken(firebaseToken)
                                Log.d("TAG--", "signInWithPhoneAuthCredential: $token")
                                if (token.isSuccessful) {
                                    token.body()?.let {
                                        repo.saveToken(it.token)
                                        name = it.name

                                        Log.d("TAG--", "Save token: ${it.token}  ${it.name}")
                                    }
                                    _loggedIn.postValue(true)
                                    _loginProg.postValue(false)
                                } else {
                                    Log.d("TAG--", "signInWithPhoneAuthCredential: ${token.message()}")
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

    private suspend fun getFirebaseToken(): String {
        return suspendCoroutine { continuation ->
            FirebaseAuth.getInstance().currentUser!!.getIdToken(true)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful)
                        continuation.resumeWith(Result.success(task.result.token!!))
                    else continuation.resumeWith(Result.failure(task.exception!!))
                }

        }
    }
}