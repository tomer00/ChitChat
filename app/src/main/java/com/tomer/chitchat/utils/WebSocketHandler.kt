package com.tomer.chitchat.utils

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

object WebSocketHandler {

    //region GLOBALS

    private var webSocket: WebSocket? = null
    private var token = ""
    private var chunkedWebSocketListener: ChunkedWebSocketListener? = null
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
                chunkedWebSocketListener?.onMessage(strB.toString())
                return
            }

            //Normal Msg IMPL
            chunkedWebSocketListener?.onMessage(text)
        }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            chunkedWebSocketListener?.onOpen()
        }
    }

    private fun tryReconnectAfter2Sec() {
        if (closedByActivityEnd)
            return

        if (token.isNotEmpty())
            retryJob = CoroutineScope(Dispatchers.IO).launch {
                delay(2000)
                Log.d("TAG--", "tryReconnectAfter2Sec: ")
                openConnection()
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

    fun setListenerAndToken(listener: ChunkedWebSocketListener, token: String) {
        chunkedWebSocketListener = listener
        this.token = token
    }

    fun sendMessage(text: String) {
        webSocket?.send(text)
    }

    fun openConnection() {
        closedByActivityEnd = false
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
    }

    //endregion COMMU
}

interface ChunkedWebSocketListener {
    fun onMessage(text: String)
    fun onOpen()
}
