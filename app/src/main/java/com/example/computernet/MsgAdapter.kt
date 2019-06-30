package com.example.computernet

import android.content.Context
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class MsgAdapter(private val mList: List<DeviceInfo>,private val context : Context): RecyclerView.Adapter<MsgAdapter.ViewHolder>() {

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v){
        var textView: TextView? = null
        var msgNum: TextView? = null
        init {
            textView = v.findViewById(R.id.usr)
            msgNum = v.findViewById(R.id.msg_num)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cardView = LayoutInflater.from(parent.context)
            .inflate(R.layout.usr_msg, parent, false) as CardView
        val holder = ViewHolder(cardView)
        cardView.setOnClickListener{
            val device = mList[holder.adapterPosition]
            ChatActivity.startThisActivity(context, device.address, false, "")
        }
        return holder
    }

    override fun getItemCount(): Int =mList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (mList[position].name != "default"){
            holder.textView!!.text = mList[position].name
        }else {
            holder.textView!!.text = mList[position].address
        }
        if (mList[position].new == 0){
            holder.msgNum!!.visibility = View.GONE
        }else {
            holder.msgNum!!.visibility = View.VISIBLE
            holder.msgNum!!.text = mList[position].new.toString()
        }
    }

}