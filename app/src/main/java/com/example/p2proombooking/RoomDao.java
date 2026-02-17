package com.example.p2proombooking;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface RoomDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<RoomEntity> rooms);

    @Query("SELECT * FROM rooms ORDER BY building ASC, number ASC, suffix ASC")
    List<RoomEntity> getAll();
}