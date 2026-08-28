package com.example.roombooking;

import static org.junit.Assert.assertEquals;

import com.example.roombooking.utils.DateTimeUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;
import java.util.TimeZone;

public class DateTimeUtilsTest {

    private TimeZone originalTimeZone;
    private Locale originalLocale;

    @Before
    public void setUp() {
        originalTimeZone = TimeZone.getDefault();
        originalLocale = Locale.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
        Locale.setDefault(Locale.FRANCE);
    }

    @After
    public void tearDown() {
        TimeZone.setDefault(originalTimeZone);
        Locale.setDefault(originalLocale);
    }

    @Test
    public void utcDateTimeIsConvertedToLocalDisplayTime() {
        assertEquals(
                "15 Jun 2026, 03:30 PM",
                DateTimeUtils.formatUtcToLocal("2026-06-15T10:00:00Z")
        );
    }

    @Test
    public void compactDateTimeUsesFullReadableDisplayFormat() {
        assertEquals(
                "15 Jun 2026, 03:30 PM",
                DateTimeUtils.formatUtcToCompactLocal("2026-06-15T10:00:00Z")
        );
    }

    @Test
    public void dateTimesEmbeddedInErrorsAreFormatted() {
        assertEquals(
                "Available after 15 Jun 2026, 03:30 PM.",
                DateTimeUtils.formatDateTimesInText(
                        "Available after 2026-06-15T10:00:00Z."
                )
        );
    }

    @Test
    public void backendDateTimeWithSpaceAndOffsetIsConvertedToLocalDisplayTime() {
        assertEquals(
                "19 Jun 2026, 05:00 PM",
                DateTimeUtils.formatUtcToLocal("2026-06-19 11:30:00+00:00")
        );
    }

    @Test
    public void invalidValueIsReturnedUnchanged() {
        assertEquals("not-a-date", DateTimeUtils.formatUtcToLocal("not-a-date"));
        assertEquals("", DateTimeUtils.formatUtcToLocal(" "));
    }
}
