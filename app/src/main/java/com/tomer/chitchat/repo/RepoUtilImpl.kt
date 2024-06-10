package com.tomer.chitchat.repo

import android.content.Context
class RepoUtilImpl(
    private val context: Context
):RepoUtils {

    private val prefUtils by lazy { context.getSharedPreferences("utils", Context.MODE_PRIVATE) }

    override fun getToken(): String =
        prefUtils.getString("token", "").toString()

    override fun saveToken(token: String) {
        prefUtils.edit().putString("token", token).apply()
    }

}