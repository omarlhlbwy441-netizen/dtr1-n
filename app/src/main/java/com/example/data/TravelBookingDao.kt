package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TravelBookingDao {
    @Query("SELECT * FROM travel_bookings ORDER BY id DESC")
    fun getAllBookings(): Flow<List<TravelBookingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: TravelBookingEntity): Long

    @Query("DELETE FROM travel_bookings WHERE id = :id")
    suspend fun deleteBookingById(id: Long)

    @Delete
    suspend fun deleteBooking(booking: TravelBookingEntity)
}
