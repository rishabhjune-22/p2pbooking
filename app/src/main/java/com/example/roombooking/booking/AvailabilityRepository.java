package com.example.roombooking.booking;

import android.content.Context;

import com.example.roombooking.api.ApiService;
import com.example.roombooking.api.RetrofitClient;
import com.example.roombooking.model.booking.BookingActionData;
import com.example.roombooking.model.common.ApiResponse;

import retrofit2.Call;

public class AvailabilityRepository {

    private final ApiService apiService;

    public AvailabilityRepository(Context context) {
        apiService = RetrofitClient.getApiService(context.getApplicationContext());
    }

    AvailabilityRepository(ApiService apiService) {
        this.apiService = apiService;
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
}
