package com.example.roombooking.room.local;

import android.content.Context;
import android.database.Cursor;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
        entities = {RoomEntity.class, CacheEntryEntity.class},
        version = 4,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "room_booking_db";

    private static volatile AppDatabase instance;

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            migrateRoomsCacheSchema(database);
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            migrateRoomsCacheSchema(database);
        }
    };

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            createCacheEntriesTable(database);
        }
    };

    public abstract RoomDao roomDao();
    public abstract CacheEntryDao cacheEntryDao();

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
                // Rooms are a server-backed cache. Migrations preserve existing rows when
                // possible; RoomRepository can still refresh stale cache from the API.
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build();
    }

    private static void createCacheEntriesTable(SupportSQLiteDatabase database) {
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `cache_entries` ("
                        + "`cacheKey` TEXT NOT NULL, "
                        + "`payloadJson` TEXT NOT NULL, "
                        + "`updatedAtMillis` INTEGER NOT NULL, "
                        + "PRIMARY KEY(`cacheKey`))"
        );
    }

    private static void migrateRoomsCacheSchema(SupportSQLiteDatabase database) {
        createCacheEntriesTable(database);

        boolean roomsTableExists = tableExists(database, "rooms");

        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `rooms_migration_new` ("
                        + "`id` INTEGER NOT NULL, "
                        + "`prefix` TEXT NOT NULL, "
                        + "`number` TEXT NOT NULL, "
                        + "`roomName` TEXT NOT NULL, "
                        + "`hostelName` TEXT NOT NULL, "
                        + "`hasAttachedBath` INTEGER NOT NULL, "
                        + "`roomType` TEXT NOT NULL, "
                        + "`selectionLabel` TEXT NOT NULL, "
                        + "`displayOrder` INTEGER NOT NULL, "
                        + "PRIMARY KEY(`id`))"
        );

        if (roomsTableExists) {
            String labelFallback = columnExists(database, "rooms", "number")
                    ? "COALESCE(NULLIF(`number`, ''), '')"
                    : "''";
            String selectionLabelExpression = columnExists(database, "rooms", "selectionLabel")
                    ? "COALESCE(NULLIF(`selectionLabel`, ''), " + labelFallback + ")"
                    : labelFallback;

            database.execSQL(
                    "INSERT OR REPLACE INTO `rooms_migration_new` ("
                            + "`id`, `prefix`, `number`, `roomName`, `hostelName`, "
                            + "`hasAttachedBath`, `roomType`, `selectionLabel`, `displayOrder`"
                            + ") SELECT "
                            + numericColumnOrDefault(database, "rooms", "id", "`rowid`") + ", "
                            + textColumnOrDefault(database, "rooms", "prefix", "") + ", "
                            + textColumnOrDefault(database, "rooms", "number", "") + ", "
                            + textColumnOrDefault(database, "rooms", "roomName", "") + ", "
                            + textColumnOrDefault(database, "rooms", "hostelName", "") + ", "
                            + numericColumnOrDefault(database, "rooms", "hasAttachedBath", "1") + ", "
                            + textColumnOrDefault(database, "rooms", "roomType", "room") + ", "
                            + selectionLabelExpression + ", "
                            + numericColumnOrDefault(database, "rooms", "displayOrder", "0")
                            + " FROM `rooms`"
            );

            database.execSQL("DROP TABLE `rooms`");
        }

        database.execSQL("ALTER TABLE `rooms_migration_new` RENAME TO `rooms`");
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_rooms_prefix` ON `rooms` (`prefix`)");
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_rooms_number` ON `rooms` (`number`)");
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_rooms_prefix_number` "
                        + "ON `rooms` (`prefix`, `number`)"
        );
    }

    private static String textColumnOrDefault(
            SupportSQLiteDatabase database,
            String tableName,
            String columnName,
            String defaultValue
    ) {
        if (columnExists(database, tableName, columnName)) {
            return "COALESCE(`" + columnName + "`, '" + defaultValue + "')";
        }

        return "'" + defaultValue + "'";
    }

    private static String numericColumnOrDefault(
            SupportSQLiteDatabase database,
            String tableName,
            String columnName,
            String defaultValue
    ) {
        return columnExists(database, tableName, columnName)
                ? "COALESCE(`" + columnName + "`, " + defaultValue + ")"
                : defaultValue;
    }

    private static boolean tableExists(SupportSQLiteDatabase database, String tableName) {
        Cursor cursor = database.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
                new Object[]{tableName}
        );
        try {
            return cursor.moveToFirst();
        } finally {
            cursor.close();
        }
    }

    private static boolean columnExists(
            SupportSQLiteDatabase database,
            String tableName,
            String columnName
    ) {
        Cursor cursor = database.query("PRAGMA table_info(`" + tableName + "`)");
        try {
            int nameColumnIndex = cursor.getColumnIndex("name");
            while (cursor.moveToNext()) {
                if (columnName.equals(cursor.getString(nameColumnIndex))) {
                    return true;
                }
            }
            return false;
        } finally {
            cursor.close();
        }
    }
}
