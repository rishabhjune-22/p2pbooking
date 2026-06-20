package com.example.roombooking.booking;

import android.content.Context;

import com.example.roombooking.api.ApiService;
import com.example.roombooking.api.RetrofitClient;
import com.example.roombooking.cache.CachePolicy;
import com.example.roombooking.cache.CacheReadResult;
import com.example.roombooking.cache.LocalJsonCacheStore;
import com.example.roombooking.model.booking.BookingActionData;
import com.example.roombooking.model.booking.BookingItem;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.model.common.PaginatedData;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import retrofit2.Call;

public class BookingRepository {

    private static final String BOOKING_PAGE_ONE_CACHE_PREFIX = "bookings:";
    private static final String PAGE_ONE_SUFFIX = ":page1";
    private static final Type BOOKING_LIST_TYPE =
            new TypeToken<List<BookingItem>>() {}.getType();

    private final ApiService apiService;
    private final LocalJsonCacheStore cacheStore;

    public BookingRepository(Context context) {
        this.apiService = RetrofitClient.getApiService(context.getApplicationContext());
        this.cacheStore = new LocalJsonCacheStore(context.getApplicationContext());
    }

    BookingRepository(ApiService apiService) {
        this(apiService, null);
    }

    BookingRepository(ApiService apiService, LocalJsonCacheStore cacheStore) {
        this.apiService = apiService;
        this.cacheStore = cacheStore;
    }

    public Call<ApiResponse<PaginatedData<BookingItem>>> getBookings(
            int page,
            String prefix,
            String arrivalFrom,
            String departureTo,
            String status
    ) {
        return apiService.getBookings(
                page,
                prefix,
                arrivalFrom,
                departureTo,
                status
        );
    }

    public Call<ApiResponse<BookingActionData>> createBooking(BookingCreateRequest request) {
        return apiService.createBooking(request);
    }

    public Call<ApiResponse<BookingItem>> getBooking(int bookingId) {
        return apiService.getBooking(bookingId);
    }

    public Call<ApiResponse<BookingActionData>> updateBooking(
            int bookingId,
            BookingUpdateRequest request
    ) {
        return apiService.updateBooking(bookingId, request);
    }

    public Call<ApiResponse<BookingActionData>> deleteBooking(int bookingId) {
        return apiService.deleteBooking(bookingId);
    }

    public void getCachedFirstPage(
            String cacheKey,
            LocalJsonCacheStore.CacheCallback<List<BookingItem>> callback
    ) {
        if (cacheStore == null) {
            callback.onResult(CacheReadResult.miss(cacheKey));
            return;
        }

        cacheStore.read(
                cacheKey,
                BOOKING_LIST_TYPE,
                CachePolicy.BOOKING_PAGE_ONE_TTL_MS,
                callback
        );
    }

    public void saveCachedFirstPage(String cacheKey, List<BookingItem> bookings) {
        if (cacheStore == null) {
            return;
        }

        cacheStore.write(cacheKey, bookings);
    }

    public void clearFirstPageCaches() {
        clearFirstPageCaches(cacheStore);
    }

    public static void clearFirstPageCaches(LocalJsonCacheStore cacheStore) {
        if (cacheStore == null) {
            return;
        }

        cacheStore.deleteByPrefix(BOOKING_PAGE_ONE_CACHE_PREFIX);
    }

    public void clearAvailabilityCachesForBookingMutation() {
        AvailabilityRepository.clearAvailabilityCaches(cacheStore);
    }

    public static String firstPageCacheKey(
            String prefix,
            String arrivalFrom,
            String departureTo,
            String status
    ) {
        return BOOKING_PAGE_ONE_CACHE_PREFIX
                + safe(prefix)
                + ":"
                + safe(arrivalFrom)
                + ":"
                + safe(departureTo)
                + ":"
                + safe(status)
                + PAGE_ONE_SUFFIX;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
