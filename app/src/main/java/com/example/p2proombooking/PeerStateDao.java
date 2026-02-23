package com.example.p2proombooking;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface PeerStateDao {

    @Query("SELECT * FROM peer_state WHERE peerUserId = :peerUserId LIMIT 1")
    PeerStateEntity get(String peerUserId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(PeerStateEntity state);
}
