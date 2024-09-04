package com.tomer.chitchat.ui.activities

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.transition.Transition.TransitionListener
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.animation.AccelerateInterpolator
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.tomer.chitchat.databinding.ActivityImageViewBinding
import com.tomer.chitchat.utils.Utils
import com.tomer.chitchat.utils.Utils.Companion.isDarkModeEnabled
import dagger.hilt.android.AndroidEntryPoint
import java.io.File


@AndroidEntryPoint
class ImageViewActivity : AppCompatActivity() {

    companion object {
        var bytesImage: ByteArray? = null
    }

    private val b by lazy { ActivityImageViewBinding.inflate(layoutInflater) }

    private var initialTouchDownY = 0f
    private val limit = 480f

    private val colBg = Array(3) { 0 }
    private var currAlfa = 255

    private val dimen = Point(0, 0)

    override fun onBackPressed() {
        b.gifImgView2.visibility = View.GONE
        super.onBackPressed()
    }

    @SuppressLint("ClickableViewAccessibility")
    private val swipeDownTouchLis = View.OnTouchListener { v, event ->
        if (v.id == b.gifImgView.id) mGestureDetector.onTouchEvent(event)
        if (b.viewIMg.isZoomed) return@OnTouchListener true
        if (event.action == MotionEvent.ACTION_DOWN) {
            initialTouchDownY = event.rawY
            return@OnTouchListener true
        }

        if (event.action == MotionEvent.ACTION_UP) {
            if (currAlfa < 10)
                onBackPressed()
            else {
                b.cont.animate().apply {
                    y(0f)
                    duration = 200
                    interpolator = AccelerateInterpolator(1.2f)
                    start()
                }
                ValueAnimator.ofInt(currAlfa, 255).apply {
                    duration = 200
                    addUpdateListener {
                        val alfa = it.animatedValue as Int
                        currAlfa = alfa
                        b.root.setBackgroundColor(Color.argb(alfa, colBg[0], colBg[1], colBg[2]))
                        b.layTopBar.alpha = currAlfa.div(255f)
                    }
                    start()
                }
            }
            initialTouchDownY = event.rawY
            return@OnTouchListener true
        }

        if (event.action == MotionEvent.ACTION_MOVE) {

            val diff = event.rawY - initialTouchDownY
            if (diff < 1) return@OnTouchListener true

            b.cont.y = diff.times(1.2f)

            val calAlfa = 255 * diff.div(limit)
            currAlfa = 255 - calAlfa.toInt().coerceAtMost(255)
            b.root.setBackgroundColor(Color.argb(currAlfa, colBg[0], colBg[1], colBg[2]))
            b.layTopBar.alpha = currAlfa.div(255f)

            return@OnTouchListener true
        }
        return@OnTouchListener true
    }

    private val mGestureDetector by lazy {
        GestureDetector(this, object : SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                return b.gifImgView.performClick()
            }
        })
    }

    private val topBarAnimClickLis = View.OnClickListener {
        if (b.layTopBar.y < 0) {
            statusBarVisi(true)
            b.layTopBar.animate().apply {
                duration = 220
                interpolator = AccelerateInterpolator(1.2f)
                y(0f)
                start()
            }
        } else {
            statusBarVisi(false)
            b.layTopBar.animate().apply {
                duration = 160
                interpolator = AccelerateInterpolator(1.2f)
                y(-b.layTopBar.height.toFloat())
                start()
            }
        }
    }

    private fun statusBarVisi(visible: Boolean) {
        if (!visible) {
            // Hide the status bar
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                window.insetsController?.hide(WindowInsets.Type.statusBars())
                return
            }
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            actionBar?.hide()
        } else {
            // Show the status bar
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                window.insetsController?.show(WindowInsets.Type.statusBars())
                return
            }
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            actionBar?.show()
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(b.root)
        enableEdgeToEdge(SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { this.isDarkModeEnabled() })

        if (!this.isDarkModeEnabled()) {
            colBg[0] = 244
            colBg[1] = 248
            colBg[2] = 251
        } else {
            colBg[0] = 4
            colBg[1] = 16
            colBg[2] = 22
        }
        b.root.setBackgroundColor(Color.argb(255, colBg[0], colBg[1], colBg[2]))
        val isGif = intent.getBooleanExtra("isGif", false)
        val isSent = intent.getBooleanExtra("isSent", false)
        val file = File(intent.getStringExtra("file") ?: "")

        b.tvDetails.text = "${intent.getStringExtra("time")}"

        b.btDelete.setOnClickListener {
            onBackPressed()
            setResult(RESULT_OK)
        }
        b.btBack.setOnClickListener {
            onBackPressed()
        }

        "You".also { b.tvPartnerName.text = it }
        if (!isSent) {
            b.btSaveToGallery.setOnClickListener {
                MediaScannerConnection.scanFile(this, arrayOf(file.absolutePath), null) { _, _ ->
                    b.root.post { Toast.makeText(this, "File saved to Gallery...", Toast.LENGTH_SHORT).show() }
                }
            }
            b.tvPartnerName.text = Utils.currentPartner?.partnerName
        }
        b.btSaveToGallery.visibility = if (isSent) View.GONE else View.VISIBLE
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val h = if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
        b.layTopBar.setPadding(0, h, 0, 0)
        if (isGif) {
            b.gifImgView.transitionName = file.name
            b.gifImgView.visibility = View.VISIBLE
            b.gifImgView.setOnTouchListener(swipeDownTouchLis)
            b.gifImgView.setOnClickListener(topBarAnimClickLis)
            Glide.with(this.baseContext)
                .asBitmap()
                .load(bytesImage)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(
                    object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            b.gifImgView.setImageBitmap(resource)
                            dimen.set(resource.width, resource.height)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                        }

                    })

            window.sharedElementEnterTransition.addListener(
                object : TransitionListener {
                    override fun onTransitionStart(transition: android.transition.Transition?) {

                    }

                    override fun onTransitionEnd(transition: android.transition.Transition?) {
                        b.gifImgView2.visibility = View.VISIBLE
                        Glide.with(this@ImageViewActivity)
                            .load(bytesImage)
                            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(b.gifImgView2)
                    }

                    override fun onTransitionCancel(transition: android.transition.Transition?) {
                    }

                    override fun onTransitionPause(transition: android.transition.Transition?) {
                    }

                    override fun onTransitionResume(transition: android.transition.Transition?) {
                    }

                }
            )
            return
        }


        b.viewIMg.setOnTouchListener(swipeDownTouchLis)
        b.viewIMg.visibility = View.VISIBLE
        b.viewIMg.transitionName = file.name
        Glide.with(this)
            .asBitmap()
            .load(bytesImage)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
            .into(
                object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        b.viewIMg.setImageBitmap(resource)
                        dimen.set(resource.width, resource.height)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                    }

                })

        b.viewIMg.setOnClickListener(topBarAnimClickLis)

    }
}