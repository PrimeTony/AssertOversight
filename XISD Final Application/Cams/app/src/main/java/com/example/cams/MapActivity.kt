package com.example.cams

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.SearchView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale

class MapActivity : AppCompatActivity(), OnMapReadyCallback {


    private lateinit var locationSwitch: Switch
    private var mGoogleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private val client = OkHttpClient()

    private var accessToken: String? = null
    private var newAccessToken: String? = null

    private var geofenceCenter: LatLng? = null
    private var geofenceCircle: Circle? = null
    private val geofenceRadius = 300.0 // in meters

    private var lastKnownLatitude: Double? = null
    private var lastKnownLongitude: Double? = null

    private lateinit var searchView: SearchView



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
        //triggering map fragment
        fetchStoredData()
//remove later
        searchView = findViewById(R.id.searchbar)
        initSearchView()


        val geofenceButton = findViewById<ImageButton>(R.id.geofenceButton)
        geofenceButton.setOnClickListener {
            // Place the geofence around the current marker location
            placeGeofence()
        }




        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        //gps fused location

        val UsertextEmail = findViewById<TextView>(R.id.emailInitial)
        //for the email button ,creating separate method
        setEmailInitialToTextView(UsertextEmail)
        applyGlowAnimation(UsertextEmail)
        UsertextEmail.setOnClickListener{gotodash()}


    }

    private fun initSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    searchLocation(query)
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    private fun searchLocation(query: String) {
        // Use Geocoder to get the location from the query
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocationName(query, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val location = LatLng(address.latitude, address.longitude)

                // Clear previous marker
                mGoogleMap?.clear()

                // Add a new marker to the map for the searched location
                mGoogleMap?.addMarker(
                    MarkerOptions()
                        .position(location)
                        .title(query)
                )

                // Move the camera to the searched location
                mGoogleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error finding location", Toast.LENGTH_SHORT).show()
        }
    }


    private fun placeGeofence() {
        // Use the marker's current location as the center
        geofenceCenter = mGoogleMap?.cameraPosition?.target

        geofenceCenter?.let {
            // Remove any existing geofence circle
            geofenceCircle?.remove()

            // Draw a new circle with lime-green color
            geofenceCircle = mGoogleMap?.addCircle(
                CircleOptions()
                    .center(it)
                    .radius(geofenceRadius) // 300 meters for now
                    .strokeColor(0xFF32CD32.toInt()) // Lime green color (ARGB format)
                    .fillColor(0x2232CD32) // Light lime green with transparency
                    .strokeWidth(5f)
            )

            // Start monitoring the marker's position
            startGeofenceMonitoring()
        }
    }

    private fun startGeofenceMonitoring() {
        lifecycleScope.launch {
            while (true) {
                // Assume `fetchLocationData` updates the car's location
                val currentLatLng =
                    lastKnownLatitude?.let { lastKnownLongitude?.let { it1 -> LatLng(it, it1) } } // Replace with actual values

                geofenceCenter?.let { center ->
                    val distance = FloatArray(1)
                    if (currentLatLng != null) {
                        Location.distanceBetween(
                            center.latitude, center.longitude,
                            currentLatLng.latitude, currentLatLng.longitude,
                            distance
                        )
                    }

                    // Check if the car has left the geofence
                    if (distance[0] > geofenceRadius) {
                        // Show popup alert
                        showGeofenceExitAlert()
                       // Exit loop as alert has been shown
                    }
                }

                // Delay the loop to check every 5 seconds
                delay(5000)
            }
        }
    }

    private fun showGeofenceExitAlert() {
        // Display an AlertDialog to notify the user
        AlertDialog.Builder(this)
            .setTitle("Geofence Alert")
            .setMessage("The car has left the geofenced area.")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }



    private fun fetchStoredData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val carDoc = db.collection("users").document(userId!!).collection("cars")
                    .get().await() // Get the first car

                val carData = carDoc.documents.firstOrNull()?.data

                if (carData != null) {
                    val username = carData["username"].toString()
                    val password = carData["password"].toString()
                    val imei = carData["imei"].toString()

                    // Display retrieved data in TextView


                    // Proceed to generate access token
                    generateAccessToken(username, password, imei)

                } else {
                    withContext(Dispatchers.Main) {
                       //
                    }
                }

            } catch (e: Exception) {
                Log.e("ManageCar", "Error fetching car data", e)
                withContext(Dispatchers.Main) {
                //
                }
            }
        }
    }

    // Function to generate MD5 hash
    private fun computeMd5Hash(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val hashBytes = md.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    // Function to generate access token
    private fun generateAccessToken(username: String, password: String, imei: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                // Generate the current timestamp
                val timestamp = System.currentTimeMillis() / 1000

                // Compute MD5 hash of the password
                val passwordHash = computeMd5Hash(password)

                // Combine the password hash with the timestamp
                val combined = passwordHash + timestamp

                // Compute the MD5 hash of the combined string
                accessToken = computeMd5Hash(combined)

                // Display the generated access token
                """
                    Timestamp: $timestamp
                    Password Hash: $passwordHash
                    Combined String: $combined
                    Access Token: $accessToken
                """.trimIndent()

                // Display the timestamp in a separate TextView
                "Timestamp: $timestamp"

                // Create the final URI (authUri)
                val authUri = "https://api.protrack365.com/api/authorization?time=$timestamp&account=$username&signature=$accessToken"

                // Make the authUri clickable


                // Proceed to fetch JSON data after a delay
                delay(1000)  // 1-second delay

                fetchNewAccessToken(authUri, imei)

            } catch (e: Exception) {
               //
            }
        }
    }

    // Function to fetch new access token from the generated URI
    private fun fetchNewAccessToken(authUri: String, imei: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = fetchFromNetwork(authUri)
                val json = JSONObject(response)

                // Extract the new access token from the JSON response
                newAccessToken = json.getJSONObject("record").getString("access_token")

                // Proceed to fetch location data with the new access token
                fetchLocationData(imei)

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {

                }
            }
        }
    }

    // Function to fetch location data using the new access token

    private fun fetchLocationData(imei: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val trackUri = "https://api.protrack365.com/api/track?access_token=$newAccessToken&imeis=$imei"
                val response = fetchFromNetwork(trackUri)
                val json = JSONObject(response)

                // Check if the response contains the 'record' array and extract the first record
                val records: JSONArray = json.getJSONArray("record")
                if (records.length() > 0) {
                    val firstRecord: JSONObject = records.getJSONObject(0)

                    // Extract latitude and longitude from the first record
                    val latitude = firstRecord.getDouble("latitude")
                    val longitude = firstRecord.getDouble("longitude")

                    lastKnownLatitude = latitude
                    lastKnownLongitude = longitude

                    // Display the fetched latitude and longitude in the TextView
                    withContext(Dispatchers.Main) {
                       """
                            Latitude: $latitude
                            Longitude: $longitude
                        """.trimIndent()

                        // Add marker to the map at the location
                        addMarkerOnMap(latitude, longitude)
                    }
                } else {
                    withContext(Dispatchers.Main) {

                    }
                }


            } catch (e: Exception) {
                withContext(Dispatchers.Main) {

                }
            }
        }
    }
    private fun addMarkerOnMap(latitude: Double, longitude: Double) {
        if (mGoogleMap != null) {
            val location = LatLng(latitude, longitude)
            mGoogleMap?.addMarker(
                MarkerOptions()
                    .position(location)
                    .title("Tracked Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
            mGoogleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        }
    }


    // Helper function to make network requests
    private suspend fun fetchFromNetwork(uri: String): String {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(uri)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                response.body?.string() ?: throw IOException("Empty response body")
            }
        }
    }


    private fun applyGlowAnimation(textView: TextView)
    {
        // Load the glow animation from resources
        val glowAnimation = AnimationUtils.loadAnimation(this, R.anim.glow_animaion)

        // Start the animation
        textView.startAnimation(glowAnimation)

        // Optionally, stop the animation after 5 seconds (5000 milliseconds)
        textView.postDelayed({
            textView.clearAnimation()
        }, 5000)

    }

    private fun gotodash() {
        val intent = Intent(this, Dashboard::class.java)
        startActivity(intent)
    }



    // method for email retrival and initial setting
    private fun setEmailInitialToTextView(textView: TextView) {
        // Get the currently signed in user details from FirebaseAuth
        val currentUser = FirebaseAuth.getInstance().currentUser

        // If the user is signed in then retrieve email their email
        currentUser?.let {
            val email = it.email// from the firebase auth
            // If the email is not empty or null, set the email initial to the TextView
            if (!email.isNullOrEmpty()) {
                val emailInitial = email.first().uppercaseChar().toString()
                textView.text = emailInitial
            }
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap
        try {
            val success = mGoogleMap?.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this, R.raw.snazzy_map_style
                    //my json map style
                )
            )

            if (success == false) {//i will know if the connection isnt successful
            }
        } catch (e: Resources.NotFoundException) {
            // Handling the exception if the JSON file is not found
            e.printStackTrace()
        }



        // this sets the initial camera position
        val pretoria = LatLng(-25.7479, 28.2293)
        mGoogleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(pretoria, 12f))


    }



    fun showBottomSheet(view: View) {// for the bottom sheet functionality
        val dialog = BottomSheetDialog(this)
        val bottomSheetView = LayoutInflater.from(this).inflate(R.layout.bottomsheet, null)// inflate so we can access the switch

        // Access the switch
        locationSwitch = bottomSheetView.findViewById(R.id.locationSwitch)

        // Restore switch state so it is not restarted every time
        locationSwitch.isChecked = mGoogleMap?.isMyLocationEnabled ?: false

        // Set a listener on the switch
        locationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                enableLocation()//mGoogleMap?.isMyLocationEnabled = true
            } else {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // Request the users location permissions if necessary
                    checkLocationPermission()
                    return@setOnCheckedChangeListener
                }
                mGoogleMap?.isMyLocationEnabled = false
            }
        }

        dialog.setContentView(bottomSheetView)
        dialog.show()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun enableLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            mGoogleMap?.isMyLocationEnabled = true

            // Get the last known location and move the camera
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    mGoogleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                }
            }
        } else {
            // Request the permission if it's not granted
            checkLocationPermission()
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}