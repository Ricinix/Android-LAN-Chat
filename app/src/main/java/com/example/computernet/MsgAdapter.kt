package com.example.computernet

import android.content.Context
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

//这是recyclerView适配器的类，在这里设置了点击事件，当用户点击某个设备图标时，从这里开启下一个活动
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
        //显示设备名，若未设置则显示设备IP地址
        holder.textView!!.text = mList[position].let {
            if (mList[position].name != "default") it.name
            else it.address
        }
        //设置未读消息数目的显示
        holder.msgNum!!.apply {
            if (mList[position].new == 0)
                visibility = View.GONE
            else{
                visibility = View.VISIBLE
                text = mList[position].new.toString()
            }
        }
    }

}