package com.tomer.chitchat.modals.msgs

class UserList(
     users: List<Long>,
     phone: String
) : Message(
    "--**USERS**--{\"phone\":$phone,\"users\":${users}}"
)
