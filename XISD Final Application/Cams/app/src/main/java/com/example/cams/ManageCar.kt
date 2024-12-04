package com.example.cams

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.io.IOException

class ManageCar : AppCompatActivity() {

    private lateinit var retrievedDataTextView: TextView
    private lateinit var accessTokenTextView: TextView
    private lateinit var jsonResponseTextView: TextView
    private lateinit var timestampTextView: TextView // TextView for timestamp
    private lateinit var authUriTextView: TextView  // TextView for clickable Auth URI

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private val client = OkHttpClient()

    private var accessToken: String? = null
    private var newAccessToken: String? = null  // New access token from the JSON response

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_car)

        // Initialize TextViews
        retrievedDataTextView = findViewById(R.id.retrievedDataTextView)
        accessTokenTextView = findViewById(R.id.accessTokenTextView)
        jsonResponseTextView = findViewById(R.id.jsonResponseTextView)
        timestampTextView = findViewById(R.id.timestampTextView) // Initialize the timestamp TextView
        authUriTextView = findViewById(R.id.authUriTextView)  // Initialize the authUri TextView

        // Fetch stored data and generate access token
        fetchStoredData()
    }

    // Function to retrieve stored data from Firestore
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
                    withContext(Dispatchers.Main) {
                        retrievedDataTextView.text = """
                            Retrieved Data:
                            Username: $username
                            Password: $password
                            IMEI: $imei
                        """.trimIndent()
                    }

                    // Proceed to generate access token
                    generateAccessToken(username, password, imei)

                } else {
                    withContext(Dispatchers.Main) {
                        retrievedDataTextView.text = "No car data found."
                    }
                }

            } catch (e: Exception) {
                Log.e("ManageCar", "Error fetching car data", e)
                withContext(Dispatchers.Main) {
                    retrievedDataTextView.text = "Error retrieving data: ${e.localizedMessage}"
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
                accessTokenTextView.text = """
                    Timestamp: $timestamp
                    Password Hash: $passwordHash
                    Combined String: $combined
                    Access Token: $accessToken
                """.trimIndent()

                // Display the timestamp in a separate TextView
                timestampTextView.text = "Timestamp: $timestamp"

                // Create the final URI (authUri)
                val authUri = "https://api.protrack365.com/api/authorization?time=$timestamp&account=$username&signature=$accessToken"

                // Make the authUri clickable
                authUriTextView.text = authUri
                authUriTextView.setOnClickListener {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(authUri))
                    startActivity(browserIntent)
                }

                // Proceed to fetch JSON data after a delay
                delay(1000)  // 1-second delay

                fetchNewAccessToken(authUri, imei)

            } catch (e: Exception) {
                accessTokenTextView.text = "Error generating access token: ${e.localizedMessage}"
                Log.e("ManageCar", "Error generating access token", e)
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
                    jsonResponseTextView.text = "Error fetching new access token: ${e.localizedMessage}"
                    Log.e("ManageCar", "Error fetching new access token", e)
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

                // Display the fetched JSON response
                withContext(Dispatchers.Main) {
                    jsonResponseTextView.text = """
                        JSON Response:
                        $json
                    """.trimIndent()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    jsonResponseTextView.text = "Error fetching JSON: ${e.localizedMessage}"
                    Log.e("ManageCar", "Error fetching JSON", e)
                }
            }
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
}
