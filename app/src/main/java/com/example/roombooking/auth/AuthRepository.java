package com.example.roombooking.auth;

import android.content.Context;

import com.example.roombooking.api.ApiService;
import com.example.roombooking.api.RetrofitClient;
import com.example.roombooking.model.auth.LoginData;
import com.example.roombooking.model.auth.RefreshTokenData;
import com.example.roombooking.model.auth.SignupData;
import com.example.roombooking.model.common.ApiResponse;

import retrofit2.Call;

public class AuthRepository {

    private final ApiService apiService;

    public AuthRepository(Context context) {
        this.apiService = RetrofitClient.getApiService(context);
    }

    public Call<ApiResponse<LoginData>> login(LoginRequest request) {
        return apiService.login(request);
    }

    public Call<ApiResponse<SignupData>> signup(SignupRequest request) {
        return apiService.signup(request);
    }

    public Call<ApiResponse<RefreshTokenData>> refresh(RefreshRequest request) {
        return apiService.refreshToken(request);
    }

    public Call<ApiResponse<Object>> logout(LogoutRequest request) {
        return apiService.logout(request);
    }
}