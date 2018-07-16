package com.rikcore.kotlinproject

import android.annotation.SuppressLint
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
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import me.leolin.shortcutbadger.ShortcutBadger
import kotlin.collections.HashMap

class LocalisationService : Service() {

    private var locationManager : LocationManager? = null
    private var userPref: SharedPreferences? = null
    private var userEdit: SharedPreferences.Editor? = null

    private var lastLocation: Location? = null

    override fun onBind(intent: Intent): IBinder {
        throw UnsupportedOperationException("Not yet implemented");
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        userPref = this.getSharedPreferences("user_info", 0)
        userEdit = userPref?.edit()
        getLocation()
        checkMessage()
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
            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locationListener)
        } catch (ex : SecurityException){
            Log.d("FAILED", "Security exception")
        }
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            localSaveLocation(location)
            val deviceId = getDeviceId()
            val deviceName = userPref!!.getString("userName", Settings.Secure.getString(contentResolver, "bluetooth_name"))
            val batLevel = getBatteryLevel()
            val currentDate = getCurrentDateInString()
            val chargeStatus = chargeStatus(applicationContext)
            val memory = getMemoryAvailable()
            val network = chkStatus()
            val uptime = getUptime()
            val speed = calculateSpeed(location)
            val myProfile = UserClass(deviceId, deviceName, batLevel, location.latitude, location.longitude, currentDate, chargeStatus, memory, network, uptime, speed)
            sendData(myProfile)
            sendMessageToActivity(location, "Position")
        }
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun localSaveLocation (location : Location){
        userEdit?.putString("latitude", location.latitude.toString())
        userEdit?.putString("longitude", location.longitude.toString())
    }

    @SuppressLint("HardwareIds")
    private fun getDeviceId() : String {
        return Settings.Secure.getString(applicationContext.contentResolver,
                Settings.Secure.ANDROID_ID)
    }

    private fun getBatteryLevel() : String {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).toString()
    }

    private fun getCurrentDateInString() : String {
        val sdf = SimpleDateFormat("dd/M/yyyy HH:mm:ss", Locale.FRANCE)
        return sdf.format(Date())
    }

    private fun sendMessageToActivity(l: Location, msg: String) {
        val intent = Intent("GPSLocationUpdates")
        intent.putExtra("Status", msg)
        val b = Bundle()
        b.putParcelable("Location", l)
        intent.putExtra("Location", b)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun sendData(user : UserClass){
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("position/" + user.deviceId)
        val userMap = HashMap<String, Any?>()
        userMap["batteryLvl"] = user.batteryLvl
        userMap["captureDate"] = user.captureDate
        userMap["chargeStatus"] = user.chargeStatus
        userMap["deviceId"] = user.deviceId
        userMap["deviceName"] = user.deviceName
        userMap["latitude"] = user.latitude
        userMap["longitude"] = user.longitude
        userMap["memoryAvailable"] = user.memoryAvailable
        userMap["networkStatus"] = user.networkStatus
        userMap["uptime"] = user.uptime
        userMap["speed"] = user.speed
        myRef.updateChildren(userMap)
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

    private fun checkMessage(){
        val ref = FirebaseDatabase.getInstance().getReference("messages/" + getDeviceId())

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                var messageQuantity = 0
                for(postSnapshot in p0.children){
                    messageQuantity++
                }
                ShortcutBadger.applyCount(applicationContext, messageQuantity)
            }

            override fun onCancelled(p0: DatabaseError) {
            }
        })
    }

    private fun calculateSpeed(location: Location) : Int {
        var speed : Int = 0
            if (this.lastLocation != null){
                /*speed = (Math.sqrt(
                        Math.pow(location.longitude - lastLocation!!.longitude, 2.0)
                                + Math.pow(location.latitude - lastLocation!!.latitude, 2.0)
                ) / (location.time - this.lastLocation!!.time)).toInt()
                Toast.makeText(applicationContext, "Manual speed : " + speed + "m/s", Toast.LENGTH_LONG).show()*/
                val dLat = Math.toRadians(location.latitude - lastLocation!!.latitude)
                val dLon = Math.toRadians(location.longitude - lastLocation!!.longitude)
                val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + (Math.cos(Math.toRadians(location.latitude))
                        * Math.cos(Math.toRadians(lastLocation!!.latitude)) * Math.sin(dLon / 2)
                        * Math.sin(dLon / 2))
                val c = 2 * Math.asin(Math.sqrt(a))
                val distanceInMeters = Math.round(6371000 * c)
                speed = (distanceInMeters * 3.6).toInt()
            }

            //if there is speed from location
            if (location.hasSpeed()){
                //get location speed
                speed = (location.speed * 3.6).toInt()
                Toast.makeText(applicationContext, "Automatic speed : " + speed + "km/h", Toast.LENGTH_LONG).show()
            }


            this.lastLocation = location
            ////////////
            //DO WHAT YOU WANT WITH speed VARIABLE
            ////////////

        return speed
    }

}
