package com.example.roombooking.room.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cache_entries")
public class CacheEntryEntity {

    @PrimaryKey
    @NonNull
    private String cacheKey;

    @NonNull
    private String payloadJson;

    private long updatedAtMillis;

    public CacheEntryEntity(
            @NonNull String cacheKey,
            @NonNull String payloadJson,
            long updatedAtMillis
    ) {
        this.cacheKey = cacheKey;
        this.payloadJson = payloadJson;
        this.updatedAtMillis = updatedAtMillis;
    }

    @NonNull
    public String getCacheKey() {
        return cacheKey;
    }

    public void setCacheKey(@NonNull String cacheKey) {
        this.cacheKey = cacheKey;
    }

    @NonNull
    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(@NonNull String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public long getUpdatedAtMillis() {
        return updatedAtMillis;
    }

    public void setUpdatedAtMillis(long updatedAtMillis) {
        this.updatedAtMillis = updatedAtMillis;
    }
}
