package com.example.data

import kotlinx.coroutines.flow.Flow

class BookingRepository(private val dao: BookingDao) {
    val allBookings: Flow<List<Booking>> = dao.getAllBookings()

    suspend fun saveBooking(booking: Booking): Long {
        return dao.insertBooking(booking)
    }

    suspend fun deleteBooking(id: Long) {
        dao.deleteBookingById(id)
    }
}

typealias TravelBookingRepository = BookingRepository
