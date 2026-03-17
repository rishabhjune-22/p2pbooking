package com.example.p2proombooking;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
        entities = {
                UserEntity.class,
                RoomEntity.class,
                BookingEntity.class,
                BookingConflictEntity.class,
                PeerStateEntity.class
        },
        version = 6,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract RoomDao roomDao();
    public abstract BookingDao bookingDao();
    public abstract BookingConflictDao bookingConflictDao();
    public abstract PeerStateDao peerStateDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "secure_booking_db"
                            )
                            .allowMainThreadQueries()
                            .addCallback(seedRoomsCallback())
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static Callback seedRoomsCallback() {
        return new Callback() {
            @Override
            public void onCreate(@NonNull SupportSQLiteDatabase db) {
                super.onCreate(db);

                new Thread(() -> {
                    try {
                        java.util.List<RoomEntity> rooms = RoomSeed.buildDefaultRooms();
                        if (rooms == null || rooms.isEmpty()) return;

                        db.beginTransaction();
                        try {
                            for (RoomEntity r : rooms) {
                                db.execSQL(
                                        "INSERT OR IGNORE INTO rooms(roomId, building, number, suffix, displayName) VALUES(?,?,?,?,?)",
                                        new Object[]{
                                                r.roomId,
                                                r.building,
                                                r.number,
                                                r.suffix,
                                                r.displayName
                                        }
                                );
                            }
                            db.setTransactionSuccessful();
                        } finally {
                            db.endTransaction();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        };
    }
}