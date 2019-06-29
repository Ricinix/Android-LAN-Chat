package com.example.computernet

data class ChatMsg(val content: String, val type: Int) {
    companion object{
        const val TYPE_RECEIVED = 0
        const val TYPE_SEND = 1
        const val TYPE_WRONG = -1
    }
}