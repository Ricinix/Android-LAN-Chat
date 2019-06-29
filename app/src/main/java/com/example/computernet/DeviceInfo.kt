package com.example.computernet

data class DeviceInfo(var name:String, val address: String, val chatList: MutableList<ChatMsg>, var new: Int)