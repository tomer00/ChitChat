package com.tomer.chitchat.repo

import com.tomer.chitchat.room.MsgMediaType
import java.io.File

interface RepoStorage {

    fun saveBytesToFolder(type: MsgMediaType, fileName: String, data: ByteArray)
    fun getBytesFromFolder(type: MsgMediaType, fileName: String): ByteArray?
    fun getFileFromFolder(type: MsgMediaType, fileName: String): File?
    fun getBytesOfVideoThumb(mediaFileName: String):ByteArray?
    fun saveVideoThumb(mediaFileName: String, data: ByteArray)
    fun isPresent(mediaFileName: String, type: MsgMediaType): Boolean
    fun deleteFile(mediaFileName: String?, msgType: MsgMediaType)
}