package com.tomer.chitchat.modals.prefs

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
    var dpNo: Int
)
