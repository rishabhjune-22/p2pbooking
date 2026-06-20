package com.example.roombooking.auth;

import com.example.roombooking.model.common.ApiResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface AuthApiService {

    @POST("api/auth/signup/")
    Call<ApiResponse<AuthResponse>> signup(@Body SignupRequest request);

    @POST("api/auth/login/")
    Call<ApiResponse<AuthResponse>> login(@Body LoginRequest request);

    @POST("api/auth/token/refresh/")
    Call<RefreshTokenResponse> refreshToken(@Body RefreshTokenRequest request);

    @GET("api/auth/me/")
    Call<ApiResponse<AuthUser>> me();

    @POST("api/auth/logout/")
    Call<ApiResponse<Void>> logout(@Body LogoutRequest request);
}
