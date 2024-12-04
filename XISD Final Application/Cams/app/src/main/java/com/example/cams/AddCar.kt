package com.example.cams

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import com.google.firebase.auth.FirebaseAuth


class AddCar : AppCompatActivity() {

    // UI Elements
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var fetchTokenButton: Button
    private lateinit var firstUriTextView: TextView
    private lateinit var imeiInput: EditText
    private lateinit var fetchLocationButton: Button
    private lateinit var secondUriTextView: TextView
    private lateinit var carNameInput: EditText
    private lateinit var numberPlateInput: EditText
    private lateinit var insertVehicleButton: Button

    // Variables
    private var accessToken: String? = null
    private val client = OkHttpClient()
    private val db = FirebaseFirestore.getInstance() // Firestore instance
    private val userId = FirebaseAuth.getInstance().currentUser?.uid


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_car)

        // Initialize UI elements
        usernameInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.passwordInput)
        fetchTokenButton = findViewById(R.id.fetchTokenButton)
        firstUriTextView = findViewById(R.id.firstUriTextView)
        imeiInput = findViewById(R.id.imeiInput)
        fetchLocationButton = findViewById(R.id.fetchLocationButton)
        secondUriTextView = findViewById(R.id.secondUriTextView)
        carNameInput = findViewById(R.id.ModelInput)
        numberPlateInput = findViewById(R.id.NumberplateInput)
        insertVehicleButton = findViewById(R.id.InsertVheicle)

        // Hide car fields initially
        imeiInput.visibility = View.GONE
        fetchLocationButton.visibility = View.GONE
        carNameInput.visibility = View.GONE
        numberPlateInput.visibility = View.GONE
        insertVehicleButton.visibility = View.GONE

        // Fetch token button click listener
        fetchTokenButton.setOnClickListener {
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                fetchAccessToken(username, password)
            } else {
                firstUriTextView.text = "Please enter both username and password"
            }
        }

        // Fetch location button click listener
        fetchLocationButton.setOnClickListener {
            val imei = imeiInput.text.toString()

            if (imei.isNotEmpty() && accessToken != null) {
                fetchLocationData(imei)
            } else {
                secondUriTextView.text = "Please enter IMEI or fetch access token"
            }
        }

        // Insert vehicle button click listener
        insertVehicleButton.setOnClickListener {
            val carName = carNameInput.text.toString()
            val numberPlate = numberPlateInput.text.toString()
            val imei = imeiInput.text.toString()
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()

            if (carName.isNotEmpty() && numberPlate.isNotEmpty()) {
                // Call function to save the vehicle data to Firestore

                val (latitude, longitude) = secondUriTextView.tag as? Pair<Double, Double> ?: Pair(0.0, 0.0)

                // Check if latitude and longitude are not 0.0 before saving
                if (latitude != 0.0 && longitude != 0.0) {
                    saveVehicleDataToFirestore(username, password, imei, carName, numberPlate, latitude, longitude)
                }
            } else {
                secondUriTextView.text = "Please enter car name and number plate"
            }

        }
    }

    // Function to fetch access token
    private fun fetchAccessToken(username: String, password: String) {
        val timestamp = System.currentTimeMillis() / 1000
        val passwordHash = computeMd5Hash(password)
        val combined = passwordHash + timestamp
        val signature = computeMd5Hash(combined)

        val authUri = "https://api.protrack365.com/api/authorization?time=$timestamp&account=$username&signature=$signature"

        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val response = fetchFromNetwork(authUri)
                val json = JSONObject(response)
                accessToken = json.getJSONObject("record").getString("access_token")

                firstUriTextView.text = "Access Token: $accessToken"
                imeiInput.visibility = View.VISIBLE
                fetchLocationButton.visibility = View.VISIBLE
            } catch (e: Exception) {
                firstUriTextView.text = "Failed to fetch access token: ${e.localizedMessage}"
                Log.e("AddCar", "Error fetching access token", e)
            }
        }
    }

    // Function to fetch location data
    private fun fetchLocationData(imei: String) {
        val trackUri = "https://api.protrack365.com/api/track?access_token=$accessToken&imeis=$imei"

        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val response = fetchFromNetwork(trackUri)
                val json = JSONObject(response)
                val records = json.getJSONArray("record")

                if (records.length() > 0) {
                    val firstRecord = records.getJSONObject(0)
                    val latitude = firstRecord.getDouble("latitude")
                    val longitude = firstRecord.getDouble("longitude")

                    secondUriTextView.text = "Latitude: $latitude\nLongitude: $longitude"
                    secondUriTextView.tag = latitude to longitude // Store lat/long for later

                    // Make car fields visible after location is fetched
                    carNameInput.visibility = View.VISIBLE
                    numberPlateInput.visibility = View.VISIBLE
                    insertVehicleButton.visibility = View.VISIBLE
                } else {
                    secondUriTextView.text = "No location data available."
                }
            } catch (e: Exception) {
                secondUriTextView.text = "Failed to fetch location data: ${e.localizedMessage}"
                Log.e("AddCar", "Error fetching location data", e)
            }
        }
    }

    // Firestore saving function
    private fun saveVehicleDataToFirestore(username: String, password: String, imei: String, carName: String, numberPlate: String, latitude: Double, longitude: Double) {
        val vehicleData = hashMapOf(
            "username" to username,
            "password" to password,
            "imei" to imei,
            "carName" to carName,
            "numberPlate" to numberPlate,
            "latitude" to latitude,
            "longitude" to longitude
        )

        userId?.let {
            db.collection("users").document(it).collection("cars")
                .add(vehicleData)
                .addOnSuccessListener { documentReference ->
                    Log.d("AddCar", "Vehicle added with ID: ${documentReference.id}")
                    secondUriTextView.text = "Vehicle information saved successfully!"
                }
                .addOnFailureListener { e ->
                    Log.w("AddCar", "Error adding vehicle", e)
                    secondUriTextView.text = "Failed to save vehicle data."
                }
        } ?: run {
            secondUriTextView.text = "User not logged in"
        }

    }

    // Helper function to compute MD5 hash
    private fun computeMd5Hash(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val hashBytes = md.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
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
}
