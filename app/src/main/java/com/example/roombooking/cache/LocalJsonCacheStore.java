package com.example.roombooking.cache;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.roombooking.room.local.AppDatabase;
import com.example.roombooking.room.local.CacheEntryDao;
import com.example.roombooking.room.local.CacheEntryEntity;
import com.example.roombooking.utils.AppDiagnostics;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.lang.reflect.Type;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalJsonCacheStore {

    public interface CacheCallback<T> {
        void onResult(CacheReadResult<T> result);
    }

    private final CacheEntryDao cacheEntryDao;
    private final ExecutorService diskExecutor;
    private final Handler mainHandler;
    private final Gson gson;

    public LocalJsonCacheStore(Context context) {
        this(
                AppDatabase.getInstance(context.getApplicationContext()).cacheEntryDao(),
                Executors.newSingleThreadExecutor(),
                new Handler(Looper.getMainLooper()),
                new Gson()
        );
    }

    LocalJsonCacheStore(
            CacheEntryDao cacheEntryDao,
            ExecutorService diskExecutor,
            Handler mainHandler,
            Gson gson
    ) {
        this.cacheEntryDao = cacheEntryDao;
        this.diskExecutor = diskExecutor;
        this.mainHandler = mainHandler;
        this.gson = gson;
    }

    public <T> void read(
            String key,
            Type type,
            long ttlMillis,
            CacheCallback<T> callback
    ) {
        diskExecutor.execute(() -> {
            CacheEntryEntity entry = cacheEntryDao.getEntry(key);
            if (entry == null || isBlank(entry.getPayloadJson())) {
                AppDiagnostics.logCacheMiss(key);
                post(callback, CacheReadResult.miss(key));
                return;
            }

            try {
                T value = gson.fromJson(entry.getPayloadJson(), type);
                boolean fresh = CachePolicy.isFresh(
                        entry.getUpdatedAtMillis(),
                        ttlMillis,
                        System.currentTimeMillis()
                );
                AppDiagnostics.logCacheHit(
                        key,
                        fresh,
                        CachePolicy.ageMillis(entry.getUpdatedAtMillis(), System.currentTimeMillis())
                );
                post(callback, CacheReadResult.hit(
                        key,
                        value,
                        entry.getUpdatedAtMillis(),
                        fresh
                ));
            } catch (JsonSyntaxException exception) {
                AppDiagnostics.logCacheCorrupt(key, exception);
                cacheEntryDao.delete(key);
                post(callback, CacheReadResult.miss(key));
            }
        });
    }

    public <T> void write(String key, T value) {
        diskExecutor.execute(() -> {
            String payloadJson = gson.toJson(value);
            cacheEntryDao.upsert(new CacheEntryEntity(
                    key,
                    payloadJson != null ? payloadJson : "",
                    System.currentTimeMillis()
            ));
            AppDiagnostics.logCacheWrite(key);
        });
    }

    public void touch(String key) {
        diskExecutor.execute(() -> {
            cacheEntryDao.upsert(new CacheEntryEntity(
                    key,
                    "{}",
                    System.currentTimeMillis()
            ));
            AppDiagnostics.logCacheWrite(key);
        });
    }

    public void deleteByPrefix(String keyPrefix) {
        diskExecutor.execute(() -> {
            cacheEntryDao.deleteByPrefix(keyPrefix);
            AppDiagnostics.logCacheInvalidated(keyPrefix);
        });
    }

    private <T> void post(CacheCallback<T> callback, CacheReadResult<T> result) {
        if (callback == null) {
            return;
        }

        mainHandler.post(() -> callback.onResult(result));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
