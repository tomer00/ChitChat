package com.tomer.chitchat.modals.rv

import com.tomer.chitchat.room.MsgMediaType
import java.io.File

data class PersonModel(
    var lastMsgId: Long,
    val phoneNo: String,
    val name: String,
    val mediaType: MsgMediaType,
    val lastDate: String,
    val lastMsg: String,
    var isSelected: Boolean,
    val unreadCount: Int = 0,
    val isOnline: Boolean,
    val fileGifImg: File?,
    val jsonText: String,
    val jsonName: String,
) {
    class Builder {
        private var lastMsgId: Long = -1L
        private var name: String = ""
        private var phoneNumber: String = ""
        private var messageMediaType: MsgMediaType = MsgMediaType.TEXT
        private var lastDate: String = ""
        private var lastMessage: String = ""
        private var unreadCount: Int = 0
        private var isOnline: Boolean = false
        private var lastMessageFile: File? = null
        private var jsonText: String = ""
        private var jsonName: String = ""


        fun phoneNumber(phoneNumber: String) = apply { this.phoneNumber = phoneNumber }
        fun name(name: String) = apply { this.name = name }
        fun messageMediaType(messageMediaType: MsgMediaType) = apply { this.messageMediaType = messageMediaType }
        fun lastDate(lastMessageTimestamp: String) = apply { this.lastDate = lastMessageTimestamp }
        fun lastMessage(lastMessage: String) = apply { this.lastMessage = lastMessage }
        fun unreadCount(unreadCount: Int) = apply { this.unreadCount = unreadCount }
        fun isOnline(isOnline: Boolean) = apply { this.isOnline = isOnline }
        fun lastMessageFile(lastMessageFile: File?) = apply { this.lastMessageFile = lastMessageFile }
        fun jsonText(messageContentJson: String) = apply { this.jsonText = messageContentJson }
        fun jsonName(messageSenderJson: String) = apply { this.jsonName = messageSenderJson }
        fun lastMsgId(lastMsgId: Long) = apply { this.lastMsgId = lastMsgId }

        fun build() = PersonModel(
            lastMsgId,
            phoneNumber,
            name,
            messageMediaType,
            lastDate,
            lastMessage,
            false,
            unreadCount,
            isOnline,
            lastMessageFile,
            jsonText = jsonText,
            jsonName = jsonName
        )
    }
}