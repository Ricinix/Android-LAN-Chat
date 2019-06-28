package com.example.computernet

import android.content.Context
import android.content.Intent
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
    private var sendPort: Int = 8988
    private var serverPort: Int = 8988
    private val mList = mutableListOf<ChatMsg>()
    private val adapter = ChatAdapter(mList)
    private var recyclerView: RecyclerView? = null
    private var deviceAddress: String = ""
    private val serverTask = ServerAsyncTask()
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
                send(editText.text.toString())
                editText.setText("")
            }
            false
        }
        sendButton.setOnClickListener {
            send(editText.text.toString())
            editText.setText("")
        }

        recyclerView!!.layoutManager = LinearLayoutManager(this)
        recyclerView!!.adapter = adapter
        if (MainActivity.debugMode == 1){
            mList.add(ChatMsg("Hello", ChatMsg.TYPE_RECEIVED))
            mList.add(ChatMsg("I'm fine. Thank you. And you?", ChatMsg.TYPE_SEND))
            adapter.notifyDataSetChanged()
        }

        val config = WifiP2pConfig()
        deviceAddress = intent.getStringExtra("deviceAddress")
        config.deviceAddress = deviceAddress
        toolbar.title = "聊天窗口 - ${intent.getStringExtra("deviceName")}"
        mWifiP2pManager?.connect(mChannel, config, object : WifiP2pManager.ActionListener{
            override fun onSuccess() {
                Toast.makeText(this@ChatActivity, "连接成功", Toast.LENGTH_LONG).show()
            }

            override fun onFailure(p0: Int) {
                Toast.makeText(this@ChatActivity, "连接失败", Toast.LENGTH_LONG).show()
            }

        })

        send("10196")
        serverTask.execute()
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

    private fun send(msgText: String){
        serverTask.cancel(true)
        SendAsyncTask(msgText).execute()
    }

    private fun receive(msgText: String){
        mList.add(ChatMsg(msgText, ChatMsg.TYPE_RECEIVED))
        adapter.notifyItemChanged(mList.size - 1)
        recyclerView!!.scrollToPosition(mList.size - 1)

        if (firstChat){
            serverPort = msgText.toInt()
            sendPort = serverPort + 96
            send(sendPort.toString())
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

    inner class ServerAsyncTask: AsyncTask<Unit, Unit, Unit>() {
        private var msgText:String = ""

        override fun doInBackground(vararg p0: Unit?) {
            while (true){
                try {
                    val serverSocket = ServerSocket(serverPort)
                    Log.e("ServerAsyncTask", "服务器开启")
                    val client: Socket = serverSocket.accept()
                    Log.e("ServerAsyncTask", "服务器连接")
                    val inputStream: InputStream = client.getInputStream()
                    val baoS = ByteArrayOutputStream()
                    copy(inputStream, baoS)
                    msgText = baoS.toString()
                    serverSocket.close()
                }catch (e: IOException){
                    Log.e("ServerAsyncTask", e.message)
                }
                receive(msgText)
            }
        }
    }

    inner class SendAsyncTask(private val msgText: String): AsyncTask<String, Unit, Unit>(){
        override fun doInBackground(vararg p0: String) {
            val socket = Socket()
            try {
                Log.e("WifiP2pSendService", "正在打开服务端")
                socket.bind(null)
                socket.connect(InetSocketAddress(deviceAddress, sendPort), 5000)

                Log.e("WifiP2pSendService", "socket状态： ${socket.isConnected}")
                val stream: OutputStream = socket.getOutputStream()
//                val cr: ContentResolver = application.contentResolver
                val inputS: InputStream = ByteArrayInputStream(msgText.toByteArray())
                copy(inputS, stream)


            }catch (e: IOException){
                Log.e("WifiP2pSendService", e.message)
            }finally {
                if (socket.isConnected){
                    try {
                        socket.close()
                    }catch (e: IOException){
                        e.printStackTrace()
                    }
                }
            }
        }

        override fun onPostExecute(result: Unit?) {
            mList.add(ChatMsg(msgText, ChatMsg.TYPE_SEND))
            adapter.notifyItemChanged(mList.size - 1)
            recyclerView!!.scrollToPosition(mList.size - 1)
            serverTask.execute()
        }
    }

    private fun copy(inputStream: InputStream, out: OutputStream){
        val buf = ByteArray(1024)
        var len: Int
        try {
            len = inputStream.read(buf)
            while (len != -1) {
                out.write(buf, 0, len)
                len = inputStream.read(buf)
            }
            out.close()
            inputStream.close()
        } catch (e: IOException) {
            Log.d("WifiP2pSendService", e.toString())
        }

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
