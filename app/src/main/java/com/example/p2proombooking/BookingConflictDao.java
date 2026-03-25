package com.example.p2proombooking;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BookingConflictDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BookingConflictEntity conflict);

    @Query("SELECT * FROM booking_conflicts ORDER BY conflictDetectedAt DESC")
    LiveData<List<BookingConflictEntity>> observeAll();

    @Query("SELECT * FROM booking_conflicts WHERE bookingId = :bookingId LIMIT 1")
    BookingConflictEntity getById(String bookingId);

    @Query("DELETE FROM booking_conflicts WHERE bookingId = :bookingId")
    void deleteById(String bookingId);

    @Query("DELETE FROM booking_conflicts")
    void deleteAll();
}
