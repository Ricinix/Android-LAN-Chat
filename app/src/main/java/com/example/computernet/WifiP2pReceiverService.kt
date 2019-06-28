package com.example.computernet

import android.app.IntentService
import android.content.Intent
import android.util.Log
import java.io.InputStream
import java.lang.Exception
import java.net.ServerSocket
import java.net.Socket

class WifiP2pReceiverService : IntentService("WifiP2pReceiverService") {
    override fun onHandleIntent(p0: Intent?) {
        Log.e("WifiP2pReceiverService", "receive service start")
        try {
            val service: ServerSocket = ServerSocket(10101)
            var buffer = ByteArray(10240)
            while (true){
                Log.e("WifiP2pReceiverService", "wait accept")
                try {
                    val socket: Socket = service.accept()
                    val streamIn: InputStream = socket.getInputStream()
                    val bytes = streamIn.read(buffer)
                    if (bytes == -1)
                        break
                }catch (e: Exception){
                    e.message
                }

            }
        }catch (e: Exception){
            Log.e("WifiP2pReceiverService", e.message)
        }
    }

}
