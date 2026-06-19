package com.example.roombooking.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;

import com.example.roombooking.booking.AvailabilityRepository;
import com.example.roombooking.booking.BookingRepository;
import com.example.roombooking.model.booking.BookingStatus;
import com.example.roombooking.room.RoomRepository;

import org.junit.Test;

import java.util.Calendar;

public class LightBackgroundSyncSchedulerTest {

    @Test
    public void uniqueWorkNameIsStable() {
        assertEquals(
                "room_booking_light_background_sync",
                LightBackgroundSyncScheduler.UNIQUE_WORK_NAME
        );
    }

    @Test
    public void intervalIsConservativeAndTwelveHours() {
        assertEquals(12L, LightBackgroundSyncScheduler.SYNC_INTERVAL_HOURS);
        assertTrue(LightBackgroundSyncScheduler.SYNC_INTERVAL_HOURS >= 6L);
    }

    @Test
    public void existingWorkPolicyKeepsSingleScheduledWork() {
        assertEquals(
                ExistingPeriodicWorkPolicy.KEEP,
                LightBackgroundSyncScheduler.existingWorkPolicy()
        );
    }

    @Test
    public void constraintsRequireNetworkConnection() {
        assertEquals(
                NetworkType.CONNECTED,
                LightBackgroundSyncScheduler.buildConstraints().getRequiredNetworkType()
        );
    }

    @Test
    public void backgroundBookingKeysMatchRepositoryKeys() {
        assertEquals(
                BookingRepository.firstPageCacheKey(null, null, null, BookingStatus.ACTIVE),
                LightBackgroundSyncWorker.bookingPageOneCacheKey(BookingStatus.ACTIVE)
        );
        assertEquals(
                BookingRepository.firstPageCacheKey(null, null, null, BookingStatus.EXPIRED),
                LightBackgroundSyncWorker.bookingPageOneCacheKey(BookingStatus.EXPIRED)
        );
    }

    @Test
    public void backgroundCalendarKeyUsesOneBasedMonthLikeLandingScreen() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2026);
        calendar.set(Calendar.MONTH, Calendar.JUNE);
        calendar.set(Calendar.DAY_OF_MONTH, 19);

        assertEquals(
                AvailabilityRepository.calendarAvailabilityCacheKey(6, 2026),
                LightBackgroundSyncWorker.calendarAvailabilityCacheKey(calendar)
        );
    }

    @Test
    public void roomCacheKeyMatchesExistingRoomsCache() {
        assertEquals("rooms:all", RoomRepository.ROOM_CACHE_KEY);
    }
}
