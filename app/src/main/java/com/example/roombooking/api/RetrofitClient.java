package com.example.roombooking.api;

import android.content.Context;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitClient {

    private static volatile ApiService apiService;

    private RetrofitClient() {
    }

    public static ApiService getApiService(Context context) {
        if (apiService == null) {
            synchronized (RetrofitClient.class) {
                if (apiService == null) {
                    apiService = createRetrofit(context.getApplicationContext()).create(ApiService.class);
                }
            }
        }
        return apiService;
    }

    private static Retrofit createRetrofit(Context context) {
        return new Retrofit.Builder()
                .baseUrl(ApiConstants.BASE_URL)
                .client(createOkHttpClient(context))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    private static OkHttpClient createOkHttpClient(Context context) {
        return new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(context))
                .authenticator(new TokenAuthenticator(context))
                .build();
    }

    public static void reset() {
        apiService = null;
    }
}