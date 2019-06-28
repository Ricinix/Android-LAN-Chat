package com.example.computernet

import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast

class ChatActivity : BaseActivity() {

    private val mList = mutableListOf<ChatMsg>()
    private val adapter = ChatAdapter(mList)
    private var recyclerView: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val toolbar: Toolbar = findViewById(R.id.toolbar_chat)
        val editText: EditText = findViewById(R.id.edit_text)
        recyclerView = findViewById(R.id.recycler_chat)

        setSupportActionBar(toolbar)

        editText.setOnEditorActionListener { textView, i, keyEvent ->
            if (i == EditorInfo.IME_ACTION_DONE){
                editText.setText("")
            }
            false
        }

        recyclerView!!.layoutManager = LinearLayoutManager(this)
        recyclerView!!.adapter = adapter
        if (MainActivity.debugMode == 1){
            mList.add(ChatMsg("Hello", ChatMsg.TYPE_RECEIVED))
            mList.add(ChatMsg("I'm fine. Thank you. And you?", ChatMsg.TYPE_SEND))
            adapter.notifyDataSetChanged()
        }

        val config = WifiP2pConfig()
        config.deviceAddress = intent.getStringExtra("deviceAddress")
        toolbar.title = "聊天窗口 - ${intent.getStringExtra("deviceName")}"
        mWifiP2pManager?.connect(mChannel, config, object : WifiP2pManager.ActionListener{
            override fun onSuccess() {
                Toast.makeText(this@ChatActivity, "连接成功", Toast.LENGTH_LONG).show()
            }

            override fun onFailure(p0: Int) {
                Toast.makeText(this@ChatActivity, "连接失败", Toast.LENGTH_LONG).show()
            }

        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_chat, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item!!.itemId){
            android.R.id.home -> quit()
            R.id.action_file_send -> fileTrans()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun fileTrans(){
        return
    }

    private fun quit(){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    companion object {
        @JvmStatic
        fun startThisActivity(context: Context, deviceName: String, deviceAddress: String){
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("deviceName", deviceName)
            intent.putExtra("deviceAddress", deviceAddress)
            context.startActivity(intent)
        }
    }
}
