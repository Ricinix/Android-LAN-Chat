package com.example.computernet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.view.MenuItem
import android.support.v4.widget.DrawerLayout
import android.support.design.widget.NavigationView
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.View
import android.widget.Toast

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {
    companion object{
        var debugMode = 0
    }
    private val mList = mutableListOf<String>()
    private val adapter = MsgAdapter(mList)
    private var msgRecyclerView: RecyclerView? = null
    private val context = this

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
            if (mWifiP2pManager == null)
                Snackbar.make(view, "不支持该功能", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            else
                Snackbar.make(view, "启动搜索", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            mWifiP2pManager?.discoverPeers(mChannel, object : WifiP2pManager.ActionListener{
                override fun onSuccess() {
                    Toast.makeText(context, "搜索成功", Toast.LENGTH_LONG).show()
                }

                override fun onFailure(p0: Int) {
                    Toast.makeText(context, "搜索失败", Toast.LENGTH_LONG).show()
                }
            })

        }
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)

        msgRecyclerView!!.layoutManager = LinearLayoutManager(this)
        msgRecyclerView!!.adapter = adapter
    }

    override fun onPeersInfo(wifiP2pDeviceList: Collection<WifiP2pDevice>) {
        mList.clear()
        for (device in wifiP2pDeviceList){
//            Log.e(TAG, "连接的设备是： ${device.deviceName} ------------ ${device.deviceAddress}")
            mList.add(device.deviceName)
        }
        adapter.notifyDataSetChanged()
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
                    mList.add("aaa")
                    mList.add("bbb")
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
}
