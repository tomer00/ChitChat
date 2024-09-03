package com.tomer.chitchat.assets

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import com.tomer.chitchat.modals.states.UiMsgModal
import com.tomer.chitchat.modals.states.UiMsgModalBuilder
import com.tomer.chitchat.utils.ConversionUtils
import com.tomer.chitchat.utils.EmojisHashingUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import javax.inject.Inject
import kotlin.random.Random

class WebAssetsRepo @Inject constructor(
    context: Context
) : RepoAssets {

    private val assetsFolder =
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) File(File(context.getExternalFilesDir("ChitChat"), "assets").absolutePath.replace("Android/data", "Android/media").replace(".chitchat/files",".chitchat"))
        else File(context.getExternalFilesDir("ChitChat"), "assets")
    private val gifAssets = File(assetsFolder, "gifs")
    private val jsonAssets = File(assetsFolder, "jsons")

    private val jsonFilesBinLink = "https://firebasestorage.googleapis.com/v0/b/chitchat-13c0f.appspot.com/o/assests%2Fjsons%2F"
    private val gifFilesBinLink = "https://firebasestorage.googleapis.com/v0/b/chitchat-13c0f.appspot.com/o/assests%2Fgifs%2F"
    private val telemojiFilesBinLink = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Telegram-Animated-Emojis/main/"
    private val googleJsonFilesBinLink = "https://fonts.gstatic.com/s/e/notoemoji/latest/"

    init {
        if (!gifAssets.exists())
            gifAssets.mkdirs()

        if (!jsonAssets.exists())
            jsonAssets.mkdirs()
    }

    override suspend fun getLottieJson(name: String, sync: Boolean): String? {
        val f = File(jsonAssets, name)
        if (f.exists()) return readJson(f)
        if (sync) return null

        val bytes = downLoadBytes("$jsonFilesBinLink$name?alt=media") ?: return null
        withContext(Dispatchers.IO) {
            FileOutputStream(f).use {
                it.write(bytes)
                it.flush()
            }
        }
        return String(bytes, Charset.defaultCharset())
    }

    override suspend fun getGoogleLottieJson(nameJson: String, sync: Boolean): String? {
        val f = File(jsonAssets, nameJson)
        if (f.exists()) return readJson(f)
        if (sync) return null

        val bytes = downLoadBytes("$googleJsonFilesBinLink$nameJson/lottie.json") ?: return null
        withContext(Dispatchers.IO) {
            FileOutputStream(f).use {
                it.write(bytes)
                it.flush()
            }
        }
        return String(bytes, Charset.defaultCharset())
    }

    override suspend fun getGifFile(name: String, sync: Boolean): File? {
        val f = File(gifAssets, name)
        if (f.exists()) return f
        if (sync) return null

        val bytes = downLoadBytes("$gifFilesBinLink$name.gif?alt=media") ?: return null
        withContext(Dispatchers.IO) {
            FileOutputStream(f).use {
                it.write(bytes)
                it.flush()
            }
        }
        return f
    }

    override suspend fun getGifTelemoji(name: String, sync: Boolean): File? {
        val f = File(gifAssets, name)
        if (f.exists()) return f
        if (sync) return null
        val bytes = downLoadBytes("$telemojiFilesBinLink${ConversionUtils.decode(name)}.webp") ?: return null
        withContext(Dispatchers.IO) {
            FileOutputStream(f).use {
                it.write(bytes)
                it.flush()
            }
        }
        return f
    }


    override suspend fun getRandomJson(): UiMsgModal {
        val b = UiMsgModalBuilder()
        val google = Random.nextBoolean()
        val entry = (if (google)
            getRandomEntryEfficiently(EmojisHashingUtils.googleJHash)
        else getRandomEntryEfficiently(EmojisHashingUtils.jHash)) ?: return b.build()

        val data = (if (google) getGoogleLottieJson(entry.value)
        else getLottieJson(entry.value)) ?: return b.build()

        b.setMsg(data)
        b.mediaFileName(entry.value)

        return b.build()
    }

    private fun <K, V> getRandomEntryEfficiently(map: Map<K, V>): Map.Entry<K, V>? {
        if (map.isEmpty()) return null
        var selectedEntry: Map.Entry<K, V>? = null
        for ((count, entry) in map.entries.withIndex()) {
            if (Random.nextInt(count + 1) == 0) {
                selectedEntry = entry
            }
        }
        return selectedEntry
    }

    override suspend fun downLoadBytes(urlString: String): ByteArray? {
        var inputStream: InputStream? = null
        var outputStream: ByteArrayOutputStream? = null
        var connection: HttpURLConnection? = null

        return try {
            val url = URL(urlString)
            connection = withContext(Dispatchers.IO) {
                url.openConnection()
            } as HttpURLConnection
            connection.requestMethod = "GET"
            withContext(Dispatchers.IO) {
                connection.connect()
            }

            withContext(Dispatchers.IO) {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("HTTP error code: ${connection.responseCode}")
                }
            }

            inputStream = connection.inputStream
            outputStream = ByteArrayOutputStream()

            val buffer = ByteArray(1024)
            var bytesRead: Int

            while (withContext(Dispatchers.IO) {
                    inputStream.read(buffer)
                }.also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            outputStream.toByteArray()
        } catch (e: Exception) {
            null
        } finally {
            withContext(Dispatchers.IO) {
                inputStream?.close()
                outputStream?.close()
            }
            connection?.disconnect()
        }
    }

    private fun readJson(file: File): String {
        FileInputStream(file).use {
            return String(it.readBytes(), Charset.defaultCharset())
        }
    }


}