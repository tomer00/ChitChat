package com.tomer.chitchat.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.app.ActivityCompat
import com.tomer.chitchat.room.ModelRoomPersonRelation
import kotlin.math.min
import kotlin.math.pow

class Utils {
    companion object {

        //        private const val IP: String = "192.168.43.167:9080"
        private const val IP: String = "35.209.235.169:9080"
        const val SERVER_LINK: String = "http://$IP"
        const val WEBSOCKET_LINK: String = "ws://$IP/socket"
        fun String.getDpLink() = "https://firebasestorage.googleapis.com/v0/b/chitchat-13c0f.appspot.com/o/dps%2F$this.webp?alt=media"


        var myPhone = ""
        var myName = ""
        var currentPartner: ModelRoomPersonRelation? = null

        @SuppressLint("DefaultLocale")
        fun humanReadableSize(size: Int): String {
            return when {
                size < 1024 -> String.format("%1$.0f B", size.toDouble())
                size < 1024.0.pow(2.0) -> String.format("%1$.0f KB", (size / 1024).toDouble())
                size < 1024.0.pow(3.0) -> String.format("%1$.1f MB", size / 1024.0.pow(2.0))
                else -> String.format("%1$.2f GB", size / 1024.0.pow(3.0))
            }
        }

        fun getFileExt(name: String): String {
            val ind = name.lastIndexOf(".")
            return try {
                name.substring(ind + 1)
            } catch (e: Exception) {
                ""
            }
        }

        fun Bitmap.centerCropBitmap(): Bitmap {
            val originalWidth = this.width
            val originalHeight = this.height

            val width = min(originalWidth, originalHeight)

            // Calculate the cropping coordinates
            val x = (originalWidth - width) / 2
            val y = (originalHeight - width) / 2

            // Create a new Bitmap by cropping the original Bitmap
            return Bitmap.createBitmap(this, x, y, width, width)
        }

        fun Activity.hideKeyBoard() {
            val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            val v = currentFocus ?: View(this)
            imm.hideSoftInputFromWindow(v.windowToken, 0)
        }

        fun Activity.showKeyBoard() {
            val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            val v = currentFocus ?: View(this)
            imm.showSoftInput(v, 0)
        }

        fun Activity.isPermissionGranted(name: String) =
            ActivityCompat.checkSelfPermission(this, name) == PackageManager.PERMISSION_GRANTED

    }

}