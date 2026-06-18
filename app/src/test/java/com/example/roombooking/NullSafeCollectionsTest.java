package com.example.roombooking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.roombooking.utils.NullSafeCollections;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class NullSafeCollectionsTest {

    @Test
    public void copyWithoutNullsReturnsEmptyListForNullInput() {
        assertTrue(NullSafeCollections.copyWithoutNulls(null).isEmpty());
    }

    @Test
    public void copyWithoutNullsPreservesOrderAndRemovesNullEntries() {
        List<String> result = NullSafeCollections.copyWithoutNulls(
                Arrays.asList("first", null, "second")
        );

        assertEquals(Arrays.asList("first", "second"), result);
    }

    @Test
    public void hasNonNullItemsRejectsNullOnlyLists() {
        assertTrue(!NullSafeCollections.hasNonNullItems(Arrays.asList(null, null)));
        assertTrue(NullSafeCollections.hasNonNullItems(Arrays.asList(null, "value")));
    }
}
