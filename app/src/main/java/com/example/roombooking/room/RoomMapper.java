package com.example.roombooking.room;

import com.example.roombooking.model.room.RoomItem;
import com.example.roombooking.room.local.RoomEntity;

import java.util.ArrayList;
import java.util.List;

public final class RoomMapper {

    private RoomMapper() {
        // Utility class. No object required.
    }

    public static RoomEntity toEntity(RoomItem item) {
        if (item == null) {
            return null;
        }

        return new RoomEntity(
                item.getId(),
                item.getSafePrefix(),
                item.getSafeNumber(),
                item.getSafeRoomName(),
                item.getSafeHostelName(),
                item.hasAttachedBath(),
                item.getSafeRoomType(),
                item.getSafeSelectionLabel(),
                item.getDisplayOrder()
        );
    }

    public static RoomItem toModel(RoomEntity entity) {
        if (entity == null) {
            return null;
        }

        RoomItem item = new RoomItem();
        item.setId(entity.getId());
        item.setPrefix(entity.getPrefix());
        item.setNumber(entity.getNumber());
        item.setRoomName(entity.getRoomName());
        item.setHostelName(entity.getHostelName());
        item.setHasAttachedBath(entity.isHasAttachedBath());
        item.setRoomType(entity.getRoomType());
        item.setSelectionLabel(entity.getSelectionLabel());
        item.setDisplayOrder(entity.getDisplayOrder());

        return item;
    }

    public static List<RoomEntity> toEntityList(List<RoomItem> items) {
        List<RoomEntity> entities = new ArrayList<>();

        if (items == null || items.isEmpty()) {
            return entities;
        }

        for (RoomItem item : items) {
            RoomEntity entity = toEntity(item);

            if (entity != null) {
                entities.add(entity);
            }
        }

        return entities;
    }

    public static List<RoomItem> toModelList(List<RoomEntity> entities) {
        List<RoomItem> items = new ArrayList<>();

        if (entities == null || entities.isEmpty()) {
            return items;
        }

        for (RoomEntity entity : entities) {
            RoomItem item = toModel(entity);

            if (item != null) {
                items.add(item);
            }
        }

        return items;
    }
}
