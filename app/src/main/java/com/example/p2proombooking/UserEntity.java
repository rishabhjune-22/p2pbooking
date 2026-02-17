package com.example.p2proombooking;
import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class UserEntity {

    @PrimaryKey
    @NonNull
    public String userId;   // UUID

    public String username; // unique

    public String displayName;

    public byte[] passwordHash;

    public byte[] salt;

    public long createdAt;
}