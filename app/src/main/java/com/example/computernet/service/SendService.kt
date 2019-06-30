package com.example.computernet.service

import android.app.IntentService
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import java.lang.Exception
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class SendService : IntentService("SendService") {
    companion object{
        const val SEND_FINISH: String = "send_finsih"
        const val CONTENT: String = "msg_text"
        const val IP_ADDRESS: String = "ip_address"
        const val PORT: String = "port"
    }
    private var msg: String? = ""
    private lateinit var mLocalBroadcastManager: LocalBroadcastManager
    override fun onCreate() {
        super.onCreate()
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this)
    }

    override fun onHandleIntent(intent: Intent?) {
        msg = intent?.getStringExtra(CONTENT)
        val ipAddress: String? = intent?.getStringExtra(IP_ADDRESS)
        val port: Int? = intent?.getIntExtra(PORT, 11791)
        val client = DatagramSocket()
        try {
            //将字符串转换成Byte数组
            val sendBytes: ByteArray? = msg?.toByteArray()
            val address: InetAddress = InetAddress.getByName(ipAddress)
            //打包发送
            val sendPacket = DatagramPacket(sendBytes, sendBytes!!.size, address, port!!)

            try {
                client.send(sendPacket)
            }catch (e: Exception){
                e.printStackTrace()
            }
        }catch (e: Exception){
            e.printStackTrace()
        }finally {
            client.close()
        }
        sendFinish()
    }

    private fun sendFinish(){
        Log.e("SendService", "成功UDP发送消息")
        val intent = Intent(SEND_FINISH)
        intent.putExtra("msg", msg)
        mLocalBroadcastManager.sendBroadcast(intent)
    }
}
