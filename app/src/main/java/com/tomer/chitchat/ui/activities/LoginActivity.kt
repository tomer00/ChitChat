package com.tomer.chitchat.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import com.tomer.chitchat.R
import com.tomer.chitchat.databinding.ActivityLoginBinding
import com.tomer.chitchat.ui.frags.FragSendOtp
import com.tomer.chitchat.ui.frags.FragVerifyOtp
import com.tomer.chitchat.viewmodals.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {


    private val b by lazy { ActivityLoginBinding.inflate(layoutInflater) }
    private val viewModal: LoginViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        supportFragmentManager.beginTransaction().replace(b.fragCont.id, FragSendOtp()).commit()


        viewModal.phone.observe(this) { phone ->
            if (phone.length == 10) {
                supportFragmentManager.beginTransaction().replace(b.fragCont.id, FragVerifyOtp()).commit()
                sendOtp(viewModal.phone.value.toString(), false)
            }
        }

        viewModal.reSend.observe(this) {
            if (it)
                sendOtp(viewModal.phone.value.toString(), true)
        }

        viewModal.loggedIn.observe(this) {
            if (it){
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }


    }

    private fun sendOtp(phone: String, isResend: Boolean) {
        val builder = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber("+91$phone")
            .setTimeout(2, TimeUnit.MINUTES)
            .setActivity(this)
            .setCallbacks(viewModal.phoneCallback)
        if (isResend) PhoneAuthProvider.verifyPhoneNumber(builder.setForceResendingToken(viewModal.resendingToken).build())
        else PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

}