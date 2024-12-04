package com.example.cams


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.cardview.widget.CardView

class Dashboard : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val cardAdd = findViewById<CardView>(R.id.AddCardview)
        val carManage= findViewById<CardView>(R.id.ManageCarview)
        val carFAQQ = findViewById<CardView>(R.id.FAQview)
        val carLogingout = findViewById<CardView>(R.id.logout)


        carLogingout.setOnClickListener {
            val intent2 = Intent(this, Logout::class.java)
            startActivity(intent2)

            finish()
        }
        carFAQQ.setOnClickListener {
            val intent2 = Intent(this, FAQ::class.java)
            startActivity(intent2)
            // Optional: Remove if you want to keep the Dashboard activity in the back stack
            finish()
        }

        cardAdd.setOnClickListener {
            val intent2 = Intent(this, AddCar::class.java)
            startActivity(intent2)
            // Optional: Remove if you want to keep the Dashboard activity in the back stack
            finish()
        }

        carManage.setOnClickListener {
            val intent2 = Intent(this, Manage::class.java)
            startActivity(intent2)
            // Optional: Remove if you want to keep the Dashboard activity in the back stack
            finish()
        }
    }
}
