package com.example.roombooking.booking;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roombooking.cache.CachePolicy;
import com.example.roombooking.cache.CacheReadResult;
import com.example.roombooking.model.booking.BookingActionData;
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

public class LandingViewModel extends ViewModel {

    public static final class DeleteBookingResult {
        private final int bookingId;
        private final String message;

        DeleteBookingResult(int bookingId, String message) {
            this.bookingId = bookingId;
            this.message = message;
        }

        public int getBookingId() {
            return bookingId;
        }

        public String getMessage() {
            return message;
        }
    }

    private static final String MESSAGE_AVAILABILITY_FAILED =
            "Failed to load room availability.";
    private static final String MESSAGE_DETAILS_FAILED =
            "Failed to load booking details.";
    private static final String MESSAGE_AVAILABLE_ROOMS_FAILED =
            "Failed to load available rooms.";
    private static final String MESSAGE_CACHED_AVAILABILITY_REFRESHING =
            "Showing cached availability. Refreshing...";
    private static final String MESSAGE_CACHED_AVAILABILITY_FINAL_CHECK =
            "Showing cached availability. Final booking will be verified by server.";
    private static final String MESSAGE_DELETE_FAILED =
            "Failed to delete booking.";
    private static final String MESSAGE_DELETE_IN_FLIGHT =
            "Deletion is already in progress.";
    private static final int MAX_NETWORK_RETRIES = 0;
    private static final long RETRY_DELAY_MS = 700L;
    private static final Object CACHE_LOCK = new Object();
    private static final Map<String, CachedAvailability> availabilityCache =
            new HashMap<>();
    private static final Map<String, CachedAvailableRooms> availableRoomsCache =
            new HashMap<>();
    private static final Map<String, CachedAvailableRoomsRange> availableRoomsRangeCache =
            new HashMap<>();

    private final AvailabilityRepository availabilityRepository;
    private final Handler retryHandler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<Boolean> availabilityLoadingLiveData =
            new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> availableRoomsLoadingLiveData =
            new MutableLiveData<>(false);
    private final MutableLiveData<List<RoomAvailabilityGroup>> availabilityGroupsLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<UiEvent<String>> toastLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> availabilityStatusLiveData =
            new MutableLiveData<>("");
    private final MutableLiveData<UiEvent<Boolean>> networkBannerLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<UiEvent<RoomAvailabilityDetailsResponse>>
            availabilityDetailsLiveData = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<AvailableRoomsResponse>>
            availableRoomsLiveData = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<AvailableRoomsRangeResponse>>
            availableRoomsRangeLiveData = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<DeleteBookingResult>>
            deleteBookingResultLiveData = new MutableLiveData<>();

    private Call<ApiResponse<RoomAvailabilityResponse>> availabilityCall;
    private Call<ApiResponse<RoomAvailabilityDetailsResponse>> availabilityDetailsCall;
    private Call<ApiResponse<AvailableRoomsResponse>> availableRoomsCall;
    private Call<ApiResponse<AvailableRoomsRangeResponse>> availableRoomsRangeCall;
    private Call<ApiResponse<BookingActionData>> deleteBookingCall;
    private Runnable pendingAvailabilityRetry;
    private Runnable pendingAvailableRoomsRetry;
    private Runnable pendingAvailableRoomsRangeRetry;
    private int availabilityCacheGeneration = 0;
    private int availableRoomsCacheGeneration = 0;
    private int availableRoomsRangeCacheGeneration = 0;
    private int observedCacheInvalidationVersion =
            AvailabilityRepository.getCacheInvalidationVersion();
    private boolean forceNextAvailabilityNetworkRefresh = false;
    private long currentAvailabilityUpdatedAtMillis = 0L;
    private boolean hasLoadedAvailability = false;
    private boolean availabilityStatusShowsFailure = false;

    private int requestedMonth = -1;
    private int requestedYear = -1;
    private String requestedDetailsPrefix = "";
    private String requestedAvailableRoomsPrefix = "";
    private String requestedRangePrefix = "";
    private String requestedAvailableRoomsCacheKey = "";
    private String requestedAvailableRoomsRangeCacheKey = "";
    private final Map<String, AvailableRoomsSheetStatus> availableRoomsSheetStatus =
            new HashMap<>();

