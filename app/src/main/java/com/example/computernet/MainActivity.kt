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
    //recyclerView相关类的声明
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

        //动态申请权限
        requestPermission(listOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE))

        //更换设备名字的按钮
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

        //设置悬浮按钮，点击则发广播，让别人发现自己
        fab.setOnClickListener { view ->
            send("#broadcast#connect#name:$deviceName#")
            Snackbar.make(view, "已成功上线", Snackbar.LENGTH_LONG).setAction("action", null).show()
        }
        //侧方drawer部件相关设置
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        //设置drawer按钮的监听
        navView.setNavigationItemSelectedListener(this)

        //recyclerView的初始化
        msgRecyclerView!!.layoutManager = LinearLayoutManager(this)
        msgRecyclerView!!.adapter = adapter

        Log.e("MainActivity", "ip地址：${intToIp(dhcpInfo.ipAddress)}")
        Log.e("MainActivity", "子网掩码：${intToIp(dhcpInfo.netmask)}")
        Log.e("MainActivity", "广播号：${getBroadcastIp(dhcpInfo.ipAddress, dhcpInfo.netmask)}")
        Log.e("MainActivity", "网关：${intToIp(dhcpInfo.gateway)}")

        send("#broadcast#connect#name:$deviceName#")
        startServer(UdpPort, localIp)
    }

    //遍历列表，找出要删除的设备并删除
    private fun removeDevice(deviceAddress: String){
        for (de in deviceList){
            if (de.address == deviceAddress){
                Log.e("MainActivity", "正在删除$deviceAddress，总共有${deviceList.size}个")
                deviceList -= de
                adapter.notifyDataSetChanged()
            }
        }
    }

    //更新设备列表
    private fun refreshDeviceList(device: DeviceInfo, reply: Boolean){
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
        if (reply)
            sendToTarget("#broadcast#confirm#name:$deviceName#", device.address)
    }

    //当接收到传输文件的请求时
    private fun receiveFileRequest(address: String, msg: String){
        for (de in deviceList){
            //有人要向本设备发送文件，则弹出对话框
            if (de.address == address){
                AlertDialog.Builder(this@MainActivity).setTitle("${
                if(de.name != "default"){de.name}else{de.address}}想向你发送文件")
                    .setIcon(android.R.drawable.sym_def_app_icon)
                    .setPositiveButton("确定接收") { p0, p1 ->
                        ChatActivity.startThisActivity(
                            this@MainActivity, address, true, msg)
                    }.setNegativeButton("拒绝接收"){ p0, p1 ->
                        send("#broadcast#file#refuse#")
                    }.show()
            }
        }
    }

    //收到聊天记录，更新小圆点提示
    private fun receiveChatMsg(address: String, msg: String){
        Log.e("MainActivity", "收到聊天信息")
        for (de in deviceList){
            if (de.address == address){
                de.chatList.add(ChatMsg(msg, ChatMsg.TYPE_RECEIVED))
                de.new++
                adapter.notifyDataSetChanged()
            }
        }
    }

    //获取广播的IP地址（主机号全为1）
    private fun getBroadcastIp(ip: Int, netMask: Int): String{
        var net: Int = ip and netMask
        net = net or netMask.inv()
        return intToIp(net)
    }

    //关闭活动时发出离线消息
    override fun onDestroy() {
        send("#broadcast#disconnect#name:$deviceName#")
        stopServer()
        super.onDestroy()
    }

    //不可见时注销广播接收器
    override fun onPause() {
        super.onPause()
        mLocalBroadcastManager.unregisterReceiver(receiver)
    }

    //注册广播接收器
    override fun onResume() {
        super.onResume()
        //设置对方为局域网全部设备
        deviceAddress = getBroadcastIp(dhcpInfo.ipAddress, dhcpInfo.netmask)
        adapter.notifyDataSetChanged()
        //设置广播接收器
        val intentFilter = IntentFilter()
        intentFilter.addAction(ServerService.RECEIVE_MSG)
        intentFilter.addAction(SendService.SEND_FINISH)
        mLocalBroadcastManager.registerReceiver(receiver, intentFilter)
    }

    //drawer的相关操作
    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    //加载菜单栏
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    //菜单栏的监听
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_quit -> {
                //关闭应用
                finish()
                true
            }
            R.id.action_clear -> {
                //删除所有设备信息
                deviceList.clear()
                adapter.notifyDataSetChanged()
                msgRecyclerView!!.scrollToPosition(0)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    //drawer的按钮监听
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
                    deviceList.add(DeviceInfo("test device 1","1.1.1.1", mutableListOf(), 0))
                    deviceList.add(DeviceInfo("test device 2", "1.1.1.1", mutableListOf(), 0))
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

    //动态申请权限
    private fun requestPermission(permissionList: List<String>){
        for (permission: String in permissionList){
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(context, arrayOf(permission), 1)
            }
        }
    }

    //广播接收器
    inner class ServiceBroadcastReceiver: BroadcastReceiver(){
        override fun onReceive(p0: Context?, intent: Intent?) {
            when (intent?.action){
                //收到UDP数据
                ServerService.RECEIVE_MSG -> {
                    //获取内容
                    val msg: String = intent.getStringExtra("msg")
                    //获取对方IP地址
                    val address: String = intent.getStringExtra(ServerService.FROM_ADDRESS)
                    Log.e("MainActivity", "收到信息为：$msg")
                    //检验是不是特殊消息
                    if (Regex("#broadcast#").containsMatchIn(msg)){
                        Log.e("MainActivity", "收到接收发送的广播")
                        val name: String = Regex("(?<=#name:).*?(?=#)").find(msg)?.value ?: "default"
                        when {
                            Regex("#connect#").containsMatchIn(msg) -> {
                                refreshDeviceList(DeviceInfo(name, address, mutableListOf(), 0), true)
                            }
                            Regex("#disconnect#").containsMatchIn(msg) -> {
                                removeDevice(address)
                            }
                            Regex("#broadcast#confirm#").containsMatchIn(msg) -> {
                                refreshDeviceList(DeviceInfo(name, address, mutableListOf(), 0), false)
                            }
                            Regex("#file#").containsMatchIn(msg) ->
                                receiveFileRequest(address, msg)
                        }
                    }else {
                        //若不是特殊消息，则判断为用户发送的，记录并提示有多少条未读消息
                        receiveChatMsg(address, msg)
                    }

                }
            }
        }
    }
}
