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

    //endregion GLOBALS-->>>

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > bmpBg.width || h > bmpBg.height) {
            thread {
                val bmp = BackgroundProvider.getBackground(bgAsset, Point(w, h), isDark, context, color, gradModel)
                bmpBg.recycle()
                bmpBg = bmp
                postInvalidate()
            }
        }

    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawBitmap(bmpBg, 0f, 0f, paint)
    }

    //region COMMU

    fun setData(isDark: Boolean, @FloatRange(0.0, 1.0) alpha: Float, @DrawableRes bgAsset: Int, @ColorInt color: Int = Color.BLACK, gradModel: GradModel? = null) {
        paint.alpha = 255f.times(alpha).toInt()
        this.isDark = isDark
        this.bgAsset = bgAsset
        this.color = color
        this.gradModel = gradModel
        postInvalidate()
    }

    //endregion COMMU

}