package com.example.computernet

import android.content.*
import android.content.pm.PackageManager
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
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.example.computernet.service.SendService
import com.example.computernet.service.ServerService

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {
    companion object{
        var debugMode = 0
    }
    private val adapter = MsgAdapter(deviceList, this)
    private var msgRecyclerView: RecyclerView? = null
    private val context = this
    private val receiver: ServiceBroadcastReceiver = ServiceBroadcastReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //加载部件
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val fab: FloatingActionButton = findViewById(R.id.fab)
        val headView: View = navView.getHeaderView(0)
        val nameTextView: TextView = headView.findViewById(R.id.header_name)
        val renameButton: ImageButton = headView.findViewById(R.id.imageView)
        msgRecyclerView = findViewById(R.id.msg_recycler_view)
        //设置toolbar
        setSupportActionBar(toolbar)

        renameButton.setOnClickListener { view ->
            val et = EditText(this)
            AlertDialog.Builder(this).setTitle("请输入设备名字")
                .setIcon(android.R.drawable.sym_def_app_icon)
                .setView(et)
                .setPositiveButton("确定") { p0, p1 ->
                    nameTextView.text = et.text.toString()
                    deviceName = et.text.toString()
                }.setNegativeButton("取消",null).show()
            send("#broadcast#connect#name:$deviceName#")
        }

        //设置悬浮按钮，点击则搜索
        fab.setOnClickListener { view ->
            send("#broadcast#connect#name:$deviceName#")
            Snackbar.make(view, "已成功上线", Snackbar.LENGTH_LONG).setAction("action", null).show()
        }
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)

        msgRecyclerView!!.layoutManager = LinearLayoutManager(this)
        msgRecyclerView!!.adapter = adapter

        Log.e("MainActivity", "ip地址：${intToIp(dhcpInfo.ipAddress)}")
        Log.e("MainActivity", "子网掩码：${intToIp(dhcpInfo.netmask)}")
        Log.e("MainActivity", "广播号：${getBroadcastIp(dhcpInfo.ipAddress, dhcpInfo.netmask)}")
        Log.e("MainActivity", "网关：${intToIp(dhcpInfo.gateway)}")

        send("#broadcast#connect#name:$deviceName#")
        startServer(serverPort, localIp)
    }

    private fun removeDevice(deviceAddress: String){
        var i = -1
        for (de in deviceList){
            i++
            if (de.address == deviceAddress){
                break
            }
        }
        if (i >= 0){
            Log.e("MainActivity", "正在删除第${i}个，总共有${deviceList.size}个")
            deviceList.removeAt(i)
            adapter.notifyDataSetChanged()
        }
    }

    private fun refreshDeviceList(device: DeviceInfo){
        var has = false
        for (de in deviceList){
            if (device.address == de.address)
                de.name = device.name
                adapter.notifyDataSetChanged()
                has = true
        }
        if (device.address == localIp){
            has = true
        }
        if (!has){
            Log.e("MainActivity", "添加第${deviceList.size + 1}个设备")
            Log.e("MainActivity", "正在添加的设备名字为: ${device.name}")
            deviceList.add(device)
            adapter.notifyItemChanged(deviceList.size - 1)
            msgRecyclerView!!.scrollToPosition(deviceList.size - 1)
        }
    }

    private fun getBroadcastIp(ip: Int, netMask: Int): String{
        var net: Int = ip and netMask
        net = net or netMask.inv()
        return intToIp(net)
    }

    override fun onDestroy() {
        send("#broadcast#disconnect#name:$deviceName#")
        stopServer()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        mLocalBroadcastManager.unregisterReceiver(receiver)
    }

    override fun onResume() {
        super.onResume()
        deviceAddress = getBroadcastIp(dhcpInfo.ipAddress, dhcpInfo.netmask)
        adapter.notifyDataSetChanged()
        val intentFilter = IntentFilter()
        intentFilter.addAction(ServerService.RECEIVE_MSG)
        intentFilter.addAction(SendService.SEND_FINISH)
        mLocalBroadcastManager.registerReceiver(receiver, intentFilter)
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
            R.id.action_clear -> {
                deviceList.clear()
                adapter.notifyDataSetChanged()
                msgRecyclerView!!.scrollToPosition(0)
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
                stopServer()
            }
            R.id.nav_gallery -> {

            }
            R.id.nav_slideshow -> {

            }
            R.id.nav_tools -> {
                if (debugMode == 0){
                    debugMode = 1
                    val deviceTest1 = DeviceInfo("test device 1","1.1.1.1", mutableListOf(), 0)
                    deviceList.add(deviceTest1)
                    val deviceTest2 = DeviceInfo("test device 2", "1.1.1.1", mutableListOf(), 0)
                    deviceList.add(deviceTest2)
                    adapter.notifyItemChanged(deviceList.size - 1)
                    msgRecyclerView!!.scrollToPosition(deviceList.size - 1)
                }else{
                    debugMode = 0
                    deviceList.clear()
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
                ServerService.RECEIVE_MSG -> {
                    val msg: String = intent.getStringExtra("msg")
                    val address: String = intent.getStringExtra(ServerService.FROM_ADDRESS)
                    Log.e("MainActivity", "收到信息为：$msg")
                    if (Regex("#broadcast#").containsMatchIn(msg)){
                        Log.e("MainActivity", "收到接收发送的广播")
                        val name: String? = Regex("(?<=#name:).*?(?=#)").find(msg)?.value
                        if (Regex("#connect#").containsMatchIn(msg)){
                            refreshDeviceList(DeviceInfo(name?:"default", address, mutableListOf(), 0))
                        }
                        else if (Regex("#disconnect#").containsMatchIn(msg)){
                            removeDevice(address)
                        }
                    }else {
                        Log.e("MainActivity", "收到聊天信息")
                        val address: String = intent.getStringExtra(ServerService.FROM_ADDRESS)
                        for (de in deviceList){
                            if (de.address == address){
                                de.chatList.add(ChatMsg(msg, ChatMsg.TYPE_RECEIVED))
                                de.new++
                                adapter.notifyDataSetChanged()
                            }
                        }
                    }

                }
            }
        }
    }
}
