package com.example.cams

data class Vehicle(
    val username: String = "",
    val password: String = "",
    val imei: String = "",
    val vehicleName: String = "",
    val numberPlate: String = "",
    val carColor: String = "",
    val accessToken: String = "" // may not be available at the time of creation
)
