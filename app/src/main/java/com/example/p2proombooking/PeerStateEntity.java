package com.example.p2proombooking;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "peer_state")
public class PeerStateEntity {

    @PrimaryKey
    @NonNull
    public String peerUserId;

    public long lastSyncedUpdatedAt;
}
