package com.tomer.chitchat.repo

interface RepoUtils {
    fun getToken():String
    fun saveToken(token:String)

    fun getName():String
    fun saveName(name:String)
    fun getTempId():Long
}