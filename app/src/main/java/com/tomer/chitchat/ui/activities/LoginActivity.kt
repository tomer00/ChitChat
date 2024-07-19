package com.tomer.chitchat.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.tomer.chitchat.R
import com.tomer.chitchat.databinding.ActivityLoginBinding
import com.tomer.chitchat.ui.frags.FragSendOtp
import com.tomer.chitchat.ui.frags.FragUpdateProfile
import com.tomer.chitchat.ui.frags.FragVerifyOtp
import com.tomer.chitchat.viewmodals.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {


    private val b by lazy { ActivityLoginBinding.inflate(layoutInflater) }
    private val viewModal: LoginViewModel by viewModels()

    private val launcher: ActivityResultLauncher<PickVisualMediaRequest> =
        registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) {
            if (it!=null)
                viewModal.imgPicked(it)
        }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = ContextCompat.getColor(this, R.color.softBg)
        setContentView(b.root)

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
            if (it)
                supportFragmentManager.beginTransaction().replace(b.fragCont.id, FragUpdateProfile()).commit()
        }

        viewModal.showSelGallery.observe(this) {
            if (it) {
                launcher.launch(
                    PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        .build()
                )
            }
        }

        viewModal.done.observe(this) {
            if (it) {
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