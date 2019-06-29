package com.example.computernet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.DhcpInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.view.MenuItem
import android.support.v4.widget.DrawerLayout
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import com.example.computernet.service.SendService
import com.example.computernet.service.ServerService
import java.net.InetAddress

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {
    companion object{
        var debugMode = 0
    }
    private var localIp: String = ""
    private val mList = mutableListOf<DeviceInfo>()
    private val adapter = MsgAdapter(mList, this)
    private var msgRecyclerView: RecyclerView? = null
    private val context = this
    private val receiver: ServiceBroadcastReceiver = ServiceBroadcastReceiver()
    private lateinit var mLocalBroadcastManager: LocalBroadcastManager
    private lateinit var wifiManager: WifiManager
    private lateinit var dhcpInfo: DhcpInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //加载部件
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val fab: FloatingActionButton = findViewById(R.id.fab)
        msgRecyclerView = findViewById(R.id.msg_recycler_view)
        //设置toolbar
        setSupportActionBar(toolbar)

        //设置悬浮按钮，点击则搜索
        fab.setOnClickListener { view ->
            searchIp()
            Snackbar.make(view, "成功告诉别人我在哪", Snackbar.LENGTH_LONG).setAction("action", null).show()
        }
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)

        msgRecyclerView!!.layoutManager = LinearLayoutManager(this)
        msgRecyclerView!!.adapter = adapter

        wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        dhcpInfo = wifiManager.dhcpInfo
        localIp = intToIp(dhcpInfo.ipAddress)
        Log.e("MainActivity", "ip地址：${intToIp(dhcpInfo.ipAddress)}")
        Log.e("MainActivity", "子网掩码：${intToIp(dhcpInfo.netmask)}")
        Log.e("MainActivity", "广播号：${getBroadcastIp(dhcpInfo.ipAddress, dhcpInfo.netmask)}")
        Log.e("MainActivity", "网关：${intToIp(dhcpInfo.gateway)}")

        send("#port:11691#ip:$localIp")
        startServer(serverPort, localIp)
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this)
    }

    override fun onPause() {
        super.onPause()
        mLocalBroadcastManager.unregisterReceiver(receiver)
    }

    override fun onResume() {
        super.onResume()
        deviceAddress = getBroadcastIp(dhcpInfo.ipAddress, dhcpInfo.netmask)
        val intentFilter = IntentFilter()
        intentFilter.addAction(ServerService.RECEIVE_MSG)
        intentFilter.addAction(SendService.SEND_FINISH)
        mLocalBroadcastManager.registerReceiver(receiver, intentFilter)
    }

    private fun refreshDeviceList(device: DeviceInfo){
        var has = false
        for (de in mList){
            if (device.address == de.address)
                has = true
        }
        if (!has){
            mList.add(device)
            adapter.notifyItemChanged(mList.size - 1)
            msgRecyclerView!!.scrollToPosition(mList.size - 1)
        }
    }

    private fun startServer(port: Int, localIp: String){
        val intent = Intent(this, ServerService::class.java)
        intent.putExtra(ServerService.PORT, port)
        startService(intent)
    }

    private fun searchIp(){
//        stopService(Intent(this, ServerService::class.java))
//        Log.e("MainActivity", "已暂停server")
        send("#port:11691#ip:$localIp")
    }

    private fun send(msgText: String){
        val intent = Intent(this, SendService::class.java)
        intent.putExtra(SendService.CONTENT, msgText)
        intent.putExtra(SendService.IP_ADDRESS, deviceAddress)
        intent.putExtra(SendService.PORT, sendPort)
        startService(intent)
    }

    private fun intToIp(paramInt: Int): String {
        return ((paramInt and 0xFF).toString() + "." + (0xFF and (paramInt shr 8)) + "." + (0xFF and (paramInt shr 16)) + "."
                + (0xFF and (paramInt shr 24)))
    }

    private fun getBroadcastIp(ip: Int, netMask: Int): String{
        var net: Int = ip and netMask
        net = net or netMask.inv()
        return intToIp(net)
    }

    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_quit -> {
                finish()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_home -> {
                // Handle the camera action
            }
            R.id.nav_gallery -> {

            }
            R.id.nav_slideshow -> {

            }
            R.id.nav_tools -> {
                if (debugMode == 0){
                    debugMode = 1
                    val deviceTest1 = DeviceInfo("test device 1", 123)
                    mList.add(deviceTest1)
                    val deviceTest2 = DeviceInfo("test device 2", 321)
                    mList.add(deviceTest2)
                    adapter.notifyItemChanged(mList.size - 1)
                    msgRecyclerView!!.scrollToPosition(mList.size - 1)
                }else{
                    debugMode = 0
                    mList.clear()
                    adapter.notifyDataSetChanged()
                    msgRecyclerView!!.scrollToPosition(0)
                }

            }
            R.id.nav_share -> {

            }
            R.id.nav_send -> {

            }
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun requestPermission(permissionList: List<String>){
        for (permission: String in permissionList){
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(context, arrayOf(permission), 1)
            }
        }
    }

    inner class ServiceBroadcastReceiver: BroadcastReceiver(){
        override fun onReceive(p0: Context?, intent: Intent?) {
            when (intent?.action){
                SendService.SEND_FINISH -> {
                    Log.e("MainActivity", "收到成功发送的广播")
//                    startServer(serverPort, localIp)
                }
                ServerService.RECEIVE_MSG -> {
                    Log.e("MainActivity", "收到接收发送的广播")
                    val msg: String = intent.getStringExtra("msg")
//                    val address: String? = Regex("#ip:.*?").find(msg)?.value
//                    val port: Int? = Regex("#port:.*?").find(msg)?.value?.toInt()
                    Log.e("MainActivity", "收到信息为：$msg")
//                    refreshDeviceList(DeviceInfo(address!!, port!!))
                    refreshDeviceList(DeviceInfo(msg, 123))
                }
            }
        }
    }
}
