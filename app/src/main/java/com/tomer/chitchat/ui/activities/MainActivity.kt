package com.tomer.chitchat.ui.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.CompoundBarcodeView
import com.tomer.chitchat.R
import com.tomer.chitchat.adap.AdapPerson
import com.tomer.chitchat.databinding.ActivityMainBinding
import com.tomer.chitchat.databinding.BarcodeDiaBinding
import com.tomer.chitchat.databinding.MsgItemBinding
import com.tomer.chitchat.modals.states.FlowType
import com.tomer.chitchat.utils.ConversionUtils
import com.tomer.chitchat.utils.Utils
import com.tomer.chitchat.viewmodals.AssetsViewModel
import com.tomer.chitchat.viewmodals.ChatViewModal
import com.tomer.chitchat.viewmodals.MainViewModal
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), AdapPerson.CallbackClick, View.OnClickListener {

    private val b by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val viewModal: MainViewModal by viewModels()
    private val chatVm: ChatViewModal by viewModels()
    private val assetsVM: AssetsViewModel by viewModels()

    private val adapter by lazy { AdapPerson(this, this) }

    private val qrDia by lazy { crQr() }
    private lateinit var barcodeView: CompoundBarcodeView
    private val callback by lazy { callBack() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = ContextCompat.getColor(this, R.color.backgroundC)
        setContentView(b.root)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        Utils.myPhone = FirebaseAuth.getInstance().currentUser?.phoneNumber?.substring(3) ?: ""

        ConversionUtils.chatVM = chatVm
        ConversionUtils.assetsVM = assetsVM
        b.apply {
            btConnect.setOnClickListener(this@MainActivity)
            imgBarcode.setOnClickListener(this@MainActivity)
            imgFab.setOnClickListener(this@MainActivity)
            btCross.setOnClickListener(this@MainActivity)
            rvPersons.adapter = adapter
        }

        viewModal.persons.observe(this) {
            adapter.submitList(it)
        }
        lifecycleScope.launch {
            chatVm.flowMsgs.collectLatest {
                runOnUiThread {
                    when (it.type) {
                        FlowType.MSG -> {}
                        FlowType.SERVER_REC -> {}
                        FlowType.PARTNER_REC -> {}
                        FlowType.TYPING -> {}
                        FlowType.NO_TYPING -> {}
                        FlowType.ONLINE -> {}
                        FlowType.OFFLINE -> {}
                        FlowType.ACCEPT_REQ -> {}
                        FlowType.REJECT_REQ -> {}
                        FlowType.INCOMING_NEW_CONNECTION_REQUEST -> viewModal.loadPersons()
                        FlowType.OPEN_NEW_CONNECTION_ACTIVITY -> startActivity(
                            Intent(this@MainActivity, ChatActivity::class.java)
                                .apply {
                                    putExtra("phone", it.fromUser)
                                }
                        )

                        else -> {}
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModal.loadPersons()
    }

    override fun onDestroy() {
        super.onDestroy()
        chatVm.closeWebSocket()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }

    override fun onClick(v: View) {
        when (v.id) {
            b.imgBarcode.id -> {
                if (checkPermission()) {
                    qrDia.show()
                    barcodeView.resume()
                    barcodeView.decodeSingle(callback)
                } else requestPermissions(arrayOf("CAMERA"), 100)
            }

            b.imgFab.id -> {
                b.layNewNumber.visibility = View.VISIBLE
                b.imgFab.visibility = View.GONE
            }

            b.btConnect.id -> {
                if (
                    b.etNewNumber.text.toString().length < 10 ||
                    !b.etNewNumber.text.isDigitsOnly()
                ) {
                    b.etNewNumber.error = "Enter Valid Number"
                    return
                }
                chatVm.connectNew(b.etNewNumber.text.toString())
                b.layNewNumber.visibility = View.GONE
                b.imgFab.visibility = View.VISIBLE
            }

            b.btCross.id -> {
                b.layNewNumber.visibility = View.GONE
                b.imgFab.visibility = View.VISIBLE
            }
        }
    }


    override fun onClick(pos: Int) {
        startActivity(
            Intent(this, ChatActivity::class.java)
                .apply {
                    putExtra("phone", adapter.currentList[pos].phoneNo)
                }
        )
    }

    override fun onLongClick(pos: Int) {

    }


//region BARCODE CALLBACK

    private fun crQr(): AlertDialog {
        val fb: BarcodeDiaBinding = BarcodeDiaBinding.inflate(layoutInflater)
        val b = AlertDialog.Builder(this)
        b.setView(fb.root)
        val qrd = b.create()
        qrd.window?.attributes?.windowAnimations = R.style.Dialog
        qrd.window?.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.trans)))
        fb.btCross.setOnClickListener {
            qrd.cancel()
        }
        fb.barcodeView.setStatusText("")
        barcodeView = fb.barcodeView
        fb.btCross.translationY = 120f
        fb.btCross.animate().translationY(0f).setDuration(400).setStartDelay(400).start()

        qrd.setOnCancelListener {
            barcodeView.pause()
        }

        return qrd
    }


    private fun callBack() = BarcodeCallback { result ->
        barcodeView.pause()
        qrDia.dismiss()
        if (!result.text.isDigitsOnly()) return@BarcodeCallback
        chatVm.connectNew(result.text)
        b.layNewNumber.visibility = View.GONE
        b.imgFab.visibility = View.VISIBLE
    }

    private fun checkPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, "CAMERA")
        return result == PackageManager.PERMISSION_GRANTED
    }

//endregion BARCODE CALLBACK


}