package com.tomer.chitchat.retro

import com.tomer.chitchat.retro.modals.LoginResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface Api {

    @FormUrlEncoded
    @POST("/login")
    suspend fun getLoginToken(
        @Field("token") token: String
    ): Response<LoginResponse>

    @FormUrlEncoded
    @POST("/update/name")
    suspend fun updateName(
        @Field("name") name: String
    ): Response<String>

}