package com.rikcore.kotlinproject

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.location.Location
import android.os.Parcelable
import android.provider.Settings
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.google.android.gms.maps.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import io.fabric.sdk.android.Fabric;




class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val LOCATION_PERMISSION_RC = 1
    private var isFocused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        Fabric.with(this, Crashlytics())

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, IntentFilter("GPSLocationUpdates"));

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
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
        }
        // Add a marker in Sydney and move the camera
        val DEFAULT_LAT_TLSE = 43.608316  //Lattitude St Sernin Tlse
        val DEFAULT_LON_TLSE = 1.441804 //Longitude St Sernin Tlse
        val toulouse = LatLng(DEFAULT_LAT_TLSE, DEFAULT_LON_TLSE)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(toulouse))
        getData()
    }

    private val mMessageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Get extra data included in the Intent
            val message = intent.getStringExtra("Status")
            val b = intent.getBundleExtra("Location")
            val lastKnownLoc = b.getParcelable<Parcelable>("Location") as Location
            if (lastKnownLoc != null && ::mMap.isInitialized && !isFocused) {
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
        var contactList = ArrayList<UserClass>()
        databaseReference.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                println("TestBed: ${snapshot.value}")
                mMap.clear()
                for (postSnapshot in snapshot.getChildren()) {
                    val deviceName = Settings.Secure.getString(getContentResolver(), "bluetooth_name")
                    var currentUser = postSnapshot.getValue(UserClass::class.java)
                    var currentUserDeviceName = currentUser!!.deviceName
                    var test = currentUser!!.deviceName
                    var latLng : LatLng = LatLng(currentUser!!.latitude!!, currentUser!!.longitude!!)
                    var bitmapDescriptor : BitmapDescriptor? = null
                    if(deviceName.equals(currentUserDeviceName)){
                        bitmapDescriptor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    }

                    mMap.addMarker(MarkerOptions()
                            .position(latLng)
                            .title(currentUser.deviceName + " " + currentUser.batteryLvl + "% " + currentUser.captureDate))
                            .setIcon(bitmapDescriptor)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        Log.d("LIIIIIIIST", (contactList.size).toString())
    }

    override fun onRequestPermissionsResult(requestCode : Int ,
                                            permissions: Array<String>,
                                            grantResults: IntArray){
        //      it is IntArray rather than Array<Int>   ---^
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d("PERMISSION", "User Permission for Location Granted")
            startService(Intent(this,LocalisationService::class.java))
        }
    }

    public override fun onResume() {
        super.onResume()
        isFocused = false
    }

}
