package com.tomer.chitchat.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.tomer.chitchat.databinding.ActivityMainBinding
import com.tomer.chitchat.viewmodals.MainViewModal
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.log


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val b by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val viewModal: MainViewModal by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(b.root)
//        Log.d("TAG--", "onCreate: ${FirebaseAuth.getInstance().currentUser!!.phoneNumber}")
//        Log.d("TAG--", "onCreate: ${FirebaseAuth.getInstance().currentUser!!.uid}")

        FirebaseAuth.getInstance().signOut()

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else
        {
            var task  = FirebaseAuth.getInstance().currentUser!!.getIdToken(true)
            task.addOnCompleteListener { 
                if (task.isSuccessful){
                    Log.d("TAG--", "onCreate: ${task.result.token}")
                }
            }
            
        }

    }
}