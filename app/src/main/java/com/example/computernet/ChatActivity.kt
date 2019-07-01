package com.example.computernet

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.AppCompatButton
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import com.example.computernet.service.SendService
import com.example.computernet.service.ServerService
import com.example.computernet.service.TCPSendService
import com.example.computernet.service.TCPServerService
import java.io.File
import android.provider.MediaStore
import android.provider.DocumentsContract
import android.content.ContentUris
import android.database.Cursor
import android.graphics.BitmapFactory
import android.support.v4.app.NotificationCompat


class ChatActivity : BaseActivity() {

    private var path: String? = null
    private var size: Long? = null
    private var connected = false
    private var mList: MutableList<ChatMsg> = mutableListOf()
    private lateinit var adapter: ChatAdapter
    private var recyclerView: RecyclerView? = null
    private var targetName = "default"

    private val receiver = ChatReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        connected = false
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        //加载组件
        val sendButton: AppCompatButton = findViewById(R.id.send_button)
        val toolbar: Toolbar = findViewById(R.id.toolbar_chat)
        val editText: EditText = findViewById(R.id.edit_text)
        recyclerView = findViewById(R.id.recycler_chat)

        setSupportActionBar(toolbar)

        //回车发送数据
        editText.setOnEditorActionListener { textView, i, keyEvent ->
            if (i == EditorInfo.IME_ACTION_DONE){
                if (editText.text.toString() != ""){
                    send(editText.text.toString())
                    editText.setText("")
                }
            }
            false
        }
        //点击发送按钮来发送数据
        sendButton.setOnClickListener {
            if (editText.text.toString() != ""){
                send(editText.text.toString())
                editText.setText("")
            }
        }

        recyclerView!!.layoutManager = LinearLayoutManager(this)
        //设置对应的Ip地址
        deviceAddress = intent.getStringExtra("deviceAddress")
        //根据设备IP来获取对应的聊天记录
        getList(deviceAddress)
        adapter = ChatAdapter(mList)
        recyclerView!!.adapter = adapter
        if (MainActivity.debugMode == 1){
            mList.add(ChatMsg("Hello", ChatMsg.TYPE_RECEIVED))
            mList.add(ChatMsg("I'm fine. Thank you. And you?", ChatMsg.TYPE_SEND))
            adapter.notifyDataSetChanged()
        }

        //设置标题
        setTitle(false)
        isConnected(deviceAddress)

