package com.tomer.chitchat.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.auth.FirebaseAuth
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.CompoundBarcodeView
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.tomer.chitchat.R
import com.tomer.chitchat.adap.AdapPerson
import com.tomer.chitchat.databinding.ActivityMainBinding
import com.tomer.chitchat.databinding.BarcodeDiaBinding
import com.tomer.chitchat.databinding.RowPersonBinding
import com.tomer.chitchat.modals.states.FlowType
import com.tomer.chitchat.room.MsgMediaType
import com.tomer.chitchat.utils.ConversionUtils
import com.tomer.chitchat.utils.EmojisHashingUtils
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
    private lateinit var ll: LinearLayoutManager

    private val qrDia by lazy { crQr() }
    private lateinit var barcodeView: CompoundBarcodeView
    private val callback by lazy { callBack() }

    private var activityLife = false

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
        }

        ll = LinearLayoutManager(this@MainActivity)
        b.rvPersons.layoutManager = ll
        b.rvPersons.adapter = adapter

        viewModal.persons.observe(this) {
            adapter.submitList(it)
        }
        lifecycleScope.launch {
            assetsVM.flowEvents.collectLatest {
                runOnUiThread {
                    when (it.type) {
                        FlowType.SHOW_BIG_GIF -> if (activityLife) {
                            val b = getRvViewIfVisible(it.fromUser) ?: return@runOnUiThread
                            b.imgLottie.visibility = View.VISIBLE
                            Glide.with(this@MainActivity)
                                .asGif()
                                .load(it.fileGif!!)
                                .skipMemoryCache(true)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .into(b.imgLottie)
                        }

                        FlowType.SHOW_BIG_JSON -> if (activityLife) {
                            val b = getRvViewIfVisible(it.fromUser) ?: return@runOnUiThread
                            try {
                                b.imgLottie.visibility = View.VISIBLE
                                b.imgLottie.setAnimationFromJson(it.data!!.msg, it.data.mediaFileName)
                                b.imgLottie.playAnimation()
                            } catch (_: Exception) {
                            }
                        }

                        else -> {}
                    }
                }
            }
        }
        lifecycleScope.launch {
            chatVm.flowMsgs.collectLatest {
                runOnUiThread {
                    Log.d("TAG--", "Main Activity Handle msg : $it")
                    when (it.type) {
                        FlowType.MSG -> if (activityLife && it.data != null) {

                            val lastMsg: String = when (it.data.msgType) {
                                MsgMediaType.TEXT, MsgMediaType.EMOJI -> it.data.msg
                                MsgMediaType.IMAGE, MsgMediaType.GIF, MsgMediaType.VIDEO, MsgMediaType.FILE -> it.data.mediaFileName ?: it.data.msgType.name
                            }
                            val b = getRvViewIfVisible(it.fromUser) ?: return@runOnUiThread
                            b.tvLastMsg.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.fore))
                            b.tvLastMsg.text = lastMsg.also { b.tvLastMsg.tag = it }
                            b.tvUnreadMsgCount.text = b.tvUnreadMsgCount.text.toString().toInt().plus(1).toString()

                            when (it.data.msgType) { //more work to be done
                                MsgMediaType.TEXT, MsgMediaType.EMOJI -> b.msgType.visibility = View.GONE
                                MsgMediaType.IMAGE, MsgMediaType.GIF, MsgMediaType.VIDEO, MsgMediaType.FILE -> b.msgType.visibility = View.VISIBLE
                            }

                            when (it.data.msgType) {
                                MsgMediaType.IMAGE, MsgMediaType.GIF -> {
                                    val byes = assetsVM.getBytesOfFile(it.data.msgType, it.data.mediaFileName.toString())
                                    Glide.with(this@MainActivity)
                                        .load(byes ?: AdapPerson.getByteArr(it.data.msg.split(",-,")[1]))
                                        .skipMemoryCache(true)
                                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                                        .into(b.imgLottie)
                                }

                                MsgMediaType.FILE -> {
                                    b.imgLottie.setImageDrawable(ContextCompat.getDrawable(this@MainActivity, AdapPerson.getDrawableId(lastMsg)))
                                }

                                MsgMediaType.VIDEO -> {
                                    Glide.with(this@MainActivity)
                                        .asBitmap()
                                        .load(AdapPerson.getByteArr(it.data.msg.split(",-,")[1]))
                                        .skipMemoryCache(true)
                                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                                        .into(
                                            object : CustomTarget<Bitmap>() {
                                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                                    val b1 = getRvViewIfVisible(it.fromUser) ?: return
                                                    b1.imgLottie.setImageBitmap(resource)
                                                }

                                                override fun onLoadCleared(placeholder: Drawable?) {
                                                }

                                                override fun onLoadFailed(errorDrawable: Drawable?) {
                                                    val b1 = getRvViewIfVisible(it.fromUser) ?: return
                                                    b1.imgLottie.setImageDrawable(ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_video))
                                                }
                                            }
                                        )
                                }

                                MsgMediaType.EMOJI -> {
                                    val nameGoogleJson = EmojisHashingUtils.googleJHash[ConversionUtils.encode(it.data.msg)]
                                    if (!nameGoogleJson.isNullOrEmpty()) {
                                        assetsVM.showGoogleJsonViaFlow(nameGoogleJson, it.fromUser)
                                        return@runOnUiThread
                                    }

                                    val nameJson = EmojisHashingUtils.jHash[ConversionUtils.encode(it.data.msg)]
                                    if (!nameJson.isNullOrEmpty()) {
                                        assetsVM.showJsonViaFlow(nameJson, it.fromUser)
                                        return@runOnUiThread
                                    }

                                    val nameGif = EmojisHashingUtils.gHash[ConversionUtils.encode(it.data.msg)]
                                    if (!nameGif.isNullOrEmpty()) {
                                        assetsVM.showGifViaFlow(nameGif, it.fromUser)
                                        return@runOnUiThread
                                    }

                                    val nameTeleGif = EmojisHashingUtils.teleHash[ConversionUtils.encode(it.data.msg)]
                                    if (!nameTeleGif.isNullOrEmpty()) {
                                        assetsVM.showTeleGifViaFlow(nameTeleGif, it.fromUser)
                                        return@runOnUiThread
                                    }
                                }

                                else -> {}
                            }

                        }

                        FlowType.SERVER_REC -> {
                            val old = adapter.currentList.find { t -> t.lastMsgId == (it.oldId ?: -2L) } ?: return@runOnUiThread
                            old.lastMsgId = it.msgId ?: old.lastMsgId
                        }

                        FlowType.TYPING -> {
                            val b = getRvViewIfVisible(it.fromUser) ?: return@runOnUiThread
                            b.tvLastMsg.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.primary))
                            "Typing...".also { b.tvLastMsg.text = it }
                        }

                        FlowType.NO_TYPING -> {
                            val b = getRvViewIfVisible(it.fromUser) ?: return@runOnUiThread
                            if (b.tvLastMsg.text == "Typing...") {
                                b.tvLastMsg.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.fore))
                                b.tvLastMsg.text = b.tvLastMsg.tag.toString()
                            }
                        }

                        FlowType.SEND_NEW_CONNECTION_REQUEST -> {
                            chatVm.connectNew(it.fromUser, false)
                        }


                        FlowType.INCOMING_NEW_CONNECTION_REQUEST -> if (activityLife) viewModal.loadPersons(adapter.currentList)
                        FlowType.ONLINE,
                        FlowType.OFFLINE -> if (activityLife) {
                            val b = getRvViewIfVisible(it.fromUser) ?: return@runOnUiThread
                            b.onlineIndi.visibility =
                                if (it.type == FlowType.OFFLINE) View.GONE
                                else View.VISIBLE
                        }


                        FlowType.OPEN_NEW_CONNECTION_ACTIVITY -> if (activityLife) startActivity(
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

    private fun getRvViewIfVisible(phone: String): RowPersonBinding? {
        val list = adapter.currentList
        for (i in list.indices) {
            if (list[i].phoneNo == phone) {
                val view = ll.findViewByPosition(i) ?: return null
                return RowPersonBinding.bind(view)
            }
        }
        return null
    }

    override fun onResume() {
        super.onResume()
        activityLife = true
        viewModal.loadPersons(adapter.currentList)
    }

    override fun onDestroy() {
        super.onDestroy()
        chatVm.closeWebSocket()
    }

    override fun onPause() {
        super.onPause()
        activityLife = false
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
                } else {
                    Dexter.withContext(this)
                        .withPermission(
                            Manifest.permission.CAMERA
                        )
                        .withListener(object : PermissionListener {
                            override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                                qrDia.show()
                                barcodeView.resume()
                                barcodeView.decodeSingle(callback)
                            }

                            override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                                Toast.makeText(this@MainActivity, "Please Provide with Camera Permission...", Toast.LENGTH_SHORT).show()
                            }

                            override fun onPermissionRationaleShouldBeShown(p0: PermissionRequest?, p1: PermissionToken?) {
                            }

                        }).check()
                }
            }

            b.imgFab.id -> {
                b.layNewNumber.visibility = View.VISIBLE
                b.imgBarcode.playAnimation()
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
                chatVm.connectNew(b.etNewNumber.text.toString(), true)
                b.layNewNumber.visibility = View.GONE
                b.imgBarcode.pauseAnimation()
                b.imgFab.visibility = View.VISIBLE
            }

            b.btCross.id -> {
                b.layNewNumber.visibility = View.GONE
                b.imgBarcode.pauseAnimation()
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
        chatVm.connectNew(result.text, true)
        b.layNewNumber.visibility = View.GONE
        b.imgBarcode.pauseAnimation()
        b.imgFab.visibility = View.VISIBLE
    }

    private fun checkPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        return result == PackageManager.PERMISSION_GRANTED
    }

//endregion BARCODE CALLBACK


}