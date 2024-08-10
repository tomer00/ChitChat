package com.tomer.chitchat.repo

import com.tomer.chitchat.room.MsgMediaType

interface RepoStorage {

    fun saveBytesToFolder(type: MsgMediaType, fileName: String, data: ByteArray)
    fun getBytesFromFolder(type: MsgMediaType, fileName: String): ByteArray?
    fun getBytesOfVideoThumb(mediaFileName: String):ByteArray?
    fun saveVideoThumb(mediaFileName: String, data: ByteArray)
    fun isPresent(mediaFileName: String, type: MsgMediaType): Boolean
}