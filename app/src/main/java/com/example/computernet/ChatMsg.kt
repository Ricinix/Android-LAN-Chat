package com.example.computernet

data class ChatMsg(val content: String, val type: Int) {
    companion object{
        val TYPE_RECEIVED = 0
        val TYPE_SEND = 1
    }
}