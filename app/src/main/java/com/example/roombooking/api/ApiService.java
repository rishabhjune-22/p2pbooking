package com.example.roombooking.api;

import com.example.roombooking.booking.AvailableRoomsRangeResponse;
import com.example.roombooking.booking.AvailableRoomsResponse;
import com.example.roombooking.booking.BookingCreateRequest;
import com.example.roombooking.booking.BookingUpdateRequest;
import com.example.roombooking.booking.RoomAvailabilityDetailsResponse;
import com.example.roombooking.booking.RoomAvailabilityResponse;
import com.example.roombooking.admin.BookingRequestDecisionRequest;
import com.example.roombooking.admin.AccountApprovalDecisionRequest;
import com.example.roombooking.admin.AccountRequestItem;
import com.example.roombooking.model.booking.BookingActionData;
import com.example.roombooking.model.booking.BookingItem;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.model.common.PaginatedData;
import com.example.roombooking.model.room.RoomItem;
import com.example.roombooking.requester.BookingRequestCreateRequest;
import com.example.roombooking.requester.BookingRequestItem;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
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

    @DELETE("api/bookings/{pk}/delete/")
    Call<ApiResponse<BookingActionData>> deleteBooking(
            @Path("pk") int bookingId
    );

    @GET("api/rooms/")
    Call<ApiResponse<PaginatedData<RoomItem>>> getRooms(
            @Query("page") int page,
            @Query("page_size") int pageSize
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

    @GET("api/requester/availability/")
    Call<ApiResponse<RoomAvailabilityResponse>> getRequesterAvailability(
            @Query("month") int month,
            @Query("year") int year,
            @Query("prefix") String prefix
    );

    @GET("api/requester/available-rooms-range/")
    Call<ApiResponse<AvailableRoomsRangeResponse>> getRequesterAvailableRoomsByDateRange(
            @Query("arrival_date") String arrivalDate,
            @Query("departure_date") String departureDate,
            @Query("prefix") String prefix
    );

    @POST("api/requester/booking-requests/")
    Call<ApiResponse<BookingRequestItem>> createRequesterBookingRequest(
            @Body BookingRequestCreateRequest request
    );

    @GET("api/requester/booking-requests/")
    Call<ApiResponse<List<BookingRequestItem>>> getRequesterBookingRequests(
            @Query("status") String status
    );

    @GET("api/requester/booking-requests/{pk}/")
    Call<ApiResponse<BookingRequestItem>> getRequesterBookingRequest(
            @Path("pk") int requestId
    );

    @PATCH("api/requester/booking-requests/{pk}/")
    Call<ApiResponse<BookingRequestItem>> updateRequesterBookingRequest(
            @Path("pk") int requestId,
            @Body BookingRequestCreateRequest request
    );

    @HTTP(method = "DELETE", path = "api/requester/booking-requests/{pk}/delete/", hasBody = true)
    Call<ApiResponse<BookingRequestItem>> deleteRequesterBookingRequest(
            @Path("pk") int requestId,
            @Body BookingRequestDecisionRequest request
    );

    @GET("api/admin/booking-requests/")
    Call<ApiResponse<List<BookingRequestItem>>> getAdminBookingRequests(
            @Query("status") String status
    );

    @GET("api/admin/booking-requests/{pk}/")
    Call<ApiResponse<BookingRequestItem>> getAdminBookingRequest(
            @Path("pk") int requestId
    );

    @POST("api/admin/booking-requests/{pk}/approve/")
    Call<ApiResponse<BookingRequestItem>> approveBookingRequest(
            @Path("pk") int requestId,
            @Body BookingRequestDecisionRequest request
    );

    @POST("api/admin/booking-requests/{pk}/approve/")
    Call<ApiResponse<BookingRequestItem>> approveBookingRequestFromForm(
            @Path("pk") int requestId,
            @Body Map<String, Object> request
    );

    @POST("api/admin/booking-requests/{pk}/reject/")
    Call<ApiResponse<BookingRequestItem>> rejectBookingRequest(
            @Path("pk") int requestId,
            @Body BookingRequestDecisionRequest request
    );

    @POST("api/admin/booking-requests/{pk}/send-back/")
    Call<ApiResponse<BookingRequestItem>> sendBackBookingRequest(
            @Path("pk") int requestId,
            @Body BookingRequestDecisionRequest request
    );

    @HTTP(method = "DELETE", path = "api/admin/booking-requests/{pk}/delete/", hasBody = true)
    Call<ApiResponse<BookingRequestItem>> deleteAdminBookingRequest(
            @Path("pk") int requestId,
            @Body BookingRequestDecisionRequest request
    );

    @GET("api/admin/requester-accounts/")
    Call<ApiResponse<List<AccountRequestItem>>> getRequesterAccountRequests(
            @Query("status") String status
    );

    @GET("api/admin/requester-accounts/{pk}/")
    Call<ApiResponse<AccountRequestItem>> getRequesterAccountRequest(
            @Path("pk") int requestId
    );

    @POST("api/admin/requester-accounts/{pk}/approve/")
    Call<ApiResponse<AccountRequestItem>> approveRequesterAccount(
            @Path("pk") int requestId,
            @Body AccountApprovalDecisionRequest request
    );

    @POST("api/admin/requester-accounts/{pk}/reject/")
    Call<ApiResponse<AccountRequestItem>> rejectRequesterAccount(
            @Path("pk") int requestId,
            @Body AccountApprovalDecisionRequest request
    );

    @GET("api/superadmin/account-requests/")
    Call<ApiResponse<List<AccountRequestItem>>> getSuperadminAccountRequests(
            @Query("role") String role,
            @Query("status") String status
    );

    @GET("api/superadmin/account-requests/{pk}/")
    Call<ApiResponse<AccountRequestItem>> getSuperadminAccountRequest(
            @Path("pk") int requestId
    );

    @POST("api/superadmin/account-requests/{pk}/approve/")
    Call<ApiResponse<AccountRequestItem>> approveSuperadminAccountRequest(
            @Path("pk") int requestId,
            @Body AccountApprovalDecisionRequest request
    );

    @POST("api/superadmin/account-requests/{pk}/reject/")
    Call<ApiResponse<AccountRequestItem>> rejectSuperadminAccountRequest(
            @Path("pk") int requestId,
            @Body AccountApprovalDecisionRequest request
    );

    @DELETE("api/superadmin/account-requests/{pk}/delete/")
    Call<ApiResponse<Map<String, Object>>> deleteSuperadminAccountRequest(
            @Path("pk") int requestId
    );

}
