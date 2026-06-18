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
import com.example.roombooking.utils.NullSafeCollections;
import com.example.roombooking.room.local.AppDatabase;
import com.example.roombooking.room.local.RoomDao;
import com.example.roombooking.room.local.RoomEntity;
import com.example.roombooking.utils.ApiErrorUtils;
import com.example.roombooking.utils.AppDiagnostics;

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
    private static final int ROOM_PAGE_SIZE = 100;
    private static final long ROOM_REFRESH_INTERVAL_MS = 6L * 60L * 60L * 1000L;
    private static final long FAILED_REFRESH_RETRY_INTERVAL_MS = 5L * 60L * 1000L;

    private static final String MESSAGE_LOAD_FAILED = "Failed to load rooms.";
    private static final String MESSAGE_NETWORK_ERROR = "Please check your internet connection.";
    private static final Object REQUEST_LOCK = new Object();
    private static final List<RoomRepositoryCallback> pendingCallbacks = new ArrayList<>();
    private static Call<ApiResponse<PaginatedData<RoomItem>>> activeRoomsCall;
    private static boolean roomsRequestInFlight = false;
    private static long lastApiRefreshSucceededAtMillis = 0L;
    private static long lastApiRefreshAttemptedAtMillis = 0L;

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
            refreshRoomsSilentlyIfStale();
            return;
        }

        loadRoomsFromLocalDatabase(callback);
    }

    public void forceRefresh(RoomRepositoryCallback callback) {
        fetchRoomsFromApi(callback);
    }

    public void clearCache() {
        RoomMemoryCache.clear();
        resetRefreshTimestamps();

        diskExecutor.execute(roomDao::clearRooms);
    }

    private void loadRoomsFromLocalDatabase(RoomRepositoryCallback callback) {
        diskExecutor.execute(() -> {
            List<RoomEntity> cachedEntities = roomDao.getAllRooms();
            List<RoomItem> cachedRooms = RoomMapper.toModelList(cachedEntities);

            if (!cachedRooms.isEmpty()) {
                RoomMemoryCache.setRooms(cachedRooms);
                sendSuccess(callback, cachedRooms, true);
                refreshRoomsSilentlyIfStale();
                return;
            }

            fetchRoomsFromApi(callback);
        });
    }

    private void refreshRoomsSilentlyIfStale() {
        if (!shouldRefreshRoomsSilently()) {
            return;
        }

        fetchRoomsFromApi(null);
    }

    private boolean shouldRefreshRoomsSilently() {
        long now = System.currentTimeMillis();

        synchronized (REQUEST_LOCK) {
            if (roomsRequestInFlight) {
                return false;
            }

            if (lastApiRefreshSucceededAtMillis > 0
                    && now - lastApiRefreshSucceededAtMillis < ROOM_REFRESH_INTERVAL_MS) {
                return false;
            }

            if (lastApiRefreshAttemptedAtMillis > 0
                    && now - lastApiRefreshAttemptedAtMillis < FAILED_REFRESH_RETRY_INTERVAL_MS) {
                return false;
            }

            return true;
        }
    }

    private void fetchRoomsFromApi(RoomRepositoryCallback callback) {
        synchronized (REQUEST_LOCK) {
            if (roomsRequestInFlight) {
                if (callback != null) {
                    pendingCallbacks.add(callback);
                }
                return;
            }

            roomsRequestInFlight = true;
            lastApiRefreshAttemptedAtMillis = System.currentTimeMillis();
            if (callback != null) {
                pendingCallbacks.add(callback);
            }
        }

        fetchRoomsPage(FIRST_PAGE, new ArrayList<>());
    }

    private void fetchRoomsPage(
            int page,
            List<RoomItem> accumulatedRooms
    ) {
        Call<ApiResponse<PaginatedData<RoomItem>>> request =
                apiService.getRooms(page, ROOM_PAGE_SIZE);
        synchronized (REQUEST_LOCK) {
            activeRoomsCall = request;
        }

        request.enqueue(new Callback<ApiResponse<PaginatedData<RoomItem>>>() {

            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<PaginatedData<RoomItem>>> call,
                    @NonNull Response<ApiResponse<PaginatedData<RoomItem>>> response
            ) {
                if (call.isCanceled() || !isCurrentRoomsCall(call)) {
                    return;
                }

                if (!isValidRoomsResponse(response)) {
                    if (response.code() == 404 && page > FIRST_PAGE) {
                        saveRoomsToCache(accumulatedRooms, finishRoomRequest());
                        return;
                    }

                    String message = ApiErrorUtils.messageFromResponse(
                            response,
                            MESSAGE_LOAD_FAILED
                    );
                    AppDiagnostics.logApiFailure("rooms", message, null);
                    finishRoomRequestWithError(message);
                    return;
                }

                PaginatedData<RoomItem> paginatedData = response.body().getData();

                List<RoomItem> pageResults = paginatedData.getResults();

                accumulatedRooms.addAll(NullSafeCollections.copyWithoutNulls(pageResults));

                if (paginatedData.hasNextPage()) {
                    fetchRoomsPage(page + 1, accumulatedRooms);
                    return;
                }

                saveRoomsToCache(accumulatedRooms, finishRoomRequest());
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<PaginatedData<RoomItem>>> call,
                    @NonNull Throwable t
            ) {
                if (call.isCanceled() || !isCurrentRoomsCall(call)) {
                    return;
                }

                AppDiagnostics.logApiFailure("rooms", MESSAGE_NETWORK_ERROR, t);
                finishRoomRequestWithError(ApiErrorUtils.networkMessage());
            }
        });
    }

    private boolean isCurrentRoomsCall(Call<ApiResponse<PaginatedData<RoomItem>>> call) {
        synchronized (REQUEST_LOCK) {
            return call == activeRoomsCall;
        }
    }

    private List<RoomRepositoryCallback> finishRoomRequest() {
        synchronized (REQUEST_LOCK) {
            List<RoomRepositoryCallback> callbacks = new ArrayList<>(pendingCallbacks);
            pendingCallbacks.clear();
            activeRoomsCall = null;
            roomsRequestInFlight = false;
            return callbacks;
        }
    }

    private void finishRoomRequestWithError(String errorMessage) {
        List<RoomRepositoryCallback> callbacks = finishRoomRequest();
        sendErrorIfNeeded(callbacks, errorMessage);
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
            List<RoomRepositoryCallback> callbacks
    ) {
        List<RoomItem> finalRooms = NullSafeCollections.copyWithoutNulls(apiRooms);
        markRoomsApiRefreshSucceeded();

        diskExecutor.execute(() -> {
            roomDao.clearRooms();
            roomDao.insertRooms(RoomMapper.toEntityList(finalRooms));

            RoomMemoryCache.setRooms(finalRooms);

            sendSuccess(callbacks, finalRooms, false);
        });
    }

    private void markRoomsApiRefreshSucceeded() {
        synchronized (REQUEST_LOCK) {
            lastApiRefreshSucceededAtMillis = System.currentTimeMillis();
        }
    }

    private void resetRefreshTimestamps() {
        synchronized (REQUEST_LOCK) {
            lastApiRefreshSucceededAtMillis = 0L;
            lastApiRefreshAttemptedAtMillis = 0L;
        }
    }

    private void sendSuccess(
            List<RoomRepositoryCallback> callbacks,
            List<RoomItem> rooms,
            boolean fromCache
    ) {
        for (RoomRepositoryCallback callback : callbacks) {
            sendSuccess(callback, rooms, fromCache);
        }
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

    private void sendErrorIfNeeded(
            List<RoomRepositoryCallback> callbacks,
            String errorMessage
    ) {
        for (RoomRepositoryCallback callback : callbacks) {
            sendErrorIfNeeded(callback, errorMessage);
        }
    }
}
