package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookings")
data class Booking(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val destination: String,
    val origin: String = "الرياض",
    val travelDate: String,
    val bookingType: String, // "جوي ✈️", "بري 🚌", "بحري 🚢"
    val passengersCount: Int = 1,
    val notes: String = "",
    val priceSar: Double = 450.0,
    val escrowId: String = "",
    val ticketRef: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

typealias TravelBookingEntity = Booking
