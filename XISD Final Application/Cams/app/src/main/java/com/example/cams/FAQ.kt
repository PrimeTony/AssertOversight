package com.example.cams

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FAQ : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faq)

        lateinit var faqRecyclerView: RecyclerView
         lateinit var faqAdapter: FAQAdapter



            // Set up the Toolbar
            val toolbar: Toolbar = findViewById(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)

            // Initialize RecyclerView and Adapter
            faqRecyclerView = findViewById(R.id.faqRecyclerView)
            faqRecyclerView.layoutManager = LinearLayoutManager(this)
            faqAdapter = FAQAdapter(generateFAQData())
            faqRecyclerView.adapter = faqAdapter
        }

        // Handle back button press
        override fun onSupportNavigateUp(): Boolean {
            onBackPressed()
            return true
        }

        // Generate FAQ data (you can replace this with a dynamic data source)
        private fun generateFAQData(): List<FAQItem> {
            return listOf(
                FAQItem("What is GPS tracking and how does it work?", "GPS tracking uses a tracking device to determine precise vehicle locations..."),
                FAQItem("Can I track multiple vehicles?", "Yes, our systems can track multiple vehicles simultaneously..."),
                FAQItem("is the tracking really live??", "Yes, the tracking system is live."),
                FAQItem("Why cant i add a vheicle??", "in order to add a vheicle , you have to have a profile first created by assertoversight as they will provide the tracker as well"),
                FAQItem("Can I add a vheicle on the website?", "No"),
                FAQItem("How do i get in for more queries ?", "send an email to cams@gmail.com or whatsapp 0763920358"),
                // Add more FAQs here as needed
            )
        }
    }


