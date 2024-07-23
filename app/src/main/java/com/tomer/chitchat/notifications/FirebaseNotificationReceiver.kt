package com.tomer.chitchat.notifications

import android.util.Log
import android.widget.Toast
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.tomer.chitchat.crypto.CryptoService
import com.tomer.chitchat.modals.states.FlowType
import com.tomer.chitchat.repo.RepoMessages
import com.tomer.chitchat.repo.RepoPersons
import com.tomer.chitchat.repo.RepoRelations
import com.tomer.chitchat.retro.Api
import com.tomer.chitchat.utils.MessageHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FirebaseNotificationReceiver : FirebaseMessagingService() {

    @Inject
    lateinit var retro: Api

    @Inject
    lateinit var repoMsgs: RepoMessages

    @Inject
    lateinit var repoPersons: RepoPersons

    @Inject
    lateinit var gson: Gson

    @Inject
    lateinit var notificationService: NotificationService

    @Inject
    lateinit var repoRelations: RepoRelations

    @Inject
    lateinit var cryptoService: CryptoService

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                retro.updateNotificationToken(token)
            } catch (e: Exception) {
                Log.e("TAG--", "onNewToken: ", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("TAG--", "onMessageReceived: ${message.data}")
        CoroutineScope(Dispatchers.IO).launch {
            MessageHandler(gson, repoMsgs, repoPersons, cryptoService, notificationService, repoRelations) { msg ->
                Log.d("TAG--", "dfdfg: dfd$msg")
                if (msg.type == FlowType.MSG) {
                    val msgmod = msg.data ?: return@MessageHandler
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            retro.sendAck(msgmod.id.toString(), msg.fromUser)
                            notificationService.showNewMessageNotification(msgmod, msg.fromUser)
                        } catch (e: Exception) {
                            Log.e("TAG--", "onMessageReceived: ", e)
                        }
                    }
                }
            }.apply {
                handelMsg(message.data["data"] ?: "")
            }
        }

    }


}