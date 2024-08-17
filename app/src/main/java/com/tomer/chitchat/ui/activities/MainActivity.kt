package com.tomer.chitchat.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
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
import com.tomer.chitchat.modals.states.MsgStatus
import com.tomer.chitchat.modals.states.MsgsFlowState
import com.tomer.chitchat.room.MsgMediaType
import com.tomer.chitchat.utils.ConversionUtils
import com.tomer.chitchat.utils.ConversionUtils.chatVM
import com.tomer.chitchat.utils.EmojisHashingUtils
import com.tomer.chitchat.utils.Utils
import com.tomer.chitchat.viewmodals.AssetsViewModel
import com.tomer.chitchat.viewmodals.ChatViewModal
import com.tomer.chitchat.viewmodals.MainViewModal
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
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

    // Method to check if dark mode is enabled
    private fun isDarkModeEnabled(): Boolean {
        val currentNightMode = resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK)
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }

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
        if (isDarkModeEnabled()) b.tvAppName.setTextColor(ContextCompat.getColor(this, R.color.white))
        ConversionUtils.chatVM = chatVm
        ConversionUtils.assetsVM = assetsVM
        b.apply {
            btConnect.setOnClickListener(this@MainActivity)
            imgBarcode.setOnClickListener(this@MainActivity)
            imgFab.setOnClickListener(this@MainActivity)
            btCross.setOnClickListener(this@MainActivity)

            btDel.setOnClickListener(this@MainActivity)
            btBack.setOnClickListener(this@MainActivity)
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
                            Glide.with(this@MainActivity)
                                .load(it.fileGif!!)
                                .skipMemoryCache(true)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .into(b.imgLottie)
                        }

                        FlowType.SHOW_BIG_JSON -> if (activityLife) {
                            val b = getRvViewIfVisible(it.fromUser) ?: return@runOnUiThread
                            try {
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
                    handelFlowMsg(it)
                }
            }
        }

        viewModal.headMenu.observe(this@MainActivity) {
            b.apply {
                btProfile.isClickable = false
                btSearch.isClickable = false
                btBack.isClickable = false
                btDel.isClickable = false
            }
            if (it) {
                val width = b.layMainHead.width
                val height = b.layMainHead.height
                b.laySelHead.visibility = View.VISIBLE
                window.statusBarColor = ContextCompat.getColor(this, R.color.backgroundSelBg)
                b.apply {
                    btBack.isClickable = true
                    btDel.isClickable = true
                }
                if (b.laySelHead.isAttachedToWindow)
                    ViewAnimationUtils.createCircularReveal(b.laySelHead, width.times(0.8f).toInt(), height.shr(1), 1f, width.toFloat()).apply {
                        duration = 340
                        start()
                    }
            } else {
                val width = b.laySelHead.width
                val height = b.laySelHead.height
                window.statusBarColor = ContextCompat.getColor(this, R.color.backgroundC)
                if (b.laySelHead.isAttachedToWindow)
                    ViewAnimationUtils.createCircularReveal(b.laySelHead, width.times(0.8f).toInt(), height.shr(1), width.toFloat(), 1f).apply {
                        duration = 200
                        doOnEnd {
                            b.apply {
                                laySelHead.visibility = View.GONE
                                btProfile.isClickable = true
                                btSearch.isClickable = true
                            }
                        }
                        start()
                    }
                else {
                    b.apply {
                        laySelHead.visibility = View.GONE
                        btProfile.isClickable = true
                        btSearch.isClickable = true
                    }
                }
            }

        }
        viewModal.selCount.observe(this@MainActivity) {
            b.root.post {
                b.tvSelCount.text = it.toString()
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

            b.btBack.id -> {
                viewModal.delSelected(false, adapter.currentList)
                for (i in adapter.currentList) {
                    if (i.isSelected) i.isSelected = false
                    val b = getRvViewIfVisible(i.phoneNo) ?: continue
                    b.root.setBackgroundColor(ContextCompat.getColor(this, R.color.trans))
                }
            }

            b.btDel.id -> viewModal.delSelected(true, adapter.currentList)
        }
    }


    override fun onClick(pos: Int) {
        if (viewModal.headMenu.value == true) {
            onLongClick(pos)
            return
        }
        startActivity(
            Intent(this, ChatActivity::class.java)
                .apply {
                    putExtra("phone", adapter.currentList[pos].phoneNo)
                }
        )
    }

    override fun onLongClick(pos: Int) {
        var isSel: Boolean
        adapter.currentList[pos].isSelected = viewModal.addDelNo(adapter.currentList[pos].phoneNo).also { isSel = it }
        val b = getRvViewIfVisible(adapter.currentList[pos].phoneNo) ?: return
        val col = if (isSel) ContextCompat.getColor(this, R.color.primary_light)
        else ContextCompat.getColor(this, R.color.backgroundC)
        b.root.setBackgroundColor(col)
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


    //region Handel FLOW MSGS

    private fun handelFlowMsg(msg: MsgsFlowState) {
        Log.d("TAG--", "Main Activity Handle msg : $msg")
        when (msg.type) {
            FlowType.MSG -> if (activityLife && msg.data != null) {

                if (!msg.isLast) return
                if (adapter.currentList.indexOfFirst { it.phoneNo == msg.fromUser } == -1) {
                    viewModal.loadPersons(adapter.currentList)
                    return
                }

                val lastMsg: String = when (msg.data.msgType) {
                    MsgMediaType.TEXT, MsgMediaType.EMOJI -> msg.data.msg
                    MsgMediaType.IMAGE, MsgMediaType.GIF, MsgMediaType.VIDEO, MsgMediaType.FILE -> msg.data.mediaFileName ?: msg.data.msgType.name
                }
                val b = getRvViewIfVisible(msg.fromUser) ?: return
                b.tvLastMsg.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.fore))
                b.tvLastMsg.text = lastMsg.also { b.tvLastMsg.tag = it }
                b.tvUnreadMsgCount.text = b.tvUnreadMsgCount.text.toString().toInt().plus(1).toString()
                b.msgStatus.visibility = View.GONE
                b.tvUnreadMsgCount.visibility = View.VISIBLE
                b.tvTime.setTextColor(ContextCompat.getColor(this, R.color.purple))

                when (msg.data.msgType) {
                    MsgMediaType.TEXT, MsgMediaType.EMOJI -> b.msgType.visibility = View.GONE
                    MsgMediaType.IMAGE, MsgMediaType.GIF, MsgMediaType.VIDEO, MsgMediaType.FILE -> b.msgType.visibility = View.VISIBLE
                }

                when (msg.data.msgType) {
                    MsgMediaType.IMAGE, MsgMediaType.GIF -> {
                        val byes = assetsVM.getBytesOfFile(msg.data.msgType, msg.data.mediaFileName.toString())
                        Glide.with(this@MainActivity)
                            .load(byes ?: AdapPerson.getByteArr(msg.data.msg.split(",-,")[1]))
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
                            .load(AdapPerson.getByteArr(msg.data.msg.split(",-,")[1]))
                            .skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(
                                object : CustomTarget<Bitmap>() {
                                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                        val b1 = getRvViewIfVisible(msg.fromUser) ?: return
                                        b1.imgLottie.setImageBitmap(resource)
                                    }

                                    override fun onLoadCleared(placeholder: Drawable?) {
                                    }

                                    override fun onLoadFailed(errorDrawable: Drawable?) {
                                        val b1 = getRvViewIfVisible(msg.fromUser) ?: return
                                        b1.imgLottie.setImageDrawable(ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_video))
                                    }
                                }
                            )
                    }

                    MsgMediaType.EMOJI -> {
                        val nameGoogleJson = EmojisHashingUtils.googleJHash[ConversionUtils.encode(msg.data.msg)]
                        if (!nameGoogleJson.isNullOrEmpty()) {
                            assetsVM.showGoogleJsonViaFlow(nameGoogleJson, msg.fromUser)
                            return
                        }

                        val nameJson = EmojisHashingUtils.jHash[ConversionUtils.encode(msg.data.msg)]
                        if (!nameJson.isNullOrEmpty()) {
                            assetsVM.showJsonViaFlow(nameJson, msg.fromUser)
                            return
                        }

                        val nameGif = EmojisHashingUtils.gHash[ConversionUtils.encode(msg.data.msg)]
                        if (!nameGif.isNullOrEmpty()) {
                            assetsVM.showGifViaFlow(nameGif, msg.fromUser)
                            return
                        }

                        val nameTeleGif = EmojisHashingUtils.teleHash[ConversionUtils.encode(msg.data.msg)]
                        if (!nameTeleGif.isNullOrEmpty()) {
                            assetsVM.showTeleGifViaFlow(nameTeleGif, msg.fromUser)
                            return
                        }
                    }

                    else -> {}
                }

            }

            FlowType.SERVER_REC -> {
                val old = adapter.currentList.find { t -> t.lastMsgId == (msg.oldId ?: -2L) } ?: return
                old.lastMsgId = msg.msgId ?: old.lastMsgId
                old.msgStatus = MsgStatus.SENT_TO_SERVER
                handleMsgStatusAnimation(true, msg.fromUser)
            }

            FlowType.PARTNER_REC -> {
                val old = adapter.currentList.find { t -> t.lastMsgId == (msg.msgId ?: -2L) } ?: return
                old.msgStatus = MsgStatus.RECEIVED
                handleMsgStatusAnimation(false, msg.fromUser)
            }

            FlowType.TYPING -> {
                val b = getRvViewIfVisible(msg.fromUser) ?: return
                b.tvLastMsg.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.primary))
                "Typing...".also { b.tvLastMsg.text = it }
            }

            FlowType.NO_TYPING -> {
                val b = getRvViewIfVisible(msg.fromUser) ?: return
                if (b.tvLastMsg.text == "Typing...") {
                    b.tvLastMsg.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.fore))
                    b.tvLastMsg.text = b.tvLastMsg.tag.toString()
                }
            }

            FlowType.SEND_NEW_CONNECTION_REQUEST -> {
                chatVm.connectNew(msg.fromUser, false)
            }


            FlowType.INCOMING_NEW_CONNECTION_REQUEST, FlowType.REQ_ACCEPTED, FlowType.REQ_REJECTED -> if (activityLife) b.root.postDelayed({ viewModal.loadPersons(adapter.currentList) }, 40)
            FlowType.ONLINE,
            FlowType.OFFLINE -> if (activityLife) {
                val b = getRvViewIfVisible(msg.fromUser) ?: return
                b.onlineIndi.setStatusAnimating(msg.type == FlowType.ONLINE)
            }


            FlowType.OPEN_NEW_CONNECTION_ACTIVITY -> if (activityLife) startActivity(
                Intent(this@MainActivity, ChatActivity::class.java)
                    .apply {
                        putExtra("phone", msg.fromUser)
                    }
            )

            else -> {}
        }
    }

    //endregion Handel FLOW MSGS

    private fun handleMsgStatusAnimation(serverRec: Boolean, phone: String) {
        val index = adapter.currentList.indexOfFirst { phone == it.phoneNo }
        if (index == -1) return
        chatVM.chatMsgs[index].status = if (serverRec) MsgStatus.SENT_TO_SERVER else MsgStatus.RECEIVED
        val b = getRvViewIfVisible(phone) ?: return
        val animDur = 200L
        lifecycleScope.launch {
            delay(animDur)
            b.msgStatus.setImageDrawable(ContextCompat.getDrawable(this@MainActivity, if (serverRec) R.drawable.ic_tick else R.drawable.ic_double_tick))
            b.msgStatus.animate().rotationYBy(-180f).setInterpolator(LinearInterpolator()).setDuration(animDur).start()
        }
        b.msgStatus.animate().rotationYBy(180f).setInterpolator(LinearInterpolator()).setDuration(animDur).start()
    }

}