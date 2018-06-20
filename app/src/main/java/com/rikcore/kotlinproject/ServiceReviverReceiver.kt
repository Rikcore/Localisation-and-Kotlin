package com.rikcore.kotlinproject

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ServiceReviverReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        val TAG = "RestartServiceReceiver"
        Log.e(TAG, "onReceive")
        context.startService(Intent(context.getApplicationContext(), LocalisationService::class.java))
    }
}
