package com.tomer.chitchat.modals.prefs

import android.graphics.Color
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.tomer.chitchat.utils.qrProvider.RenderModel

class RenderConvertor {

    @TypeConverter
    fun fromRenderModel(renderModel: RenderModel): String {
        return Gson().toJson(renderModel)
    }

    @TypeConverter
    fun toRenderModel(string: String): RenderModel {
        return try {
            Gson().fromJson(string, RenderModel::class.java)
        } catch (_: Exception) {
            RenderModel(1f, Color.parseColor("#005FEB"))
        }
    }
}