package com.tomer.chitchat.utils.qrProvider

import androidx.annotation.ColorInt
import androidx.annotation.FloatRange

data class RenderModel(
    @FloatRange(0.0, 1.0)
    val alpha: Float,
    @ColorInt
    val color: Int,
    val grad: GradModel? = null
)
