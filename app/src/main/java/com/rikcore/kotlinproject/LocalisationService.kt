package com.rikcore.kotlinproject

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import android.provider.Settings
import android.support.v4.content.LocalBroadcastManager
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.os.*
import android.os.Debug.getMemoryInfo
import android.os.Environment.getExternalStorageDirectory
import android.widget.Toast
import android.net.ConnectivityManager
import android.telephony.CellInfoGsm
import android.telephony.TelephonyManager
import android.telephony.CellSignalStrengthLte
import android.telephony.CellInfoLte
import android.telephony.CellSignalStrengthGsm
import android.telephony.CellSignalStrengthWcdma
import android.telephony.CellInfoWcdma
import android.telephony.CellInfo




class LocalisationService : Service() {

    private var locationManager : LocationManager? = null

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        getLocation()
        return Service.START_REDELIVER_INTENT
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
            val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).toString()
            val deviceName = Settings.Secure.getString(getContentResolver(), "bluetooth_name")
            val sdf = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.FRANCE)
            val currentDate = sdf.format(Date())
            val chargeStatus = chargeStatus(applicationContext)
            val memory = getMemoryAvailable()
            val network = chkStatus()
            Log.d("LOCALISATION", (location.latitude).toString() + " " + (location.longitude).toString() + " " + batLevel + "%")
            val myProfile = UserClass(deviceName, batLevel, location.latitude, location.longitude, currentDate, chargeStatus, memory, network)
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
        val myRef = database.getReference("position/" + user.deviceName)

        myRef.setValue(user)
    }

    fun chargeStatus(context: Context) : String {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        if (plugged == BatteryManager.BATTERY_PLUGGED_AC){
            return "Branché sur secteur"
        } else if(plugged == BatteryManager.BATTERY_PLUGGED_USB){
            return "Branché en USB"
        } else if(plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS){
            return "Charge sans fil"
        } else {
            return "Sur batterie"
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
        if (wifi.isConnectedOrConnecting) {
            return "Wifi"
        } else if (mobile.isConnectedOrConnecting) {
            return "Cellulaire"
        } else {
            return "Impossible"
        }
    }
}
