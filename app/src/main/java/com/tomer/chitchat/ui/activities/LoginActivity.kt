package com.tomer.chitchat.ui.activities

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.tomer.chitchat.R
import com.tomer.chitchat.databinding.ActivityLoginBinding
import com.tomer.chitchat.ui.frags.FragSendOtp
import com.tomer.chitchat.ui.frags.FragUpdateProfile
import com.tomer.chitchat.ui.frags.FragVerifyOtp
import com.tomer.chitchat.utils.SmsReceiver
import com.tomer.chitchat.utils.Utils.Companion.isPermissionGranted
import com.tomer.chitchat.viewmodals.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {


    private val b by lazy { ActivityLoginBinding.inflate(layoutInflater) }
    private val viewModal: LoginViewModel by viewModels()

    private val launcher: ActivityResultLauncher<PickVisualMediaRequest> =
        registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) {
            viewModal.showGallery(false)
            if (it != null)
                viewModal.imgPicked(it)
        }

    override fun onDestroy() {
        super.onDestroy()
        unRegister()
    }

    private var currFrag: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = ContextCompat.getColor(this, R.color.backgroundC)
        setContentView(b.root)

        viewModal.sendOtp.observe(this) {
            if (it && viewModal.phone.value?.length == 10)
                sendOtp(viewModal.phone.value.toString(), false)
        }

        viewModal.reSend.observe(this) {
            if (it)
                sendOtp(viewModal.phone.value.toString(), true)
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

        viewModal.currentFrag.observe(this) { no ->
            when (no) {
                1 -> supportFragmentManager.beginTransaction().replace(b.fragCont.id, FragSendOtp().also { currFrag = it }).commit()
                2 -> supportFragmentManager.beginTransaction().replace(b.fragCont.id, FragVerifyOtp().also { currFrag = it }).commit()
                else -> {
                    supportFragmentManager.beginTransaction().replace(b.fragCont.id, FragUpdateProfile().also { currFrag = it }).commit()
                    unRegister()
                }
            }
        }

        lifecycleScope.launch {
            viewModal.flowToasts.collectLatest {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, it, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModal.done.observe(this) {
            if (it) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.READ_SMS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_PHONE_NUMBERS,
                Manifest.permission.RECEIVE_SMS,
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(p0: MultiplePermissionsReport) {
                }

                override fun onPermissionRationaleShouldBeShown(p0: MutableList<PermissionRequest>?, p1: PermissionToken?) {
                }
            }).check()

    }

    private fun sendOtp(phone: String, isResend: Boolean) {
        registerSmsReceiver()
        if (!isResend)
            viewModal.setCurrentFrag(2)
        viewModal.setSendOtp(false)
        viewModal.setReSend(false)
        val builder = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber("+91$phone")
            .setTimeout(2, TimeUnit.MINUTES)
            .setActivity(this)
            .setCallbacks(viewModal.phoneCallback)
        if (isResend) PhoneAuthProvider.verifyPhoneNumber(builder.setForceResendingToken(viewModal.resendingToken).build())
        else PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    //region SMS BROADCAST RECEIVER

    private val reciverOtp = SmsReceiver().apply {
        setOnOtpRececived {
            try{
                val otp = it.subSequence(0, 6)
                if (otp.isDigitsOnly()) {
                    if (currFrag is FragVerifyOtp) {
                        viewModal.otpTemp = otp.toString()
                        (currFrag as FragVerifyOtp).setTextMobile()
                    }
                }
            }catch (_:Exception){}
        }
    }

    private fun registerSmsReceiver() {
        if (isPermissionGranted(Manifest.permission.READ_SMS) && isPermissionGranted(Manifest.permission.RECEIVE_SMS))
            registerReceiver(reciverOtp, IntentFilter("android.provider.Telephony.SMS_RECEIVED"))
        else
            Dexter.withContext(this)
                .withPermissions(
                    Manifest.permission.READ_SMS,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_PHONE_NUMBERS,
                    Manifest.permission.RECEIVE_SMS,
                ).withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(p0: MultiplePermissionsReport) {
                    }

                    override fun onPermissionRationaleShouldBeShown(p0: MutableList<PermissionRequest>?, p1: PermissionToken?) {
                    }
                }).check()
    }

    private fun unRegister() {
        try{
            unregisterReceiver(reciverOtp)
        }catch (_:Exception){}
    }

    //endregion SMS BROADCAST RECEIVER
}