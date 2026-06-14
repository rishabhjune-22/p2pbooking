package com.example.roombooking.booking;

import java.util.Objects;

public class CalendarDayItem {

    public static final int TYPE_EMPTY = 0;
    public static final int TYPE_AVAILABLE = 1;
    public static final int TYPE_NOT_AVAILABLE = 2;
    public static final int TYPE_HALF_AVAILABLE = 3;
    public static final int TYPE_LESS_THAN_HALF_AVAILABLE = 4;

    private final int dayNumber;
    private final String date;
    private final int availabilityType;
    private final int availableRooms;

    public CalendarDayItem(
            int dayNumber,
            String date,
            int availabilityType,
            int availableRooms
    ) {
        this.dayNumber = dayNumber;
        this.date = date != null ? date.trim() : null;
        this.availabilityType = availabilityType;
        this.availableRooms = availableRooms;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public String getDate() {
        return date;
    }

    public String getSafeDate() {
        return date != null ? date : "";
    }

    public int getAvailabilityType() {
        return availabilityType;
    }

    public int getAvailableRooms() {
        return availableRooms;
    }

    public boolean isEmpty() {
        return availabilityType == TYPE_EMPTY;
    }

    public boolean hasSameContent(CalendarDayItem other) {
        if (other == null) return false;

        return dayNumber == other.dayNumber
                && availabilityType == other.availabilityType
                && availableRooms == other.availableRooms
                && Objects.equals(date, other.date);
    }
}