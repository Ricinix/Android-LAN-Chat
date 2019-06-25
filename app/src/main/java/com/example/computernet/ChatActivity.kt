package com.example.computernet

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class ChatActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val intent = Intent()
    }

    companion object {
        @JvmStatic
        fun startThisActivity(context: Context){
            val intent = Intent(context, ChatActivity::class.java)
            context.startActivity(intent)
        }
    }
}
