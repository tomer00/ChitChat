package com.tomer.chitchat.viewmodals

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.tomer.chitchat.modals.msgs.UserList
import com.tomer.chitchat.repo.RepoMessages
import com.tomer.chitchat.repo.RepoUtils
import com.tomer.chitchat.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject

@HiltViewModel
class ChatViewModal @Inject constructor(
    private val repoUtils: RepoUtils,
    private val repoMsgs: RepoMessages,
) : ViewModel() {

    private var webSocket: WebSocket? = null
    private var token = ""

    private val webSocketListener = object : WebSocketListener() {
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            openWebSocket(token)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            openWebSocket(token)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
        }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            sendMsg(
                UserList(
                    repoMsgs.getCurrentUsers(),
                    FirebaseAuth.getInstance()
                        .currentUser!!.phoneNumber
                        .toString().toLong()).toString())
        }
    }


    init {
        token = repoUtils.getToken()
        webSocket = openWebSocket(token)
    }

    fun sendMsg(msg:String){
        webSocket?.send(msg)
    }

    private fun openWebSocket(token:String):WebSocket{
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(Utils.WEBSOCKET_LINK) // Replace with your server URL
            .addHeader("Authorization", "Bearer $token")
            .build()
        return client.newWebSocket(request, webSocketListener)
    }

}