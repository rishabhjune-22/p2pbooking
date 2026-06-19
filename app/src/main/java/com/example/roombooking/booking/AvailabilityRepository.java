package com.example.roombooking.booking;

import android.content.Context;

import com.example.roombooking.api.ApiService;
import com.example.roombooking.api.RetrofitClient;
import com.example.roombooking.cache.CachePolicy;
import com.example.roombooking.cache.CacheReadResult;
import com.example.roombooking.cache.LocalJsonCacheStore;
import com.example.roombooking.model.booking.BookingActionData;
import com.example.roombooking.model.common.ApiResponse;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import retrofit2.Call;

public class AvailabilityRepository {

    private static final String AVAILABILITY_CACHE_PREFIX = "availability:";
    private static final String AVAILABLE_ROOMS_CACHE_PREFIX = "available_rooms:";
    private static final String AVAILABLE_ROOMS_RANGE_CACHE_PREFIX =
            "available_rooms_range:";
    private static final Object CACHE_INVALIDATION_LOCK = new Object();
    private static final Type AVAILABILITY_GROUP_LIST_TYPE =
            new TypeToken<List<RoomAvailabilityGroup>>() {}.getType();
    private static int cacheInvalidationVersion = 0;

    private final ApiService apiService;
    private final LocalJsonCacheStore cacheStore;

    public AvailabilityRepository(Context context) {
        apiService = RetrofitClient.getApiService(context.getApplicationContext());
        cacheStore = new LocalJsonCacheStore(context.getApplicationContext());
    }

    AvailabilityRepository(ApiService apiService) {
        this(apiService, null);
    }

    AvailabilityRepository(ApiService apiService, LocalJsonCacheStore cacheStore) {
        this.apiService = apiService;
        this.cacheStore = cacheStore;
    }

    public Call<ApiResponse<RoomAvailabilityResponse>> getRoomAvailability(
            int month,
            int year
    ) {
        return apiService.getRoomAvailability(month, year);
    }

    public Call<ApiResponse<RoomAvailabilityDetailsResponse>> getRoomAvailabilityDetails(
            String date,
            String prefix
    ) {
        return apiService.getRoomAvailabilityDetails(date, prefix);
    }

    public Call<ApiResponse<AvailableRoomsResponse>> getAvailableRoomsByDate(
            String date,
            String prefix
    ) {
        return apiService.getAvailableRoomsByDate(date, prefix);
    }

    public Call<ApiResponse<AvailableRoomsRangeResponse>> getAvailableRoomsByDateRange(
            String arrivalDate,
            String departureDate,
            String prefix
    ) {
        return apiService.getAvailableRoomsByDateRange(arrivalDate, departureDate, prefix);
    }

    public Call<ApiResponse<BookingActionData>> deleteBooking(int bookingId) {
        return apiService.deleteBooking(bookingId);
    }

    public void getCachedCalendarAvailability(
            int month,
            int year,
            LocalJsonCacheStore.CacheCallback<List<RoomAvailabilityGroup>> callback
    ) {
        String cacheKey = calendarAvailabilityCacheKey(month, year);
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

    public void saveCachedCalendarAvailability(
            int month,
            int year,
            List<RoomAvailabilityGroup> groups
    ) {
        if (cacheStore == null) {
            return;
        }

        cacheStore.write(calendarAvailabilityCacheKey(month, year), groups);
    }

    public void getCachedAvailableRoomsByDate(
            String date,
            String prefix,
            LocalJsonCacheStore.CacheCallback<AvailableRoomsResponse> callback
    ) {
        String cacheKey = availableRoomsCacheKey(prefix, date);
        if (cacheStore == null) {
            callback.onResult(CacheReadResult.miss(cacheKey));
            return;
        }

        cacheStore.read(
                cacheKey,
                AvailableRoomsResponse.class,
                CachePolicy.AVAILABLE_ROOMS_TTL_MS,
                callback
        );
    }

    public void saveCachedAvailableRoomsByDate(
            String date,
            String prefix,
            AvailableRoomsResponse response
    ) {
        if (cacheStore == null || response == null) {
            return;
        }

        cacheStore.write(availableRoomsCacheKey(prefix, date), response);
    }

    public void getCachedAvailableRoomsByDateRange(
            String arrivalDate,
            String departureDate,
            String prefix,
            LocalJsonCacheStore.CacheCallback<AvailableRoomsRangeResponse> callback
    ) {
        String cacheKey = availableRoomsRangeCacheKey(prefix, arrivalDate, departureDate);
        if (cacheStore == null) {
            callback.onResult(CacheReadResult.miss(cacheKey));
            return;
        }

        cacheStore.read(
                cacheKey,
                AvailableRoomsRangeResponse.class,
                CachePolicy.AVAILABLE_ROOMS_RANGE_TTL_MS,
                callback
        );
    }

    public void saveCachedAvailableRoomsByDateRange(
            String arrivalDate,
            String departureDate,
            String prefix,
            AvailableRoomsRangeResponse response
    ) {
        if (cacheStore == null || response == null) {
            return;
        }

        cacheStore.write(
                availableRoomsRangeCacheKey(prefix, arrivalDate, departureDate),
                response
        );
    }

    public void clearCalendarAvailabilityCache() {
        if (cacheStore == null) {
            return;
        }

        cacheStore.deleteByPrefix(AVAILABILITY_CACHE_PREFIX);
        incrementCacheInvalidationVersion();
    }

    public void clearAvailabilityCaches() {
        clearAvailabilityCaches(cacheStore);
    }

    public void clearBookingFirstPageCachesForMutation() {
        BookingRepository.clearFirstPageCaches(cacheStore);
    }

    static void clearAvailabilityCaches(LocalJsonCacheStore cacheStore) {
        if (cacheStore == null) {
            incrementCacheInvalidationVersion();
            return;
        }

        cacheStore.deleteByPrefix(AVAILABILITY_CACHE_PREFIX);
        cacheStore.deleteByPrefix(AVAILABLE_ROOMS_CACHE_PREFIX);
        cacheStore.deleteByPrefix(AVAILABLE_ROOMS_RANGE_CACHE_PREFIX);
        incrementCacheInvalidationVersion();
    }

    public static int getCacheInvalidationVersion() {
        synchronized (CACHE_INVALIDATION_LOCK) {
            return cacheInvalidationVersion;
        }
    }

    private static void incrementCacheInvalidationVersion() {
        synchronized (CACHE_INVALIDATION_LOCK) {
            cacheInvalidationVersion++;
        }
    }

    public static String calendarAvailabilityCacheKey(int month, int year) {
        return AVAILABILITY_CACHE_PREFIX + year + "-" + month;
    }

    public static String availableRoomsCacheKey(String prefix, String date) {
        return AVAILABLE_ROOMS_CACHE_PREFIX + safe(prefix) + ":" + safe(date);
    }

    public static String availableRoomsRangeCacheKey(
            String prefix,
            String arrivalDate,
            String departureDate
    ) {
        return AVAILABLE_ROOMS_RANGE_CACHE_PREFIX
                + safe(prefix)
                + ":"
                + safe(arrivalDate)
                + ":"
                + safe(departureDate);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
