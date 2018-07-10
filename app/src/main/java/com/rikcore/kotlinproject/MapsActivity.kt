package com.rikcore.kotlinproject

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.*
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
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
import com.google.android.gms.maps.model.*
import com.google.firebase.storage.FirebaseStorage
import io.fabric.sdk.android.Fabric;
import java.net.URL

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {


    private lateinit var mMap: GoogleMap
    private val LOCATION_PERMISSION_RC = 1
    private var isFocused = false
    private var userPref: SharedPreferences? = null
    private var userEdit: SharedPreferences.Editor? = null
    private lateinit var editTextName : EditText
    private lateinit var buttonName : Button
    private lateinit var buttonImage : Button
    private lateinit var settingsLayout : ConstraintLayout
    private lateinit var progressBar: ProgressBar

    private var localeCacheMap = HashMap<String, Marker>()

    private lateinit var sensorManager : SensorManager
    private var mAccelLast : Double = 0.0

    private val RESULT_LOAD_IMAGE = 111
    private lateinit var deviceId : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        Fabric.with(this, Crashlytics())
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)

        userPref =this.getSharedPreferences("user_info", 0)
        userEdit = userPref?.edit()

        deviceId = Settings.Secure.getString(applicationContext.getContentResolver(),
                Settings.Secure.ANDROID_ID)


        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, IntentFilter("GPSLocationUpdates"));

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        editTextName = findViewById(R.id.editTextName)
        buttonName = findViewById(R.id.buttonName)
        buttonImage = findViewById(R.id.buttonImage)
        settingsLayout = findViewById(R.id.settingsLayout)
        progressBar = findViewById(R.id.progressBar)

        progressBar.visibility = View.VISIBLE
        settingsLayout.bringToFront()


        buttonName.setOnClickListener {
            val name = editTextName.text.toString()
            if (name != ""){
                userEdit?.putString("userName", name)
                userEdit?.apply()
                val userHashMap = HashMap<String, String>()
                userHashMap["deviceName"] = name
                val ref = FirebaseDatabase.getInstance().getReference("position/" + deviceId)
                ref.updateChildren(userHashMap as Map<String, Any>)
                editTextName.text.clear()
                editTextName.hint = name
                Toast.makeText(this, "Pseudo changé", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Merci de choisir un pseudo", Toast.LENGTH_LONG).show()
            }
        }

        buttonImage.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), RESULT_LOAD_IMAGE)
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
            startService(Intent(this,LocalisationService::class.java))
            getData()
        }

        val defaultLat = userPref?.getString("latitude", "43.608316")
        val defaultLong = userPref?.getString("longitude", "1.441804")
        val defaultPos = LatLng(defaultLat!!.toDouble(), defaultLong!!.toDouble())
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPos, 15f))

    }

    private val mMessageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Get extra data included in the Intent
            val message = intent.getStringExtra("Status")
            val b = intent.getBundleExtra("Location")
            val lastKnownLoc = b.getParcelable<Parcelable>("Location") as Location
            if (::mMap.isInitialized && !isFocused) {
                isFocused = true
                val myPosLatLng = LatLng(lastKnownLoc.latitude, lastKnownLoc.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLng(myPosLatLng))
                val zoom : CameraUpdate = CameraUpdateFactory.zoomTo(15f)
                mMap.animateCamera(zoom)
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun getData(){
        val databaseReference = FirebaseDatabase.getInstance().getReference("position")
        databaseReference.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                for (postSnapshot in snapshot.getChildren()) {
                    val currentUser = postSnapshot.getValue(UserClass::class.java)

                    if (localeCacheMap[currentUser!!.deviceId] == null){
                        putMarker(currentUser)
                    } else if (currentUser.latitude != localeCacheMap[currentUser.deviceId]!!.position.latitude || currentUser.longitude != localeCacheMap[currentUser.deviceId]!!.position.longitude){
                        localeCacheMap[currentUser.deviceId]!!.position = LatLng(currentUser.latitude!!, currentUser.longitude!!)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun putMarker(currentUser : UserClass){
        Thread({
            val latLng = LatLng(currentUser.latitude!!, currentUser.longitude!!)
            val bitmapDescriptor: BitmapDescriptor?
            bitmapDescriptor = if(currentUser.pictureUrl != null){
                val realUrl = URL(currentUser.pictureUrl)
                val bmp : Bitmap = BitmapFactory.decodeStream(realUrl.openConnection().getInputStream())
                BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bmp,120, 120, false))
            } else {
                BitmapDescriptorFactory.fromResource(R.drawable.risitete)
            }

            runOnUiThread({
                val marker : Marker = mMap.addMarker(MarkerOptions()
                        .position(latLng)
                        .title(currentUser.deviceName + " " + currentUser.batteryLvl + "% " + currentUser.captureDate)
                        .icon(bitmapDescriptor))

                localeCacheMap[currentUser.deviceId!!] = marker
            })
        }).start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RESULT_LOAD_IMAGE && data?.data != null){
            val mStorage = FirebaseStorage.getInstance().getReference("Profile Pictures" + "/" + deviceId)
            val image = data.data
            mStorage.putFile(image).addOnSuccessListener {
                mStorage.downloadUrl.addOnSuccessListener {
                    val urlString = it.toString()
                    val ref = FirebaseDatabase.getInstance().getReference("position/" + deviceId)
                    val updateMap = HashMap<String, String>()
                    updateMap["pictureUrl"] = urlString
                    ref.updateChildren(updateMap as Map<String, Any>)
                    Toast.makeText(this, "Image enregistrée, mise à jour à la prochaine localisation", Toast.LENGTH_LONG).show()

                }
            }

        }
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


