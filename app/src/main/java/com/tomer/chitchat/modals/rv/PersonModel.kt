package com.tomer.chitchat.modals.rv

import com.tomer.chitchat.room.MsgMediaType

data class PersonModel(
    val phoneNo:String,
    val name: String,
    val mediaType: MsgMediaType,
    val lastDate:String,
    val lastMsg:String,
    var isSelected : Boolean,
    val unreadCount : Int = 0,
    val isOnline : Boolean
)