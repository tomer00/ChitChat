package com.tomer.chitchat.repo

import com.tomer.chitchat.modals.prefs.MyPrefs

interface RepoUtils {
    fun getToken(): String
    fun saveToken(token: String)

    fun getPrefs(): MyPrefs
    fun savePrefs(mod: MyPrefs)

    fun getPhone(): String
    fun savePhone(phone: String)

    fun getTempId(): Long
}