package com.rikcore.kotlinproject

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.hardware.*
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.location.Location
import android.os.Parcelable
import android.provider.Settings
import android.support.constraint.ConstraintLayout
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.view.View
import android.widget.*
import com.google.android.gms.maps.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import io.fabric.sdk.android.Fabric;

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {


    private lateinit var mMap: GoogleMap
    private val LOCATION_PERMISSION_RC = 1
    private var isFocused = false
    private var userPref: SharedPreferences? = null
    private var userEdit: SharedPreferences.Editor? = null
    private lateinit var switchLocation : Switch
    private lateinit var editTextName : EditText
    private lateinit var buttonName : Button
    private lateinit var settingsLayout : ConstraintLayout

    private lateinit var sensorManager : SensorManager
    private var mAccelLast : Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        Fabric.with(this, Crashlytics())
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)

        userPref =this.getSharedPreferences("user_info", 0)
        userEdit = userPref?.edit()

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, IntentFilter("GPSLocationUpdates"));

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        switchLocation = findViewById(R.id.actionbar_switch)
        editTextName = findViewById(R.id.editTextName)
        buttonName = findViewById(R.id.buttonName)
        settingsLayout = findViewById(R.id.settingsLayout)
        settingsLayout.bringToFront()

        switchLocation.isChecked = userPref!!.getBoolean("isVisible", true)

        switchLocation.setOnCheckedChangeListener { buttonView, isChecked ->
            userEdit?.putBoolean("isVisible", isChecked)
            userEdit?.apply()
        }

        buttonName.setOnClickListener {
            val name = editTextName.text.toString()
            if (!name.equals("")){
                userEdit?.putString("userName", name)
                userEdit?.apply()
                Toast.makeText(this, "Pseudo enregistré, mise à jour à la prochaine localisation", Toast.LENGTH_LONG).show()
                editTextName.text.clear()
                editTextName.hint = name
            } else {
                Toast.makeText(this, "Merci de choisir un pseudo", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_RC)
        } else {
            switchLocation.visibility = View.VISIBLE
            startService(Intent(this,LocalisationService::class.java))
            getData()
        }

        val defaultLat = userPref?.getString("latitude", "43.608316")
        val defaultLong = userPref?.getString("longitude", "1.441804")
        val defaultPos = LatLng(defaultLat!!.toDouble(), defaultLong!!.toDouble())
        mMap.moveCamera(CameraUpdateFactory.newLatLng(defaultPos))
        val zoom : CameraUpdate = CameraUpdateFactory.zoomTo(15f)
        mMap.animateCamera(zoom)

    }

    private val mMessageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Get extra data included in the Intent
            val message = intent.getStringExtra("Status")
            val b = intent.getBundleExtra("Location")
            val lastKnownLoc = b.getParcelable<Parcelable>("Location") as Location
            if (::mMap.isInitialized && !isFocused) {
                isFocused = true
                var myPosLatLng = LatLng(lastKnownLoc.latitude, lastKnownLoc.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLng(myPosLatLng))
                var zoom : CameraUpdate = CameraUpdateFactory.zoomTo(15f)
                mMap.animateCamera(zoom)
            }
        }
    }

    private fun getData(){
        var databaseReference = FirebaseDatabase.getInstance().getReference("position")
        databaseReference.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                mMap.clear()
                for (postSnapshot in snapshot.getChildren()) {
                    val deviceName = Settings.Secure.getString(getContentResolver(), "bluetooth_name")
                    val currentUser = postSnapshot.getValue(UserClass::class.java)
                    if(currentUser!!.isVisible){
                        val currentUserDeviceName = currentUser.deviceName
                        val latLng = LatLng(currentUser.latitude!!, currentUser.longitude!!)
                        var bitmapDescriptor: BitmapDescriptor?
                        if(deviceName != null && deviceName.equals(currentUserDeviceName) || currentUser.deviceName.equals(userPref!!.getString("userName", deviceName))){
                            bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.quintero)
                            editTextName.hint = currentUserDeviceName
                        } else {
                            bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.risitete)
                        }
                        mMap.addMarker(MarkerOptions()
                                .position(latLng)
                                .title(currentUser.deviceName + " " + currentUser.batteryLvl + "% " + currentUser.captureDate))
                                .setIcon(bitmapDescriptor)
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    override fun onRequestPermissionsResult(requestCode : Int ,
                                            permissions: Array<String>,
                                            grantResults: IntArray){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d("PERMISSION", "User Permission for Location Granted")
            startService(Intent(this,LocalisationService::class.java))
            getData()
        }
    }

    public override fun onResume() {
        super.onResume()
        isFocused = false
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    override fun onSensorChanged(p0: SensorEvent?) {
        val x = p0!!.values[0]
        val y = p0.values[1]
        val z = p0.values[2]
        val mAccelCurrent = Math.sqrt((x * x + y * y + z * z).toDouble())
        val delta = mAccelCurrent - mAccelLast
        mAccelLast = mAccelCurrent
        if (delta > 8) {
            if(settingsLayout.visibility == View.VISIBLE){
                settingsLayout.visibility = View.GONE
            } else {
                settingsLayout.visibility= View.VISIBLE
            }
        }
    }
}


