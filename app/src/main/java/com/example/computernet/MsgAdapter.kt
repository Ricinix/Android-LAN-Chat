package com.example.computernet

import android.net.wifi.p2p.WifiP2pDevice
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast

class MsgAdapter(private val mList: List<WifiP2pDevice>, private val activity: MainActivity): RecyclerView.Adapter<MsgAdapter.ViewHolder>() {

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v){
        var textView: TextView? = null
        init {
            textView = v.findViewById(R.id.usr)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cardView = LayoutInflater.from(parent.context)
            .inflate(R.layout.usr_msg, parent, false) as CardView
        val holder = ViewHolder(cardView)
        cardView.setOnClickListener{
            val device = mList[holder.adapterPosition]
            when (device.status){
                WifiP2pDevice.AVAILABLE ->
                    ChatActivity.startThisActivity(parent.context, device.deviceName, device.deviceAddress)
                WifiP2pDevice.CONNECTED ->
                    activity.removeConnect()
                WifiP2pDevice.FAILED ->
                    Toast.makeText(parent.context, "连接失败", Toast.LENGTH_LONG).show()
                WifiP2pDevice.INVITED ->
                    activity.cancelInvite()
                WifiP2pDevice.UNAVAILABLE ->
                    Toast.makeText(parent.context, "不可连接", Toast.LENGTH_LONG).show()
            }
        }
        return holder
    }

    override fun getItemCount(): Int =mList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView!!.text = mList[position].deviceName
    }

}