package com.tomer.chitchat.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.graphics.drawable.IconCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.tomer.chitchat.R
import com.tomer.chitchat.modals.states.UiMsgModal
import com.tomer.chitchat.ui.activities.ChatActivity
import com.tomer.chitchat.utils.Utils
import com.tomer.chitchat.utils.Utils.Companion.getDpLink

class AndroidNotificationService(
    private val context: Context
) : NotificationService {

    private val notiMan by lazy { NotificationManagerCompat.from(context) }

    override fun showNewUserNotification(phonePartner: String, namePartner: String) {
        if (!notiMan.areNotificationsEnabled()) return
        if (notiMan.getNotificationChannelCompat("new_user") == null)
            createNewUserChannel()

        val i = Intent(context, ChatActivity::class.java)
        i.putExtra("phone",phonePartner)
        val pendingIntent = PendingIntentCompat.getActivity(context, 0, i, PendingIntent.FLAG_ONE_SHOT, false)

        Glide.with(context)
            .asBitmap()
            .load(phonePartner.getDpLink())
            .override(120)
            .circleCrop()
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val notification = NotificationCompat.Builder(context, "new_user")
                        .setContentTitle("New Request...")
                        .setContentText("User $namePartner trying to connect")
                        .setSmallIcon(IconCompat.createWithBitmap(resource))
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build()

                    notiMan.notify(phonePartner.hashCode(), notification)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }

            })


    }

    @SuppressLint("MissingPermission")
    override fun showNewMessageNotification(msg: UiMsgModal?, phonePartner: String) {
        if (msg == null) return
        if (!notiMan.areNotificationsEnabled()) return
        if (notiMan.getNotificationChannelCompat("new_msg") == null)
            createNewMsgChannel()

        val i = Intent(context, ChatActivity::class.java)
        i.putExtra("phone",phonePartner)
        val pendingIntent = PendingIntentCompat.getActivity(context, 0, i, PendingIntent.FLAG_ONE_SHOT, false)

        val notification = NotificationCompat.Builder(context, "new_msg")
            .setContentTitle("New Message from ${msg.id}")
            .setContentText(msg.msg)
            .setSmallIcon(R.drawable.round_image_24)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notiMan.notify(phonePartner.hashCode(), notification)
    }


    private fun createNewMsgChannel() {
        val channel = NotificationChannel("new_msg", "New Message Notification", NotificationManager.IMPORTANCE_HIGH)
        channel.description = "Notification chanel for new incoming messages"
        channel.enableLights(true)
        channel.lightColor = Color.GRAY
        channel.enableVibration(true)

        notiMan.createNotificationChannel(channel)
    }

    private fun createNewUserChannel() {
        val channel = NotificationChannel("new_user", "New User Notification", NotificationManager.IMPORTANCE_LOW)
        channel.description = "Notification chanel for new users to request connection"
        channel.enableLights(true)
        channel.lightColor = Color.BLUE
        channel.enableVibration(true)

        notiMan.createNotificationChannel(channel)
    }
}