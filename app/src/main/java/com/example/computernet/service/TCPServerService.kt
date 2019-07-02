package com.example.computernet.service

import android.app.IntentService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.Exception
import java.net.ServerSocket
import java.net.Socket

class TCPServerService : IntentService("TCPServerService") {
    companion object{
        const val PORT: String = "tcp_server_service_port"
        const val FILE_URL: String = "tcp_server_service_file_url"
        const val LENGTH_PROGRESS: String = "tcp_server_service_length_progress"
        const val NOW_PROGRESS: String = "tcp_server_service_now_progress"

        const val PROGRESS_UPDATE: String = "tcp_server_service_progress_update"
        const val RECEIVE_FINISH: String = "tcp_server_service_receive_finish"
        const val SHUTDOWN: String = "tcp_server_service_receive_shutdown"
    }

    private val mLocalBroadcastManager = LocalBroadcastManager.getInstance(this)
    private val receiver = TcpServerReceiver()
    private lateinit var serverSocket: ServerSocket
    private lateinit var connect: Socket

    override fun onHandleIntent(intent: Intent?) {
        val port: Int = intent?.getIntExtra(PORT, 11697) ?: 11697
        val path: String = intent?.getStringExtra(FILE_URL) ?: ""
        val fileSize: Long = intent?.getLongExtra(LENGTH_PROGRESS, 100) ?: 100
        val intentFilter = IntentFilter(SHUTDOWN)
        mLocalBroadcastManager.registerReceiver(receiver, intentFilter)
        try {
            serverSocket = ServerSocket(port)
            serverSocket.soTimeout = 3000
            //连接socket
            connect = serverSocket.accept()
            Log.e("TcpServerService", "已成功启动TcpServer")
            Log.e("TcpServerService", "路径为$path")
            //打开文件
            val f = File(path)
            val dirs = File(f.parent)
            Log.e("TcpServerService", "正在接收的文件名为${f.name}")
            if (!dirs.exists()){
                dirs.mkdirs()
                Log.e("TcpServerService", "创建Download目录")
            }
            //创建文件
            if (f.createNewFile())
                Log.e("TcpServerService", "成功创建文件 $path")
            else
                Log.e("TcpServerService", "已存在")

            //获取输入流
            val inStream: InputStream = connect.getInputStream()
            val fileOutputStream  = FileOutputStream(f)
            val buffer = ByteArray(10240)
            var len: Int = 0
            var total: Long = 0
            while (inStream.read(buffer).also { len = it } != -1 ){
                fileOutputStream.write(buffer, 0, len)
                total += len
                updateProgress(total, fileSize)
            }
            fileOutputStream.close()
            inStream.close()
            serverSocket.close()
            receiveFinish()
        }catch (e: Exception){
            e.printStackTrace()
        }finally {
            Log.e("TcpServerService", "已结束")
        }
        mLocalBroadcastManager.unregisterReceiver(receiver)
    }

    //更新进度条
    private fun updateProgress(now: Long, size: Long){
        Log.e("TcpSendService", "正在更新进度条：$now / $size")
        val intent = Intent(PROGRESS_UPDATE)
        intent.putExtra(NOW_PROGRESS, now)
        intent.putExtra(LENGTH_PROGRESS, size)
        mLocalBroadcastManager.sendBroadcast(intent)
    }

    //接收成功的通知
    private fun receiveFinish(){
        Log.e("TcpServerService", "成功接收文件")
        val intent = Intent(RECEIVE_FINISH)
        mLocalBroadcastManager.sendBroadcast(intent)
    }

    //关闭TCP server
    inner class TcpServerReceiver: BroadcastReceiver(){
        override fun onReceive(p0: Context?, intent: Intent?) {
            when (intent?.action){
                SHUTDOWN -> {
                    serverSocket.takeIf { !it.isClosed }?.close()
                    connect.takeIf { it.isConnected }?.close()
                }
            }
        }

    }
}
