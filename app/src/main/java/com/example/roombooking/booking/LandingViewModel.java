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
    private static final String MESSAGE_DELETE_FAILED =
            "Failed to delete booking.";
    private static final String MESSAGE_DELETE_IN_FLIGHT =
            "Deletion is already in progress.";
    private static final int MAX_NETWORK_RETRIES = 0;
    private static final long RETRY_DELAY_MS = 700L;
    private static final Object CACHE_LOCK = new Object();
    private static final Map<String, CachedAvailability> availabilityCache =
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
    private boolean forceNextAvailabilityNetworkRefresh = false;

    private int requestedMonth = -1;
    private int requestedYear = -1;
    private String requestedDetailsPrefix = "";
    private String requestedAvailableRoomsPrefix = "";
    private String requestedRangePrefix = "";

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
            showCachedAvailability(memoryAvailability.groups, "memory", actionStartedAtMillis);
            if (isFresh(
                    memoryAvailability.updatedAtMillis,
                    CachePolicy.CALENDAR_AVAILABILITY_TTL_MS
            )) {
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
                        networkBannerLiveData.setValue(new UiEvent<>(true));
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
                            ApiErrorUtils.networkMessage(),
                            t
                    );
                    networkBannerLiveData.setValue(new UiEvent<>(true));
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
        cancelAvailableRoomsRequests();
        cancelPendingAvailableRoomsRetry();
        cancelPendingAvailableRoomsRangeRetry();
        requestedAvailableRoomsPrefix = safe(prefix);
        loadAvailableRoomsForDateInternal(date, requestedAvailableRoomsPrefix, 0);
    }

    private void loadAvailableRoomsForDateInternal(
            String date,
            String prefix,
            int retryAttempt
    ) {
        availableRoomsLoadingLiveData.setValue(true);

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
                if (!isCurrentPrefixCall(call, availableRoomsCall, requestedAvailableRoomsPrefix)) {
                    return;
                }

                availableRoomsCall = null;

                if (!isValidAvailableRoomsResponse(response)) {
                    if (shouldRetry(retryAttempt, response.code())) {
                        scheduleAvailableRoomsRetry(date, prefix, retryAttempt + 1);
                        return;
                    }

                    availableRoomsLoadingLiveData.setValue(false);
                    String message = ApiErrorUtils.messageFromResponse(
                            response,
                            MESSAGE_AVAILABLE_ROOMS_FAILED
                    );
                    AppDiagnostics.logApiFailure("available_rooms", message, null);
                    toastLiveData.setValue(new UiEvent<>(message));
                    return;
                }

                availableRoomsLoadingLiveData.setValue(false);
                networkBannerLiveData.setValue(new UiEvent<>(false));
                availableRoomsLiveData.setValue(new UiEvent<>(response.body().getData()));
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<AvailableRoomsResponse>> call,
                    @NonNull Throwable t
            ) {
                if (!isCurrentPrefixCall(call, availableRoomsCall, requestedAvailableRoomsPrefix)) {
                    return;
                }

                availableRoomsCall = null;
                if (!call.isCanceled()) {
                    if (shouldRetry(retryAttempt)) {
                        scheduleAvailableRoomsRetry(date, prefix, retryAttempt + 1);
                        return;
                    }

                    availableRoomsLoadingLiveData.setValue(false);
                    AppDiagnostics.logApiFailure(
                            "available_rooms",
                            ApiErrorUtils.networkMessage(),
                            t
                    );
                    networkBannerLiveData.setValue(new UiEvent<>(true));
                }
            }
        });
    }

    public void loadAvailableRoomsForDateRange(
            String arrivalDate,
            String departureDate,
            String prefix
    ) {
        cancelAvailableRoomsRequests();
        cancelPendingAvailableRoomsRetry();
        cancelPendingAvailableRoomsRangeRetry();
        requestedRangePrefix = safe(prefix);
        loadAvailableRoomsForDateRangeInternal(
                arrivalDate,
                departureDate,
                requestedRangePrefix,
                0
        );
    }

    private void loadAvailableRoomsForDateRangeInternal(
            String arrivalDate,
            String departureDate,
            String prefix,
            int retryAttempt
    ) {
        availableRoomsLoadingLiveData.setValue(true);

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
                if (!isCurrentPrefixCall(call, availableRoomsRangeCall, requestedRangePrefix)) {
                    return;
                }

                availableRoomsRangeCall = null;

                if (!isValidAvailableRoomsRangeResponse(response)) {
                    if (shouldRetry(retryAttempt, response.code())) {
                        scheduleAvailableRoomsRangeRetry(
                                arrivalDate,
                                departureDate,
                                prefix,
                                retryAttempt + 1
                        );
                        return;
                    }

                    availableRoomsLoadingLiveData.setValue(false);
                    String message = ApiErrorUtils.messageFromResponse(
                            response,
                            MESSAGE_AVAILABLE_ROOMS_FAILED
                    );
                    AppDiagnostics.logApiFailure("available_rooms_range", message, null);
                    toastLiveData.setValue(new UiEvent<>(message));
                    return;
                }

                availableRoomsLoadingLiveData.setValue(false);
                networkBannerLiveData.setValue(new UiEvent<>(false));
                availableRoomsRangeLiveData.setValue(new UiEvent<>(response.body().getData()));
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<AvailableRoomsRangeResponse>> call,
                    @NonNull Throwable t
            ) {
                if (!isCurrentPrefixCall(call, availableRoomsRangeCall, requestedRangePrefix)) {
                    return;
                }

                availableRoomsRangeCall = null;
                if (!call.isCanceled()) {
                    if (shouldRetry(retryAttempt)) {
                        scheduleAvailableRoomsRangeRetry(
                                arrivalDate,
                                departureDate,
                                prefix,
                                retryAttempt + 1
                        );
                        return;
                    }

                    availableRoomsLoadingLiveData.setValue(false);
                    AppDiagnostics.logApiFailure(
                            "available_rooms_range",
                            ApiErrorUtils.networkMessage(),
                            t
                    );
                    networkBannerLiveData.setValue(new UiEvent<>(true));
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
            int retryAttempt
    ) {
        cancelPendingAvailableRoomsRetry();
        long delayMillis = RETRY_DELAY_MS * retryAttempt;
        pendingAvailableRoomsRetry = () -> {
            pendingAvailableRoomsRetry = null;
            loadAvailableRoomsForDateInternal(date, prefix, retryAttempt);
        };
        retryHandler.postDelayed(pendingAvailableRoomsRetry, delayMillis);
    }

    private void scheduleAvailableRoomsRangeRetry(
            String arrivalDate,
            String departureDate,
            String prefix,
            int retryAttempt
    ) {
        cancelPendingAvailableRoomsRangeRetry();
        long delayMillis = RETRY_DELAY_MS * retryAttempt;
        pendingAvailableRoomsRangeRetry = () -> {
            pendingAvailableRoomsRangeRetry = null;
            loadAvailableRoomsForDateRangeInternal(
                    arrivalDate,
                    departureDate,
                    prefix,
                    retryAttempt
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
            synchronized (CACHE_LOCK) {
                availabilityCache.put(
                        result.getKey(),
                        new CachedAvailability(groups, result.getUpdatedAtMillis())
                );
            }
            showCachedAvailability(groups, "disk", actionStartedAtMillis);

            if (result.isFresh()) {
                return;
            }

            loadAvailabilityInternal(month, year, 0, false, true, actionStartedAtMillis);
            return;
        }

        loadAvailabilityInternal(month, year, 0, !hasVisibleAvailability(), false,
                actionStartedAtMillis);
    }

    private void showCachedAvailability(
            List<RoomAvailabilityGroup> groups,
            String source,
            long actionStartedAtMillis
    ) {
        availabilityLoadingLiveData.setValue(false);
        networkBannerLiveData.setValue(new UiEvent<>(false));
        availabilityGroupsLiveData.setValue(NullSafeCollections.copyWithoutNulls(groups));
        AppDiagnostics.logUiUpdated(
                "availability_calendar",
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

    private void clearAvailabilityCaches() {
        synchronized (CACHE_LOCK) {
            availabilityCache.clear();
        }
        availabilityRepository.clearCalendarAvailabilityCache();
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
