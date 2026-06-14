package com.example.roombooking.api;

import com.example.roombooking.booking.AvailableRoomsRangeResponse;
import com.example.roombooking.booking.AvailableRoomsResponse;
import com.example.roombooking.booking.BookingCancelRequest;
import com.example.roombooking.booking.BookingCreateRequest;
import com.example.roombooking.booking.BookingUpdateRequest;
import com.example.roombooking.booking.RoomAvailabilityDetailsResponse;
import com.example.roombooking.booking.RoomAvailabilityResponse;
import com.example.roombooking.model.booking.BookingActionData;
import com.example.roombooking.model.booking.BookingItem;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.model.common.PaginatedData;
import com.example.roombooking.model.room.RoomItem;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @GET("api/bookings/")
    Call<ApiResponse<PaginatedData<BookingItem>>> getBookings(
            @Query("page") int page,
            @Query("prefix") String prefix,
            @Query("arrival_from") String arrivalFrom,
            @Query("departure_to") String departureTo,
            @Query("status") String status
    );

    @GET("api/bookings/{pk}/")
    Call<ApiResponse<BookingItem>> getBooking(
            @Path("pk") int bookingId
    );

    @POST("api/bookings/create/")
    Call<ApiResponse<BookingActionData>> createBooking(
            @Body BookingCreateRequest request
    );

    @PATCH("api/bookings/{pk}/edit/")
    Call<ApiResponse<BookingActionData>> updateBooking(
            @Path("pk") int bookingId,
            @Body BookingUpdateRequest request
    );

    @POST("api/bookings/{pk}/cancel/")
    Call<ApiResponse<BookingActionData>> cancelBooking(
            @Path("pk") int bookingId,
            @Body BookingCancelRequest request
    );

    @GET("api/rooms/")
    Call<ApiResponse<PaginatedData<RoomItem>>> getRooms(
            @Query("page") int page
    );

    @GET("api/bookings/availability/")
    Call<ApiResponse<RoomAvailabilityResponse>> getRoomAvailability(
            @Query("month") int month,
            @Query("year") int year
    );

    @GET("api/bookings/availability/details/")
    Call<ApiResponse<RoomAvailabilityDetailsResponse>> getRoomAvailabilityDetails(
            @Query("date") String date,
            @Query("prefix") String prefix
    );

    @GET("api/room-available-rooms/")
    Call<ApiResponse<AvailableRoomsResponse>> getAvailableRoomsByDate(
            @Query("date") String date,
            @Query("prefix") String prefix
    );

    @GET("api/room-available-rooms-range/")
    Call<ApiResponse<AvailableRoomsRangeResponse>> getAvailableRoomsByDateRange(
            @Query("arrival_date") String arrivalDate,
            @Query("departure_date") String departureDate,
            @Query("prefix") String prefix
    );
}
