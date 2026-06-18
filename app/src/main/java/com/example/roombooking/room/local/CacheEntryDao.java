package com.example.roombooking.room.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CacheEntryDao {

    @Query("SELECT * FROM cache_entries WHERE cacheKey = :cacheKey LIMIT 1")
    CacheEntryEntity getEntry(String cacheKey);

    @Query("SELECT * FROM cache_entries WHERE cacheKey LIKE :keyPrefix || '%'")
    List<CacheEntryEntity> getEntriesByPrefix(String keyPrefix);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(CacheEntryEntity entry);

    @Query("DELETE FROM cache_entries WHERE cacheKey = :cacheKey")
    void delete(String cacheKey);

    @Query("DELETE FROM cache_entries WHERE cacheKey LIKE :keyPrefix || '%'")
    void deleteByPrefix(String keyPrefix);
}
