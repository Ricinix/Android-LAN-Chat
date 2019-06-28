package com.example.computernet

import android.app.IntentService
import android.content.ContentResolver
import android.content.Intent
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

class WifiP2pSendService : IntentService("WifiP2pSendService") {
    companion object{
        @JvmStatic
        val MSG_CONTENT: String = "msg_content"
        @JvmStatic
        val ACTION_SEND_MSG: String = "com.example.computerNet.SEND_MSG"
        @JvmStatic
        val EXTRAS_ADDRESS: String = "go_address"
        @JvmStatic
        val EXTRAS_GROUP_OWNER_PORT: String = "go_port"
    }
    override fun onHandleIntent(intent: Intent?) {

        if (intent?.action.equals(ACTION_SEND_MSG)){
            val msgText: String? = intent?.getStringExtra(MSG_CONTENT)
            val host: String? = intent?.getStringExtra(EXTRAS_ADDRESS)
            val socket = Socket()
            val port: Int = intent?.getIntExtra(EXTRAS_GROUP_OWNER_PORT, 8988) ?: 8988

            try {
                Log.e("WifiP2pSendService", "正在打开服务端")
                socket.bind(null)
                socket.connect(InetSocketAddress(host, port), 5000)

                Log.e("WifiP2pSendService", "socket状态： ${socket.isConnected}")
                val stream: OutputStream = socket.getOutputStream()
//                val cr: ContentResolver = application.contentResolver
                val inputS: InputStream = ByteArrayInputStream(msgText?.toByteArray())
                copy(inputS, stream)


            }catch (e: IOException){
                Log.e("WifiP2pSendService", e.message)
            }finally {
                if (socket.isConnected){
                    try {
                        socket.close()
                    }catch (e: IOException){
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun copy(inputStream: InputStream, out: OutputStream){
        val buf = ByteArray(1024)
        var len: Int
        try {
            len = inputStream.read(buf)
            while (len != -1) {
                out.write(buf, 0, len)
                len = inputStream.read(buf)
            }
            out.close()
            inputStream.close()
        } catch (e: IOException) {
            Log.d("WifiP2pSendService", e.toString())
        }

    }


}
