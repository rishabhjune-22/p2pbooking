package com.example.roombooking.booking;

import android.content.Context;

import com.example.roombooking.api.ApiService;
import com.example.roombooking.api.RetrofitClient;
import com.example.roombooking.model.booking.BookingActionData;
import com.example.roombooking.model.booking.BookingItem;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.model.common.PaginatedData;

import retrofit2.Call;

public class BookingRepository {

    private final ApiService apiService;

    public BookingRepository(Context context) {
        this.apiService = RetrofitClient.getApiService(context.getApplicationContext());
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

    public Call<ApiResponse<BookingActionData>> cancelBooking(
            int bookingId,
            BookingCancelRequest request
    ) {
        return apiService.cancelBooking(bookingId, request);
    }
}
