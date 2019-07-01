package com.example.computernet

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.DhcpInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import com.example.computernet.service.SendService
import com.example.computernet.service.ServerService

open class BaseActivity: AppCompatActivity(){

    companion object{
        @JvmStatic
        private val TAG = "BaseActivity"
        var serverPort: Int = 11791
        var sendPort: Int = 11791
        const val TcpPort: Int = 11697
        var deviceAddress: String = "255"
        var deviceName: String = "default"
        val deviceList = mutableListOf<DeviceInfo>()
    }
    var localIp: String = ""
    lateinit var dhcpInfo: DhcpInfo
    private lateinit var wifiManager: WifiManager

    lateinit var mLocalBroadcastManager: LocalBroadcastManager
    lateinit var mNotificationManager: NotificationManager

    //启动UDP发送的服务
    fun send(msgText: String){
        val intent = Intent(this, SendService::class.java)
        intent.putExtra(SendService.CONTENT, msgText)
        intent.putExtra(SendService.IP_ADDRESS, deviceAddress)
        intent.putExtra(SendService.PORT, sendPort)
        startService(intent)
    }

    //向特定IP地址发送UDP
    fun sendToTarget(msgText: String, ip: String){
        val intent = Intent(this, SendService::class.java)
        intent.putExtra(SendService.CONTENT, msgText)
        intent.putExtra(SendService.IP_ADDRESS, ip)
        intent.putExtra(SendService.PORT, sendPort)
        startService(intent)
    }

    //停止UDP的接收监听
    fun stopServer(){
        mLocalBroadcastManager.sendBroadcast(Intent(ServerService.STOP_SERVER))
    }

    //开启UDP的接收监听
    fun startServer(port: Int, localHost: String){
        val intent = Intent(this, ServerService::class.java)
        intent.putExtra(ServerService.PORT, port)
        intent.putExtra(ServerService.LOCAL_HOST, localHost)
        startService(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this)
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = NotificationChannel("Progress", "Translate", NotificationManager.IMPORTANCE_HIGH)
        mNotificationManager.createNotificationChannel(notificationChannel)
        wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        dhcpInfo = wifiManager.dhcpInfo
        localIp = intToIp(dhcpInfo.ipAddress)
    }

    //IP地址的转换
    fun intToIp(paramInt: Int): String {
        return ((paramInt and 0xFF).toString() + "." + (0xFF and (paramInt shr 8)) + "." + (0xFF and (paramInt shr 16)) + "."
                + (0xFF and (paramInt shr 24)))
    }


}