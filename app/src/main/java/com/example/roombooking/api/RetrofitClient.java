package com.example.roombooking.api;

import android.content.Context;

import com.example.roombooking.auth.AuthApiService;
import com.example.roombooking.auth.AuthInterceptor;
import com.example.roombooking.auth.TokenRefreshAuthenticator;

import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitClient {

    private static final long CONNECT_TIMEOUT_SECONDS = 10L;
    private static final long READ_TIMEOUT_SECONDS = 30L;
    private static final long WRITE_TIMEOUT_SECONDS = 30L;
    private static final long CALL_TIMEOUT_SECONDS = 45L;
    private static final long KEEP_ALIVE_MINUTES = 5L;
    private static final long PING_INTERVAL_SECONDS = 30L;
    private static final int MAX_IDLE_CONNECTIONS = 5;
    private static final int MAX_REQUESTS = 32;
    private static final int MAX_REQUESTS_PER_HOST = 8;

    private static volatile ApiService apiService;
    private static volatile AuthApiService authApiService;
    private static volatile AuthApiService refreshAuthApiService;
    private static volatile OkHttpClient okHttpClient;
    private static volatile OkHttpClient refreshOkHttpClient;

    private RetrofitClient() {
        // Utility class. No object required.
    }

    public static ApiService getApiService(Context context) {
        if (apiService == null) {
            synchronized (RetrofitClient.class) {
                if (apiService == null) {
                    apiService = createRetrofit(context.getApplicationContext())
                            .create(ApiService.class);
                }
            }
        }

        return apiService;
    }

    public static AuthApiService getAuthApiService(Context context) {
        if (authApiService == null) {
            synchronized (RetrofitClient.class) {
                if (authApiService == null) {
                    authApiService = createRetrofit(context.getApplicationContext())
                            .create(AuthApiService.class);
                }
            }
        }

        return authApiService;
    }

    public static AuthApiService getRefreshAuthApiService(Context context) {
        if (refreshAuthApiService == null) {
            synchronized (RetrofitClient.class) {
                if (refreshAuthApiService == null) {
                    refreshAuthApiService = createRefreshRetrofit()
                            .create(AuthApiService.class);
                }
            }
        }

        return refreshAuthApiService;
    }

    private static Retrofit createRetrofit(Context appContext) {
        return new Retrofit.Builder()
                .baseUrl(ApiConstants.BASE_URL)
                .client(getOkHttpClient(appContext))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    private static Retrofit createRefreshRetrofit() {
        return new Retrofit.Builder()
                .baseUrl(ApiConstants.BASE_URL)
                .client(getRefreshOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    private static OkHttpClient getOkHttpClient(Context appContext) {
        if (okHttpClient == null) {
            synchronized (RetrofitClient.class) {
                if (okHttpClient == null) {
                    okHttpClient = createOkHttpClient(appContext);
                }
            }
        }

        return okHttpClient;
    }

    private static OkHttpClient getRefreshOkHttpClient() {
        if (refreshOkHttpClient == null) {
            synchronized (RetrofitClient.class) {
                if (refreshOkHttpClient == null) {
                    refreshOkHttpClient = createBaseOkHttpBuilder().build();
                }
            }
        }

        return refreshOkHttpClient;
    }

    private static OkHttpClient createOkHttpClient(Context appContext) {
        return createBaseOkHttpBuilder()
                .addInterceptor(new AuthInterceptor(appContext))
                .authenticator(new TokenRefreshAuthenticator(appContext))
                .build();
    }

    private static OkHttpClient.Builder createBaseOkHttpBuilder() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(MAX_REQUESTS);
        dispatcher.setMaxRequestsPerHost(MAX_REQUESTS_PER_HOST);

        return new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .pingInterval(PING_INTERVAL_SECONDS, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(
                        MAX_IDLE_CONNECTIONS,
                        KEEP_ALIVE_MINUTES,
                        TimeUnit.MINUTES
                ))
                .retryOnConnectionFailure(true);
    }

    public static void reset() {
        synchronized (RetrofitClient.class) {
            apiService = null;
            authApiService = null;
            refreshAuthApiService = null;
            okHttpClient = null;
            refreshOkHttpClient = null;
        }
    }
}
