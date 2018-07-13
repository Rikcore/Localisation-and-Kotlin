package com.rikcore.kotlinproject

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.support.compat.R.id.async
import android.view.View
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.util.concurrent.atomic.AtomicBoolean


class CustomWindowsAdapter(private var context: Context) : GoogleMap.InfoWindowAdapter {

    private var contentView : View

    init {
        val inflater = LayoutInflater.from(context)
        contentView = inflater.inflate(R.layout.custom_window_info, null)
    }


    override fun getInfoContents(p0: Marker?): View {
        val textViewName = contentView.findViewById<TextView>(R.id.textViewName)
        val textViewBattery = contentView.findViewById<TextView>(R.id.textViewBattery)
        val textViewDate = contentView.findViewById<TextView>(R.id.textViewDate)
        val imageViewProfile = contentView.findViewById<ImageView>(R.id.imageViewProfile)

        val snippetSplitted = p0!!.snippet.split("¤")
        val dateString = snippetSplitted[0]
        val batteryLvl = snippetSplitted[1]

        textViewName.text = p0.title
        textViewName.setTextColor(context.resources.getColor(R.color.colorPrimary))
        textViewBattery.text = batteryLvl + "%"
        textViewBattery.setTextColor(context.resources.getColor(R.color.colorPrimary))
        textViewDate.text = dateString
        textViewDate.setTextColor(context.resources.getColor(R.color.colorPrimary))

        val user : UserClass = p0.tag as UserClass

        if(user.pictureUrl != null){
            Picasso.with(context).load(user.pictureUrl).into(imageViewProfile, object :Callback {
                override fun onSuccess() {
                    if (p0.isInfoWindowShown) {
                        p0.hideInfoWindow()
                        p0.showInfoWindow()
                    }
                }

                override fun onError() {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }
            })
        } else {
            imageViewProfile.setImageResource(R.drawable.risitete)
        }

        return contentView
    }

    override fun getInfoWindow(p0: Marker?): View? {
        return null
    }


}