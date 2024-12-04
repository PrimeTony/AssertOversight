package com.example.cams
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Manage : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var carAdapter: CarAdapter
    private val carList = mutableListOf<Car>()
    private val carIds = mutableListOf<String>()
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage)

        // Set up the Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Enable the back button in the Toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Initialize RecyclerView and Adapter
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        carAdapter = CarAdapter(this, carList, carIds)
        recyclerView.adapter = carAdapter

        if (userId != null) {
            db.collection("users").document(userId).collection("cars")
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val car = document.toObject(Car::class.java)
                        carList.add(car)
                        carIds.add(document.id)
                    }
                    carAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Log.e("Manage", "Error fetching cars", e)
                }
        } else {
            Log.e("Manage", "User not logged in")
        }
    }

    // Handle back button press
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()  // This will trigger the default back button behavior
        return true
    }
}
