package com.example.roombooking.booking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class RepositoryCacheKeyTest {

    @Test
    public void bookingFirstPageCacheKeyIncludesFilterState() {
        String activeKey = BookingRepository.firstPageCacheKey(
                "Beta",
                "2026-06-01",
                "2026-06-30",
                "active"
        );
        String expiredKey = BookingRepository.firstPageCacheKey(
                "Beta",
                "2026-06-01",
                "2026-06-30",
                "expired"
        );

        assertEquals("bookings:Beta:2026-06-01:2026-06-30:active:page1", activeKey);
        assertNotEquals(activeKey, expiredKey);
    }

    @Test
    public void calendarCacheKeyIsMonthScoped() {
        assertEquals(
                "availability:2026-6",
                AvailabilityRepository.calendarAvailabilityCacheKey(6, 2026)
        );
    }

    @Test
    public void availableRoomsCacheKeyIsDateAndPrefixScoped() {
        assertEquals(
                "available_rooms:Beta:2026-06-20",
                AvailabilityRepository.availableRoomsCacheKey("Beta", "2026-06-20")
        );
    }

    @Test
    public void availableRoomsRangeCacheKeyIsRangeAndPrefixScoped() {
        assertEquals(
                "available_rooms_range:Gamma:2026-06-20:2026-06-22",
                AvailabilityRepository.availableRoomsRangeCacheKey(
                        "Gamma",
                        "2026-06-20",
                        "2026-06-22"
                )
        );
    }

    @Test
    public void availabilityCacheInvalidationUpdatesSharedVersion() {
        int before = AvailabilityRepository.getCacheInvalidationVersion();

        AvailabilityRepository.clearAvailabilityCaches(null);

        assertEquals(before + 1, AvailabilityRepository.getCacheInvalidationVersion());
    }
}
