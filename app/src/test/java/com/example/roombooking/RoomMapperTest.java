package com.example.roombooking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.example.roombooking.model.room.RoomItem;
import com.example.roombooking.room.RoomMapper;
import com.example.roombooking.room.local.RoomEntity;

import org.junit.Test;

import java.util.Arrays;

public class RoomMapperTest {

    @Test
    public void mapperPreservesRoomFieldsAcrossRoundTrip() {
        RoomItem source = new RoomItem(12, "Beta", "104", "Beta 104");

        RoomEntity entity = RoomMapper.toEntity(source);
        RoomItem mapped = RoomMapper.toModel(entity);

        assertNotNull(entity);
        assertNotNull(mapped);
        assertEquals(source.getId(), mapped.getId());
        assertEquals(source.getPrefix(), mapped.getPrefix());
        assertEquals(source.getNumber(), mapped.getNumber());
        assertEquals(source.getRoomName(), mapped.getRoomName());
    }

    @Test
    public void listMapperSkipsNullEntries() {
        RoomItem room = new RoomItem(1, "Gamma", "201", "Gamma 201");

        assertEquals(1, RoomMapper.toEntityList(Arrays.asList(null, room)).size());
        assertNull(RoomMapper.toEntity(null));
        assertNull(RoomMapper.toModel(null));
    }
}