    public LandingViewModel(AvailabilityRepository availabilityRepository) {
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

    private static final class CachedAvailableRooms {
        private final AvailableRoomsResponse response;
        private final long updatedAtMillis;

        private CachedAvailableRooms(
                AvailableRoomsResponse response,
                long updatedAtMillis
        ) {
            this.response = response;
            this.updatedAtMillis = updatedAtMillis;
        }
    }

    private static final class CachedAvailableRoomsRange {
        private final AvailableRoomsRangeResponse response;
        private final long updatedAtMillis;

        private CachedAvailableRoomsRange(
                AvailableRoomsRangeResponse response,
                long updatedAtMillis
        ) {
            this.response = response;
            this.updatedAtMillis = updatedAtMillis;
        }
    }

    private static final class AvailableRoomsSheetStatus {
        private final long updatedAtMillis;
        private final boolean refreshing;
        private final String failureMessage;

        private AvailableRoomsSheetStatus(
                long updatedAtMillis,
                boolean refreshing,
                String failureMessage
        ) {
            this.updatedAtMillis = updatedAtMillis;
            this.refreshing = refreshing;
            this.failureMessage = failureMessage;
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

    public LiveData<UiEvent<String>> getToastLiveData() {
        return toastLiveData;
    }

    public LiveData<String> getAvailabilityStatusLiveData() {
        return availabilityStatusLiveData;
    }

    public LiveData<UiEvent<Boolean>> getNetworkBannerLiveData() {
        return networkBannerLiveData;
    }

    public LiveData<UiEvent<RoomAvailabilityDetailsResponse>>
    getAvailabilityDetailsLiveData() {
        return availabilityDetailsLiveData;
    }

    public LiveData<UiEvent<AvailableRoomsResponse>> getAvailableRoomsLiveData() {
        return availableRoomsLiveData;
    }

    public LiveData<UiEvent<AvailableRoomsRangeResponse>> getAvailableRoomsRangeLiveData() {
        return availableRoomsRangeLiveData;
    }

    public LiveData<UiEvent<DeleteBookingResult>> getDeleteBookingResultLiveData() {
        return deleteBookingResultLiveData;
    }

    public void invalidateCalendarAvailabilityCacheForMutation() {
        clearAvailabilityCaches();
        forceNextAvailabilityNetworkRefresh = true;
    }

    public void loadAvailability(int month, int year) {
        long actionStartedAtMillis = System.currentTimeMillis();
        if (syncCacheInvalidationVersion()) {
            forceNextAvailabilityNetworkRefresh = true;
        }
        requestedMonth = month;
        requestedYear = year;
        cancelPendingAvailabilityRetry();

        cancelCall(availabilityCall);
        availabilityCall = null;

        if (forceNextAvailabilityNetworkRefresh) {
            forceNextAvailabilityNetworkRefresh = false;
            availabilityLoadingLiveData.setValue(!hasVisibleAvailability());
            loadAvailabilityInternal(month, year, 0, !hasVisibleAvailability(), true,
                    actionStartedAtMillis);
            return;
        }

        CachedAvailability memoryAvailability = getCachedAvailability(month, year);
        if (memoryAvailability != null) {
            boolean fresh = isFresh(
                    memoryAvailability.updatedAtMillis,
                    CachePolicy.CALENDAR_AVAILABILITY_TTL_MS
            );
            showCachedAvailability(
                    memoryAvailability.groups,
                    "memory",
                    memoryAvailability.updatedAtMillis,
                    !fresh,
                    actionStartedAtMillis
            );
            if (fresh) {
                return;
            }

            loadAvailabilityInternal(month, year, 0, false, true, actionStartedAtMillis);
            return;
        }

        int generation = ++availabilityCacheGeneration;
        availabilityLoadingLiveData.setValue(!hasVisibleAvailability());
        availabilityRepository.getCachedCalendarAvailability(month, year, result -> {
            if (generation != availabilityCacheGeneration
                    || month != requestedMonth
                    || year != requestedYear) {
                return;
            }

            handleCachedAvailabilityResult(result, month, year, actionStartedAtMillis);
        });
    }

    public void refreshAvailability(int month, int year) {
        if (availabilityCall != null) {
            AppDiagnostics.logEvent("availability_manual_refresh_skipped_duplicate");
            return;
        }

        requestedMonth = month;
        requestedYear = year;
        cancelPendingAvailabilityRetry();
        updateAvailabilityStatus(SyncStatusFormatter.REFRESHING);
        loadAvailabilityInternal(
                month,
                year,
                0,
                !hasVisibleAvailability(),
                hasVisibleAvailability(),
                System.currentTimeMillis()
        );
    }

    public void refreshAvailabilityIfStaleOnForeground(int month, int year) {
        if (availabilityCall != null) {
            AppDiagnostics.logEvent("availability_foreground_refresh_skipped_in_flight");
            return;
        }

        if (!hasLoadedAvailability || currentAvailabilityUpdatedAtMillis <= 0L) {
            AppDiagnostics.logEvent("availability_foreground_refresh_skipped_no_visible_data");
            return;
        }

        if (isFresh(
                currentAvailabilityUpdatedAtMillis,
                CachePolicy.CALENDAR_AVAILABILITY_TTL_MS
        )) {
            updateLastUpdatedAvailabilityStatus();
            AppDiagnostics.logEvent("availability_foreground_refresh_skipped_cache_fresh");
            return;
        }

        requestedMonth = month;
        requestedYear = year;
        updateAvailabilityStatus(SyncStatusFormatter.REFRESHING);
        loadAvailabilityInternal(month, year, 0, false, true, System.currentTimeMillis());
    }

    public void refreshVisibleSyncStatusAge() {
        if (availabilityCall == null
                && !availabilityStatusShowsFailure
                && hasLoadedAvailability
                && currentAvailabilityUpdatedAtMillis > 0L) {
            updateLastUpdatedAvailabilityStatus();
        }
    }

    private void loadAvailabilityInternal(
            int month,
            int year,
            int retryAttempt,
            boolean showLoading,
            boolean quietFailure
    ) {
        loadAvailabilityInternal(
                month,
                year,
                retryAttempt,
                showLoading,
                quietFailure,
                System.currentTimeMillis()
        );
    }

    private void loadAvailabilityInternal(
            int month,
            int year,
            int retryAttempt,
            boolean showLoading,
            boolean quietFailure,
            long actionStartedAtMillis
    ) {
        if (showLoading) {
            availabilityLoadingLiveData.setValue(true);
        }

        String cacheKey = AvailabilityRepository.calendarAvailabilityCacheKey(month, year);
        long requestStartedAtMillis = System.currentTimeMillis();
        AppDiagnostics.logNetworkStart("availability_calendar", cacheKey);
        Call<ApiResponse<RoomAvailabilityResponse>> request =
                availabilityRepository.getRoomAvailability(month, year);
        availabilityCall = request;
        request.enqueue(new Callback<ApiResponse<RoomAvailabilityResponse>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<RoomAvailabilityResponse>> call,
                    @NonNull Response<ApiResponse<RoomAvailabilityResponse>> response
            ) {
                if (!isCurrentAvailabilityCall(call, month, year)) {
                    return;
                }

                availabilityCall = null;

                if (!isValidAvailabilityResponse(response)) {
                    if (shouldRetry(retryAttempt, response.code())) {
                        scheduleAvailabilityRetry(
                                month,
                                year,
                                retryAttempt + 1,
                                showLoading,
                                quietFailure,
                                actionStartedAtMillis
                        );
                        return;
                    }

                    availabilityLoadingLiveData.setValue(false);
                    String message = ApiErrorUtils.messageFromResponse(
                            response,
                            MESSAGE_AVAILABILITY_FAILED
                    );
                    AppDiagnostics.logApiFailure("availability_calendar", message, null);
                    if (quietFailure) {
                        keepCachedAvailabilityAfterFailure(
                                ApiErrorUtils.cachedDataMessageForHttpCode(response.code())
                        );
                    } else {
                        toastLiveData.setValue(new UiEvent<>(message));
                    }
                    logAvailabilityNetworkResponse(
                            cacheKey,
                            response.code(),
                            requestStartedAtMillis
                    );
                    return;
                }

                availabilityLoadingLiveData.setValue(false);
                networkBannerLiveData.setValue(new UiEvent<>(false));
                RoomAvailabilityResponse data = response.body().getData();
                List<RoomAvailabilityGroup> groups = data.hasGroups()
                        ? NullSafeCollections.copyWithoutNulls(data.getGroups())
                        : new ArrayList<>();
                cacheAvailability(month, year, groups);
                availabilityGroupsLiveData.setValue(groups);
                hasLoadedAvailability = true;
                updateLastUpdatedAvailabilityStatus();
                AppDiagnostics.logUiUpdated(
                        "availability_calendar",
                        "network",
                        System.currentTimeMillis() - actionStartedAtMillis
                );
                logAvailabilityNetworkResponse(cacheKey, response.code(), requestStartedAtMillis);
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<RoomAvailabilityResponse>> call,
                    @NonNull Throwable t
            ) {
                if (!isCurrentAvailabilityCall(call, month, year)) {
                    return;
                }

                availabilityCall = null;
                if (!call.isCanceled()) {
                    if (shouldRetry(retryAttempt)) {
                        scheduleAvailabilityRetry(
                                month,
                                year,
                                retryAttempt + 1,
                                showLoading,
                                quietFailure,
                                actionStartedAtMillis
                        );
                        return;
                    }

                    availabilityLoadingLiveData.setValue(false);
                    AppDiagnostics.logApiFailure(
                            "availability_calendar",
                            ApiErrorUtils.messageFromThrowable(t),
                            t
                    );
                    if (quietFailure && hasLoadedAvailability) {
                        keepCachedAvailabilityAfterFailure(
                                ApiErrorUtils.cachedDataMessageForThrowable(t)
                        );
                    } else {
                        networkBannerLiveData.setValue(new UiEvent<>(true));
                    }
                    logAvailabilityNetworkResponse(cacheKey, 0, requestStartedAtMillis);
                }
            }
        });
    }

    public void loadAvailabilityDetails(String date, String prefix) {
        requestedDetailsPrefix = safe(prefix);

        cancelCall(availabilityDetailsCall);
        Call<ApiResponse<RoomAvailabilityDetailsResponse>> request =
                availabilityRepository.getRoomAvailabilityDetails(date, requestedDetailsPrefix);
        availabilityDetailsCall = request;
        request.enqueue(new Callback<ApiResponse<RoomAvailabilityDetailsResponse>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<RoomAvailabilityDetailsResponse>> call,
                    @NonNull Response<ApiResponse<RoomAvailabilityDetailsResponse>> response
            ) {
                if (!isCurrentPrefixCall(call, availabilityDetailsCall, requestedDetailsPrefix)) {
                    return;
                }

                availabilityDetailsCall = null;
                if (!isValidAvailabilityDetailsResponse(response)) {
                    String message = ApiErrorUtils.messageFromResponse(
                            response,
                            MESSAGE_DETAILS_FAILED
                    );
                    AppDiagnostics.logApiFailure("availability_details", message, null);
                    toastLiveData.setValue(new UiEvent<>(message));
                    return;
                }

                networkBannerLiveData.setValue(new UiEvent<>(false));
                availabilityDetailsLiveData.setValue(new UiEvent<>(response.body().getData()));
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<RoomAvailabilityDetailsResponse>> call,
                    @NonNull Throwable t
            ) {
                if (!isCurrentPrefixCall(call, availabilityDetailsCall, requestedDetailsPrefix)) {
                    return;
                }

                availabilityDetailsCall = null;
                if (!call.isCanceled()) {
                    AppDiagnostics.logApiFailure(
                            "availability_details",
                            ApiErrorUtils.networkMessage(),
                            t
                    );
                    networkBannerLiveData.setValue(new UiEvent<>(true));
                }
            }
        });
    }

    public void loadAvailableRoomsForDate(String date, String prefix) {
        syncCacheInvalidationVersion();
        cancelPendingAvailableRoomsRetry();
        cancelPendingAvailableRoomsRangeRetry();
        cancelCall(availableRoomsRangeCall);
        availableRoomsRangeCall = null;
        requestedRangePrefix = "";
        requestedAvailableRoomsPrefix = safe(prefix);
        String cacheKey = AvailabilityRepository.availableRoomsCacheKey(
                requestedAvailableRoomsPrefix,
                date
        );

        if (availableRoomsCall != null && cacheKey.equals(requestedAvailableRoomsCacheKey)) {
            AppDiagnostics.logEvent("available_rooms_single_flight_skipped key=" + cacheKey);
            return;
        }

        if (availableRoomsCall != null) {
            cancelCall(availableRoomsCall);
            availableRoomsCall = null;
        }

        requestedAvailableRoomsCacheKey = cacheKey;
        long actionStartedAtMillis = System.currentTimeMillis();
        CachedAvailableRooms memoryResponse = getCachedAvailableRooms(cacheKey);
        if (memoryResponse != null && memoryResponse.response != null) {
            boolean fresh = isFresh(
                    memoryResponse.updatedAtMillis,
                    CachePolicy.AVAILABLE_ROOMS_TTL_MS
            );
            updateAvailableRoomsSheetStatus(cacheKey, memoryResponse.updatedAtMillis, !fresh);
            showCachedAvailableRooms(memoryResponse.response, "memory", actionStartedAtMillis);
            if (fresh) {
                return;
            }

            loadAvailableRoomsForDateInternal(
                    date,
                    requestedAvailableRoomsPrefix,
                    cacheKey,
                    0,
                    false,
                    true,
                    actionStartedAtMillis
            );
            return;
        }

        int generation = ++availableRoomsCacheGeneration;
        availableRoomsLoadingLiveData.setValue(true);
        availabilityRepository.getCachedAvailableRoomsByDate(
                date,
                requestedAvailableRoomsPrefix,
                result -> {
                    if (generation != availableRoomsCacheGeneration
                            || !cacheKey.equals(requestedAvailableRoomsCacheKey)) {
                        AppDiagnostics.logEvent(
                                "available_rooms_cache_result_skipped key=" + cacheKey
                        );
                        return;
                    }

                    handleCachedAvailableRoomsResult(
                            result,
                            date,
                            requestedAvailableRoomsPrefix,
                            actionStartedAtMillis
                    );
                }
        );
    }

    private void loadAvailableRoomsForDateInternal(
            String date,
            String prefix,
            String cacheKey,
            int retryAttempt,
            boolean showLoading,
            boolean quietFailure,
            long actionStartedAtMillis
    ) {
        if (availableRoomsCall != null && cacheKey.equals(requestedAvailableRoomsCacheKey)) {
            AppDiagnostics.logEvent("available_rooms_single_flight_skipped key=" + cacheKey);
            return;
        }

        if (showLoading) {
            availableRoomsLoadingLiveData.setValue(true);
        }

        long requestStartedAtMillis = System.currentTimeMillis();
        AppDiagnostics.logNetworkStart("available_rooms", cacheKey);
        Call<ApiResponse<AvailableRoomsResponse>> request =
                availabilityRepository.getAvailableRoomsByDate(
                        date,
                        prefix
                );
        availableRoomsCall = request;
        request.enqueue(new Callback<ApiResponse<AvailableRoomsResponse>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<AvailableRoomsResponse>> call,
                    @NonNull Response<ApiResponse<AvailableRoomsResponse>> response
            ) {
                if (!isCurrentAvailableRoomsCall(call, cacheKey)) {
                    return;
                }

                availableRoomsCall = null;

                if (!isValidAvailableRoomsResponse(response)) {
                    if (shouldRetry(retryAttempt, response.code())) {
                        scheduleAvailableRoomsRetry(
                                date,
                                prefix,
                                cacheKey,
                                retryAttempt + 1,
                                showLoading,
                                quietFailure,
                                actionStartedAtMillis
                        );
                        return;
                    }

                    availableRoomsLoadingLiveData.setValue(false);
                    String message = ApiErrorUtils.messageFromResponse(
                            response,
                            MESSAGE_AVAILABLE_ROOMS_FAILED
                    );
                    AppDiagnostics.logApiFailure("available_rooms", message, null);
                    if (quietFailure) {
                        keepCachedAvailableRoomsAfterFailure(
                                cacheKey,
                                ApiErrorUtils.cachedDataMessageForHttpCode(response.code())
                        );
                    } else {
                        toastLiveData.setValue(new UiEvent<>(message));
                    }
                    logAvailableRoomsNetworkResponse(cacheKey, response.code(), requestStartedAtMillis);
                    return;
                }

                availableRoomsLoadingLiveData.setValue(false);
                networkBannerLiveData.setValue(new UiEvent<>(false));
                AvailableRoomsResponse data = response.body().getData();
                cacheAvailableRooms(cacheKey, date, prefix, data);
                updateAvailableRoomsSheetStatus(cacheKey, System.currentTimeMillis(), false);
                availableRoomsLiveData.setValue(new UiEvent<>(data));
                AppDiagnostics.logUiUpdated(
                        "available_rooms",
                        "network",
                        System.currentTimeMillis() - actionStartedAtMillis
                );
                logAvailableRoomsNetworkResponse(cacheKey, response.code(), requestStartedAtMillis);
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<AvailableRoomsResponse>> call,
                    @NonNull Throwable t
            ) {
                if (!isCurrentAvailableRoomsCall(call, cacheKey)) {
                    return;
                }

                availableRoomsCall = null;
                if (!call.isCanceled()) {
                    if (shouldRetry(retryAttempt)) {
                        scheduleAvailableRoomsRetry(
                                date,
                                prefix,
                                cacheKey,
                                retryAttempt + 1,
                                showLoading,
                                quietFailure,
                                actionStartedAtMillis
                        );
                        return;
                    }

                    availableRoomsLoadingLiveData.setValue(false);
                    AppDiagnostics.logApiFailure(
                            "available_rooms",
                            ApiErrorUtils.messageFromThrowable(t),
                            t
                    );
                    if (quietFailure) {
                        keepCachedAvailableRoomsAfterFailure(
                                cacheKey,
                                ApiErrorUtils.cachedDataMessageForThrowable(t)
                        );
                    } else {
                        networkBannerLiveData.setValue(new UiEvent<>(true));
                    }
                    logAvailableRoomsNetworkResponse(cacheKey, 0, requestStartedAtMillis);
                }
            }
        });
    }

    public void loadAvailableRoomsForDateRange(
            String arrivalDate,
            String departureDate,
            String prefix
    ) {
        syncCacheInvalidationVersion();
        cancelPendingAvailableRoomsRetry();
        cancelPendingAvailableRoomsRangeRetry();
        cancelCall(availableRoomsCall);
        availableRoomsCall = null;
        requestedAvailableRoomsPrefix = "";
        requestedRangePrefix = safe(prefix);
        String cacheKey = AvailabilityRepository.availableRoomsRangeCacheKey(
                requestedRangePrefix,
                arrivalDate,
                departureDate
        );

        if (availableRoomsRangeCall != null
                && cacheKey.equals(requestedAvailableRoomsRangeCacheKey)) {
            AppDiagnostics.logEvent("available_rooms_range_single_flight_skipped key=" + cacheKey);
            return;
        }

        if (availableRoomsRangeCall != null) {
            cancelCall(availableRoomsRangeCall);
            availableRoomsRangeCall = null;
        }

        requestedAvailableRoomsRangeCacheKey = cacheKey;
        long actionStartedAtMillis = System.currentTimeMillis();
        CachedAvailableRoomsRange memoryResponse = getCachedAvailableRoomsRange(cacheKey);
        if (memoryResponse != null && memoryResponse.response != null) {
            boolean fresh = isFresh(
                    memoryResponse.updatedAtMillis,
                    CachePolicy.AVAILABLE_ROOMS_RANGE_TTL_MS
            );
            updateAvailableRoomsSheetStatus(cacheKey, memoryResponse.updatedAtMillis, !fresh);
            showCachedAvailableRoomsRange(
                    memoryResponse.response,
                    "memory",
                    actionStartedAtMillis
            );
            if (fresh) {
                return;
            }

            loadAvailableRoomsForDateRangeInternal(
                    arrivalDate,
                    departureDate,
                    requestedRangePrefix,
                    cacheKey,
                    0,
                    false,
                    true,
                    actionStartedAtMillis
            );
            return;
        }

        int generation = ++availableRoomsRangeCacheGeneration;
        availableRoomsLoadingLiveData.setValue(true);
        availabilityRepository.getCachedAvailableRoomsByDateRange(
                arrivalDate,
                departureDate,
                requestedRangePrefix,
                result -> {
                    if (generation != availableRoomsRangeCacheGeneration
                            || !cacheKey.equals(requestedAvailableRoomsRangeCacheKey)) {
                        AppDiagnostics.logEvent(
                                "available_rooms_range_cache_result_skipped key=" + cacheKey
                        );
                        return;
                    }

                    handleCachedAvailableRoomsRangeResult(
                            result,
                            arrivalDate,
                            departureDate,
                            requestedRangePrefix,
                            actionStartedAtMillis
                    );
                }
        );
    }

    private void loadAvailableRoomsForDateRangeInternal(
            String arrivalDate,
            String departureDate,
            String prefix,
            String cacheKey,
            int retryAttempt,
            boolean showLoading,
            boolean quietFailure,
            long actionStartedAtMillis
    ) {
        if (availableRoomsRangeCall != null
                && cacheKey.equals(requestedAvailableRoomsRangeCacheKey)) {
            AppDiagnostics.logEvent("available_rooms_range_single_flight_skipped key=" + cacheKey);
            return;
        }

        if (showLoading) {
            availableRoomsLoadingLiveData.setValue(true);
        }

        long requestStartedAtMillis = System.currentTimeMillis();
        AppDiagnostics.logNetworkStart("available_rooms_range", cacheKey);
        Call<ApiResponse<AvailableRoomsRangeResponse>> request =
                availabilityRepository.getAvailableRoomsByDateRange(
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
                if (!isCurrentAvailableRoomsRangeCall(call, cacheKey)) {
                    return;
                }

                availableRoomsRangeCall = null;

                if (!isValidAvailableRoomsRangeResponse(response)) {
                    if (shouldRetry(retryAttempt, response.code())) {
                        scheduleAvailableRoomsRangeRetry(
                                arrivalDate,
                                departureDate,
                                prefix,
                                cacheKey,
                                retryAttempt + 1,
                                showLoading,
                                quietFailure,
                                actionStartedAtMillis
                        );
                        return;
                    }

                    availableRoomsLoadingLiveData.setValue(false);
                    String message = ApiErrorUtils.messageFromResponse(
                            response,
                            MESSAGE_AVAILABLE_ROOMS_FAILED
                    );
                    AppDiagnostics.logApiFailure("available_rooms_range", message, null);
                    if (quietFailure) {
                        keepCachedAvailableRoomsRangeAfterFailure(
                                cacheKey,
                                ApiErrorUtils.cachedDataMessageForHttpCode(response.code())
                        );
                    } else {
                        toastLiveData.setValue(new UiEvent<>(message));
                    }
                    logAvailableRoomsRangeNetworkResponse(
                            cacheKey,
                            response.code(),
                            requestStartedAtMillis
                    );
                    return;
                }

                availableRoomsLoadingLiveData.setValue(false);
                networkBannerLiveData.setValue(new UiEvent<>(false));
                AvailableRoomsRangeResponse data = response.body().getData();
                cacheAvailableRoomsRange(cacheKey, arrivalDate, departureDate, prefix, data);
                updateAvailableRoomsSheetStatus(cacheKey, System.currentTimeMillis(), false);
                availableRoomsRangeLiveData.setValue(new UiEvent<>(data));
                AppDiagnostics.logUiUpdated(
                        "available_rooms_range",
                        "network",
                        System.currentTimeMillis() - actionStartedAtMillis
                );
                logAvailableRoomsRangeNetworkResponse(
                        cacheKey,
                        response.code(),
                        requestStartedAtMillis
                );
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<AvailableRoomsRangeResponse>> call,
                    @NonNull Throwable t
            ) {
                if (!isCurrentAvailableRoomsRangeCall(call, cacheKey)) {
                    return;
                }

                availableRoomsRangeCall = null;
                if (!call.isCanceled()) {
                    if (shouldRetry(retryAttempt)) {
                        scheduleAvailableRoomsRangeRetry(
                                arrivalDate,
                                departureDate,
                                prefix,
                                cacheKey,
                                retryAttempt + 1,
                                showLoading,
                                quietFailure,
                                actionStartedAtMillis
                        );
                        return;
                    }

                    availableRoomsLoadingLiveData.setValue(false);
                    AppDiagnostics.logApiFailure(
                            "available_rooms_range",
                            ApiErrorUtils.messageFromThrowable(t),
                            t
                    );
                    if (quietFailure) {
                        keepCachedAvailableRoomsRangeAfterFailure(
                                cacheKey,
                                ApiErrorUtils.cachedDataMessageForThrowable(t)
                        );
                    } else {
                        networkBannerLiveData.setValue(new UiEvent<>(true));
                    }
                    logAvailableRoomsRangeNetworkResponse(cacheKey, 0, requestStartedAtMillis);
                }
            }
        });
    }

    public void deleteBooking(int bookingId) {
        if (deleteBookingCall != null) {
            toastLiveData.setValue(new UiEvent<>(MESSAGE_DELETE_IN_FLIGHT));
            return;
        }

        Call<ApiResponse<BookingActionData>> request =
                availabilityRepository.deleteBooking(bookingId);
        deleteBookingCall = request;
        request.enqueue(new Callback<ApiResponse<BookingActionData>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<BookingActionData>> call,
                    @NonNull Response<ApiResponse<BookingActionData>> response
            ) {
                if (call != deleteBookingCall) {
                    return;
                }

                deleteBookingCall = null;
                if (!response.isSuccessful()
                        || response.body() == null
                        || !response.body().isSuccess()) {
                    String message = ApiErrorUtils.messageFromResponse(
                            response,
                            MESSAGE_DELETE_FAILED
                    );
                    AppDiagnostics.logBookingMutationFailure("delete", bookingId, message);
                    toastLiveData.setValue(new UiEvent<>(message));
                    return;
                }

                networkBannerLiveData.setValue(new UiEvent<>(false));
                clearAvailabilityCaches();
                availabilityRepository.clearBookingFirstPageCachesForMutation();
                forceNextAvailabilityNetworkRefresh = true;
                deleteBookingResultLiveData.setValue(new UiEvent<>(
                        new DeleteBookingResult(
                                bookingId,
                                response.body().getSafeMessage()
                        )
                ));
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<BookingActionData>> call,
                    @NonNull Throwable t
            ) {
                if (call != deleteBookingCall) {
                    return;
                }

                deleteBookingCall = null;
                if (!call.isCanceled()) {
                    String message = ApiErrorUtils.networkMessage();
                    AppDiagnostics.logBookingMutationFailure("delete", bookingId, message, t);
                    networkBannerLiveData.setValue(new UiEvent<>(true));
                    toastLiveData.setValue(new UiEvent<>(message));
                }
            }
        });
    }

    private boolean isCurrentAvailabilityCall(
            Call<ApiResponse<RoomAvailabilityResponse>> call,
            int month,
            int year
    ) {
        return call == availabilityCall
                && month == requestedMonth
                && year == requestedYear;
    }

    private boolean isCurrentPrefixCall(
            Call<?> callbackCall,
            Call<?> trackedCall,
            String requestedPrefix
    ) {
        return callbackCall == trackedCall && requestedPrefix != null;
    }

    private boolean isCurrentAvailableRoomsCall(
            Call<ApiResponse<AvailableRoomsResponse>> callbackCall,
            String cacheKey
    ) {
        return callbackCall == availableRoomsCall
                && cacheKey.equals(requestedAvailableRoomsCacheKey);
    }

    private boolean isCurrentAvailableRoomsRangeCall(
            Call<ApiResponse<AvailableRoomsRangeResponse>> callbackCall,
            String cacheKey
    ) {
        return callbackCall == availableRoomsRangeCall
                && cacheKey.equals(requestedAvailableRoomsRangeCacheKey);
    }

    private boolean shouldRetry(int retryAttempt) {
        return retryAttempt < MAX_NETWORK_RETRIES;
    }

    private boolean shouldRetry(int retryAttempt, int httpCode) {
        return shouldRetry(retryAttempt)
                && (httpCode == 500 || httpCode == 502 || httpCode == 503 || httpCode == 504);
    }

    private void scheduleAvailabilityRetry(
            int month,
            int year,
            int retryAttempt,
            boolean showLoading,
            boolean quietFailure,
            long actionStartedAtMillis
    ) {
        cancelPendingAvailabilityRetry();
        long delayMillis = RETRY_DELAY_MS * retryAttempt;
        pendingAvailabilityRetry = () -> {
            pendingAvailabilityRetry = null;
            loadAvailabilityInternal(
                    month,
                    year,
                    retryAttempt,
                    showLoading,
                    quietFailure,
                    actionStartedAtMillis
            );
        };
        retryHandler.postDelayed(pendingAvailabilityRetry, delayMillis);
    }

    private void scheduleAvailableRoomsRetry(
            String date,
            String prefix,
            String cacheKey,
            int retryAttempt,
            boolean showLoading,
            boolean quietFailure,
            long actionStartedAtMillis
    ) {
        cancelPendingAvailableRoomsRetry();
        long delayMillis = RETRY_DELAY_MS * retryAttempt;
        pendingAvailableRoomsRetry = () -> {
            pendingAvailableRoomsRetry = null;
            loadAvailableRoomsForDateInternal(
                    date,
                    prefix,
                    cacheKey,
                    retryAttempt,
                    showLoading,
                    quietFailure,
                    actionStartedAtMillis
            );
        };
        retryHandler.postDelayed(pendingAvailableRoomsRetry, delayMillis);
    }

    private void scheduleAvailableRoomsRangeRetry(
            String arrivalDate,
            String departureDate,
            String prefix,
            String cacheKey,
            int retryAttempt,
            boolean showLoading,
            boolean quietFailure,
            long actionStartedAtMillis
    ) {
        cancelPendingAvailableRoomsRangeRetry();
        long delayMillis = RETRY_DELAY_MS * retryAttempt;
        pendingAvailableRoomsRangeRetry = () -> {
            pendingAvailableRoomsRangeRetry = null;
            loadAvailableRoomsForDateRangeInternal(
                    arrivalDate,
                    departureDate,
                    prefix,
                    cacheKey,
                    retryAttempt,
                    showLoading,
                    quietFailure,
                    actionStartedAtMillis
            );
        };
        retryHandler.postDelayed(pendingAvailableRoomsRangeRetry, delayMillis);
    }

    private void cancelPendingAvailabilityRetry() {
        if (pendingAvailabilityRetry != null) {
            retryHandler.removeCallbacks(pendingAvailabilityRetry);
            pendingAvailabilityRetry = null;
        }
    }

    private void cancelPendingAvailableRoomsRetry() {
        if (pendingAvailableRoomsRetry != null) {
            retryHandler.removeCallbacks(pendingAvailableRoomsRetry);
            pendingAvailableRoomsRetry = null;
        }
    }

    private void cancelPendingAvailableRoomsRangeRetry() {
        if (pendingAvailableRoomsRangeRetry != null) {
            retryHandler.removeCallbacks(pendingAvailableRoomsRangeRetry);
            pendingAvailableRoomsRangeRetry = null;
        }
    }

    private void cancelAvailableRoomsRequests() {
        cancelCall(availableRoomsCall);
        cancelCall(availableRoomsRangeCall);
        availableRoomsCall = null;
        availableRoomsRangeCall = null;
    }

    private void cacheAvailability(
            int month,
            int year,
            List<RoomAvailabilityGroup> groups
    ) {
        String cacheKey = AvailabilityRepository.calendarAvailabilityCacheKey(month, year);
        long updatedAtMillis = System.currentTimeMillis();
        currentAvailabilityUpdatedAtMillis = updatedAtMillis;
        List<RoomAvailabilityGroup> cachedGroups = NullSafeCollections.copyWithoutNulls(groups);
        synchronized (CACHE_LOCK) {
            availabilityCache.put(
                    cacheKey,
                    new CachedAvailability(cachedGroups, updatedAtMillis)
            );
        }
        availabilityRepository.saveCachedCalendarAvailability(month, year, cachedGroups);
    }

    private CachedAvailability getCachedAvailability(int month, int year) {
        synchronized (CACHE_LOCK) {
            CachedAvailability availability =
                    availabilityCache.get(AvailabilityRepository.calendarAvailabilityCacheKey(
                            month,
                            year
                    ));
            if (availability == null) {
                return null;
            }

            return new CachedAvailability(
                    availability.groups,
                    availability.updatedAtMillis
            );
        }
    }

    private void handleCachedAvailabilityResult(
            CacheReadResult<List<RoomAvailabilityGroup>> result,
            int month,
            int year,
            long actionStartedAtMillis
    ) {
        if (result.isHit()) {
            List<RoomAvailabilityGroup> groups = NullSafeCollections.copyWithoutNulls(
                    result.getValue()
            );
            boolean fresh = result.isFresh();
            synchronized (CACHE_LOCK) {
                availabilityCache.put(
                        result.getKey(),
                        new CachedAvailability(groups, result.getUpdatedAtMillis())
                );
            }
            showCachedAvailability(
                    groups,
                    "disk",
                    result.getUpdatedAtMillis(),
                    !fresh,
                    actionStartedAtMillis
            );

            if (fresh) {
                return;
            }

            loadAvailabilityInternal(month, year, 0, false, true, actionStartedAtMillis);
            return;
        }

        loadAvailabilityInternal(month, year, 0, !hasVisibleAvailability(), false,
                actionStartedAtMillis);
    }

    private void handleCachedAvailableRoomsResult(
            CacheReadResult<AvailableRoomsResponse> result,
            String date,
            String prefix,
            long actionStartedAtMillis
    ) {
        if (result.isHit() && result.getValue() != null) {
            boolean fresh = result.isFresh();
            cacheAvailableRoomsInMemory(
                    result.getKey(),
                    result.getValue(),
                    result.getUpdatedAtMillis()
            );
            updateAvailableRoomsSheetStatus(result.getKey(), result.getUpdatedAtMillis(), !fresh);
            showCachedAvailableRooms(result.getValue(), "disk", actionStartedAtMillis);

            if (fresh) {
                return;
            }

            loadAvailableRoomsForDateInternal(
                    date,
                    prefix,
                    result.getKey(),
                    0,
                    false,
                    true,
                    actionStartedAtMillis
            );
            return;
        }

        loadAvailableRoomsForDateInternal(
                date,
                prefix,
                result.getKey(),
                0,
                true,
                false,
                actionStartedAtMillis
        );
    }

    private void handleCachedAvailableRoomsRangeResult(
            CacheReadResult<AvailableRoomsRangeResponse> result,
            String arrivalDate,
            String departureDate,
            String prefix,
            long actionStartedAtMillis
    ) {
        if (result.isHit() && result.getValue() != null) {
            boolean fresh = result.isFresh();
            cacheAvailableRoomsRangeInMemory(
                    result.getKey(),
                    result.getValue(),
                    result.getUpdatedAtMillis()
            );
            updateAvailableRoomsSheetStatus(result.getKey(), result.getUpdatedAtMillis(), !fresh);
            showCachedAvailableRoomsRange(result.getValue(), "disk", actionStartedAtMillis);

            if (fresh) {
                return;
            }

            loadAvailableRoomsForDateRangeInternal(
                    arrivalDate,
                    departureDate,
                    prefix,
                    result.getKey(),
                    0,
                    false,
                    true,
                    actionStartedAtMillis
            );
            return;
        }

        loadAvailableRoomsForDateRangeInternal(
                arrivalDate,
                departureDate,
                prefix,
                result.getKey(),
                0,
                true,
                false,
                actionStartedAtMillis
        );
    }

    private void showCachedAvailability(
            List<RoomAvailabilityGroup> groups,
            String source,
            long updatedAtMillis,
            boolean refreshing,
            long actionStartedAtMillis
    ) {
        availabilityLoadingLiveData.setValue(false);
        networkBannerLiveData.setValue(new UiEvent<>(false));
        currentAvailabilityUpdatedAtMillis = updatedAtMillis;
        hasLoadedAvailability = true;
        updateAvailabilityStatus(refreshing
                ? SyncStatusFormatter.SHOWING_CACHED_REFRESHING
                : SyncStatusFormatter.lastUpdated(updatedAtMillis));
        availabilityGroupsLiveData.setValue(NullSafeCollections.copyWithoutNulls(groups));
        AppDiagnostics.logUiUpdated(
                "availability_calendar",
                "cache_" + source,
                System.currentTimeMillis() - actionStartedAtMillis
        );
    }

    private void showCachedAvailableRooms(
            AvailableRoomsResponse response,
            String source,
            long actionStartedAtMillis
    ) {
        availableRoomsLoadingLiveData.setValue(false);
        networkBannerLiveData.setValue(new UiEvent<>(false));
        availableRoomsLiveData.setValue(new UiEvent<>(response));
        AppDiagnostics.logUiUpdated(
                "available_rooms",
                "cache_" + source,
                System.currentTimeMillis() - actionStartedAtMillis
        );
    }

    private void showCachedAvailableRoomsRange(
            AvailableRoomsRangeResponse response,
            String source,
            long actionStartedAtMillis
    ) {
        availableRoomsLoadingLiveData.setValue(false);
        networkBannerLiveData.setValue(new UiEvent<>(false));
        availableRoomsRangeLiveData.setValue(new UiEvent<>(response));
        AppDiagnostics.logUiUpdated(
                "available_rooms_range",
                "cache_" + source,
                System.currentTimeMillis() - actionStartedAtMillis
        );
    }

    private boolean hasVisibleAvailability() {
        List<RoomAvailabilityGroup> groups = availabilityGroupsLiveData.getValue();
        return groups != null;
    }

    private boolean isFresh(long updatedAtMillis, long ttlMillis) {
        return CachePolicy.isFresh(updatedAtMillis, ttlMillis, System.currentTimeMillis());
    }

    private void updateLastUpdatedAvailabilityStatus() {
        updateAvailabilityStatus(
                SyncStatusFormatter.lastUpdated(currentAvailabilityUpdatedAtMillis),
                false
        );
    }

    private void keepCachedAvailabilityAfterFailure(String message) {
        updateAvailabilityStatus(message, true);
        AppDiagnostics.logEvent("availability_calendar_kept_cached_after_failure");
    }

    private void updateAvailabilityStatus(String message) {
        updateAvailabilityStatus(message, false);
    }

    private void updateAvailabilityStatus(String message, boolean failureState) {
        availabilityStatusShowsFailure = failureState;
        availabilityStatusLiveData.setValue(message != null ? message : "");
    }

    public String getAvailableRoomsSheetStatus(AvailableRoomsResponse response) {
        if (response == null) {
            return SyncStatusFormatter.FINAL_BOOKING_VERIFIED;
        }

        return getSheetStatus(AvailabilityRepository.availableRoomsCacheKey(
                response.getPrefix(),
                response.getDate()
        ));
    }

    public String getAvailableRoomsRangeSheetStatus(AvailableRoomsRangeResponse response) {
        if (response == null) {
            return SyncStatusFormatter.FINAL_BOOKING_VERIFIED;
        }

        return getSheetStatus(AvailabilityRepository.availableRoomsRangeCacheKey(
                response.getPrefix(),
                response.getArrivalDate(),
                response.getDepartureDate()
        ));
    }

    private String getSheetStatus(String cacheKey) {
        AvailableRoomsSheetStatus status = availableRoomsSheetStatus.get(cacheKey);
        if (status == null) {
            return SyncStatusFormatter.FINAL_BOOKING_VERIFIED;
        }

        if (status.failureMessage != null && !status.failureMessage.trim().isEmpty()) {
            return status.failureMessage + "\n" + SyncStatusFormatter.FINAL_BOOKING_VERIFIED;
        }

        if (status.refreshing) {
            return SyncStatusFormatter.cachedAvailabilityRefreshing();
        }

        return SyncStatusFormatter.availabilityDecisionText(status.updatedAtMillis);
    }

    private void updateAvailableRoomsSheetStatus(
            String cacheKey,
            long updatedAtMillis,
            boolean refreshing
    ) {
        availableRoomsSheetStatus.put(
                cacheKey,
                new AvailableRoomsSheetStatus(updatedAtMillis, refreshing, "")
        );
    }

    private void keepCachedAvailableRoomsAfterFailure(String cacheKey, String message) {
        availableRoomsSheetStatus.put(
                cacheKey,
                new AvailableRoomsSheetStatus(0L, false, fallbackSavedDataMessage(message))
        );

        CachedAvailableRooms cachedResponse = getCachedAvailableRooms(cacheKey);
        if (cachedResponse != null && cachedResponse.response != null) {
            availableRoomsLiveData.setValue(new UiEvent<>(cachedResponse.response));
        }
        AppDiagnostics.logEvent("available_rooms_kept_cached_after_failure");
    }

    private void keepCachedAvailableRoomsRangeAfterFailure(String cacheKey, String message) {
        availableRoomsSheetStatus.put(
                cacheKey,
                new AvailableRoomsSheetStatus(0L, false, fallbackSavedDataMessage(message))
        );

        CachedAvailableRoomsRange cachedResponse = getCachedAvailableRoomsRange(cacheKey);
        if (cachedResponse != null && cachedResponse.response != null) {
            availableRoomsRangeLiveData.setValue(new UiEvent<>(cachedResponse.response));
        }
        AppDiagnostics.logEvent("available_rooms_range_kept_cached_after_failure");
    }

    private String fallbackSavedDataMessage(String message) {
        return message != null && !message.trim().isEmpty()
                ? message.trim()
                : SyncStatusFormatter.OFFLINE_SAVED_DATA;
    }

    private void clearAvailabilityCaches() {
        synchronized (CACHE_LOCK) {
            availabilityCache.clear();
            availableRoomsCache.clear();
            availableRoomsRangeCache.clear();
            availableRoomsSheetStatus.clear();
        }
        availabilityRepository.clearAvailabilityCaches();
        observedCacheInvalidationVersion = AvailabilityRepository.getCacheInvalidationVersion();
    }

    private boolean syncCacheInvalidationVersion() {
        int currentVersion = AvailabilityRepository.getCacheInvalidationVersion();
        if (currentVersion == observedCacheInvalidationVersion) {
            return false;
        }

        synchronized (CACHE_LOCK) {
            availabilityCache.clear();
            availableRoomsCache.clear();
            availableRoomsRangeCache.clear();
            availableRoomsSheetStatus.clear();
        }
        availabilityCacheGeneration++;
        availableRoomsCacheGeneration++;
        availableRoomsRangeCacheGeneration++;
        observedCacheInvalidationVersion = currentVersion;
        AppDiagnostics.logCacheInvalidated("availability_memory");
        return true;
    }

    private void cacheAvailableRooms(
            String cacheKey,
            String date,
            String prefix,
            AvailableRoomsResponse response
    ) {
        long updatedAtMillis = System.currentTimeMillis();
        cacheAvailableRoomsInMemory(cacheKey, response, updatedAtMillis);
        availabilityRepository.saveCachedAvailableRoomsByDate(date, prefix, response);
    }

    private void cacheAvailableRoomsInMemory(
            String cacheKey,
            AvailableRoomsResponse response,
            long updatedAtMillis
    ) {
        synchronized (CACHE_LOCK) {
            availableRoomsCache.put(cacheKey, new CachedAvailableRooms(
                    response,
                    updatedAtMillis
            ));
        }
    }

    private CachedAvailableRooms getCachedAvailableRooms(String cacheKey) {
        synchronized (CACHE_LOCK) {
            CachedAvailableRooms cachedResponse = availableRoomsCache.get(cacheKey);
            if (cachedResponse == null) {
                return null;
            }

            return new CachedAvailableRooms(
                    cachedResponse.response,
                    cachedResponse.updatedAtMillis
            );
        }
    }

    private void cacheAvailableRoomsRange(
            String cacheKey,
            String arrivalDate,
            String departureDate,
            String prefix,
            AvailableRoomsRangeResponse response
    ) {
        long updatedAtMillis = System.currentTimeMillis();
        cacheAvailableRoomsRangeInMemory(cacheKey, response, updatedAtMillis);
        availabilityRepository.saveCachedAvailableRoomsByDateRange(
                arrivalDate,
                departureDate,
                prefix,
                response
        );
    }

    private void cacheAvailableRoomsRangeInMemory(
            String cacheKey,
            AvailableRoomsRangeResponse response,
            long updatedAtMillis
    ) {
        synchronized (CACHE_LOCK) {
            availableRoomsRangeCache.put(cacheKey, new CachedAvailableRoomsRange(
                    response,
                    updatedAtMillis
            ));
        }
    }

    private CachedAvailableRoomsRange getCachedAvailableRoomsRange(String cacheKey) {
        synchronized (CACHE_LOCK) {
            CachedAvailableRoomsRange cachedResponse =
                    availableRoomsRangeCache.get(cacheKey);
            if (cachedResponse == null) {
                return null;
            }

            return new CachedAvailableRoomsRange(
                    cachedResponse.response,
                    cachedResponse.updatedAtMillis
            );
        }
    }

    private void logAvailabilityNetworkResponse(
            String cacheKey,
            int httpCode,
            long requestStartedAtMillis
    ) {
        AppDiagnostics.logNetworkResponse(
                "availability_calendar",
                cacheKey,
                httpCode,
                System.currentTimeMillis() - requestStartedAtMillis
        );
    }

    private void logAvailableRoomsNetworkResponse(
            String cacheKey,
            int httpCode,
            long requestStartedAtMillis
    ) {
        AppDiagnostics.logNetworkResponse(
                "available_rooms",
                cacheKey,
                httpCode,
                System.currentTimeMillis() - requestStartedAtMillis
        );
    }

    private void logAvailableRoomsRangeNetworkResponse(
            String cacheKey,
            int httpCode,
            long requestStartedAtMillis
    ) {
        AppDiagnostics.logNetworkResponse(
                "available_rooms_range",
                cacheKey,
                httpCode,
                System.currentTimeMillis() - requestStartedAtMillis
        );
    }

    private boolean isValidAvailabilityResponse(
            Response<ApiResponse<RoomAvailabilityResponse>> response
    ) {
        return response.isSuccessful()
                && response.body() != null
                && response.body().isSuccess()
                && response.body().getData() != null;
    }

    private boolean isValidAvailabilityDetailsResponse(
            Response<ApiResponse<RoomAvailabilityDetailsResponse>> response
    ) {
        return response.isSuccessful()
                && response.body() != null
                && response.body().isSuccess()
                && response.body().getData() != null;
    }

    private boolean isValidAvailableRoomsResponse(
            Response<ApiResponse<AvailableRoomsResponse>> response
    ) {
        return response.isSuccessful()
                && response.body() != null
                && response.body().isSuccess()
                && response.body().getData() != null;
    }

    private boolean isValidAvailableRoomsRangeResponse(
            Response<ApiResponse<AvailableRoomsRangeResponse>> response
    ) {
        return response.isSuccessful()
                && response.body() != null
                && response.body().isSuccess()
                && response.body().getData() != null;
    }

    private void cancelCall(Call<?> call) {
        if (call != null && !call.isCanceled()) {
            call.cancel();
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    protected void onCleared() {
        cancelPendingAvailabilityRetry();
        cancelPendingAvailableRoomsRetry();
        cancelPendingAvailableRoomsRangeRetry();
        cancelCall(availabilityCall);
        cancelCall(availabilityDetailsCall);
        cancelCall(availableRoomsCall);
        cancelCall(availableRoomsRangeCall);
        cancelCall(deleteBookingCall);
        super.onCleared();
    }
}
