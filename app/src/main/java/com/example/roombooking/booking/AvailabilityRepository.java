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
    private static final Type AVAILABILITY_GROUP_LIST_TYPE =
            new TypeToken<List<RoomAvailabilityGroup>>() {}.getType();

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

    public void clearCalendarAvailabilityCache() {
        if (cacheStore == null) {
            return;
        }

        cacheStore.deleteByPrefix(AVAILABILITY_CACHE_PREFIX);
    }

    public static String calendarAvailabilityCacheKey(int month, int year) {
        return AVAILABILITY_CACHE_PREFIX + year + "-" + month;
    }
}
