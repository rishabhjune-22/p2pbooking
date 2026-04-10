package com.example.roombooking.room;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.roombooking.api.ApiService;
import com.example.roombooking.api.RetrofitClient;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.model.common.PaginatedData;
import com.example.roombooking.model.room.RoomItem;
import com.example.roombooking.room.local.AppDatabase;
import com.example.roombooking.room.local.RoomDao;
import com.example.roombooking.room.local.RoomEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoomRepository {

    public interface RoomRepositoryCallback {
        void onResult(RoomResult result);
    }

    private final ApiService apiService;
    private final RoomDao roomDao;
    private final Executor diskExecutor;
    private final Handler mainHandler;

    public RoomRepository(Context context) {
        Context appContext = context.getApplicationContext();
        this.apiService = RetrofitClient.getApiService(appContext);
        this.roomDao = AppDatabase.getInstance(appContext).roomDao();
        this.diskExecutor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void getRooms(RoomRepositoryCallback callback) {
        if (RoomMemoryCache.hasRooms()) {
            callback.onResult(new RoomResult(RoomMemoryCache.getRooms(), null, true));
            refreshRoomsInBackground(null);
            return;
        }

        diskExecutor.execute(() -> {
            List<RoomEntity> cachedEntities = roomDao.getAllRooms();
            List<RoomItem> cachedRooms = RoomMapper.toModelList(cachedEntities);

            if (!cachedRooms.isEmpty()) {
                RoomMemoryCache.setRooms(cachedRooms);

                mainHandler.post(() ->
                        callback.onResult(new RoomResult(cachedRooms, null, true))
                );

                refreshRoomsInBackground(null);
            } else {
                refreshRoomsInBackground(callback);
            }
        });
    }

    public void forceRefresh(RoomRepositoryCallback callback) {
        refreshRoomsInBackground(callback);
    }

    private void refreshRoomsInBackground(RoomRepositoryCallback callback) {
        fetchAllPages(1, new ArrayList<>(), callback);
    }

    private void fetchAllPages(int page, List<RoomItem> accumulator, RoomRepositoryCallback callback) {
        apiService.getRooms(page).enqueue(new Callback<ApiResponse<PaginatedData<RoomItem>>>() {
            @Override
            public void onResponse(
                    Call<ApiResponse<PaginatedData<RoomItem>>> call,
                    Response<ApiResponse<PaginatedData<RoomItem>>> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<PaginatedData<RoomItem>> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        PaginatedData<RoomItem> data = apiResponse.getData();

                        if (data.getResults() != null) {
                            accumulator.addAll(data.getResults());
                        }

                        if (data.getNext() != null) {
                            fetchAllPages(page + 1, accumulator, callback);
                            return;
                        }

                        saveApiRooms(accumulator, callback);
                        return;
                    }
                }

                if (callback != null) {
                    String errorMessage = "Failed to load rooms.";
                    mainHandler.post(() ->
                            callback.onResult(new RoomResult(null, errorMessage, false))
                    );
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<PaginatedData<RoomItem>>> call, Throwable t) {
                if (callback != null) {
                    mainHandler.post(() ->
                            callback.onResult(new RoomResult(null, "Please check your internet connection.", false))
                    );
                }
            }
        });
    }

    private void saveApiRooms(List<RoomItem> apiRooms, RoomRepositoryCallback callback) {
        diskExecutor.execute(() -> {
            roomDao.clearRooms();
            roomDao.insertRooms(RoomMapper.toEntityList(apiRooms));

            RoomMemoryCache.setRooms(apiRooms);

            if (callback != null) {
                mainHandler.post(() ->
                        callback.onResult(new RoomResult(apiRooms, null, false))
                );
            }
        });
    }

    public void clearCache() {
        RoomMemoryCache.clear();

        diskExecutor.execute(roomDao::clearRooms);
    }
}