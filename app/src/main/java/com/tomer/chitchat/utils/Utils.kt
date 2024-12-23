package com.tomer.chitchat.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.tomer.chitchat.room.ModelRoomPersonRelation
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

        val Number.px: Float
            get() = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), Resources.getSystem().displayMetrics
            )

        fun Number.toSP(resources: Resources) = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, this.toFloat(), resources.displayMetrics
        )


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

        fun Activity.isDarkModeEnabled(): Boolean {
            val currentNightMode = resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK)
            return currentNightMode == Configuration.UI_MODE_NIGHT_YES
        }

        fun Activity.isLandscapeOrientation() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

}

fun clipText(con: Context, text: String, count: Int) {
    val clipManager = con.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = ClipData.newPlainText("Messages", text)
    clipManager.setPrimaryClip(clipData)
    if (count > 1) Toast.makeText(con, "$count Messages copied", Toast.LENGTH_SHORT).show()
    else Toast.makeText(con, "Message copied", Toast.LENGTH_SHORT).show()
}