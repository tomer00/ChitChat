package com.tomer.chitchat.retro

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface Api {

    @FormUrlEncoded
    @POST("/login")
    fun getLoginToken(@Field("token") token: String): String

}