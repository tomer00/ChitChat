package com.tomer.chitchat.repo

interface RepoUtils {
    fun getToken():String
    fun saveToken(token:String)
}