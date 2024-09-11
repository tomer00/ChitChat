package com.tomer.chitchat.ui.activities

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.transition.Transition.TransitionListener
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withCreated
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.gson.Gson
import com.tomer.chitchat.R
import com.tomer.chitchat.databinding.ActivitySettingsBinding
import com.tomer.chitchat.modals.prefs.MyPrefs
import com.tomer.chitchat.utils.Utils.Companion.hideKeyBoard
import com.tomer.chitchat.utils.Utils.Companion.isDarkModeEnabled
import com.tomer.chitchat.utils.Utils.Companion.isLandscapeOrientation
import com.tomer.chitchat.utils.Utils.Companion.px
import com.tomer.chitchat.utils.Utils.Companion.showKeyBoard
import com.tomer.chitchat.utils.qrProvider.AssetsProvider
import com.tomer.chitchat.utils.qrProvider.QrImageProvider
import com.tomer.chitchat.viewmodals.SettingsMyPrefViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random
import kotlin.random.nextInt

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity(), View.OnClickListener {

    private val b by lazy { ActivitySettingsBinding.inflate(layoutInflater) }
    private val vm: SettingsMyPrefViewModel by viewModels()

    private val launcher: ActivityResultLauncher<PickVisualMediaRequest> =
        registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) {
            if (it != null)
                lifecycleScope.launch { vm.uploadDp(setAndSaveBitmapForDP(it) ?: return@launch) }
        }

    private val imeListener = OnEditorActionListener { v, actionId, _ ->
        if (actionId != EditorInfo.IME_ACTION_DONE) return@OnEditorActionListener false
        if (v.id == b.tvName.id) {
            vm.setName(v.text.toString().trim())
            hideKeyBoard()
            v.clearFocus()
        }
        if (v.id == b.tvAbout.id) {
            vm.setAbout(v.text.toString().trim())
            hideKeyBoard()
            v.clearFocus()
        }
        true
    }

    //region LIFECYCLE

    override fun onBackPressed() {

        if (b.layQr.visibility == View.VISIBLE) {
            vm.toggleQr()
            return
        }

        b.btAddDp.animate().apply {
            scaleX(0f)
            scaleY(0f)
            duration = 80
            interpolator = AccelerateInterpolator(1.4f)
            start()
        }
        hideKeyBoard()
        b.root.postDelayed({ super.onBackPressed() }, 40)
    }

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

        window.sharedElementEnterTransition.addListener(
            object : TransitionListener {
                override fun onTransitionStart(transition: android.transition.Transition?) {
                    b.apply {
                        b.btAddDp.scaleX = 0f
                        b.btAddDp.scaleY = 0f
                    }
                }

                override fun onTransitionEnd(transition: android.transition.Transition?) {
                    b.btAddDp.animate().apply {
                        scaleX(1f)
                        scaleY(1f)
                        duration = 120
                        interpolator = AccelerateInterpolator(1.4f)
                        start()
                    }
                }

                override fun onTransitionCancel(transition: android.transition.Transition?) {
                }

                override fun onTransitionPause(transition: android.transition.Transition?) {
                }

                override fun onTransitionResume(transition: android.transition.Transition?) {
                }


            }
        )

        b.apply {
            btBack.setOnClickListener(this@SettingsActivity)
            btShowQr.setOnClickListener(this@SettingsActivity)
            btChangeName.setOnClickListener(this@SettingsActivity)
            btChangeAbout.setOnClickListener(this@SettingsActivity)
            btAddDp.setOnClickListener(this@SettingsActivity)

            tvName.setOnEditorActionListener(imeListener)
            tvAbout.setOnEditorActionListener(imeListener)
        }

        populateMsgs()
        b.apply {
            sliderTextSize.addOnChangeListener { _, value, _ ->
                vm.setProgTextSize(value)
            }
            sliderCorners.addOnChangeListener { _, value, _ ->
                vm.setProgCorners(value)
            }
        }

        b.apply {
            b.tvPhone.text = vm.phone
        }

        vm.myPrefs.observe(this) {
            updateUI(it)
        }

        vm.qrDia.observe(this) {
            if (it) {
                b.imgQr.post {
                    val width = b.cardFlipper.width
                    val height = b.cardFlipper.height
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            val qr = QrImageProvider.getQRBMP(
                                "chit://${vm.phone}", Random.nextInt(1..6), Random.nextInt(1..5), this@SettingsActivity,
                                width, height, vm.myPrefs.value?.name.toString(), isDarkModeEnabled(), vm.qrAss[Random.nextInt(vm.qrAss.size)]
                            )
                            withCreated {
                                b.imgQr.setImageBitmap(qr)
                            }
                        }
                    }
                }
            }
            if (b.root.isAttachedToWindow)
                animQr(it)
            else b.apply {
                if (it) {
                    layQr.visibility = View.VISIBLE
                    layProfile.visibility = View.GONE
                } else {
                    layQr.visibility = View.GONE
                    layProfile.visibility = View.VISIBLE
                }
            }
        }
        vm.dpFile.observe(this) {
            if (it == null) return@observe
            lifecycleScope.launch {
                Glide.with(this@SettingsActivity)
                    .asBitmap()
                    .load(it)
                    .circleCrop()
                    .error(R.drawable.ic_avatar)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            b.imgSelectDp.setImageBitmap(resource)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                        }

                    })
            }
        }

        vm.textSize.observe(this) {
            b.tvProgTextSize.text = it.toInt().toString()
            b.sliderTextSize.value = it
            val b1 = b.item1
            val b2 = b.item2

            b1.msgTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, it)
            b1.RepTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, it.times(.86f))
            b2.msgTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, it)
        }

        vm.corners.observe(this) {
            b.tvProgCorners.text = it.toInt().toString()
            b.sliderCorners.value = it
            val b1 = b.item1
            val b2 = b.item2

            b2.msgBg.setData(true, it.px, ContextCompat.getColor(this, R.color.softBg))

            b1.msgBg.setData(false, it.px, ContextCompat.getColor(this, R.color.primary))
        }

        lifecycleScope.launch {
            vm.flowEvents.collectLatest {
                handleFlow(it)
            }
        }
    }

    //endregion LIFECYCLE

    //region FLOW EVENTS

    @Inject
    lateinit var gson: Gson

    private fun handleFlow(data: Pair<SettingsMyPrefViewModel.SettingEvents, String>) {
        when (data.first) {
            SettingsMyPrefViewModel.SettingEvents.UPDATE_PREF -> vm.myPrefs.value?.let { updateUI(it) }
            SettingsMyPrefViewModel.SettingEvents.SHOW_TOAST -> Toast.makeText(this, data.second, Toast.LENGTH_SHORT).show()
            SettingsMyPrefViewModel.SettingEvents.ERROR_NAME -> {
                b.tvName.error = "Enter name"
                b.root.postDelayed({
                    b.tvName.requestFocus()
                    showKeyBoard()
                }, 100)
            }

            SettingsMyPrefViewModel.SettingEvents.ERROR_ABOUT -> {
                b.tvAbout.error = "Enter about"
                b.root.postDelayed({
                    b.tvAbout.requestFocus()
                    showKeyBoard()
                }, 100)
            }

            else -> {}
        }
    }

    private fun updateUI(mod: MyPrefs) {
        b.apply {
            tvName.setText(mod.name)
            tvAbout.setText(mod.about)
        }
    }

    //endregion FLOW EVENTS

    //region CLICK LIS

    override fun onClick(v: View) {
        when (v.id) {
            b.btBack.id -> onBackPressed()
            b.btShowQr.id -> vm.toggleQr()
            b.btChangeName.id -> {
                b.tvName.requestFocus()
                b.tvName.setSelection(b.tvName.text.length)
                showKeyBoard()
            }

            b.btChangeAbout.id -> {
                b.tvAbout.requestFocus()
                b.tvAbout.setSelection(b.tvAbout.text.length)
                showKeyBoard()
            }

            b.btAddDp.id -> {
                launcher.launch(
                    PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        .build()
                )
            }
        }
    }

    //endregion CLICK LIS

    private fun animQr(showQr: Boolean) {
        val animDur = 200L
        lifecycleScope.launch {
            delay(animDur)
            b.apply {
                if (showQr) {
                    layQr.visibility = View.VISIBLE
                    layProfile.visibility = View.GONE
                } else {
                    layQr.visibility = View.GONE
                    layProfile.visibility = View.VISIBLE
                }
            }
            b.cardFlipper.animate().scaleX(1f).scaleY(1f).rotationY(0f).setInterpolator(LinearInterpolator()).setDuration(animDur).start()
        }
        b.cardFlipper.animate().scaleX(.3f).scaleY(.3f).rotationY(180f).setInterpolator(LinearInterpolator()).setDuration(animDur).start()
    }


    private suspend fun setAndSaveBitmapForDP(imgUri: Uri): Bitmap? {
        return suspendCoroutine { continuation ->
            Glide.with(this@SettingsActivity)
                .asBitmap()
                .load(imgUri)
                .override(720)
                .centerCrop()
                .into(
                    object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            continuation.resumeWith(Result.success(resource))
                            Glide.with(this@SettingsActivity)
                                .asBitmap()
                                .load(resource)
                                .centerCrop()
                                .circleCrop()
                                .into(b.imgSelectDp)
                        }

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            super.onLoadFailed(errorDrawable)
                            continuation.resumeWith(Result.success(null))
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                        }
                    }
                )
        }
    }


    //region UI MSGS


    private fun populateMsgs() {
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
        b1.msgBg.setData(false, 12f.px,ContextCompat.getColor(this, R.color.primary))

        "Slide to change text size".also { b1.msgTv.text = it }
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
        val colSoft =
        b2.msgBg.setData(false, 12f.px,ContextCompat.getColor(this, R.color.softBg))

        "Adjust corner radius".also { b2.msgTv.text = it }
        b2.emojiTv.visibility = View.GONE
        b2.RepTv.visibility = View.GONE


        b1.repImgRv.visibility = View.GONE
        b2.repImgRv.visibility = View.GONE
        b1.mediaCont.visibility = View.GONE
        b1.imgFileType.visibility = View.GONE
        b2.mediaCont.visibility = View.GONE
        b2.imgFileType.visibility = View.GONE
        b2.imgFileType.visibility = View.GONE

        b.imgBg.setData(this@SettingsActivity.isDarkModeEnabled(), .8f, vm.qrAss[Random.nextInt(vm.qrAss.size)], gradModel = AssetsProvider.gradType[Random.nextInt(1..5)])
    }

    //endregion UI MSGS
}