package com.example.roombooking.booking;

import com.example.roombooking.model.room.RoomItem;
import com.example.roombooking.model.room.RoomInventory;
import com.example.roombooking.model.room.RoomPrefix;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class RoomSpinnerEntries {

    private RoomSpinnerEntries() {
    }

    static List<RoomSpinnerEntry> build(List<RoomItem> rooms) {
        List<RoomSpinnerEntry> entries = new ArrayList<>();
        entries.add(RoomSpinnerEntry.prompt());

        for (String prefix : RoomPrefix.displayOrder()) {
            List<RoomItem> prefixRooms = roomsForPrefix(rooms, prefix);
            if (prefixRooms.isEmpty()) continue;

            RoomItem firstRoom = prefixRooms.get(0);
            entries.add(RoomSpinnerEntry.header(
                    prefix + " — " + firstRoom.getSafeHostelName()
            ));

            if (RoomPrefix.DELTA.equals(prefix)) {
                addSection(entries, prefixRooms, "chairman_flat", "Chairman Flat");
                addSection(entries, prefixRooms, "room", "Rooms");
            } else {
                addRooms(entries, prefixRooms, null);
            }
        }

        return entries;
    }

    static List<RoomSpinnerEntry> buildAvailable(List<AvailableRoomItem> rooms) {
        List<RoomSpinnerEntry> entries = new ArrayList<>();
        entries.add(RoomSpinnerEntry.prompt());

        for (String prefix : RoomPrefix.displayOrder()) {
            List<AvailableRoomItem> prefixRooms = availableRoomsForPrefix(rooms, prefix);
            if (prefixRooms.isEmpty()) continue;

            entries.add(RoomSpinnerEntry.header(prefix));

            for (int index = 0; index < prefixRooms.size(); index++) {
                AvailableRoomItem availableRoom = prefixRooms.get(index);
                RoomItem room = toRoomItem(availableRoom, prefix, index);
                entries.add(RoomSpinnerEntry.room(
                        room,
                        availableRoomLabel(prefix, availableRoom)
                ));
            }
        }

        if (entries.size() == 1) {
            entries.add(RoomSpinnerEntry.header("No available rooms for selected dates"));
        }

        return entries;
    }

    private static List<RoomItem> roomsForPrefix(List<RoomItem> rooms, String prefix) {
        List<RoomItem> result = new ArrayList<>();
        for (RoomItem room : rooms) {
            if (room != null && prefix.equalsIgnoreCase(room.getSafePrefix())) {
                result.add(room);
            }
        }
        result.sort(Comparator.comparingInt(RoomItem::getDisplayOrder));
        return result;
    }

    private static List<AvailableRoomItem> availableRoomsForPrefix(
            List<AvailableRoomItem> rooms,
            String prefix
    ) {
        List<AvailableRoomItem> result = new ArrayList<>();
        for (AvailableRoomItem room : RoomInventory.visibleAvailableRooms(prefix, rooms)) {
            String roomPrefix = room.getPrefix();
            if (roomPrefix != null && prefix.equalsIgnoreCase(roomPrefix.trim())) {
                result.add(room);
            }
        }
        return result;
    }

    private static RoomItem toRoomItem(
            AvailableRoomItem availableRoom,
            String fallbackPrefix,
            int displayOrder
    ) {
        RoomItem room = new RoomItem();
        room.setId(availableRoom.getRoomId());
        room.setPrefix(firstNonBlank(availableRoom.getPrefix(), fallbackPrefix));
        room.setNumber(availableRoom.getSafeSelectionLabel());
        room.setRoomName(availableRoom.getSafeRoomName());
        room.setSelectionLabel(availableRoom.getSafeSelectionLabel());
        room.setDisplayOrder(displayOrder);
        return room;
    }

    private static String availableRoomLabel(String prefix, AvailableRoomItem room) {
        String label = RoomInventory.displayAvailableRoomLabel(prefix, room);
        if (!room.isPartiallyAvailable()) {
            return label;
        }

        String date = room.getSafeAvailableFromDate();
        String time = room.getSafeAvailableFromTime();
        if (!date.isEmpty() && !time.isEmpty()) {
            return label + " (Partial, from " + date + " " + time + ")";
        }
        if (!time.isEmpty()) {
            return label + " (Partial, from " + time + ")";
        }
        return label + " (Partial)";
    }

    private static void addSection(
            List<RoomSpinnerEntry> entries,
            List<RoomItem> rooms,
            String roomType,
            String heading
    ) {
        boolean hasRooms = false;
        for (RoomItem room : rooms) {
            if (roomType.equals(room.getSafeRoomType())) {
                hasRooms = true;
                break;
            }
        }
        if (!hasRooms) return;

        entries.add(RoomSpinnerEntry.header(heading));
        addRooms(entries, rooms, roomType);
    }

    private static void addRooms(
            List<RoomSpinnerEntry> entries,
            List<RoomItem> rooms,
            String requiredType
    ) {
        for (RoomItem room : rooms) {
            if (requiredType == null || requiredType.equals(room.getSafeRoomType())) {
                entries.add(RoomSpinnerEntry.room(room));
            }
        }
    }

    private static String firstNonBlank(String first, String second) {
        String safeFirst = first != null ? first.trim() : "";
        return safeFirst.isEmpty() ? (second != null ? second.trim() : "") : safeFirst;
    }
}
