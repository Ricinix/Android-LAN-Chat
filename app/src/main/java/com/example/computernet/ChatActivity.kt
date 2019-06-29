package com.example.computernet

import android.content.Context
import android.content.Intent
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
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class ChatActivity : BaseActivity() {

    private var firstChat: Boolean = true
    private val mList = mutableListOf<ChatMsg>()
    private val adapter = ChatAdapter(mList)
    private var recyclerView: RecyclerView? = null
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
                editText.text.toString()
                editText.setText("")
            }
            false
        }
        sendButton.setOnClickListener {
            editText.text.toString()
            editText.setText("")
        }

        recyclerView!!.layoutManager = LinearLayoutManager(this)
        recyclerView!!.adapter = adapter
        if (MainActivity.debugMode == 1){
            mList.add(ChatMsg("Hello", ChatMsg.TYPE_RECEIVED))
            mList.add(ChatMsg("I'm fine. Thank you. And you?", ChatMsg.TYPE_SEND))
            adapter.notifyDataSetChanged()
        }

        deviceAddress = intent.getStringExtra("deviceAddress")
        toolbar.title = "聊天窗口 - $deviceAddress(对方未连接)"

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
        mList.add(ChatMsg(msgText, ChatMsg.TYPE_RECEIVED))
        adapter.notifyItemChanged(mList.size - 1)
        recyclerView!!.scrollToPosition(mList.size - 1)

        if (firstChat){
            serverPort = msgText.toInt()
            sendPort = serverPort + 96
            sendPort.toString()
            firstChat = false
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
