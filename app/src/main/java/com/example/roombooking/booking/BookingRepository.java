package com.example.roombooking.booking;

import android.content.Context;

import com.example.roombooking.api.ApiService;
import com.example.roombooking.api.RetrofitClient;
import com.example.roombooking.model.booking.BookingItem;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.model.common.PaginatedData;
import com.example.roombooking.model.booking.BookingActionData;
import retrofit2.Call;

public class BookingRepository {

    private final ApiService apiService;

    public BookingRepository(Context context) {
        this.apiService = RetrofitClient.getApiService(context.getApplicationContext());
    }

    public Call<ApiResponse<PaginatedData<BookingItem>>> getBookings(int page) {
        return apiService.getBookings(page);
    }

    public Call<ApiResponse<BookingActionData>> createBooking(BookingCreateRequest request) {
        return apiService.createBooking(request);
    }

    public Call<ApiResponse<BookingActionData>> updateBooking(int bookingId, BookingUpdateRequest request) {
        return apiService.updateBooking(bookingId, request);
    }

    public Call<ApiResponse<BookingActionData>> cancelBooking(int bookingId, BookingCancelRequest request) {
        return apiService.cancelBooking(bookingId, request);
    }
}