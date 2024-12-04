package com.example.cams

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class Logout : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logout)

        // Call the logout function when the activity starts
        logoutUser()
    }

    private fun logoutUser() {
        val auth = FirebaseAuth.getInstance()
        auth.signOut() // Sign out the current user

        // Redirect user to login screen (MainActivity)
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Close the Logout activity
    }
}
