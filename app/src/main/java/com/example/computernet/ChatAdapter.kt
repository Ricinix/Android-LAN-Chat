package com.example.computernet

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

class ChatAdapter(private val mList:List<ChatMsg>): RecyclerView.Adapter<ChatAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.msg_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = mList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val msg = mList[position]
        when {
            msg.type == ChatMsg.TYPE_RECEIVED -> {
                holder.leftLayout!!.visibility = View.VISIBLE
                holder.rightLayout!!.visibility = View.GONE
                holder.wrongLayout!!.visibility = View.GONE
                holder.leftMsg!!.text = msg.content
            }
            msg.type == ChatMsg.TYPE_SEND -> {
                holder.rightLayout!!.visibility = View.VISIBLE
                holder.leftLayout!!.visibility = View.GONE
                holder.wrongLayout!!.visibility = View.GONE
                holder.rightMsg!!.text = msg.content
            }
            msg.type == ChatMsg.TYPE_WRONG -> {
                holder.wrongLayout!!.visibility = View.VISIBLE
                holder.leftLayout!!.visibility = View.GONE
                holder.rightLayout!!.visibility = View.GONE
                holder.wrongMsg!!.text = msg.content
            }
        }
    }

    inner class ViewHolder(v: View): RecyclerView.ViewHolder(v){
        var leftLayout: LinearLayout? = null
        var rightLayout: LinearLayout? = null
        var wrongLayout: LinearLayout? = null
        var leftMsg: TextView? = null
        var rightMsg: TextView? = null
        var wrongMsg: TextView? = null

        init {
            leftLayout = v.findViewById(R.id.left_layout)
            rightLayout = v.findViewById(R.id.right_layout)
            wrongLayout = v.findViewById(R.id.wrong_layout)
            leftMsg = v.findViewById(R.id.left_msg)
            rightMsg = v.findViewById(R.id.right_msg)
            wrongMsg = v.findViewById(R.id.wrong_msg)
        }
    }

}