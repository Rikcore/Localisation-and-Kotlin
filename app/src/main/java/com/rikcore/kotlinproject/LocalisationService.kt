package com.rikcore.kotlinproject

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import android.provider.Settings
import android.support.v4.content.LocalBroadcastManager
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*
import android.os.*
import android.net.ConnectivityManager

class LocalisationService : Service() {

    private var locationManager : LocationManager? = null
    private var userPref: SharedPreferences? = null
    private var userEdit: SharedPreferences.Editor? = null

    override fun onBind(intent: Intent): IBinder {
        throw UnsupportedOperationException("Not yet implemented");
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        userPref = this.getSharedPreferences("user_info", 0)
        userEdit = userPref?.edit()
        getLocation()
        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager?.removeUpdates(locationListener)
        sendBroadcast(Intent("YouWillNeverKillMe"))
        stopSelf()
    }

    private fun getLocation(){
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        try {
            locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0L, 0f, locationListener)
        } catch (ex : SecurityException){
            Log.d("FAILED", "Security exception")
        }
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            //Do some shit
            userEdit?.putString("latitude", location.latitude.toString())
            userEdit?.putString("longitude", location.longitude.toString())
            val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).toString()
            val deviceName = Settings.Secure.getString(getContentResolver(), "bluetooth_name")
            val sdf = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.FRANCE)
            val currentDate = sdf.format(Date())
            val chargeStatus = chargeStatus(applicationContext)
            val memory = getMemoryAvailable()
            val network = chkStatus()
            Log.d("LOCALISATION", (location.latitude).toString() + " " + (location.longitude).toString() + " " + batLevel + "%")
            val myProfile = UserClass(userPref!!.getString("userName", deviceName), batLevel, location.latitude, location.longitude, currentDate, chargeStatus, memory, network, userPref!!.getBoolean("isVisible", true), getUptime())
            sendData(myProfile)
            sendMessageToActivity(location, "Position")
        }
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun sendMessageToActivity(l: Location, msg: String) {
        val intent = Intent("GPSLocationUpdates")
        // You can also include some extra data.
        intent.putExtra("Status", msg)
        val b = Bundle()
        b.putParcelable("Location", l)
        intent.putExtra("Location", b)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun sendData(user : UserClass){
        val database = FirebaseDatabase.getInstance()
        val nodeId = Settings.Secure.getString(applicationContext.getContentResolver(),
                Settings.Secure.ANDROID_ID)
        val myRef = database.getReference("position/" + nodeId)

        myRef.setValue(user)
    }

    fun chargeStatus(context: Context) : String {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        return when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Branché sur secteur"
            BatteryManager.BATTERY_PLUGGED_USB -> "Branché en USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Charge sans fil"
            else -> "Sur batterie"
        }
    }

    fun getMemoryAvailable() : String{
        val stat = StatFs(Environment.getExternalStorageDirectory().getPath())
        val bytesAvailable: Long
        bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
        val megAvailable = bytesAvailable / (1024 * 1024)
        val gigAvailable = megAvailable / 1024
        Log.e("", "Available MB : $megAvailable")
        return gigAvailable.toString() + " GB"
    }

    fun chkStatus() : String {
        val connMgr = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        val mobile = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
        return when {
            wifi.isConnectedOrConnecting -> "Wifi"
            mobile.isConnectedOrConnecting -> "Cellulaire"
            else -> "Impossible"
        }
    }

    fun getUptime() : Int {
        return (SystemClock.elapsedRealtime() / 1000 / 60 / 60).toInt()
    }


}