        //打开活动的时候就要接收文件传输
        if (intent.getBooleanExtra("readyToReceive", false)){
            val msgText: String = intent.getStringExtra("msg")
            val fileSize: Long = Regex("(?<=#size:).*?(?=#)").find(msgText)?.value?.toLong() ?: 0
            val fileName: String = Regex("(?<=#name:).*?(?=#)").find(msgText)?.value ?: "download"
            val url = Environment.DIRECTORY_DOWNLOADS + "/" + fileName
            receiveFile(fileSize, url)
        }
    }

    //获取要发送文件的路径
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1){
            val uri: Uri? = data?.data
            path = getPath(this,uri)
            Log.e("ChatActivity", "选择的文件路径为$path")
            val file = File(path)
            Log.e("ChatActivity", "已发送传输请求，等待确认，文件名为${file.name}, 文件大小为${file.length()}")
            send("#broadcast#file#size:${file.length()}#name:${file.name}#")
//            fileTrans(path ?: "", file.length())
            size = file.length()
            path = data?.dataString
            if (!connected)
                Toast.makeText(this, "对方不在线", Toast.LENGTH_LONG).show()
        }
    }

    //活动不可见时注销广播接收器
    override fun onPause() {
        super.onPause()
        mLocalBroadcastManager.unregisterReceiver(receiver)
    }

    //注册广播接收器
    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
        val intentFilter = IntentFilter()
        intentFilter.addAction(SendService.SEND_FINISH)
        intentFilter.addAction(ServerService.RECEIVE_MSG)
        intentFilter.addAction(TCPSendService.PROGRESS_UPDATE)
        intentFilter.addAction(TCPSendService.SEND_FINISH)
        intentFilter.addAction(TCPServerService.PROGRESS_UPDATE)
        intentFilter.addAction(TCPServerService.RECEIVE_FINISH)
        mLocalBroadcastManager.registerReceiver(receiver, intentFilter)
    }

    //加载菜单栏
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_chat, menu)
        return true
    }

    //传输文件的按钮监听
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item!!.itemId){
            android.R.id.home -> quit()
            R.id.action_file_send -> {
                chooseFile()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    //获取对应设备的聊天记录
    private fun getList(address: String){
        for (de in deviceList){
            if (de.address == address){
                mList = de.chatList
                targetName = de.name
            }
        }
    }

    //判断对方是否在线
    private fun isConnected(address: String){
        for (de in deviceList){
            if (de.address == address){
                connected = true
                setTitle(true)
                de.new = 0
            }
        }
    }

    //收到信息时的相应处理
    private fun receive(msgText: String, address: String){
        //判断是不是特殊消息
        if (Regex("#broadcast#").containsMatchIn(msgText)){
            val name: String? = Regex("(?<=#name:).*?(?=#)").find(msgText)?.value
            //判断是否有人上线
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
                sendToTarget("#broadcast#confirm#name:$deviceName", address)
            }else if(connected and Regex("#broadcast#disconnect#").containsMatchIn(msgText)){
                //判断是否有人离线
                var i = -1
                for (de in deviceList){
                    i++
                    if (de.address == address){
                        break
                    }
                }
                if (i >= 0){
                    deviceList.removeAt(i)
                    //若是当前聊天的人，则设置为未连接
                    if (address == deviceAddress){
                        connected = false
                        setTitle(false)
                    }
                }
            }else if(Regex("#broadcast#confirm#").containsMatchIn(msgText)){
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
            }else if(Regex("#broadcast#file#").containsMatchIn(msgText)){
                //若是文件传输的请求
                when {
                    //对方拒绝接收文件
                    Regex("#refuse#").containsMatchIn(msgText) -> {
                        Log.e("ChatActivity", "对方拒绝接收")
                        Toast.makeText(this, "对方拒绝接收", Toast.LENGTH_LONG).show()
                        val intent = Intent(TCPSendService.SEND_SHUTDOWN)
                        mLocalBroadcastManager.sendBroadcast(intent)
                    }
                    //收到对方的确认
                    Regex("#confirm#").containsMatchIn(msgText) -> {
                        Log.e("ChatActivity", "收到传输请求的确认，开始传输文件")
                        fileTrans()
                    }
                    //收到传输请求
                    else -> {
                        val fileSize: Long = Regex("(?<=#size:).*?(?=#)").find(msgText)?.value?.toLong() ?: 0
                        val fileName: String = Regex("(?<=#name:).*?(?=#)").find(msgText)?.value ?: "download"
                        val url = Environment.getExternalStorageDirectory().absolutePath + "/Download/" + fileName

                        receiveFile(fileSize, url)
                    }
                }
            }
        }else {
            if (address == deviceAddress){
                mList.add(ChatMsg(msgText, ChatMsg.TYPE_RECEIVED))
                adapter.notifyItemChanged(mList.size - 1)
                recyclerView!!.scrollToPosition(mList.size - 1)
            }else{
                var i = -1
                for (de in deviceList){
                    i++
                    if (de.address == address){
                        break
                    }
                }
                if (i >= 0){
                    deviceList[i].chatList.add(ChatMsg(msgText, ChatMsg.TYPE_RECEIVED))
                    deviceList[i].new++
                }
            }
        }
    }

    //接收文件
    private fun receiveFile(size: Long, url: String){
        AlertDialog.Builder(this).setTitle("对方想给你发送一个文件")
                .setIcon(android.R.drawable.sym_def_app_icon)
                .setPositiveButton("接收") { p0, p1 ->
                    //启动TCP服务端的服务
                    startTcpServer(size, url)
                    //发送确认应答
                    send("#broadcast#file#confirm#")
                }.setNegativeButton("拒绝") { p0, p1 ->
                //发送拒绝应答
                send("#broadcast#file#refuse#")
            }.show()
    }

    //开启TCP服务端服务
    private fun startTcpServer(size: Long, url: String){
        val intent = Intent(this, TCPServerService::class.java)
        intent.putExtra(TCPServerService.PORT, TcpPort)
        intent.putExtra(TCPServerService.LENGTH_PROGRESS, size)
        intent.putExtra(TCPServerService.FILE_URL, url)
        startService(intent)
    }

    //设置标题
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

    //刷新聊天信息
    private fun refreshSendMsg(msgText: String){
        if (!Regex("#broadcast#").containsMatchIn(msgText)){
            //如果连接，正常发送
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

    //选择文件
    private fun chooseFile(){
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, 1)
    }

    //启动TCP客户端服务
    private fun fileTrans(){
        val intent = Intent(this, TCPSendService::class.java)
        intent.putExtra(TCPSendService.ADDRESS, deviceAddress)
        intent.putExtra(TCPSendService.URL, path)
        intent.putExtra(TCPSendService.PORT, TcpPort)
        intent.putExtra(TCPSendService.LENGTH_PROGRESS, size)
        startService(intent)

    }

    //退出
    private fun quit(){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    //更新进度条
    private fun updateProgress(now: Long, length: Long, id: Int){
        val progress: Int = ((now * 100) / length).toInt()
        val notification = NotificationCompat.Builder(this, "Progress")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setContentTitle("文件传输")
            .setContentText("$progress%")
            .setProgress(100, progress, false)
            .build()
        notification.flags = Notification.FLAG_ONGOING_EVENT
        mNotificationManager.notify(id, notification)
    }

    //成功接收的通知
    private fun tranSucceed(id: Int){
        mNotificationManager.cancel(id)
        val notification = NotificationCompat.Builder(this, "Progress")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setContentTitle("传输文件")
            .setContentText("已完成")
            .build()
        mNotificationManager.notify(3, notification)
    }

    inner class ChatReceiver: BroadcastReceiver(){
        override fun onReceive(p0: Context?, intent: Intent?) {
            when (intent?.action){
                //接收广播，发通知
                ServerService.RECEIVE_MSG -> receive(intent.getStringExtra("msg"),
                    intent.getStringExtra(ServerService.FROM_ADDRESS))
                SendService.SEND_FINISH -> refreshSendMsg(intent.getStringExtra("msg"))
                TCPSendService.SEND_FINISH -> tranSucceed(1)
                TCPSendService.PROGRESS_UPDATE ->
                    updateProgress(intent.getLongExtra(TCPSendService.NOW_PROGRESS, 0),
                        intent.getLongExtra(TCPSendService.LENGTH_PROGRESS, 0), 1)
                TCPServerService.RECEIVE_FINISH -> tranSucceed(2)
                TCPServerService.PROGRESS_UPDATE ->
                    updateProgress(intent.getLongExtra(TCPServerService.NOW_PROGRESS, 0),
                        intent.getLongExtra(TCPServerService.LENGTH_PROGRESS, 0), 2)
            }
        }
    }

    companion object {
        @JvmStatic
        fun startThisActivity(context: Context, deviceAddress: String, readyToReceive: Boolean, msg: String){
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("msg", msg)
            intent.putExtra("readyToReceive", readyToReceive)
            intent.putExtra("deviceAddress", deviceAddress)
            context.startActivity(intent)
        }
    }

    //uri解析
    private fun getPath(context: Context?, uri: Uri?): String? {
        if (context == null || uri == null)
            return null
        if (DocumentsContract.isDocumentUri(context, uri)
        ) {
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }
            } else if (isDownloadsDocument(uri)) {
                val id = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"),
                    java.lang.Long.valueOf(id)
                )
                return getDataColumn(context, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                when (type) {
                    "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                val selection = MediaStore.Images.Media._ID + "=?"
                val selectionArgs = arrayOf(split[1])
                return getDataColumn(context, contentUri, selection, selectionArgs)
            }
        } else if ("content".equals(uri.scheme!!, ignoreCase = true)) {
            return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme!!, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

    private fun getDataColumn(context: Context, uri: Uri?, selection: String?, selectionArgs: Array<String>?): String? {
        var cursor: Cursor? = null
        val column = MediaStore.Images.Media.DATA
        val projection = arrayOf(column)
        try {
            cursor = context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } finally {
            cursor!!.close()
        }
        return null
    }

}
