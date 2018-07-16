package com.rikcore.kotlinproject

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(context: Context, var messageList: ArrayList<Message>) : BaseAdapter() {

    private val layoutInflater = LayoutInflater.from(context)

    override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
        val view: View
        if(p1 == null){
            view = this.layoutInflater.inflate(R.layout.message_item, p2, false)
            val textViewSender = view.findViewById<TextView>(R.id.textViewSender)
            val textViewDate = view.findViewById<TextView>(R.id.textViewDate)
            val textViewContent = view.findViewById<TextView>(R.id.textViewContent)

            val message = messageList[p0]
            textViewSender.text = message.sender
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = message.timeStamp!!
            val date = sdf.format(calendar.time)
            textViewDate.text = date
            textViewContent.text = message.content
        } else {
            view = p1
        }
        return view
    }

    override fun getItem(p0: Int): Any {
        return messageList[p0]
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }

    override fun getCount(): Int {
        return messageList.count()
    }
}