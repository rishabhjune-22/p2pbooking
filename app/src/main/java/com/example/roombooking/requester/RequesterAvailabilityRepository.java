package com.example.roombooking.requester;

import android.content.Context;

import com.example.roombooking.api.ApiService;
import com.example.roombooking.api.RetrofitClient;
import com.example.roombooking.booking.AvailableRoomsRangeResponse;
import com.example.roombooking.booking.RoomAvailabilityGroup;
import com.example.roombooking.booking.RoomAvailabilityResponse;
import com.example.roombooking.cache.CachePolicy;
import com.example.roombooking.cache.CacheReadResult;
import com.example.roombooking.cache.LocalJsonCacheStore;
import com.example.roombooking.model.common.ApiResponse;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import retrofit2.Call;

public class RequesterAvailabilityRepository {

    private static final String REQUESTER_AVAILABILITY_CACHE_PREFIX =
            "requester:availability:";
    private static final Object CACHE_INVALIDATION_LOCK = new Object();
    private static final Type AVAILABILITY_GROUP_LIST_TYPE =
            new TypeToken<List<RoomAvailabilityGroup>>() {}.getType();
    private static int cacheInvalidationVersion = 0;

    private final ApiService apiService;
    private final LocalJsonCacheStore cacheStore;

    public RequesterAvailabilityRepository(Context context) {
        Context appContext = context.getApplicationContext();
        apiService = RetrofitClient.getApiService(appContext);
        cacheStore = new LocalJsonCacheStore(appContext);
    }

    RequesterAvailabilityRepository(ApiService apiService, LocalJsonCacheStore cacheStore) {
        this.apiService = apiService;
        this.cacheStore = cacheStore;
    }

    public Call<ApiResponse<RoomAvailabilityResponse>> getRequesterAvailability(
            int month,
            int year,
            String prefix
    ) {
        return apiService.getRequesterAvailability(month, year, safe(prefix));
    }

    public Call<ApiResponse<AvailableRoomsRangeResponse>> getRequesterAvailableRoomsByDateRange(
            String arrivalDate,
            String departureDate,
            String prefix
    ) {
        return apiService.getRequesterAvailableRoomsByDateRange(
                safe(arrivalDate),
                safe(departureDate),
                safe(prefix)
        );
    }

    public void getCachedRequesterAvailability(
            int userId,
            String prefix,
            int month,
            int year,
            LocalJsonCacheStore.CacheCallback<List<RoomAvailabilityGroup>> callback
    ) {
        String cacheKey = requesterAvailabilityCacheKey(userId, prefix, year, month);
        if (cacheStore == null) {
            callback.onResult(CacheReadResult.miss(cacheKey));
            return;
        }

        cacheStore.read(
                cacheKey,
                AVAILABILITY_GROUP_LIST_TYPE,
                CachePolicy.CALENDAR_AVAILABILITY_TTL_MS,
                callback
        );
    }

    public void saveCachedRequesterAvailability(
            int userId,
            String prefix,
            int month,
            int year,
            List<RoomAvailabilityGroup> groups
    ) {
        if (cacheStore == null) {
            return;
        }

        cacheStore.write(
                requesterAvailabilityCacheKey(userId, prefix, year, month),
                groups
        );
    }

    public static void clearRequesterAvailabilityCaches(LocalJsonCacheStore cacheStore) {
        if (cacheStore != null) {
            cacheStore.deleteByPrefix(REQUESTER_AVAILABILITY_CACHE_PREFIX);
        }
        incrementCacheInvalidationVersion();
    }

    public static int getCacheInvalidationVersion() {
        synchronized (CACHE_INVALIDATION_LOCK) {
            return cacheInvalidationVersion;
        }
    }

    public static String requesterAvailabilityCacheKey(
            int userId,
            String prefix,
            int year,
            int month
    ) {
        return REQUESTER_AVAILABILITY_CACHE_PREFIX
                + userId
                + ":"
                + safe(prefix)
                + ":"
                + year
                + ":"
                + month;
    }

    private static void incrementCacheInvalidationVersion() {
        synchronized (CACHE_INVALIDATION_LOCK) {
            cacheInvalidationVersion++;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
