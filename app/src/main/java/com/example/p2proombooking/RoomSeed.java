package com.example.p2proombooking;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RoomSeed {

    public static List<RoomEntity> buildDefaultRooms() {
        List<RoomEntity> list = new ArrayList<>();

        // Delta
        add(list, "Delta", 101, "A", "B", "C", "D");
        add(list, "Delta", 102, "A", "B", "C", "D");

        // Gamma
        add(list, "Gamma", 101, "A", "B", "C");
        add(list, "Gamma", 102, "A", "B", "C");

        // Beta
        add(list, "Beta", 1001, "A", "B");
        add(list, "Beta", 1002, "A", "B");
        add(list, "Beta", 1003, "A", "B");
        add(list, "Beta", 1004, "A", "B");
        add(list, "Beta", 1103, "A", "B");
        add(list, "Beta", 1104, "A", "B");

        return list;
    }

    private static void add(List<RoomEntity> out, String building, int number, String... suffixes) {
        String b = building.trim().toUpperCase(Locale.ROOT);

        for (String sfx : suffixes) {
            String s = sfx.trim().toUpperCase(Locale.ROOT);

            RoomEntity r = new RoomEntity();
            r.building = b;
            r.number = number;
            r.suffix = s;
            r.roomId = makeRoomId(b, number, s);
            r.displayName = toTitle(building) + " " + number + s;

            out.add(r);
        }
    }

    private static String makeRoomId(String buildingUpper, int number, String suffixUpper) {
        // pad small numbers for better sorting; big ones remain as-is
        // 101 -> 0101, 102 -> 0102, 1001 -> 1001
        String num = (number < 1000) ? String.format(Locale.ROOT, "%04d", number)
                : String.valueOf(number);
        return buildingUpper + "_" + num + "_" + suffixUpper;
    }

    private static String toTitle(String s) {
        if (s == null || s.isEmpty()) return s;
        String lower = s.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}