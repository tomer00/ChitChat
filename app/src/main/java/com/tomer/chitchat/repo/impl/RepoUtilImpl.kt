package com.tomer.chitchat.repo.impl

import android.content.Context
import com.tomer.chitchat.repo.RepoUtils

class RepoUtilImpl(
    private val context: Context
) : RepoUtils {

    private val prefUtils by lazy { context.getSharedPreferences("utils", Context.MODE_PRIVATE) }

    override fun getToken(): String =
        prefUtils.getString("token", "").toString()

    override fun saveToken(token: String) {
        prefUtils.edit().putString("token", token).apply()
    }

    override fun getName() =
        prefUtils.getString("userName", "").toString()

    override fun saveName(name: String) {
        prefUtils.edit().putString("userName", name).apply()
    }

    override fun getTempId(): Long {
        val tempId = prefUtils.getLong("tempId", 0L)
        prefUtils.edit().putLong("tempId", tempId + 1).apply()
        return tempId
    }

}