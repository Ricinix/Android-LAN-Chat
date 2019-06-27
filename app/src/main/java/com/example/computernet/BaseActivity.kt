package com.example.computernet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast

open class BaseActivity: AppCompatActivity(), Wifip2pActionListener {

    companion object{
        @JvmStatic
        private val TAG = "BaseActivity"
    }
    var mWifiP2pManager: WifiP2pManager? = null
    var mChannel: WifiP2pManager.Channel? = null
    private lateinit var mWifiP2pReceiver: WifiP2pReceiver
    private lateinit var mWifiP2pInfo: WifiP2pInfo

    class WifiP2pReceiver(private val wifiP2pManager: WifiP2pManager?,
                          private val channel: WifiP2pManager.Channel?,
                          private val listener: Wifip2pActionListener): BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action: String? = intent!!.action
            when(action){
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    //检查现在P2P可不可用
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    when (state) {
                        WifiP2pManager.WIFI_P2P_STATE_ENABLED -> listener.wifiP2pEnabled(true)
                        else -> listener.wifiP2pEnabled(false)
                    }
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    //当发现新可连接设备
                    Log.e(TAG, "发现新设备")
                    val peers: WifiP2pDeviceList = intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST)
                    listener.onPeersInfo(peers.deviceList)
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    //设备连接发生变化
                    val networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO) as NetworkInfo
                    if (networkInfo.isConnected){
                        wifiP2pManager!!.requestConnectionInfo(channel) {
                                info -> listener.onConnection(info!!)
                        }
                    }else {
                        listener.onDisconnection()
                    }
                }
                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    //设备信息发生变化
                    val device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE) as WifiP2pDevice
                    listener.onDeviceInfo(device)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mWifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
        mChannel = mWifiP2pManager?.initialize(this, mainLooper, this)
        mWifiP2pReceiver = WifiP2pReceiver(mWifiP2pManager, mChannel, this)
    }

    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        registerReceiver(mWifiP2pReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mWifiP2pReceiver)
    }

    override fun wifiP2pEnabled(enabled: Boolean) {
        if (!enabled)
            Toast.makeText(this, "P2p不可用，请打开Wifi后再尝试", Toast.LENGTH_LONG).show()
    }

    override fun onConnection(wifiP2pInfo: WifiP2pInfo) {
        mWifiP2pInfo = wifiP2pInfo
//        Toast.makeText(this, "Info: $wifiP2pInfo", Toast.LENGTH_LONG).show()
        Log.e(TAG, "info: $mWifiP2pInfo")
    }

    override fun onDisconnection() {
//        Toast.makeText(this, "连接断开", Toast.LENGTH_LONG).show()
        Log.e(TAG, "连接断开")
    }

    override fun onDeviceInfo(wifiP2pDevice: WifiP2pDevice) {
//        Toast.makeText(this, "当前设备名字 ${wifiP2pDevice.deviceName}", Toast.LENGTH_LONG).show()
        Log.e(TAG, "当前设备名字 ${wifiP2pDevice.deviceName}")
    }

    override fun onPeersInfo(wifiP2pDeviceList: Collection<WifiP2pDevice>){
        val deviceNameList = mutableListOf<String>()
        for (device in wifiP2pDeviceList){
//           Log.e(TAG, "连接的设备是： ${device.deviceName} ------------ ${device.deviceAddress}")
            deviceNameList.add(device.deviceName)
        }
    }

    override fun onChannelDisconnected() {
        return
    }
}