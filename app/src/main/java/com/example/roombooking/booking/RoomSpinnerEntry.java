package com.example.roombooking.booking;

import com.example.roombooking.model.room.RoomItem;
import com.example.roombooking.model.room.RoomInventory;

final class RoomSpinnerEntry {

    private final String label;
    private final RoomItem room;
    private final boolean header;

    private RoomSpinnerEntry(String label, RoomItem room, boolean header) {
        this.label = label;
        this.room = room;
        this.header = header;
    }

    static RoomSpinnerEntry prompt() {
        return new RoomSpinnerEntry("Select Room", null, false);
    }

    static RoomSpinnerEntry header(String label) {
        return new RoomSpinnerEntry(label, null, true);
    }

    static RoomSpinnerEntry room(RoomItem room) {
        return new RoomSpinnerEntry(RoomInventory.displayRoomLabel(room), room, false);
    }

    RoomItem getRoom() {
        return room;
    }

    boolean isSelectable() {
        return room != null;
    }

    boolean isHeader() {
        return header;
    }

    @Override
    public String toString() {
        return label;
    }
}
