package com.rikcore.kotlinproject

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.provider.Settings
import android.support.constraint.ConstraintLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.view.animation.TranslateAnimation
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
import me.leolin.shortcutbadger.ShortcutBadger
import java.net.URL

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {


    private lateinit var mMap: GoogleMap
    private val LOCATION_PERMISSION_RC = 1
    private var isFocused = false
    private var userPref: SharedPreferences? = null
    private var userEdit: SharedPreferences.Editor? = null
    private lateinit var editTextName : EditText
    private lateinit var buttonName : Button
    private lateinit var buttonImage : Button
    private lateinit var listViewMessage : ListView
    private lateinit var settingsLayout : ConstraintLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var floatingActionButtonOverlay: FloatingActionButton

    private var localeCacheMap = HashMap<String, Marker>()
    private var localeCacheImageUrl = HashMap<String, String>()

    private val RESULT_LOAD_IMAGE = 111
    private lateinit var deviceId : String

    private lateinit var customWindowsAdapter: CustomWindowsAdapter

    private var isHiding = true

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        Fabric.with(this, Crashlytics())

        userPref = this.getSharedPreferences("user_info", 0)
        userEdit = userPref?.edit()

        deviceId = Settings.Secure.getString(applicationContext.contentResolver,
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
        listViewMessage = findViewById(R.id.listViewMessage)
        settingsLayout = findViewById(R.id.settingsLayout)
        progressBar = findViewById(R.id.progressBar)
        floatingActionButtonOverlay = findViewById(R.id.floatingActionButtonOverlay)


        progressBar.visibility = View.VISIBLE
        settingsLayout.bringToFront()
        editTextName.hint = userPref!!.getString("userName", "Pseudo")


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

        listViewMessage.setOnItemClickListener { adapterView, view, i, l ->
            val selectedMessage = listViewMessage.adapter.getItem(i) as Message
            val nodeId = selectedMessage.timeStamp
            val ref = FirebaseDatabase.getInstance().getReference("messages/" + deviceId + "/" + nodeId)
            ref.setValue(null)
        }

        floatingActionButtonOverlay.setOnClickListener {
            if(isHiding){
                slideDown(settingsLayout)
            } else {
                slideUp(settingsLayout)
            }
            isHiding = !isHiding
            val anim = android.view.animation.AnimationUtils.loadAnimation(floatingActionButtonOverlay.context,  R.anim.shake)
            anim.duration = 200L
            floatingActionButtonOverlay.startAnimation(anim)
        }

    }

    private fun openChat(user : UserClass){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Send message to : " + user.deviceName)
        builder.setMessage("Max 140 characters")
        val editTextMessage = EditText(this)
        builder.setView(editTextMessage)

        builder.setPositiveButton("Ok"){dialog, which ->
            Toast.makeText(applicationContext,"Sending",Toast.LENGTH_SHORT).show()
            val timeStamp = System.currentTimeMillis()
            val ref = FirebaseDatabase.getInstance().getReference("messages/" + user.deviceId + "/" + timeStamp)
            val message = Message(userPref!!.getString("userName", Settings.Secure.getString(contentResolver, "bluetooth_name")), deviceId, user.deviceName!!, user.deviceId!!, editTextMessage.text.toString(), timeStamp)
            ref.setValue(message)
        }

        val dialog : AlertDialog = builder.create()
        dialog.show()
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
            getMyMessage()
        }

        val defaultLat = userPref?.getString("latitude", "43.608316")
        val defaultLong = userPref?.getString("longitude", "1.441804")
        val defaultPos = LatLng(defaultLat!!.toDouble(), defaultLong!!.toDouble())
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPos, 15f))

        mMap.setOnInfoWindowLongClickListener {
            val selectedUser = it.tag as UserClass
            openChat(selectedUser)
        }
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
                    val loopedUser = postSnapshot.getValue(UserClass::class.java)
                    val loopedUserId = loopedUser!!.deviceId

                    if (localeCacheMap[loopedUserId] == null){
                        putMarker(loopedUser)
                    } else if (loopedUser.deviceName != localeCacheMap[loopedUserId]!!.title){
                        localeCacheMap[loopedUserId]!!.title = loopedUser.deviceName
                        localeCacheMap[loopedUserId]!!.tag = loopedUser

                    } else if (loopedUser.pictureUrl != localeCacheImageUrl[loopedUserId]){
                        localeCacheMap[loopedUserId]!!.remove()
                        putMarker(loopedUser)
                    } else if (loopedUser.latitude != localeCacheMap[loopedUserId]!!.position.latitude || loopedUser.longitude != localeCacheMap[loopedUserId]!!.position.longitude){
                        localeCacheMap[loopedUserId]!!.position = LatLng(loopedUser.latitude!!, loopedUser.longitude!!)
                        localeCacheMap[loopedUserId]!!.snippet = loopedUser.captureDate + "¤" + loopedUser.batteryLvl
                        localeCacheMap[loopedUserId]!!.tag = loopedUser
                    }
                }
                customWindowsAdapter = CustomWindowsAdapter(this@MapsActivity)
                mMap.setInfoWindowAdapter(customWindowsAdapter)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MapsActivity, "Network error", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun getMyMessage(){
        val ref = FirebaseDatabase.getInstance().getReference("messages/" + deviceId)

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                val messageList = ArrayList<Message>()
                for(postSnapshot in p0.children){
                    val loopedMessage = postSnapshot.getValue(Message::class.java)
                    messageList.add(loopedMessage!!)
                }
                val messageAdapter = MessageAdapter(this@MapsActivity, messageList)
                listViewMessage.adapter = messageAdapter
            }

            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(this@MapsActivity, "Failed to get messages", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun putMarker(currentUser : UserClass){
        val latLng = LatLng(currentUser.latitude!!, currentUser.longitude!!)
        val marker : Marker = mMap.addMarker(MarkerOptions()
                .position(latLng)
                .title(currentUser.deviceName)
                .snippet(currentUser.captureDate + "¤" + currentUser.batteryLvl)
        )
        marker.tag = currentUser
        localeCacheMap[currentUser.deviceId!!] = marker
        if(currentUser.pictureUrl != null){
            localeCacheImageUrl[currentUser.deviceId!!] = currentUser.pictureUrl!!
        }
        Thread({

            val bitmapDescriptor: BitmapDescriptor?
            bitmapDescriptor = if(currentUser.pictureUrl != null){
                val realUrl = URL(currentUser.pictureUrl)
                val bmp : Bitmap = BitmapFactory.decodeStream(realUrl.openConnection().getInputStream())
                BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bmp,180, 180, false))
            } else {
                BitmapDescriptorFactory.fromResource(R.mipmap.globe_icon_mini)
            }

            runOnUiThread({
                localeCacheMap[currentUser.deviceId!!]!!.setIcon(bitmapDescriptor)
            })
        }).start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RESULT_LOAD_IMAGE && data?.data != null){
            val image = data.data
            val inputStream = contentResolver.openInputStream(image)
            val datasize = inputStream.available()

            if(datasize < 200000){
                saveImage(image)
            } else {
                Toast.makeText(this, "Image trop volumineuse, 200ko maximum.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveImage (uri: Uri){
        val mStorage = FirebaseStorage.getInstance().getReference("Profile Pictures" + "/" + deviceId + System.currentTimeMillis())
        mStorage.putFile(uri).addOnSuccessListener {
            mStorage.downloadUrl.addOnSuccessListener {
                val urlString = it.toString()
                val ref = FirebaseDatabase.getInstance().getReference("position/" + deviceId)
                val updateMap = HashMap<String, String>()
                updateMap["pictureUrl"] = urlString
                ref.updateChildren(updateMap as Map<String, Any>)
                Toast.makeText(this, "Image enregistrée", Toast.LENGTH_LONG).show()
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
            getMyMessage()
        }
    }

    public override fun onResume() {
        super.onResume()
        isFocused = false
    }

    private fun slideUp(view: View) {
        view.animate()
                .translationY(- view.height.toFloat())
                .duration = 600
    }

    private fun slideDown(view: View) {
        view.animate()
                .translationY(view.height.toFloat())
                .duration = 600
    }
}


