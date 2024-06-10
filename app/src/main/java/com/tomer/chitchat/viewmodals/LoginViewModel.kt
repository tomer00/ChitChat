package com.tomer.chitchat.viewmodals

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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
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

    fun setOtp(otp: String) {
        signInWithPhoneAuthCredential(otp)
        _loginProg.value = true
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
                Log.d("TAG--", "onCodeSent: $verificationId")
                _codeSend.value = true
            }

            override fun onVerificationCompleted(p0: PhoneAuthCredential) {

            }

            override fun onVerificationFailed(p0: FirebaseException) {
                _codeSend.value = false
                _loginProg.value = false
            }
        }
    }

    private fun signInWithPhoneAuthCredential(otp: String) {
        FirebaseAuth.getInstance().signInWithCredential(PhoneAuthProvider.getCredential(verificationId, otp))
            .addOnCompleteListener { task: Task<AuthResult> ->
                if (task.isSuccessful) {
                    viewModelScope.launch {
                        val firebaseToken = getFirebaseToken()
                        try{

                        val token = retro.getLoginToken(firebaseToken)
                        Log.d("TAG--", "signInWithPhoneAuthCredential: $token ")
                        repo.saveToken(token)
                        }catch (e: Exception){
                            e.printStackTrace()
                        }
                    }
                }
            }
    }

    private suspend fun getFirebaseToken(): String {
        return suspendCoroutine { continuation ->
            FirebaseAuth.getInstance().currentUser!!.getIdToken(true)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resumeWith(Result.success(task.result.token!!))
                    }else continuation.resumeWith(Result.failure(task.exception!!))
                }

        }
    }
}