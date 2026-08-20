package com.example.roombooking.requester;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roombooking.booking.AvailableRoomsRangeResponse;
import com.example.roombooking.booking.RoomAvailabilityGroup;
import com.example.roombooking.booking.RoomAvailabilityResponse;
import com.example.roombooking.cache.CachePolicy;
import com.example.roombooking.cache.CacheReadResult;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.utils.ApiErrorUtils;
import com.example.roombooking.utils.AppDiagnostics;
import com.example.roombooking.utils.NullSafeCollections;
import com.example.roombooking.utils.SyncStatusFormatter;
import com.example.roombooking.utils.UiEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RequesterLandingViewModel extends ViewModel {

    private static final String MESSAGE_AVAILABILITY_FAILED =
            "Failed to load requester availability.";
    private static final Object CACHE_LOCK = new Object();
    private static final Map<String, CachedAvailability> availabilityCache =
            new HashMap<>();

    private final RequesterAvailabilityRepository availabilityRepository;
    private final MutableLiveData<Boolean> availabilityLoadingLiveData =
            new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> availableRoomsLoadingLiveData =
            new MutableLiveData<>(false);
    private final MutableLiveData<List<RoomAvailabilityGroup>> availabilityGroupsLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<String> availabilityStatusLiveData =
            new MutableLiveData<>("");
    private final MutableLiveData<UiEvent<String>> toastLiveData = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<Boolean>> networkBannerLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<UiEvent<AvailableRoomsRangeResponse>>
            availableRoomsRangeLiveData = new MutableLiveData<>();

    private Call<ApiResponse<RoomAvailabilityResponse>> availabilityCall;
    private Call<ApiResponse<AvailableRoomsRangeResponse>> availableRoomsRangeCall;
    private int cacheGeneration = 0;
    private int observedCacheInvalidationVersion =
            RequesterAvailabilityRepository.getCacheInvalidationVersion();
    private long currentAvailabilityUpdatedAtMillis = 0L;
    private boolean hasLoadedAvailability = false;
    private boolean availabilityStatusShowsFailure = false;
    private int requestedUserId = 0;
    private int requestedMonth = -1;
    private int requestedYear = -1;
    private String requestedPrefix = "";
    private String visibleCacheKey = "";

    public RequesterLandingViewModel(
            RequesterAvailabilityRepository availabilityRepository
    ) {
        this.availabilityRepository = availabilityRepository;
    }

    private static final class CachedAvailability {
        private final List<RoomAvailabilityGroup> groups;
        private final long updatedAtMillis;

        private CachedAvailability(
                List<RoomAvailabilityGroup> groups,
                long updatedAtMillis
        ) {
            this.groups = NullSafeCollections.copyWithoutNulls(groups);
            this.updatedAtMillis = updatedAtMillis;
        }
    }

    public LiveData<Boolean> getAvailabilityLoadingLiveData() {
        return availabilityLoadingLiveData;
    }

    public LiveData<Boolean> getAvailableRoomsLoadingLiveData() {
        return availableRoomsLoadingLiveData;
    }

    public LiveData<List<RoomAvailabilityGroup>> getAvailabilityGroupsLiveData() {
        return availabilityGroupsLiveData;
    }

    public LiveData<String> getAvailabilityStatusLiveData() {
        return availabilityStatusLiveData;
    }

    public LiveData<UiEvent<String>> getToastLiveData() {
        return toastLiveData;
    }

    public LiveData<UiEvent<Boolean>> getNetworkBannerLiveData() {
        return networkBannerLiveData;
    }

    public LiveData<UiEvent<AvailableRoomsRangeResponse>> getAvailableRoomsRangeLiveData() {
        return availableRoomsRangeLiveData;
    }

    public void loadAvailability(int userId, String prefix, int month, int year) {
        long actionStartedAtMillis = System.currentTimeMillis();
        if (syncCacheInvalidationVersion()) {
            clearVisibleAvailability();
        }

        requestedUserId = userId;
        requestedPrefix = safe(prefix);
        requestedMonth = month;
        requestedYear = year;
        cancelCall(availabilityCall);
        availabilityCall = null;

        String cacheKey = cacheKey(userId, requestedPrefix, month, year);
        CachedAvailability memoryAvailability = getCachedAvailability(cacheKey);
        if (memoryAvailability != null) {
            boolean fresh = isFresh(
                    memoryAvailability.updatedAtMillis,
                    CachePolicy.CALENDAR_AVAILABILITY_TTL_MS
            );
            showCachedAvailability(
                    cacheKey,
                    memoryAvailability.groups,
                    "memory",
                    memoryAvailability.updatedAtMillis,
                    !fresh,
                    actionStartedAtMillis
            );
            if (fresh) {
                return;
            }

            loadAvailabilityInternal(
                    userId,
                    requestedPrefix,
                    month,
                    year,
                    cacheKey,
                    false,
                    true,
                    actionStartedAtMillis
            );
            return;
        }

        int generation = ++cacheGeneration;
        availabilityLoadingLiveData.setValue(true);
        if (!cacheKey.equals(visibleCacheKey)) {
            availabilityGroupsLiveData.setValue(new ArrayList<>());
            currentAvailabilityUpdatedAtMillis = 0L;
            hasLoadedAvailability = false;
        }
        updateAvailabilityStatus(SyncStatusFormatter.REFRESHING);

        availabilityRepository.getCachedRequesterAvailability(
                userId,
                requestedPrefix,
                month,
                year,
                result -> {
                    if (generation != cacheGeneration
                            || !cacheKey.equals(cacheKey(
                                    requestedUserId,
                                    requestedPrefix,
                                    requestedMonth,
                                    requestedYear
                            ))) {
                        return;
                    }

                    handleCachedAvailabilityResult(
                            result,
                            userId,
                            requestedPrefix,
                            month,
                            year,
                            actionStartedAtMillis
                    );
                }
        );
    }

    public void refreshAvailability(int userId, String prefix, int month, int year) {
        String cacheKey = cacheKey(userId, prefix, month, year);
        if (availabilityCall != null) {
            AppDiagnostics.logEvent("requester_availability_manual_refresh_skipped_duplicate");
            return;
        }

        requestedUserId = userId;
        requestedPrefix = safe(prefix);
        requestedMonth = month;
        requestedYear = year;
        updateAvailabilityStatus(SyncStatusFormatter.REFRESHING);
        loadAvailabilityInternal(
                userId,
                requestedPrefix,
                month,
                year,
                cacheKey,
                !hasVisibleAvailabilityFor(cacheKey),
                hasVisibleAvailabilityFor(cacheKey),
                System.currentTimeMillis()
        );
    }

    public void refreshAvailabilityIfStaleOnForeground(
            int userId,
            String prefix,
            int month,
            int year
    ) {
        if (availabilityCall != null) {
            AppDiagnostics.logEvent(
                    "requester_availability_foreground_refresh_skipped_in_flight"
            );
            return;
        }

        String cacheKey = cacheKey(userId, prefix, month, year);
        if (!hasVisibleAvailabilityFor(cacheKey) || currentAvailabilityUpdatedAtMillis <= 0L) {
            AppDiagnostics.logEvent(
                    "requester_availability_foreground_refresh_skipped_no_visible_data"
            );
            loadAvailability(userId, prefix, month, year);
            return;
        }

        if (isFresh(
                currentAvailabilityUpdatedAtMillis,
                CachePolicy.CALENDAR_AVAILABILITY_TTL_MS
        )) {
            updateLastUpdatedAvailabilityStatus();
            AppDiagnostics.logEvent(
                    "requester_availability_foreground_refresh_skipped_cache_fresh"
            );
            return;
        }

        AppDiagnostics.logEvent(
                "requester_availability_foreground_refresh_cache_first_stale"
        );
        loadAvailability(userId, prefix, month, year);
    }

    public void refreshVisibleSyncStatusAge() {
        if (availabilityCall == null
                && !availabilityStatusShowsFailure
                && hasLoadedAvailability
                && currentAvailabilityUpdatedAtMillis > 0L) {
            updateLastUpdatedAvailabilityStatus();
        }
    }

    public void loadAvailableRoomsForDateRange(
            String arrivalDate,
            String departureDate,
            String prefix
    ) {
        if (availableRoomsRangeCall != null) {
            AppDiagnostics.logEvent(
                    "requester_available_rooms_range_skipped_duplicate"
            );
            return;
        }

        String cacheKey = "requester:available_rooms_range:"
                + safe(prefix)
                + ":"
                + safe(arrivalDate)
                + ":"
                + safe(departureDate);
        long requestStartedAtMillis = System.currentTimeMillis();
        availableRoomsLoadingLiveData.setValue(true);
        AppDiagnostics.logNetworkStart("requester_available_rooms_range", cacheKey);

        Call<ApiResponse<AvailableRoomsRangeResponse>> request =
                availabilityRepository.getRequesterAvailableRoomsByDateRange(
                        arrivalDate,
                        departureDate,
                        prefix
                );
        availableRoomsRangeCall = request;
        request.enqueue(new Callback<ApiResponse<AvailableRoomsRangeResponse>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<AvailableRoomsRangeResponse>> call,
                    @NonNull Response<ApiResponse<AvailableRoomsRangeResponse>> response
            ) {
                if (call != availableRoomsRangeCall) {
                    return;
                }

                availableRoomsRangeCall = null;
                availableRoomsLoadingLiveData.setValue(false);
                AppDiagnostics.logNetworkResponse(
                        "requester_available_rooms_range",
                        cacheKey,
                        response.code(),
                        System.currentTimeMillis() - requestStartedAtMillis
                );

                if (!isValidAvailableRoomsRangeResponse(response)) {
                    String message = ApiErrorUtils.messageFromResponse(
                            response,
                            "Could not load available rooms."
                    );
                    AppDiagnostics.logApiFailure(
                            "requester_available_rooms_range",
                            message,
                            null
                    );
                    toastLiveData.setValue(new UiEvent<>(message));
                    return;
                }

                networkBannerLiveData.setValue(new UiEvent<>(false));
                availableRoomsRangeLiveData.setValue(
                        new UiEvent<>(response.body().getData())
                );
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<AvailableRoomsRangeResponse>> call,
                    @NonNull Throwable t
            ) {
                if (call != availableRoomsRangeCall) {
                    return;
                }

                availableRoomsRangeCall = null;
                availableRoomsLoadingLiveData.setValue(false);
                if (call.isCanceled()) {
                    return;
                }

                String message = ApiErrorUtils.messageFromThrowable(t);
                AppDiagnostics.logApiFailure(
                        "requester_available_rooms_range",
                        message,
                        t
                );
                networkBannerLiveData.setValue(new UiEvent<>(true));
                toastLiveData.setValue(new UiEvent<>(message));
            }
        });
    }

    private void handleCachedAvailabilityResult(
            CacheReadResult<List<RoomAvailabilityGroup>> result,
            int userId,
            String prefix,
            int month,
            int year,
            long actionStartedAtMillis
    ) {
        String cacheKey = cacheKey(userId, prefix, month, year);
        if (result != null && result.isHit()) {
            boolean fresh = result.isFresh();
            cacheAvailabilityInMemory(
                    cacheKey,
                    result.getValue(),
                    result.getUpdatedAtMillis()
            );
            showCachedAvailability(
                    cacheKey,
                    result.getValue(),
                    "disk",
                    result.getUpdatedAtMillis(),
                    !fresh,
                    actionStartedAtMillis
            );
            if (fresh) {
                return;
            }

            loadAvailabilityInternal(
                    userId,
                    prefix,
                    month,
                    year,
                    cacheKey,
                    false,
                    true,
                    actionStartedAtMillis
            );
            return;
        }

        loadAvailabilityInternal(
                userId,
                prefix,
                month,
                year,
                cacheKey,
                true,
                false,
                actionStartedAtMillis
        );
    }

    private void loadAvailabilityInternal(
            int userId,
            String prefix,
            int month,
            int year,
            String cacheKey,
            boolean showLoading,
            boolean quietFailure,
            long actionStartedAtMillis
    ) {
        if (showLoading) {
            availabilityLoadingLiveData.setValue(true);
        }

        long requestStartedAtMillis = System.currentTimeMillis();
        AppDiagnostics.logNetworkStart("requester_availability_calendar", cacheKey);
        Call<ApiResponse<RoomAvailabilityResponse>> request =
                availabilityRepository.getRequesterAvailability(month, year, prefix);
        availabilityCall = request;
        request.enqueue(new Callback<ApiResponse<RoomAvailabilityResponse>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<RoomAvailabilityResponse>> call,
                    @NonNull Response<ApiResponse<RoomAvailabilityResponse>> response
            ) {
                if (!isCurrentAvailabilityCall(call, cacheKey)) {
                    return;
                }

                availabilityCall = null;
                availabilityLoadingLiveData.setValue(false);

                if (!isValidAvailabilityResponse(response)) {
                    String message = ApiErrorUtils.messageFromResponse(
                            response,
                            MESSAGE_AVAILABILITY_FAILED
                    );
                    AppDiagnostics.logApiFailure(
                            "requester_availability_calendar",
                            message,
                            null
                    );
                    if (quietFailure && hasVisibleAvailabilityFor(cacheKey)) {
                        keepCachedAvailabilityAfterFailure(
                                ApiErrorUtils.cachedDataMessageForHttpCode(response.code())
                        );
                    } else {
                        updateAvailabilityStatus(message, true);
                        toastLiveData.setValue(new UiEvent<>(message));
                    }
                    logNetworkResponse(cacheKey, response.code(), requestStartedAtMillis);
                    return;
                }

                networkBannerLiveData.setValue(new UiEvent<>(false));
                RoomAvailabilityResponse data = response.body().getData();
                List<RoomAvailabilityGroup> groups = data.hasGroups()
                        ? NullSafeCollections.copyWithoutNulls(data.getGroups())
                        : new ArrayList<>();
                cacheAvailability(userId, prefix, month, year, cacheKey, groups);
                availabilityGroupsLiveData.setValue(groups);
                updateLastUpdatedAvailabilityStatus();
                AppDiagnostics.logUiUpdated(
                        "requester_availability_calendar",
                        "network",
                        System.currentTimeMillis() - actionStartedAtMillis
                );
                logNetworkResponse(cacheKey, response.code(), requestStartedAtMillis);
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<RoomAvailabilityResponse>> call,
                    @NonNull Throwable t
            ) {
                if (!isCurrentAvailabilityCall(call, cacheKey)) {
                    return;
                }

                availabilityCall = null;
                availabilityLoadingLiveData.setValue(false);
                if (call.isCanceled()) {
                    return;
                }

                String message = ApiErrorUtils.messageFromThrowable(t);
                AppDiagnostics.logApiFailure(
                        "requester_availability_calendar",
                        message,
                        t
                );
                if (quietFailure && hasVisibleAvailabilityFor(cacheKey)) {
                    keepCachedAvailabilityAfterFailure(
                            ApiErrorUtils.cachedDataMessageForThrowable(t)
                    );
                } else {
                    updateAvailabilityStatus(message, true);
                    networkBannerLiveData.setValue(new UiEvent<>(true));
                }
                logNetworkResponse(cacheKey, 0, requestStartedAtMillis);
            }
        });
    }

    private void showCachedAvailability(
            String cacheKey,
            List<RoomAvailabilityGroup> groups,
            String source,
            long updatedAtMillis,
            boolean refreshing,
            long actionStartedAtMillis
    ) {
        availabilityLoadingLiveData.setValue(false);
        networkBannerLiveData.setValue(new UiEvent<>(false));
        visibleCacheKey = cacheKey;
        currentAvailabilityUpdatedAtMillis = updatedAtMillis;
        hasLoadedAvailability = true;
        updateAvailabilityStatus(refreshing
                ? SyncStatusFormatter.SHOWING_CACHED_REFRESHING
                : SyncStatusFormatter.lastUpdated(updatedAtMillis));
        availabilityGroupsLiveData.setValue(NullSafeCollections.copyWithoutNulls(groups));
        AppDiagnostics.logUiUpdated(
                "requester_availability_calendar",
                "cache_" + safe(source),
                System.currentTimeMillis() - actionStartedAtMillis
        );
    }

    private void cacheAvailability(
            int userId,
            String prefix,
            int month,
            int year,
            String cacheKey,
            List<RoomAvailabilityGroup> groups
    ) {
        long updatedAtMillis = System.currentTimeMillis();
        cacheAvailabilityInMemory(cacheKey, groups, updatedAtMillis);
        availabilityRepository.saveCachedRequesterAvailability(
                userId,
                prefix,
                month,
                year,
                NullSafeCollections.copyWithoutNulls(groups)
        );
        visibleCacheKey = cacheKey;
        currentAvailabilityUpdatedAtMillis = updatedAtMillis;
        hasLoadedAvailability = true;
        availabilityStatusShowsFailure = false;
    }

    private void keepCachedAvailabilityAfterFailure(String message) {
        updateAvailabilityStatus(message, true);
        AppDiagnostics.logEvent("requester_availability_kept_cached_after_failure");
    }

    private void updateLastUpdatedAvailabilityStatus() {
        updateAvailabilityStatus(
                SyncStatusFormatter.lastUpdated(currentAvailabilityUpdatedAtMillis),
                false
        );
    }

    private void updateAvailabilityStatus(String message) {
        updateAvailabilityStatus(message, false);
    }

    private void updateAvailabilityStatus(String message, boolean failureState) {
        availabilityStatusShowsFailure = failureState;
        availabilityStatusLiveData.setValue(message != null ? message : "");
    }

    private boolean syncCacheInvalidationVersion() {
        int currentVersion = RequesterAvailabilityRepository.getCacheInvalidationVersion();
        if (currentVersion == observedCacheInvalidationVersion) {
            return false;
        }

        observedCacheInvalidationVersion = currentVersion;
        synchronized (CACHE_LOCK) {
            availabilityCache.clear();
        }
        return true;
    }

    private void clearVisibleAvailability() {
        availabilityGroupsLiveData.setValue(new ArrayList<>());
        visibleCacheKey = "";
        currentAvailabilityUpdatedAtMillis = 0L;
        hasLoadedAvailability = false;
    }

    private boolean isCurrentAvailabilityCall(
            Call<ApiResponse<RoomAvailabilityResponse>> call,
            String cacheKey
    ) {
        return call == availabilityCall && cacheKey.equals(cacheKey(
                requestedUserId,
                requestedPrefix,
                requestedMonth,
                requestedYear
        ));
    }

    private boolean isValidAvailabilityResponse(
            Response<ApiResponse<RoomAvailabilityResponse>> response
    ) {
        return response != null
                && response.isSuccessful()
                && response.body() != null
                && response.body().isSuccess()
                && response.body().getData() != null;
    }

    private boolean isValidAvailableRoomsRangeResponse(
            Response<ApiResponse<AvailableRoomsRangeResponse>> response
    ) {
        return response != null
                && response.isSuccessful()
                && response.body() != null
                && response.body().isSuccess()
                && response.body().getData() != null;
    }

    private boolean hasVisibleAvailabilityFor(String cacheKey) {
        return hasLoadedAvailability && cacheKey.equals(visibleCacheKey);
    }

    private boolean isFresh(long updatedAtMillis, long ttlMillis) {
        return CachePolicy.isFresh(updatedAtMillis, ttlMillis, System.currentTimeMillis());
    }

    private void logNetworkResponse(
            String cacheKey,
            int httpCode,
            long requestStartedAtMillis
    ) {
        AppDiagnostics.logNetworkResponse(
                "requester_availability_calendar",
                cacheKey,
                httpCode,
                System.currentTimeMillis() - requestStartedAtMillis
        );
    }

    private static void cacheAvailabilityInMemory(
            String cacheKey,
            List<RoomAvailabilityGroup> groups,
            long updatedAtMillis
    ) {
        synchronized (CACHE_LOCK) {
            availabilityCache.put(
                    cacheKey,
                    new CachedAvailability(groups, updatedAtMillis)
            );
        }
    }

    private static CachedAvailability getCachedAvailability(String cacheKey) {
        synchronized (CACHE_LOCK) {
            return availabilityCache.get(cacheKey);
        }
    }

    public static void clearInMemoryAvailabilityCachesForLogout() {
        synchronized (CACHE_LOCK) {
            availabilityCache.clear();
        }
    }

    private static void cancelCall(Call<?> call) {
        if (call != null && !call.isCanceled()) {
            call.cancel();
        }
    }

    private static String cacheKey(int userId, String prefix, int month, int year) {
        return RequesterAvailabilityRepository.requesterAvailabilityCacheKey(
                userId,
                prefix,
                year,
                month
        );
    }

    private static String safe(String value) {
        return value != null ? value.trim() : "";
    }

    @Override
    protected void onCleared() {
        cancelCall(availabilityCall);
        cancelCall(availableRoomsRangeCall);
        availabilityCall = null;
        availableRoomsRangeCall = null;
        super.onCleared();
    }
}
