package com.example.p2proombooking;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BookingConflictDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(BookingConflictEntity c);

    @Query("SELECT * FROM booking_conflicts ORDER BY detectedAt DESC")
    List<BookingConflictEntity> getAll();

    @Query("SELECT * FROM booking_conflicts WHERE bookingId = :id LIMIT 1")
    BookingConflictEntity getById(String id);

    @Query("DELETE FROM booking_conflicts WHERE bookingId = :id")
    void deleteById(String id);
}
