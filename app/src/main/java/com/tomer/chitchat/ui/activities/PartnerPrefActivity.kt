package com.tomer.chitchat.ui.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.tomer.chitchat.R
import com.tomer.chitchat.databinding.ActivityPartnerPrefBinding
import com.tomer.chitchat.room.ModelPartnerPref
import com.tomer.chitchat.utils.Utils.Companion.isDarkModeEnabled
import com.tomer.chitchat.utils.Utils.Companion.isLandscapeOrientation
import com.tomer.chitchat.utils.Utils.Companion.px
import com.tomer.chitchat.viewmodals.SettingsPartnerPrefViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class PartnerPrefActivity : AppCompatActivity(), View.OnClickListener {

    private val b by lazy { ActivityPartnerPrefBinding.inflate(layoutInflater) }
    private val vm: SettingsPartnerPrefViewModel by viewModels()

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK)
                this.setResult(RESULT_OK)
        }


    //region LIFE CYCLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(b.root)
        if (isLandscapeOrientation()) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                window.insetsController?.hide(WindowInsets.Type.statusBars())
                return
            }
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            actionBar?.hide()
        }

        val phone = intent.getStringExtra("phone") ?: ""
        b.imgProfile.transitionName = phone
        Glide.with(this)
            .asBitmap()
            .load(File(intent.getStringExtra("dpFile") ?: ""))
            .circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .error(R.drawable.def_avatar)
            .into(b.imgProfile)

        b.apply {
            btBack.setOnClickListener(this@PartnerPrefActivity)
            btPhone.setOnClickListener(this@PartnerPrefActivity)
            btVideo.setOnClickListener(this@PartnerPrefActivity)
            btWallpaper.setOnClickListener(this@PartnerPrefActivity)

            switchNoti.setOnCheckedChangeListener { _, isChecked -> vm.setNotification(isChecked) }
            switchChatLock.setOnCheckedChangeListener { _, isChecked -> vm.setChatLock(isChecked) }
        }

        vm.myPrefs.observe(this) { mod ->
            b.apply {
                populateMsgs(mod)
                tvNameBig.text = mod.name
                tvNameSmall.text = mod.name
                "+91 ${mod.phone.substring(0, 5)} ${mod.phone.substring(5)}".also { tvPhone.text = it }
                tvAbout.text = mod.about.ifEmpty { "Hey there using Chit Chat!!!" }
                switchNoti.isChecked = mod.notificationAllowed
                switchChatLock.isChecked = mod.chatLocked
            }
        }

        vm.sharedContent.observe(this) { list ->
            if (list.isEmpty()) {

                return@observe
            }
            val size100PX = 100.px.toInt()
            val imgParam = LinearLayout.LayoutParams(size100PX, size100PX).apply { marginStart = 4.px.toInt() }
            val paramFirstView = LinearLayout.LayoutParams(8.px.toInt(), size100PX)
            val paramLastView = LinearLayout.LayoutParams(12.px.toInt(), size100PX)
            b.contMedia.addView(Space(this).apply { layoutParams = paramFirstView })
            for (i in list) {
                val img = ImageView(this).apply {
                    layoutParams = imgParam
                    scaleX = 0f
                    scaleY = 0f
                }
                Glide.with(img)
                    .load(i)
                    .error(R.drawable.round_image_24)
                    .skipMemoryCache(true)
                    .placeholder(R.drawable.round_image_24)
                    .override(size100PX, size100PX)
                    .transform(CenterCrop(), RoundedCorners(24))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(img)
                b.contMedia.addView(img)
            }
            b.contMedia.addView(Space(this).apply { layoutParams = paramLastView })
            b.contMedia.post {
                for (i in 0 until b.contMedia.childCount) {
                    b.contMedia.getChildAt(i).animate().apply {
                        scaleX(1f)
                        scaleY(1f)
                        startDelay = (i * 60L) + 100
                        duration = 140
                        interpolator = AccelerateInterpolator(1.2f)
                        start()
                    }
                }
            }
        }
        vm.loadPref(phone)
    }

    //endregion LIFE CYCLE

    //region CLICK LISTENER

    override fun onClick(v: View) {
        when (v.id) {
            b.btBack.id -> super.onBackPressedDispatcher.onBackPressed()
            b.btWallpaper.id -> launcher.launch(Intent(this, WallpaperActivity::class.java))
            b.btPhone.id -> {}
            b.btVideo.id -> {}
        }
    }
    //endregion CLICK LISTENER

    //region UI MSGS

    private fun populateMsgs(mod: ModelPartnerPref) {
        b.imgBg.setData(isDarkModeEnabled(), mod.background.alpha, mod.backgroundAsset, mod.background.color, mod.background.grad)

        val b1 = b.item1
        val b2 = b.item2

        val params1 = b1.msgLay.layoutParams as ConstraintLayout.LayoutParams
        params1.horizontalBias = 0f
        b1.msgLay.setLayoutParams(params1)
        b1.innerLay.gravity = Gravity.START
        b1.msgLay.gravity = Gravity.START
        b1.msgBg.foreground = ContextCompat.getDrawable(this, R.drawable.msg_rec)
        b1.RepTv.setGravity(Gravity.START)
        b1.msgTv.setTextColor(ContextCompat.getColor(this, R.color.white))
        b1.contTime.gravity = Gravity.START
        b1.imgMsgStatus.visibility = View.GONE
        if (mod.accent.grad == null)
            b1.msgBg.setData(false, vm.myPref.msgItemCorners.px, mod.accent.color)
        else b1.msgBg.setData(false, vm.myPref.msgItemCorners.px, mod.accent.grad)

        "Change accent color or gradient".also { b1.msgTv.text = it }
        b1.emojiTv.visibility = View.GONE
        "Hii there!".also { b1.RepTv.text = it }


        val params = b2.msgLay.layoutParams as ConstraintLayout.LayoutParams
        params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        params.horizontalBias = 1f
        b2.msgLay.setLayoutParams(params)
        b2.innerLay.gravity = Gravity.END
        b2.msgLay.gravity = Gravity.END
        b2.msgBg.foreground = ContextCompat.getDrawable(this, R.drawable.msg_sent)
        b2.RepTv.setGravity(Gravity.END)
        b2.msgTv.setTextColor(ContextCompat.getColor(this, R.color.fore))
        b2.contTime.gravity = Gravity.END
        b2.imgMsgStatus.visibility = View.GONE
        b2.msgBg.setData(true, vm.myPref.msgItemCorners.px, ContextCompat.getColor(this, R.color.softBg))

        "Change background and pattern".also { b2.msgTv.text = it }
        b2.emojiTv.visibility = View.GONE
        b2.RepTv.visibility = View.GONE


        b1.repImgRv.visibility = View.GONE
        b2.repImgRv.visibility = View.GONE
        b1.mediaCont.visibility = View.GONE
        b1.imgFileType.visibility = View.GONE
        b2.mediaCont.visibility = View.GONE
        b2.imgFileType.visibility = View.GONE
        b2.imgFileType.visibility = View.GONE
    }

    //endregion UI MSGS
}