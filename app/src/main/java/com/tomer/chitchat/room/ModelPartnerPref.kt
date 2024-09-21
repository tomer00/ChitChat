package com.tomer.chitchat.room

import androidx.annotation.DrawableRes
import androidx.annotation.IntRange
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tomer.chitchat.utils.qrProvider.RenderModel

@Entity(tableName = "partner_prefs")
data class ModelPartnerPref(
    @PrimaryKey
    val phone: String,
    var name: String,
    val background: RenderModel,
    val accent: RenderModel,
    @DrawableRes
    val backgroundAsset: Int,
    val dpNo: Int,
    val about: String,
    @IntRange(0, Long.MAX_VALUE)
    val lastOnlineMillis: Long,
    var notificationAllowed:Boolean,
    var chatLocked:Boolean,
)