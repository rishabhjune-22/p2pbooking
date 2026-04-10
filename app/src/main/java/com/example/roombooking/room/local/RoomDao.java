package com.example.roombooking.room.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface RoomDao {

    @Query("SELECT * FROM rooms ORDER BY prefix ASC, number ASC")
    List<RoomEntity> getAllRooms();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRooms(List<RoomEntity> rooms);

    @Query("DELETE FROM rooms")
    void clearRooms();
}