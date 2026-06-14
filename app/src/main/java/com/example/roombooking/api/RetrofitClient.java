package com.example.roombooking.api;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitClient {

    private static final long CONNECT_TIMEOUT_SECONDS = 15L;
    private static final long READ_TIMEOUT_SECONDS = 30L;
    private static final long WRITE_TIMEOUT_SECONDS = 30L;

    private static volatile ApiService apiService;
    private static volatile OkHttpClient okHttpClient;

    private RetrofitClient() {
        // Utility class. No object required.
    }

    public static ApiService getApiService(Context context) {
        if (apiService == null) {
            synchronized (RetrofitClient.class) {
                if (apiService == null) {
                    apiService = createRetrofit().create(ApiService.class);
                }
            }
        }

        return apiService;
    }

    private static Retrofit createRetrofit() {
        return new Retrofit.Builder()
                .baseUrl(ApiConstants.BASE_URL)
                .client(getOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    private static OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            synchronized (RetrofitClient.class) {
                if (okHttpClient == null) {
                    okHttpClient = createOkHttpClient();
                }
            }
        }

        return okHttpClient;
    }

    private static OkHttpClient createOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public static void reset() {
        synchronized (RetrofitClient.class) {
            apiService = null;
            okHttpClient = null;
        }
    }
}