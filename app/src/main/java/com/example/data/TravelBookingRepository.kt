package com.example.data

import kotlinx.coroutines.flow.Flow

class TravelBookingRepository(private val dao: TravelBookingDao) {
    val allBookings: Flow<List<TravelBookingEntity>> = dao.getAllBookings()

    suspend fun saveBooking(booking: TravelBookingEntity): Long {
        return dao.insertBooking(booking)
    }

    suspend fun deleteBooking(id: Long) {
        dao.deleteBookingById(id)
    }
}
