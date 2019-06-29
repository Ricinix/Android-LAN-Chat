package com.example.computernet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v7.widget.AppCompatButton
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import com.example.computernet.service.SendService
import com.example.computernet.service.ServerService

class ChatActivity : BaseActivity() {

    private var connected = false
    private var mList: MutableList<ChatMsg> = mutableListOf()
    private lateinit var adapter: ChatAdapter
    private var recyclerView: RecyclerView? = null
    var targetName = "default"

    private val receiver = ChatReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        connected = false
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
                    send(editText.text.toString())
                    editText.setText("")
                }
            }
            false
        }
        sendButton.setOnClickListener {
            if (editText.text.toString() != ""){
                send(editText.text.toString())
                editText.setText("")
            }
        }

        recyclerView!!.layoutManager = LinearLayoutManager(this)
        //设置对应的Ip地址
        deviceAddress = intent.getStringExtra("deviceAddress")
        getList(deviceAddress)
        adapter = ChatAdapter(mList)
        recyclerView!!.adapter = adapter
        if (MainActivity.debugMode == 1){
            mList.add(ChatMsg("Hello", ChatMsg.TYPE_RECEIVED))
            mList.add(ChatMsg("I'm fine. Thank you. And you?", ChatMsg.TYPE_SEND))
            adapter.notifyDataSetChanged()
        }
        setTitle(false)

        isConnected(deviceAddress)
    }

    override fun onPause() {
        super.onPause()
        mLocalBroadcastManager.unregisterReceiver(receiver)
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
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

    private fun getList(address: String){
        for (de in deviceList){
            if (de.address == address){
                mList = de.chatList
                targetName = de.name
            }
        }
    }

    private fun isConnected(address: String){
        for (de in deviceList){
            if (de.address == address){
                connected = true
                setTitle(true)
                de.new = 0
            }
        }
    }

    private fun receive(msgText: String, address: String){
        if (Regex("#broadcast#").containsMatchIn(msgText)){
            val name: String? = Regex("(?<=#name:).*?(?=#)").find(msgText)?.value
            if (Regex("#connect#").containsMatchIn(msgText)){
                var has = false
                for (de in deviceList){
                    if (address == de.address)
                        has = true
                }
                if (address == localIp){
                    has = true
                }
                if (!has){
                    deviceList.add(DeviceInfo(name?:"default", address, mutableListOf(), 0))
                }
                if (address == deviceAddress){
                    connected = true
                    setTitle(true)
                }
            }else if(connected and Regex("#broadcast#disconnect#").containsMatchIn(msgText)){
                var i = -1
                for (de in deviceList){
                    i++
                    if (de.address == address){
                        break
                    }
                }
                if (i >= 0){
                    deviceList.removeAt(i)
                    if (address == deviceAddress){
                        connected = false
                        setTitle(false)
                    }
                }
            }
        }else {
            mList.add(ChatMsg(msgText, ChatMsg.TYPE_RECEIVED))
            adapter.notifyItemChanged(mList.size - 1)
            recyclerView!!.scrollToPosition(mList.size - 1)
        }
    }

    private fun setTitle(connect: Boolean){
        if (targetName != "default"){
            if (connect){
                supportActionBar?.title = "$targetName(已连接)"
            }else {
                supportActionBar?.title = "$targetName(未连接)"
            }
        }else{
            if (connect){
                supportActionBar?.title = "$deviceAddress(已连接)"
            }else{
                supportActionBar?.title = "$deviceAddress(未连接)"
            }
        }
    }

    private fun refreshSendMsg(msgText: String){
        if (!Regex("#broadcast#").containsMatchIn(msgText)){
            if (connected){
                mList.add(ChatMsg(msgText, ChatMsg.TYPE_SEND))
            }
            else{
                mList.add(ChatMsg(msgText, ChatMsg.TYPE_WRONG))
            }
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
                ServerService.RECEIVE_MSG -> receive(intent.getStringExtra("msg"),
                    intent.getStringExtra(ServerService.FROM_ADDRESS))
                SendService.SEND_FINISH -> refreshSendMsg(intent.getStringExtra("msg"))
            }
        }
    }

    companion object {
        @JvmStatic
        fun startThisActivity(context: Context, deviceAddress: String){
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("deviceAddress", deviceAddress)
            context.startActivity(intent)
        }
    }
}
