package com.example.roombooking.room;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoomRepository {

    public interface RoomRepositoryCallback {
        void onResult(RoomResult result);
    }

    private static final int FIRST_PAGE = 1;

    private static final String MESSAGE_LOAD_FAILED = "Failed to load rooms.";
    private static final String MESSAGE_NETWORK_ERROR = "Please check your internet connection.";

    private final ApiService apiService;
    private final RoomDao roomDao;
    private final ExecutorService diskExecutor;
    private final Handler mainHandler;

    public RoomRepository(Context context) {
        Context appContext = context.getApplicationContext();

        apiService = RetrofitClient.getApiService(appContext);
        roomDao = AppDatabase.getInstance(appContext).roomDao();
        diskExecutor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public void getRooms(RoomRepositoryCallback callback) {
        if (RoomMemoryCache.hasRooms()) {
            sendSuccess(callback, RoomMemoryCache.getRooms(), true);
            refreshRoomsSilently();
            return;
        }

        loadRoomsFromLocalDatabase(callback);
    }

    public void forceRefresh(RoomRepositoryCallback callback) {
        fetchRoomsFromApi(callback);
    }

    public void clearCache() {
        RoomMemoryCache.clear();

        diskExecutor.execute(roomDao::clearRooms);
    }

    private void loadRoomsFromLocalDatabase(RoomRepositoryCallback callback) {
        diskExecutor.execute(() -> {
            List<RoomEntity> cachedEntities = roomDao.getAllRooms();
            List<RoomItem> cachedRooms = RoomMapper.toModelList(cachedEntities);

            if (!cachedRooms.isEmpty()) {
                RoomMemoryCache.setRooms(cachedRooms);
                sendSuccess(callback, cachedRooms, true);
                refreshRoomsSilently();
                return;
            }

            fetchRoomsFromApi(callback);
        });
    }

    private void refreshRoomsSilently() {
        fetchRoomsFromApi(null);
    }

    private void fetchRoomsFromApi(RoomRepositoryCallback callback) {
        fetchRoomsPage(FIRST_PAGE, new ArrayList<>(), callback);
    }

    private void fetchRoomsPage(
            int page,
            List<RoomItem> accumulatedRooms,
            RoomRepositoryCallback callback
    ) {
        apiService.getRooms(page).enqueue(new Callback<ApiResponse<PaginatedData<RoomItem>>>() {

            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<PaginatedData<RoomItem>>> call,
                    @NonNull Response<ApiResponse<PaginatedData<RoomItem>>> response
            ) {
                if (!isValidRoomsResponse(response)) {
                    sendErrorIfNeeded(callback, MESSAGE_LOAD_FAILED);
                    return;
                }

                PaginatedData<RoomItem> paginatedData = response.body().getData();

                List<RoomItem> pageResults = paginatedData.getResults();

                if (pageResults != null && !pageResults.isEmpty()) {
                    accumulatedRooms.addAll(pageResults);
                }

                if (paginatedData.hasNextPage()) {
                    fetchRoomsPage(page + 1, accumulatedRooms, callback);
                    return;
                }

                saveRoomsToCache(accumulatedRooms, callback);
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<PaginatedData<RoomItem>>> call,
                    @NonNull Throwable t
            ) {
                sendErrorIfNeeded(callback, MESSAGE_NETWORK_ERROR);
            }
        });
    }

    private boolean isValidRoomsResponse(
            Response<ApiResponse<PaginatedData<RoomItem>>> response
    ) {
        return response.isSuccessful()
                && response.body() != null
                && response.body().isSuccess()
                && response.body().getData() != null;
    }

    private void saveRoomsToCache(
            List<RoomItem> apiRooms,
            RoomRepositoryCallback callback
    ) {
        List<RoomItem> finalRooms = apiRooms != null
                ? new ArrayList<>(apiRooms)
                : new ArrayList<>();

        diskExecutor.execute(() -> {
            roomDao.clearRooms();
            roomDao.insertRooms(RoomMapper.toEntityList(finalRooms));

            RoomMemoryCache.setRooms(finalRooms);

            sendSuccess(callback, finalRooms, false);
        });
    }

    private void sendSuccess(
            RoomRepositoryCallback callback,
            List<RoomItem> rooms,
            boolean fromCache
    ) {
        if (callback == null) return;

        mainHandler.post(() ->
                callback.onResult(RoomResult.success(rooms, fromCache))
        );
    }

    private void sendErrorIfNeeded(
            RoomRepositoryCallback callback,
            String errorMessage
    ) {
        if (callback == null) return;

        mainHandler.post(() ->
                callback.onResult(RoomResult.error(errorMessage))
        );
    }
}