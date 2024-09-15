package com.tomer.chitchat.ui.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import com.tomer.chitchat.R
import com.tomer.chitchat.utils.qrProvider.AssetsProvider
import com.tomer.chitchat.utils.qrProvider.BackgroundProvider
import com.tomer.chitchat.utils.qrProvider.GradModel
import kotlin.concurrent.thread
import kotlin.math.roundToInt

class DoodleView : View {


    //region CONSTRUCTOR

    constructor(con: Context) : super(con)

    constructor(con: Context, attributeSet: AttributeSet) : super(con, attributeSet)

    constructor(context: Context, attr: AttributeSet, defStyle: Int) : super(context, attr, defStyle)


    //endregion CONSTRUCTOR

    //region GLOBALS-->>>

    @ColorInt
    private var color = Color.BLACK

    @DrawableRes
    private var bgAsset = R.drawable.pattern_7
    private var isDark = true
    private var gradModel: GradModel? = AssetsProvider.gradType.getOrDefault(4, null)
    private var bmpBg = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
    private val paint = Paint().apply {
        isAntiAlias = true
    }

    private var parallaxFactor = 4f
    private var parallaxMaxBounds = 100f

    private var initialX = 100f
    private var initialY = 100f

    private var actualX = 100f
    private var actualY = 100f

    //endregion GLOBALS-->>>

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > bmpBg.width || h > bmpBg.height) {
            thread(start = true, name = "Renderer", priority = Thread.MAX_PRIORITY) {
                val bmp = BackgroundProvider.getBackground(bgAsset, Point(w + 200, h + 200), isDark, context, color, gradModel)
                bmpBg.recycle()
                bmpBg = bmp
                postInvalidate()
            }
        }

    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawBitmap(bmpBg, -actualX, -actualY, paint)
    }

    //region COMMU

    fun setData(
        isDark: Boolean, @FloatRange(0.0, 1.0) alpha: Float, @DrawableRes bgAsset: Int,
        @ColorInt color: Int = Color.BLACK, gradModel: GradModel? = null
    ) {
        paint.alpha = 255f.times(alpha).roundToInt()
        this.isDark = isDark
        this.bgAsset = bgAsset
        this.color = color
        this.gradModel = gradModel
        postInvalidate()
    }

    fun setParallaxFactor(fac: Float) {
        parallaxFactor = fac
        parallaxMaxBounds = fac.times(25f).coerceAtMost(200f)
        initialX = parallaxMaxBounds.times(.5f).also { actualX = it }
        initialY = parallaxMaxBounds.times(.5f).also { actualY = it }
    }

    fun onSensorEvent(x: Float, y: Float) {
        val tempActualX = initialX - (x * parallaxFactor)
        val tempActualY = initialY + (y * parallaxFactor)

        val addX = (tempActualX - actualX).coerceAtMost(parallaxFactor).coerceAtLeast(-parallaxFactor)
        val addY = (tempActualY - actualY).coerceAtMost(parallaxFactor).coerceAtLeast(-parallaxFactor)

        actualX = (addX + actualX).coerceAtMost(parallaxMaxBounds).coerceAtLeast(0f)
        actualY = (addY + actualY).coerceAtMost(parallaxMaxBounds).coerceAtLeast(0f)

        postInvalidate()

    }

    //endregion COMMU

}