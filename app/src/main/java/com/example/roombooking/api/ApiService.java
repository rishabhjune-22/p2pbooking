package com.example.roombooking.api;

import com.example.roombooking.auth.SignupRequest;
import com.example.roombooking.model.auth.LoginData;
import com.example.roombooking.auth.LoginRequest;
import com.example.roombooking.auth.LogoutRequest;
import com.example.roombooking.auth.RefreshRequest;
import com.example.roombooking.model.auth.RefreshTokenData;
import com.example.roombooking.model.auth.SignupData;
import com.example.roombooking.model.booking.BookingActionData;
import com.example.roombooking.booking.BookingCancelRequest;
import com.example.roombooking.booking.BookingCreateRequest;
import com.example.roombooking.model.booking.BookingItem;
import com.example.roombooking.booking.BookingUpdateRequest;
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

    @POST("api/token/")
    Call<ApiResponse<LoginData>> login(@Body LoginRequest request);

    @POST("api/accounts/signup/")
    Call<ApiResponse<SignupData>> signup(@Body SignupRequest request);

    @POST("api/token/refresh/")
    Call<ApiResponse<RefreshTokenData>> refreshToken(@Body RefreshRequest request);

    @GET("api/bookings/")
    Call<ApiResponse<PaginatedData<BookingItem>>> getBookings(@Query("page") int page);

    @POST("api/accounts/logout/")
    Call<ApiResponse<Object>> logout(@Body LogoutRequest request);

    @POST("api/bookings/{pk}/cancel/")
    Call<ApiResponse<BookingActionData>> cancelBooking(
            @Path("pk") int bookingId,
            @Body BookingCancelRequest request
    );

    @POST("api/bookings/create/")
    Call<ApiResponse<BookingActionData>> createBooking(@Body BookingCreateRequest request);

    @PATCH("api/bookings/{pk}/edit/")
    Call<ApiResponse<BookingActionData>> updateBooking(
            @Path("pk") int bookingId,
            @Body BookingUpdateRequest request
    );


    @GET("api/rooms/")
    Call<ApiResponse<PaginatedData<RoomItem>>> getRooms(@Query("page") int page);

    @retrofit2.http.GET("api/accounts/encryption-material/")
    retrofit2.Call<com.example.roombooking.model.common.ApiResponse<com.example.roombooking.security.EncryptionMaterialData>> getEncryptionMaterial();
}