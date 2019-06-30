package com.example.computernet.service

import android.app.IntentService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import java.io.*
import java.lang.Exception
import java.net.Socket


class TCPSendService : IntentService("TCPSendService") {

    companion object{
        //intent值
        const val URL: String = "tcp_send_service_url"
        const val ADDRESS: String = "tcp_send_service_address"
        const val PORT: String = "tcp_send_service_port"
        const val LENGTH_PROGRESS: String = "tcp_send_service_length_progress"
        const val NOW_PROGRESS: String = "tcp_send_service_now_progress"

        //action值
        const val PROGRESS_UPDATE: String = "tcp_send_service_progress_update"
        const val SEND_FINISH: String = "tcp_send_service_send_finish"
        const val SEND_SHUTDOWN: String = "tcp_send_service_send_shutdown"
    }

    private lateinit var mLocalBroadcastManager: LocalBroadcastManager
    private lateinit var client: Socket
    override fun onCreate() {
        super.onCreate()
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this)
    }

    override fun onHandleIntent(intent: Intent?) {
        val url: String = intent?.getStringExtra(URL) ?: ""
        val address: String = intent?.getStringExtra(ADDRESS) ?: "127.0.0.1"
        val port: Int = intent?.getIntExtra(PORT, 11697) ?: 11697
        val length: Long = intent?.getLongExtra(LENGTH_PROGRESS, 100) ?: 100
        try {
            client = Socket(address, port)
            val out: OutputStream = client.getOutputStream()
            val cr = applicationContext.contentResolver
            val inputStream: InputStream? = cr.openInputStream(Uri.parse(url))
//            val fileInputStream = FileInputStream(File(url))
            var len = 0
            val buffer = ByteArray(1024)
            var total: Long = 0

            while (inputStream!!.read(buffer).also { len = it } != -1 ){
                out.write(buffer, 0, len)
                total += len
                updateProgress(total, length)
            }
//            len = fileInputStream.read(buffer)
//            while (len != -1){
//                out.write(buffer, 0, len)
//                total += len
//                updateProgress(total, )
//            }
            out.close()
            inputStream.close()
            client.takeIf { it.isConnected }?.close()
        }catch (e: Exception) {
            e.printStackTrace()
        }finally {
            sendFinish()
        }
    }

    private fun sendFinish(){
        Log.e("TCPSendService", "成功发送文件")
        val intent = Intent(SEND_FINISH)
        mLocalBroadcastManager.sendBroadcast(intent)
    }

    private fun updateProgress(now: Long, length: Long){
        Log.e("TcpSendService", "正在更新进度条：$now / $length")
        val intent = Intent(PROGRESS_UPDATE)
        intent.putExtra(NOW_PROGRESS, now)
        intent.putExtra(LENGTH_PROGRESS, length)
        mLocalBroadcastManager.sendBroadcast(intent)
    }

    inner class TcpSendReceiver: BroadcastReceiver(){
        override fun onReceive(p0: Context?, intent: Intent?) {
            when (intent?.action){
                SEND_SHUTDOWN -> {
                    client.close()
                    Log.e("TCPSendService", "已关闭TCP发送")
                }
            }
        }

    }
}
