package com.example.p2proombooking;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BookingDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insert(BookingEntity booking);

    @Query("SELECT * FROM bookings WHERE status != 'CANCELED' ORDER BY startUtc DESC")
    List<BookingEntity> getAll();

    // Conflicts only within the same room:
    // overlap if newStart < existingEnd AND newEnd > existingStart
    @Query("SELECT * FROM bookings " +
            "WHERE roomId = :roomId AND status != 'CANCELED' " +
            "AND (:newStartUtc < endUtc) AND (:newEndUtc > startUtc) " +
            "ORDER BY startUtc ASC")
    List<BookingEntity> findOverlaps(String roomId, long newStartUtc, long newEndUtc);

    @Query("DELETE FROM bookings")
    void deleteAll();
}