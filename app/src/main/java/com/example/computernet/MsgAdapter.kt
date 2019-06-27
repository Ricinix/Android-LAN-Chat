package com.example.computernet

import android.net.wifi.p2p.WifiP2pDevice
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class MsgAdapter(private val mList: List<WifiP2pDevice>): RecyclerView.Adapter<MsgAdapter.ViewHolder>() {

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v){
        var textView: TextView? = null
        init {
            textView = v.findViewById(R.id.usr)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cardView = LayoutInflater.from(parent.context)
            .inflate(R.layout.usr_msg, parent, false) as CardView
        cardView.setOnClickListener{
            ChatActivity.startThisActivity(parent.context)
        }
        return ViewHolder(cardView)
    }

    override fun getItemCount(): Int =mList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView!!.text = mList[position].deviceName
    }

}