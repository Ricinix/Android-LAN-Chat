package com.example.computernet.service

import android.app.IntentService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket


class ServerService : IntentService("ServerService") {
    companion object{
        const val RECEIVE_MSG: String = "receive_msg"
        const val PORT: String = "port"
        const val STOP_SERVER: String = "stop_server"
    }

    private val mLocalBroadcastManager = LocalBroadcastManager.getInstance(this)
    private lateinit var service: DatagramSocket
    private var running: Boolean = true

    override fun onHandleIntent(intent: Intent?) {

        val intentFilter = IntentFilter()
        intentFilter.addAction(STOP_SERVER)
        val receiver = ServerReceiver()
        mLocalBroadcastManager.registerReceiver(receiver, intentFilter)
        val port: Int? = intent?.getIntExtra(PORT, 11791)
        //包装IP地址
        //创建服务端的DatagramSocket对象，需要传入端口号
        service = DatagramSocket(port!!)

        var receiveMsg: String
        try {
            val receiveBytes = ByteArray(2048)
            //创建接受信息的包对象
            val receivePacket = DatagramPacket(receiveBytes, receiveBytes.size)

            Log.e("ServerService", "成功启动server")
            //开启一个死循环，不断接受数据
            while (running) {
                try {
                    //接收数据，程序会阻塞到这一步，直到收到一个数据包为止
                    service.receive(receivePacket)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Log.e("ServerService", "成功接收信息")
                //解析收到的数据
                receiveMsg = String(receivePacket.data, 0, receivePacket.length)
                //解析客户端地址
//                val clientAddress = receivePacket.address

//                //解析客户端端口
//                val clientPort = receivePacket.port

//                //组建响应信息
//                val response = "Hello world " + System.currentTimeMillis() + " " + receiveMsg
//                val responseBuf = response.toByteArray()
//                //创建响应信息的包对象，由于要发送到目的地址，所以要加上目的主机的地址和端口号
//                val responsePacket = DatagramPacket(responseBuf, responseBuf.size, clientAddress, clientPort)
//
//                try {
//                    //发送数据
//                    service.send(responsePacket)
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
                receiveMsg(receiveMsg)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            //关闭DatagramSocket对象
            service.close()
        }
        mLocalBroadcastManager.unregisterReceiver(receiver)
        Log.e("ServerService", "server关闭")
    }

    private fun receiveMsg(receiveMsg: String){
        Log.e("ServerService", "收到信息：$receiveMsg")
        val intent = Intent(RECEIVE_MSG)
        intent.putExtra("msg", receiveMsg)
        mLocalBroadcastManager.sendBroadcast(intent)
    }

    inner class ServerReceiver: BroadcastReceiver(){
        override fun onReceive(p0: Context?, intent: Intent?) {
            when (intent?.action){
                STOP_SERVER -> {
                    service.close()
                    running = false
                }
            }
        }

    }
}
