package com.example.cams

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class SignUp : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        lateinit var auth: FirebaseAuth



        auth = FirebaseAuth.getInstance()

        val emailField = findViewById<EditText>(R.id.EmailSign)
        val passwordField = findViewById<EditText>(R.id.PasswordSign)
        val signUpButton = findViewById<Button>(R.id.SignInButton)
        val loginlink = findViewById<TextView>(R.id.LogLink)

        signUpButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign up success, update UI with the signed-in user's information
                        Toast.makeText(this, "Sign Up Successful", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        // If sign up fails, display a message to the user.
                        Toast.makeText(this, "Sign Up Failed", Toast.LENGTH_SHORT).show()
                    }
                }

        }
        loginlink.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

        }

    }
}
