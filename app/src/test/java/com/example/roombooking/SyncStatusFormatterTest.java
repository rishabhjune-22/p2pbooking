package com.example.roombooking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.roombooking.utils.SyncStatusFormatter;

import org.junit.Test;

public class SyncStatusFormatterTest {

    @Test
    public void lastUpdatedFormatsZeroSecondsAsJustNow() {
        assertEquals(
                "Last updated just now",
                SyncStatusFormatter.lastUpdated(10_000L, 10_000L)
        );
    }

    @Test
    public void lastUpdatedFormatsThirtySecondsAsJustNow() {
        assertEquals(
                "Last updated just now",
                SyncStatusFormatter.lastUpdated(10_000L, 40_000L)
        );
    }

    @Test
    public void lastUpdatedFormatsSixtyOneSecondsAsOneMinute() {
        assertEquals(
                "Last updated 1 min ago",
                SyncStatusFormatter.lastUpdated(10_000L, 71_000L)
        );
    }

    @Test
    public void lastUpdatedFormatsTwoMinutes() {
        assertEquals(
                "Last updated 2 mins ago",
                SyncStatusFormatter.lastUpdated(10_000L, 130_000L)
        );
    }

    @Test
    public void lastUpdatedFormatsSixtyMinutesAsOneHour() {
        assertEquals(
                "Last updated 1 hr ago",
                SyncStatusFormatter.lastUpdated(10_000L, 3_610_000L)
        );
    }

    @Test
    public void lastUpdatedFormatsMultipleMinutes() {
        assertEquals(
                "Last updated 3 mins ago",
                SyncStatusFormatter.lastUpdated(1_000L, 181_000L)
        );
    }

    @Test
    public void offlineCachedStateUsesSavedDataMessage() {
        assertEquals(
                "Offline. Showing saved data.",
                SyncStatusFormatter.OFFLINE_SAVED_DATA
        );
        assertEquals(
                "Offline. Showing saved data.\nFinal booking will be verified by server.",
                SyncStatusFormatter.offlineAvailabilitySaved()
        );
    }

    @Test
    public void availabilityDecisionTextKeepsServerValidationWarning() {
        assertTrue(
                SyncStatusFormatter.availabilityDecisionText(System.currentTimeMillis())
                        .contains("Final booking will be verified by server.")
        );
    }
}
