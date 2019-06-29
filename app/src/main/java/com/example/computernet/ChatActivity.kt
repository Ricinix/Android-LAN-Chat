package com.example.computernet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pManager
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.AppCompatButton
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import com.example.computernet.service.SendService
import com.example.computernet.service.ServerService
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class ChatActivity : BaseActivity() {

    private var firstChat: Boolean = true
    private val mList = mutableListOf<ChatMsg>()
    private val adapter = ChatAdapter(mList)
    private var recyclerView: RecyclerView? = null

    private val receiver = ChatReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val sendButton: AppCompatButton = findViewById(R.id.send_button)
        val toolbar: Toolbar = findViewById(R.id.toolbar_chat)
        val editText: EditText = findViewById(R.id.edit_text)
        recyclerView = findViewById(R.id.recycler_chat)

        setSupportActionBar(toolbar)

        editText.setOnEditorActionListener { textView, i, keyEvent ->
            if (i == EditorInfo.IME_ACTION_DONE){
                if (editText.text.toString() != ""){
                    editText.setText("")
                    send(editText.text.toString())
                }
            }
            false
        }
        sendButton.setOnClickListener {
            if (editText.text.toString() != ""){
                editText.setText("")
                send(editText.text.toString())
            }
        }

        recyclerView!!.layoutManager = LinearLayoutManager(this)
        recyclerView!!.adapter = adapter
        if (MainActivity.debugMode == 1){
            mList.add(ChatMsg("Hello", ChatMsg.TYPE_RECEIVED))
            mList.add(ChatMsg("I'm fine. Thank you. And you?", ChatMsg.TYPE_SEND))
            adapter.notifyDataSetChanged()
        }

        deviceAddress = intent.getStringExtra("deviceAddress")
        supportActionBar?.title = "$deviceAddress(未连接)"

        send("#port:${intent.getIntExtra("devicePort", 11679)}#ip:$deviceAddress#status:ready#")
        Log.e("ChatActivity", "已发送ready通知")
    }

    override fun onPause() {
        super.onPause()
        mLocalBroadcastManager.unregisterReceiver(receiver)
    }

    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter()
        intentFilter.addAction(SendService.SEND_FINISH)
        intentFilter.addAction(ServerService.RECEIVE_MSG)
        mLocalBroadcastManager.registerReceiver(receiver, intentFilter)
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

    private fun receive(msgText: String){
        if (firstChat){
            val status = Regex("(?<=#status:).*?(?=#)").find(msgText)?.value ?: "not"
            if (status == "ready"){
                Log.e("ChatActivity", "收到更换serverPort通知")
                serverPort = Regex("(?<=#port:).*?(?=#)").find(msgText)?.value?.toInt() ?: 11791
                stopServer()
                send("#port:$serverPort#ip:$deviceAddress#status:confirm")
                startServer(serverPort)
                Log.e("ChatActivity", "已更换serverPort")
                supportActionBar?.title = "$deviceAddress(已连接)"
                firstChat = false
            }else if(status == "confirm"){
                Log.e("ChatActivity", "收到更换sendPort通知")
                sendPort = Regex("(?<=#port:).*?(?=#)").find(msgText)?.value?.toInt() ?: 11791
                Log.e("ChatActivity", "已更换sendPort")
                supportActionBar?.title = "$deviceAddress(已连接)"
                firstChat = false
            }
        }else {
            mList.add(ChatMsg(msgText, ChatMsg.TYPE_RECEIVED))
            adapter.notifyItemChanged(mList.size - 1)
            recyclerView!!.scrollToPosition(mList.size - 1)
        }
    }

    private fun refreshSendMsg(msgText: String){
        if (!firstChat){
            mList.add(ChatMsg(msgText, ChatMsg.TYPE_SEND))
            adapter.notifyItemChanged(mList.size - 1)
            recyclerView!!.scrollToPosition(mList.size - 1)
        }
    }

    private fun fileTrans(){
        return
    }

    private fun quit(){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    inner class ChatReceiver: BroadcastReceiver(){
        override fun onReceive(p0: Context?, intent: Intent?) {
            when (intent?.action){
                ServerService.RECEIVE_MSG -> receive(intent.getStringExtra("msg"))
                SendService.SEND_FINISH -> refreshSendMsg(intent.getStringExtra("msg"))
            }
        }
    }

    companion object {
        @JvmStatic
        fun startThisActivity(context: Context, deviceAddress: String, devicePort: Int){
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("devicePort", devicePort)
            intent.putExtra("deviceAddress", deviceAddress)
            context.startActivity(intent)
        }
    }
}
