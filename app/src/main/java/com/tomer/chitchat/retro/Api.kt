package com.tomer.chitchat.retro

import com.tomer.chitchat.retro.modals.LoginResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

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

    @FormUrlEncoded
    @POST("/update/firebase")
    suspend fun updateNotificationToken(
        @Field("notiId") name: String
    ): Response<String>

    @FormUrlEncoded
    @POST("/update/about")
    suspend fun updateAbout(
        @Field("about") name: String
    ): Response<String>

    @Multipart
    @POST("/update/uploadImage")
    suspend fun uploadProfileImage(
        @Part file: MultipartBody.Part
    ): Response<String>



}