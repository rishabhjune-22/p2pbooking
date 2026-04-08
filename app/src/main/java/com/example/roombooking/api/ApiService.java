package com.example.roombooking.api;
import com.example.roombooking.booking.BookingCancelRequest;
import com.example.roombooking.booking.BookingCancelResponse;
import com.example.roombooking.booking.BookingCreateRequest;
import com.example.roombooking.booking.BookingCreateResponse;
import com.example.roombooking.booking.BookingListResponse;
import com.example.roombooking.booking.BookingUpdateRequest;
import com.example.roombooking.booking.BookingUpdateResponse;
import com.example.roombooking.auth.LoginRequest;
import com.example.roombooking.auth.LogoutRequest;
import com.example.roombooking.auth.RefreshRequest;
import com.example.roombooking.auth.RefreshResponse;
import com.example.roombooking.auth.SignupRequest;
import com.example.roombooking.auth.SignupResponse;
import com.example.roombooking.auth.TokenResponse;
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
    @POST("/api/accounts/logout/")
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
