package com.example.roombooking.room.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {RoomEntity.class},
        version = 2,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "room_booking_db";

    private static volatile AppDatabase instance;

    public abstract RoomDao roomDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = buildDatabase(context.getApplicationContext());
                }
            }
        }

        return instance;
    }

    private static AppDatabase buildDatabase(Context appContext) {
        return Room.databaseBuilder(
                        appContext,
                        AppDatabase.class,
                        DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .build();
    }
}