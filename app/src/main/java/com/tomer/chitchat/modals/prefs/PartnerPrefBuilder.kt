package com.tomer.chitchat.modals.prefs

import android.graphics.Color
import androidx.annotation.DrawableRes
import androidx.annotation.IntRange
import com.tomer.chitchat.R
import com.tomer.chitchat.room.ModelPartnerPref
import com.tomer.chitchat.utils.qrProvider.AssetsProvider
import com.tomer.chitchat.utils.qrProvider.RenderModel

class PartnerPrefBuilder(private val phone: String, private var name: String = "") {
    private var background: RenderModel = RenderModel(1f, Color.DKGRAY, AssetsProvider.gradType.getOrDefault(4, null))
    private var accent: RenderModel = RenderModel(1f, Color.parseColor("#005FEB"))
    private var backgroundAsset: Int = R.drawable.pattern_7
    private var dpNo: Int = 1
    private var about: String = ""
    private var lastOnlineMillis: Long = System.currentTimeMillis()
    private var notificationAllowed: Boolean = true
    private var chatLocked: Boolean = false

    fun setName(name: String): PartnerPrefBuilder {
        this.name = name
        return this
    }

    fun setBackground(background: RenderModel): PartnerPrefBuilder {
        this.background = background
        return this
    }

    fun setAccent(accent: RenderModel): PartnerPrefBuilder {
        this.accent = accent
        return this
    }

    fun setBackgroundAsset(@DrawableRes backgroundAsset: Int): PartnerPrefBuilder {
        this.backgroundAsset = backgroundAsset
        return this
    }

    fun setDpNo(dpNo: Int): PartnerPrefBuilder {
        this.dpNo = dpNo
        return this
    }

    fun setAbout(about: String): PartnerPrefBuilder {
        this.about = about
        return this
    }

    fun setLastOnlineMillis(@IntRange(from = 0, to = Long.MAX_VALUE) lastOnlineMillis: Long): PartnerPrefBuilder {
        this.lastOnlineMillis = lastOnlineMillis
        return this
    }

    fun setNotificationAllowed(notificationAllowed: Boolean): PartnerPrefBuilder {
        this.notificationAllowed = notificationAllowed
        return this
    }

    fun setChatLocked(chatLocked: Boolean): PartnerPrefBuilder {
        this.chatLocked = chatLocked
        return this
    }

    fun build(): ModelPartnerPref {
        return ModelPartnerPref(
            phone,
            name,
            background,
            accent,
            backgroundAsset,
            dpNo,
            about,
            lastOnlineMillis,
            notificationAllowed,
            chatLocked
        )
    }
}