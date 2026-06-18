package com.example.roombooking.model.room;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class RoomPrefix {

    public static final String BETA = "Beta";
    public static final String GAMMA = "Gamma";
    public static final String DELTA = "Delta";
    public static final String ALL_BUILDINGS = "All Buildings";

    private static final List<String> DISPLAY_ORDER =
            Collections.unmodifiableList(Arrays.asList(DELTA, GAMMA, BETA));

    private static final List<String> FILTER_OPTIONS =
            Collections.unmodifiableList(Arrays.asList(
                    ALL_BUILDINGS,
                    DELTA,
                    GAMMA,
                    BETA
            ));

    private RoomPrefix() {
        // Utility class. No object required.
    }

    public static List<String> displayOrder() {
        return DISPLAY_ORDER;
    }

    public static List<String> filterOptions() {
        return FILTER_OPTIONS;
    }

    public static boolean isAllBuildings(String value) {
        return ALL_BUILDINGS.equalsIgnoreCase(value);
    }
}
