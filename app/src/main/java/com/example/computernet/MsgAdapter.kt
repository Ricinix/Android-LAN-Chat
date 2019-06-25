package com.example.computernet

import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView

class MsgAdapter(val mlist: List<String>): RecyclerView.Adapter<MsgAdapter.ViewHolder>() {

    class ViewHolder(val cardView: CardView) : RecyclerView.ViewHolder(cardView){
        val textView: TextView = cardView.findViewById(R.id.usr)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cardView = LayoutInflater.from(parent.context)
            .inflate(R.layout.usr_msg, parent, false) as CardView
        return ViewHolder(cardView)
    }

    override fun getItemCount(): Int =mlist.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = mlist[position]
    }

}