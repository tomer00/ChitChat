package com.tomer.chitchat.modals.states

enum class FlowType {
    MSG,
    UPLOAD_SUCCESS,
    UPLOAD_FAILS,
    DOWNLOAD_SUCCESS,
    DOWNLOAD_FAILS,
    RELOAD_RV,

    SERVER_REC,
    PARTNER_REC,

    TYPING,
    NO_TYPING,
    ONLINE,
    OFFLINE,
    SEND_NEW_CONNECTION_REQUEST,

    ACCEPT_REQ,
    REJECT_REQ,

    CHANGE_GIF,
    SHOW_BIG_JSON,
    SHOW_BIG_GIF,
}