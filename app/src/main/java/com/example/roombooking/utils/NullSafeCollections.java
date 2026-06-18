package com.example.roombooking.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class NullSafeCollections {

    private NullSafeCollections() {
        // Utility class. No object required.
    }

    @NonNull
    public static <T> List<T> copyWithoutNulls(@Nullable List<T> items) {
        List<T> result = new ArrayList<>();

        if (items == null || items.isEmpty()) {
            return result;
        }

        for (T item : items) {
            if (item != null) {
                result.add(item);
            }
        }

        return result;
    }

    public static boolean hasNonNullItems(@Nullable List<?> items) {
        if (items == null || items.isEmpty()) {
            return false;
        }

        for (Object item : items) {
            if (item != null) {
                return true;
            }
        }

        return false;
    }
}
