package com.tomer.chitchat.ui.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.tomer.chitchat.R
import com.tomer.chitchat.databinding.ActivityLoginBinding
import com.tomer.chitchat.databinding.ActivityMainBinding
import com.tomer.chitchat.ui.frags.FragSendOtp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {


    private val b by lazy { ActivityLoginBinding.inflate(layoutInflater) }
    private val viewModal: ViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        FirebaseAuth.getInstance()

        supportFragmentManager.beginTransaction().replace(b.fragCont.id, FragSendOtp()).commit()
    }
}