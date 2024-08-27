package com.tomer.chitchat.utils

import android.graphics.Bitmap
import android.icu.text.SimpleDateFormat
import com.tomer.chitchat.utils.Utils.Companion.centerCropBitmap
import com.tomer.chitchat.viewmodals.AssetsViewModel
import com.tomer.chitchat.viewmodals.ChatViewModal
import java.io.ByteArrayOutputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.util.Base64
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ConversionUtils {

    // Converts a long to a Base64 string
    fun toBase64(l: Long) = Base64.getEncoder().encodeToString(ByteBuffer.allocate(Long.SIZE_BYTES).putLong(l).array())

    // Converts a Base64 string to a long
    fun fromBase64(s: String) = ByteBuffer.wrap(Base64.getDecoder().decode(s)).getLong()

    fun byteArrToBase64(arr: ByteArray): String {
        return Base64.getEncoder().encodeToString(arr)
    }

    fun base64ToByteArr(str: String): ByteArray {
        return Base64.getDecoder().decode(str)
    }

    fun convertToWebp(bmp: Bitmap): ByteArray {
        val baos = ByteArrayOutputStream()
        bmp
            .centerCropBitmap()
            .compress(Bitmap.CompressFormat.WEBP, 80, baos)
        return baos.toByteArray()
    }

    fun encode(data: String): String {
        return try {
            URLEncoder.encode(data, "UTF-8")
        } catch (e: Exception) {
            data
        }
    }

    fun decode(data: String): String {
        return try {
            URLDecoder.decode(data, "UTF-8")
        } catch (e: Exception) {
            data
        }
    }

    fun millisToTimeText(millis: Long): String {
        val date = Date(millis)
        val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return format.format(date)
    }

    fun millisToFullDateText(millis: Long): String {
        val date = Date(millis)
        val format = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
        return format.format(date)
    }

    fun getRelativeTime(millis: Long): String {
        val providedCal = Calendar.getInstance()
        providedCal.timeInMillis = millis
        val today = Calendar.getInstance()

        val daysGap = today.get(Calendar.DAY_OF_YEAR) - providedCal.get(Calendar.DAY_OF_YEAR)

        if (daysGap == 0)
            return millisToTimeText(millis)
         else if (daysGap == 1) return "Yesterday"
        else {
            val date = Date(millis)
            val format = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            return format.format(date)
        }


    }
}
