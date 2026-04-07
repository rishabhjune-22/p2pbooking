package com.example.roombooking;

import com.example.roombooking.api.TokenRefreshApi;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TokenRefreshClient {

    private static final String BASE_URL = "http://10.50.26.74:8000/";
    private static Retrofit retrofit;

    public static TokenRefreshApi getApi() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(TokenRefreshApi.class);
    }
}