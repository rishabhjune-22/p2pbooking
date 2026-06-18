package com.example.roombooking.cache;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CachePolicyTest {

    @Test
    public void cacheTtlsMatchPhaseOnePolicy() {
        assertTrue(CachePolicy.ROOMS_TTL_MS == 24L * 60L * 60L * 1000L);
        assertTrue(CachePolicy.BOOKING_PAGE_ONE_TTL_MS >= 30L * 1000L);
        assertTrue(CachePolicy.BOOKING_PAGE_ONE_TTL_MS <= 60L * 1000L);
        assertTrue(CachePolicy.CALENDAR_AVAILABILITY_TTL_MS >= 15L * 1000L);
        assertTrue(CachePolicy.CALENDAR_AVAILABILITY_TTL_MS <= 30L * 1000L);
    }

    @Test
    public void freshnessUsesStrictTtlWindow() {
        long now = 10_000L;
        long ttl = 1_000L;

        assertTrue(CachePolicy.isFresh(9_001L, ttl, now));
        assertFalse(CachePolicy.isFresh(9_000L, ttl, now));
        assertFalse(CachePolicy.isFresh(0L, ttl, now));
        assertFalse(CachePolicy.isFresh(9_500L, 0L, now));
    }
}
