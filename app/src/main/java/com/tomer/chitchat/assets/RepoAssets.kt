package com.tomer.chitchat.assets

import com.tomer.chitchat.modals.states.UiMsgModal
import java.io.File

interface RepoAssets {

    suspend fun getLottieJson(name: String): String?
    suspend fun getGoogleLottieJson(nameJson: String): String?

    suspend fun getGifFile(name: String): File?
    suspend fun getGifTelemoji(name: String): File?

    suspend fun getRandomJson(): UiMsgModal

    suspend fun downLoadBytes(urlString: String) : ByteArray?

}