package com.example.roombooking.api;

import com.example.roombooking.auth.RefreshRequest;
import com.example.roombooking.auth.RefreshResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface TokenRefreshApi {

    @POST("api/token/refresh/")
    Call<RefreshResponse> refreshToken(@Body RefreshRequest request);
}