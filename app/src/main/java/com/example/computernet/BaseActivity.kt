package com.example.computernet

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.example.computernet.service.SendService
import com.example.computernet.service.ServerService
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

open class BaseActivity: AppCompatActivity(){

    companion object{
        @JvmStatic
        private val TAG = "BaseActivity"
        var serverPort: Int = 11791
        var sendPort: Int = 11791
        var deviceAddress: String = "255"

        class ServerAsyncTask: AsyncTask<Unit, Unit, Unit>() {
            private var msgText:String = ""

            override fun doInBackground(vararg p0: Unit?) {
                while (!isCancelled){
                    try {
                        val serverSocket = ServerSocket(serverPort)
                        Log.e("ServerAsyncTask", "服务器开启")
                        val client: Socket = serverSocket.accept()
                        Log.e("ServerAsyncTask", "服务器连接")
                        val inputStream: InputStream = client.getInputStream()
                        val baoS = ByteArrayOutputStream()
                        copy(inputStream, baoS)
                        msgText = baoS.toString()
                        serverSocket.close()
                    }catch (e: IOException){
                        Log.e("ServerAsyncTask", e.message)
                    }
                }
            }
        }

        class SendAsyncTask: AsyncTask<String, Unit, Unit>(){
            //第一个参数为ip
            override fun doInBackground(vararg params: String) {
                val socket = Socket()
                try {
                    Log.e("WifiP2pSendService", "正在打开服务端")
                    socket.bind(null)
                    socket.connect(InetSocketAddress(deviceAddress, sendPort), 5000)

                    Log.e("WifiP2pSendService", "socket状态： ${socket.isConnected}")
                    val stream: OutputStream = socket.getOutputStream()
//                val cr: ContentResolver = application.contentResolver
                    val inputS: InputStream = ByteArrayInputStream(params[0].toByteArray())
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

    lateinit var mLocalBroadcastManager: LocalBroadcastManager

    fun send(msgText: String){
        val intent = Intent(this, SendService::class.java)
        intent.putExtra(SendService.CONTENT, msgText)
        intent.putExtra(SendService.IP_ADDRESS, deviceAddress)
        intent.putExtra(SendService.PORT, sendPort)
        startService(intent)
    }

    fun stopServer(){
        mLocalBroadcastManager.sendBroadcast(Intent(ServerService.STOP_SERVER))
    }

    fun startServer(port: Int){
        val intent = Intent(this, ServerService::class.java)
        intent.putExtra(ServerService.PORT, port)
        startService(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this)
    }
}