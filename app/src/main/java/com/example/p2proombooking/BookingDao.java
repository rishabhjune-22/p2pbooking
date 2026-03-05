package com.example.p2proombooking;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface BookingDao {

    // =============================
    // INSERT / UPDATE
    // =============================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BookingEntity booking);

    @Update
    void update(BookingEntity booking);

    // =============================
    // LIVE OBSERVERS (UI)
    // =============================

    @Query("SELECT * FROM bookings WHERE status = 'ACTIVE' AND deletedFlag = 0 ORDER BY startUtc DESC")
    LiveData<List<BookingEntity>> observeAllActive();

    @Query("SELECT * FROM bookings WHERE deletedFlag = 0 ORDER BY startUtc DESC")
    LiveData<List<BookingEntity>> observeAllIncludingCanceled();


    // =============================
    // NON-LIVE READS (SYNC ENGINE)
    // =============================

    @Query("SELECT * FROM bookings WHERE status = 'ACTIVE' AND deletedFlag = 0 ORDER BY startUtc DESC")
    List<BookingEntity> getAllActive();

    @Query("SELECT * FROM bookings WHERE deletedFlag = 0 ORDER BY startUtc DESC")
    List<BookingEntity> getAllIncludingCanceled();

    @Query("SELECT * FROM bookings WHERE bookingId = :bookingId LIMIT 1")
    BookingEntity getById(String bookingId);


    // =============================
    // OVERLAP CHECKS
    // =============================

    @Query("SELECT * FROM bookings " +
            "WHERE roomId = :roomId " +
            "AND status = 'ACTIVE' " +
            "AND deletedFlag = 0 " +
            "AND (:newStartUtc < endUtc) " +
            "AND (:newEndUtc > startUtc)")
    List<BookingEntity> findOverlaps(
            String roomId,
            long newStartUtc,
            long newEndUtc
    );

    @Query("SELECT * FROM bookings " +
            "WHERE roomId = :roomId " +
            "AND status = 'ACTIVE' " +
            "AND deletedFlag = 0 " +
            "AND bookingId != :excludeBookingId " +
            "AND (:newStartUtc < endUtc) " +
            "AND (:newEndUtc > startUtc)")
    List<BookingEntity> findOverlapsExcluding(
            String roomId,
            String excludeBookingId,
            long newStartUtc,
            long newEndUtc
    );

    // FAST overlap detection for SyncEngine
    @Query("SELECT * FROM bookings " +
            "WHERE roomId = :roomId " +
            "AND status = 'ACTIVE' " +
            "AND deletedFlag = 0 " +
            "AND (:newStartUtc < endUtc) " +
            "AND (:newEndUtc > startUtc) " +
            "LIMIT 1")
    BookingEntity findFirstOverlap(
            String roomId,
            long newStartUtc,
            long newEndUtc
    );


    // =============================
    // CANCEL BOOKING
    // =============================

    @Query("UPDATE bookings SET " +
            "status = 'CANCELED', " +
            "canceledByUserId = :canceledByUserId, " +
            "canceledAt = :now, " +
            "updatedAt = :now, " +
            "syncFlag = 'PENDING_SYNC', " +
            "version = version + 1 " +
            "WHERE bookingId = :bookingId AND deletedFlag = 0")
    void cancelBooking(
            String bookingId,
            String canceledByUserId,
            long now
    );


    // =============================
    // SYNC OPERATIONS
    // =============================

    @Query("SELECT * FROM bookings " +
            "WHERE syncFlag = 'PENDING_SYNC' " +
            "AND status != 'CONFLICTED' " +
            "AND deletedFlag = 0 " +
            "ORDER BY updatedAt ASC")
    List<BookingEntity> getPendingSync();

    @Query("SELECT * FROM bookings " +
            "WHERE updatedAt > :since " +
            "AND deletedFlag = 0 " +
            "AND status != 'CONFLICTED' " +
            "ORDER BY updatedAt ASC")
    List<BookingEntity> getChangesSince(long since);

    @Query("UPDATE bookings SET " +
            "syncFlag = 'SYNCED', " +
            "lastSyncedAt = :now " +
            "WHERE bookingId = :bookingId AND deletedFlag = 0")
    void markSynced(String bookingId, long now);

    @Query("UPDATE bookings SET " +
            "syncFlag = 'SYNCED', " +
            "lastSyncedAt = :now " +
            "WHERE updatedAt <= :maxUpdatedAt " +
            "AND syncFlag = 'PENDING_SYNC' " +
            "AND deletedFlag = 0")
    void markSyncedUpTo(long maxUpdatedAt, long now);
}