package com.tomer.chitchat.utils

import android.util.Log
import com.google.gson.Gson
import com.tomer.chitchat.crypto.CryptoService
import com.tomer.chitchat.modals.msgs.BulkReceived
import com.tomer.chitchat.modals.states.FlowType
import com.tomer.chitchat.modals.states.MsgsFlowState
import com.tomer.chitchat.notifications.NotificationService
import com.tomer.chitchat.repo.RepoMessages
import com.tomer.chitchat.repo.RepoPersons
import com.tomer.chitchat.repo.RepoRelations
import com.tomer.chitchat.repo.RepoStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class WebSocketHandler(
    repoMsgs: RepoMessages,
    repoStorage: RepoStorage,
    repoPersons: RepoPersons,
    gson: Gson,
    private val notificationService: NotificationService,
    private val repoRelations: RepoRelations,
    cryptoService: CryptoService,
) {


    //region HANDEL FLOWS
    val flowMsgs = MutableSharedFlow<MsgsFlowState>()
    val flowConnection = MutableSharedFlow<Boolean>()
    private val msgHandler = MessageHandler(gson, repoMsgs, repoPersons, cryptoService, notificationService, repoRelations, repoStorage) { msg ->
        CoroutineScope(Dispatchers.IO).launch {
            flowMsgs.emit(msg)
        }
        if (msg.type == FlowType.MSG && Utils.currentPartner?.partnerId.toString() != msg.fromUser) {
            notificationService.showNewMessageNotification(msg.data, msg.fromUser, repoRelations.getRelation(msg.fromUser)?.partnerName ?: msg.fromUser)
        } else if (msg.type == FlowType.SEND_PR)
            sendMessage("${msg.fromUser}*ACK-PR${msg.msgId}")
        else if (msg.type == FlowType.SEND_BULK_REC)
            sendMessage(BulkReceived(msg.fromUser, msg.data?.msg ?: "").toString())
    }

    //endregion HANDEL FLOWS

    //region GLOBALS

    private var webSocket: WebSocket? = null
    private var token = ""
    private var closedByActivityEnd = false

    private val webSocketListener = object : WebSocketListener() {
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            tryReconnectAfter2Sec()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            tryReconnectAfter2Sec()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d("TAG--", "onMessageNEW: $text")
            //Handle Chunkes IMPL
            if (text.elementAt(0) == 'C') {
                val chunkId = ConversionUtils.fromBase64(text.substring(1, 13))
                val chunkIndex = text.substring(13, 16).toInt()
                val chunkSize = text.substring(16, 19).toInt()

                val arr = chunkHash[chunkId]
                if (arr == null) {
                    val arr1 = Array(chunkSize) { "" }
                    chunkHash[chunkId] = arr1
                    arr1[chunkIndex] = text.substring(19)
                    return
                }
                arr[chunkIndex] = text.substring(19)

                for (i in arr) {
                    if (i.isEmpty()) return
                }

                val strB = StringBuilder()
                for (i in arr) strB.append(i)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        msgHandler.handelMsg(toString())
                    } catch (e: Exception) {
                        Log.e("TAG--", "onMessage: ", e)
                    }
                }
                return
            }

            //Normal Msg IMPL
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    msgHandler.handelMsg(text)
                } catch (e: Exception) {
                    Log.e("TAG--", "onMessage: ", e)
                }
            }
        }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.e("TAG--", "ONOPEN SOCKET: ")
            CoroutineScope(Dispatchers.IO).launch {
                flowConnection.emit(true)
            }
        }
    }

    private fun tryReconnectAfter2Sec() {
        if (closedByActivityEnd) {
            webSocket = null
            return
        }

        if (token.isNotEmpty())
            retryJob = CoroutineScope(Dispatchers.IO).launch {
                webSocket = null
                delay(2000)
                Log.d("TAG--", "tryReconnectAfter2Sec: ")
                openConnection(token)
            }
    }

    private var pingingJob: Job = createNewPingingJob()
    private var retryJob: Job? = null


    private fun createNewPingingJob(): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(150000)
                if (webSocket != null) {
                    webSocket?.send("PING")
                } else continue
            }
        }
    }

    val chunkHash = HashMap<Long, Array<String>>()

    //endregion GLOBALS

    //region COMMU

    fun sendMessage(text: String) {
        webSocket?.send(text)
    }

    fun openConnection(token: String) {
        this.token = token
        closedByActivityEnd = false
        if (webSocket != null) {
            Log.d("TAG--", "openConnection: AND WEBSOCKET IS NOT NULL")
            return
        }
        webSocket = try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(Utils.WEBSOCKET_LINK)
                .addHeader("Authorization", "Bearer $token")
                .build()
            client.newWebSocket(request, webSocketListener)
        } catch (e: Exception) {
            null
        }

        pingingJob.cancel()
        pingingJob = createNewPingingJob()
    }

    @Throws(Exception::class)
    fun closeConnection() {
        closedByActivityEnd = true
        webSocket!!.close(1001, "Closed for activity close")
        pingingJob.cancel()
        retryJob?.cancel()
        Log.d("TAG--", "closeConnection: ")
    }

    //endregion COMMU
}
