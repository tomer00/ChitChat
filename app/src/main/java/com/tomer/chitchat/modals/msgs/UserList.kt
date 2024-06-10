package com.tomer.chitchat.modals.msgs

data class UserList(
    val users: List<Long>,
    val phone: Long
) {
    override fun toString(): String =
        "--**USERS**--{\"phone\":$phone,\"users\":${users}}"
}
