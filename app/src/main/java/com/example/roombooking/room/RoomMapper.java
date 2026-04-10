package com.example.roombooking.room;

import com.example.roombooking.model.room.RoomItem;
import com.example.roombooking.room.local.RoomEntity;

import java.util.ArrayList;
import java.util.List;

public final class RoomMapper {

    private RoomMapper() {
    }

    public static RoomEntity toEntity(RoomItem item) {
        return new RoomEntity(
                item.getId(),
                safe(item.getPrefix()),
                safe(item.getNumber()),
                safe(item.getRoomName())
        );
    }

    public static RoomItem toModel(RoomEntity entity) {
        RoomItem item = new RoomItem();
        item.setId(entity.getId());
        item.setPrefix(entity.getPrefix());
        item.setNumber(entity.getNumber());
        item.setRoomName(entity.getRoomName());
        return item;
    }

    public static List<RoomEntity> toEntityList(List<RoomItem> items) {
        List<RoomEntity> entities = new ArrayList<>();
        if (items == null) {
            return entities;
        }

        for (RoomItem item : items) {
            entities.add(toEntity(item));
        }
        return entities;
    }

    public static List<RoomItem> toModelList(List<RoomEntity> entities) {
        List<RoomItem> items = new ArrayList<>();
        if (entities == null) {
            return items;
        }

        for (RoomEntity entity : entities) {
            items.add(toModel(entity));
        }
        return items;
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }
}