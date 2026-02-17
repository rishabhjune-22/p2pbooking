package com.example.p2proombooking;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.util.Arrays;

@Database(entities = {UserEntity.class, RoomEntity.class, BookingEntity.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract RoomDao roomDao();
    public abstract BookingDao bookingDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "secure_booking_db"
                            )
                            .allowMainThreadQueries() // demo only
                            .addCallback(prepopulateRoomsCallback(context.getApplicationContext()))
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
    private static Callback prepopulateRoomsCallback(Context appCtx) {
        return new Callback() {
            @Override
            public void onCreate(@NonNull SupportSQLiteDatabase db) {
                super.onCreate(db);

                new Thread(() -> {
                    // IMPORTANT: don't rely on INSTANCE being ready here
                    AppDatabase database = AppDatabase.getInstance(appCtx);

                    database.roomDao().insertAll(RoomSeed.buildDefaultRooms());
                }).start();
            }
        };
    }
}