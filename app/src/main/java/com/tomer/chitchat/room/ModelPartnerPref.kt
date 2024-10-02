package com.tomer.chitchat.room

import androidx.annotation.IntRange
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tomer.chitchat.utils.qrProvider.RenderModel

@Entity(tableName = "partner_prefs")
data class ModelPartnerPref(
    @PrimaryKey
    val phone: String,
    var name: String,
    var background: RenderModel,
    var accent: RenderModel,
    var backgroundAssetNo: Int,
    var dpNo: Int,
    var about: String,
    @IntRange(0, Long.MAX_VALUE)
    val lastOnlineMillis: Long,
    var notificationAllowed: Boolean,
    var chatLocked: Boolean,
)