package com.example.computernet

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup

class ChatAdapter(val mlist:List<String>): RecyclerView.Adapter<ChatAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getItemCount(): Int = mlist.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    class ViewHolder(val view: View): RecyclerView.ViewHolder(view){
        val d=1
    }
}