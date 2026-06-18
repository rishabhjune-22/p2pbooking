package com.example.roombooking.cache;

public final class CacheReadResult<T> {

    private final String key;
    private final T value;
    private final long updatedAtMillis;
    private final boolean hit;
    private final boolean fresh;

    private CacheReadResult(
            String key,
            T value,
            long updatedAtMillis,
            boolean hit,
            boolean fresh
    ) {
        this.key = key;
        this.value = value;
        this.updatedAtMillis = updatedAtMillis;
        this.hit = hit;
        this.fresh = fresh;
    }

    public static <T> CacheReadResult<T> hit(
            String key,
            T value,
            long updatedAtMillis,
            boolean fresh
    ) {
        return new CacheReadResult<>(key, value, updatedAtMillis, true, fresh);
    }

    public static <T> CacheReadResult<T> miss(String key) {
        return new CacheReadResult<>(key, null, 0L, false, false);
    }

    public String getKey() {
        return key;
    }

    public T getValue() {
        return value;
    }

    public long getUpdatedAtMillis() {
        return updatedAtMillis;
    }

    public boolean isHit() {
        return hit;
    }

    public boolean isFresh() {
        return fresh;
    }
}
