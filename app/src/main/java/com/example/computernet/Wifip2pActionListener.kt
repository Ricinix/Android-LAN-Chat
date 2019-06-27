package com.example.computernet

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager

interface Wifip2pActionListener: WifiP2pManager.ChannelListener{

    fun wifiP2pEnabled(enabled: Boolean)

    fun onConnection(wifiP2pInfo: WifiP2pInfo)

    fun onDisconnection()

    fun onDeviceInfo(wifiP2pDevice: WifiP2pDevice)

    fun onPeersInfo(wifiP2pDeviceList: Collection<WifiP2pDevice>)
}