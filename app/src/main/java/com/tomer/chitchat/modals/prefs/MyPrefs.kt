package com.tomer.chitchat.modals.prefs

import androidx.annotation.FloatRange
import com.google.gson.annotations.SerializedName

data class MyPrefs(
    @SerializedName("phone")
    val phone: String,
    @SerializedName("name")
    var name: String,
    @SerializedName("about")
    var about: String,
    @SerializedName("msgItemCorners")
    var msgItemCorners: Float,
    @SerializedName("textSize")
    var textSize: Float,
    @SerializedName("dpNo")
    var dpNo: Int,
    @SerializedName("parallaxFactor")
    @FloatRange(0.0, 8.2)
    var parallaxFactor: Float
)
