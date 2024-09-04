package com.tomer.chitchat.repo.impl

import android.content.Context
import android.os.Build
import com.tomer.chitchat.repo.RepoStorage
import com.tomer.chitchat.room.MsgMediaType
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

class RepoFileStorage @Inject constructor(
    context: Context
) : RepoStorage {


    private val folder =
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q)
            File(
                File(context.getExternalFilesDir("ChitChat"), "media")
                    .absolutePath.replace("Android/data", "Android/media")
                    .replace(".chitchat/files", ".chitchat")
            )
        else File(context.getExternalFilesDir("ChitChat"), "media")

    override fun saveBytesToFolder(type: MsgMediaType, fileName: String, data: ByteArray) {
        val folderChild = File(folder, "/$type")
        folderChild.mkdirs()
        val file = File(folderChild, fileName)
        FileOutputStream(file).use {
            it.write(data)
        }
    }

    override fun saveVideoThumb(mediaFileName: String, data: ByteArray) {
        val folderChild = File(folder, "/VideoThumbs")
        folderChild.mkdirs()
        val file = File(folderChild, mediaFileName)
        FileOutputStream(file).use {
            it.write(data)
        }
    }

    override fun isPresent(mediaFileName: String, type: MsgMediaType): Boolean {
        val folderChild = File(folder, "/$type")
        val file = File(folderChild, mediaFileName)
        return file.exists()
    }

    override fun deleteFile(mediaFileName: String?, msgType: MsgMediaType) {
        if (mediaFileName == null) return
        val file = File(folder, "/$msgType/$mediaFileName")
        if (file.exists()) file.delete()
    }

    override fun getBytesFromFolder(type: MsgMediaType, fileName: String): ByteArray? {
        val file = File(folder, "/$type/$fileName")
        if (!file.exists()) return null
        FileInputStream(file).use {
            return it.readBytes()
        }
    }

    override fun getFileFromFolder(type: MsgMediaType, fileName: String): File? {
        val file = File(folder, "/$type/$fileName")
        return if (!file.exists()) null
        else file
    }

    override fun getBytesOfVideoThumb(mediaFileName: String): ByteArray? {
        val file = File(folder, "/VideoThumbs/$mediaFileName")
        if (!file.exists()) return null
        FileInputStream(file).use {
            return it.readBytes()
        }
    }


}