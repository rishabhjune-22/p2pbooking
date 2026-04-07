package com.example.roombooking.api;

import com.example.roombooking.BookingCancelRequest;
import com.example.roombooking.BookingCancelResponse;
import com.example.roombooking.BookingCreateRequest;
import com.example.roombooking.BookingCreateResponse;
import com.example.roombooking.BookingListResponse;
import com.example.roombooking.BookingUpdateRequest;
import com.example.roombooking.BookingUpdateResponse;
import com.example.roombooking.LoginRequest;
import com.example.roombooking.LogoutRequest;
import com.example.roombooking.RefreshRequest;
import com.example.roombooking.RefreshResponse;
import com.example.roombooking.SignupRequest;
import com.example.roombooking.SignupResponse;
import com.example.roombooking.TokenResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    @POST("api/token/")
    Call<TokenResponse> login(@Body LoginRequest request);
    @POST("api/accounts/signup/")
    Call<SignupResponse> signup(@Body SignupRequest request);
    @POST("api/token/refresh/")
    Call<RefreshResponse> refreshToken(@Body RefreshRequest request);

    @GET("api/bookings/")
    Call<BookingListResponse> getBookings(@Query("page") int page);
    @POST("api/logout/")
    Call<Void> logout(@Body LogoutRequest request);

    @POST("api/bookings/{pk}/cancel/")
    Call<BookingCancelResponse> cancelBooking(
            @retrofit2.http.Path("pk") int bookingId,
            @Body BookingCancelRequest request
    );

    @POST("api/bookings/create/")
    Call<BookingCreateResponse> createBooking(@Body BookingCreateRequest request);

    @PATCH("api/bookings/{pk}/edit/")
    Call<BookingUpdateResponse> updateBooking(
            @retrofit2.http.Path("pk") int bookingId,
            @Body BookingUpdateRequest request
    );
}
