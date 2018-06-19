package com.rikcore.kotlinproject

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.widget.Button
import android.widget.Toast


class MainActivity : AppCompatActivity() {


    private val LOCATION_PERMISSION_RC = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_RC)

            return
        }

        val buttonStart = findViewById<Button>(R.id.buttonStart)
        val buttonStop = findViewById<Button>(R.id.buttonStop)

        buttonStart.setOnClickListener {
            Log.d("SERVICE STATE", (isMyServiceRunning(LocalisationService::class.java, this)).toString())
            if(!isMyServiceRunning(LocalisationService::class.java, this)){
                startService(Intent(this,LocalisationService::class.java))
            } else {
                Toast.makeText(this, "Service en cours", Toast.LENGTH_LONG).show()
            }
        }
        buttonStop.setOnClickListener {
            Log.d("SERVICE STATE", (isMyServiceRunning(LocalisationService::class.java, this)).toString())
            if(isMyServiceRunning(LocalisationService::class.java, this)){
                stopService(Intent(this, LocalisationService::class.java))
            } else {
                Toast.makeText(this, "Service déjà à l'arrêt", Toast.LENGTH_LONG).show()
            }
        }


    }

    fun isMyServiceRunning(serviceClass: Class<*>, context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
