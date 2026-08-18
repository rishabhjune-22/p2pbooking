package com.example.roombooking.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.roombooking.api.ApiService;
import com.example.roombooking.api.RetrofitClient;
import com.example.roombooking.auth.AuthSessionManager;
import com.example.roombooking.booking.AvailabilityRepository;
import com.example.roombooking.booking.RoomAvailabilityGroup;
import com.example.roombooking.booking.RoomAvailabilityResponse;
import com.example.roombooking.booking.BookingRepository;
import com.example.roombooking.model.booking.BookingItem;
import com.example.roombooking.model.booking.BookingStatus;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.model.common.PaginatedData;
import com.example.roombooking.model.room.RoomInventory;
import com.example.roombooking.model.room.RoomPrefix;
import com.example.roombooking.model.room.RoomItem;
import com.example.roombooking.requester.RequesterAvailabilityRepository;
import com.example.roombooking.room.RoomMapper;
import com.example.roombooking.room.RoomMemoryCache;
import com.example.roombooking.room.RoomRepository;
import com.example.roombooking.room.local.AppDatabase;
import com.example.roombooking.room.local.CacheEntryDao;
import com.example.roombooking.room.local.CacheEntryEntity;
import com.example.roombooking.room.local.RoomDao;
import com.example.roombooking.utils.AppDiagnostics;
import com.example.roombooking.utils.NullSafeCollections;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import retrofit2.Response;

public class LightBackgroundSyncWorker extends Worker {

    private static final int FIRST_PAGE = 1;

    private final ApiService apiService;
    private final AppDatabase database;
    private final RoomDao roomDao;
    private final CacheEntryDao cacheEntryDao;
    private final Gson gson;

    public LightBackgroundSyncWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams
    ) {
        super(context, workerParams);

        Context appContext = context.getApplicationContext();
        apiService = RetrofitClient.getApiService(appContext);
        database = AppDatabase.getInstance(appContext);
        roomDao = database.roomDao();
        cacheEntryDao = database.cacheEntryDao();
        gson = new Gson();
    }

    @NonNull
    @Override
    public Result doWork() {
        AuthSessionManager sessionManager = new AuthSessionManager(getApplicationContext());
        if (!sessionManager.isLoggedIn() || !sessionManager.isApproved()) {
            AppDiagnostics.logEvent("background_sync_worker_skipped_not_authenticated");
            return Result.success();
        }

        if (sessionManager.isRequester()) {
            return doRequesterWork(sessionManager);
        }

        if (!sessionManager.isAdminLike()) {
            AppDiagnostics.logEvent("background_sync_worker_skipped_unsupported_role");
            return Result.success();
        }

        AppDiagnostics.logEvent("background_sync_started role=" + sessionManager.getUserRole());

        SyncOutcome roomsOutcome = syncRooms();
        SyncOutcome activeBookingsOutcome = syncBookingPageOne(BookingStatus.ACTIVE);
        SyncOutcome expiredBookingsOutcome = syncBookingPageOne(BookingStatus.EXPIRED);
        SyncOutcome calendarOutcome = syncCurrentMonthAvailability();

        SyncSummary summary = new SyncSummary();
        summary.add(roomsOutcome);
        summary.add(activeBookingsOutcome);
        summary.add(expiredBookingsOutcome);
        summary.add(calendarOutcome);

        AppDiagnostics.logEvent(
                "background_sync_finished"
                        + " successCount=" + summary.successCount
                        + " retryableFailureCount=" + summary.retryableFailureCount
                        + " permanentFailureCount=" + summary.permanentFailureCount
        );

        if (summary.successCount > 0) {
            return Result.success();
        }

        if (summary.retryableFailureCount > 0) {
            return Result.retry();
        }

        return Result.failure();
    }

    private Result doRequesterWork(AuthSessionManager sessionManager) {
        AppDiagnostics.logEvent("background_sync_requester_started");

        SyncSummary summary = new SyncSummary();
        for (String prefix : RoomPrefix.displayOrder()) {
            summary.add(syncRequesterCurrentMonthAvailability(
                    sessionManager.getUserId(),
                    prefix
            ));
        }

        AppDiagnostics.logEvent(
                "background_sync_requester_finished"
                        + " successCount=" + summary.successCount
                        + " retryableFailureCount=" + summary.retryableFailureCount
                        + " permanentFailureCount=" + summary.permanentFailureCount
        );

        if (summary.successCount > 0) {
            return Result.success();
        }

        if (summary.retryableFailureCount > 0) {
            return Result.retry();
        }

        return Result.failure();
    }

    private SyncOutcome syncRooms() {
        String operation = "background_sync_rooms";
        String failureEvent = "background_sync_rooms_failure";
        String cacheKey = RoomRepository.ROOM_CACHE_KEY;
        long startedAtMillis = System.currentTimeMillis();
        AppDiagnostics.logNetworkStart(operation, cacheKey);

        try {
            Response<ApiResponse<PaginatedData<RoomItem>>> response =
                    apiService.getRooms(FIRST_PAGE, RoomRepository.ROOM_PAGE_SIZE).execute();
            AppDiagnostics.logNetworkResponse(
                    operation,
                    cacheKey,
                    response.code(),
                    System.currentTimeMillis() - startedAtMillis
            );

            if (!isValidPageResponse(response)) {
                logSyncFailure(failureEvent, operation, "HTTP " + response.code(), null);
                return outcomeForHttpCode(response.code());
            }

            List<RoomItem> rooms = RoomInventory.visibleRooms(
                    response.body().getData().getResults()
            );
            saveRooms(rooms);
            AppDiagnostics.logEvent("background_sync_rooms_success count=" + rooms.size());
            return SyncOutcome.success();
        } catch (IOException exception) {
            return handleException(failureEvent, operation, exception, true);
        } catch (RuntimeException exception) {
            return handleException(failureEvent, operation, exception, false);
        }
    }

    private SyncOutcome syncBookingPageOne(String status) {
        String normalizedStatus = safe(status);
        String operation = "background_sync_" + normalizedStatus + "_bookings";
        String successEvent = "background_sync_" + normalizedStatus + "_bookings_success";
        String failureEvent = "background_sync_" + normalizedStatus + "_bookings_failure";
        String cacheKey = bookingPageOneCacheKey(status);
        long startedAtMillis = System.currentTimeMillis();
        AppDiagnostics.logNetworkStart(operation, cacheKey);

        try {
            Response<ApiResponse<PaginatedData<BookingItem>>> response =
                    apiService.getBookings(FIRST_PAGE, null, null, null, status).execute();
            AppDiagnostics.logNetworkResponse(
                    operation,
                    cacheKey,
                    response.code(),
                    System.currentTimeMillis() - startedAtMillis
            );

            if (!isValidPageResponse(response)) {
                logSyncFailure(failureEvent, operation, "HTTP " + response.code(), null);
                return outcomeForHttpCode(response.code());
            }

            List<BookingItem> bookings = NullSafeCollections.copyWithoutNulls(
                    response.body().getData().getResults()
            );
            writeJsonCache(cacheKey, bookings);
            AppDiagnostics.logEvent(successEvent + " count=" + bookings.size());
            return SyncOutcome.success();
        } catch (IOException exception) {
            return handleException(failureEvent, operation, exception, true);
        } catch (RuntimeException exception) {
            return handleException(failureEvent, operation, exception, false);
        }
    }

    private SyncOutcome syncCurrentMonthAvailability() {
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);
        String cacheKey = calendarAvailabilityCacheKey(calendar);
        String operation = "background_sync_calendar";
        String failureEvent = "background_sync_calendar_failure";
        long startedAtMillis = System.currentTimeMillis();
        AppDiagnostics.logNetworkStart(operation, cacheKey);

        try {
            Response<ApiResponse<RoomAvailabilityResponse>> response =
                    apiService.getRoomAvailability(month, year).execute();
            AppDiagnostics.logNetworkResponse(
                    operation,
                    cacheKey,
                    response.code(),
                    System.currentTimeMillis() - startedAtMillis
            );

            if (!isValidDataResponse(response)) {
                logSyncFailure(failureEvent, operation, "HTTP " + response.code(), null);
                return outcomeForHttpCode(response.code());
            }

            RoomAvailabilityResponse data = response.body().getData();
            List<RoomAvailabilityGroup> groups = data.hasGroups()
                    ? NullSafeCollections.copyWithoutNulls(data.getGroups())
                    : new ArrayList<>();
            writeJsonCache(cacheKey, groups);
            AppDiagnostics.logEvent(
                    "background_sync_calendar_success"
                            + " month=" + month
                            + " year=" + year
                            + " groupCount=" + groups.size()
            );
            return SyncOutcome.success();
        } catch (IOException exception) {
            return handleException(failureEvent, operation, exception, true);
        } catch (RuntimeException exception) {
            return handleException(failureEvent, operation, exception, false);
        }
    }

    private SyncOutcome syncRequesterCurrentMonthAvailability(int userId, String prefix) {
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);
        String safePrefix = safe(prefix);
        String cacheKey = RequesterAvailabilityRepository.requesterAvailabilityCacheKey(
                userId,
                safePrefix,
                year,
                month
        );
        String operation = "background_sync_requester_availability";
        String failureEvent = "background_sync_requester_availability_failure";
        long startedAtMillis = System.currentTimeMillis();
        AppDiagnostics.logNetworkStart(operation, cacheKey);

        try {
            Response<ApiResponse<RoomAvailabilityResponse>> response =
                    apiService.getRequesterAvailability(month, year, safePrefix).execute();
            AppDiagnostics.logNetworkResponse(
                    operation,
                    cacheKey,
                    response.code(),
                    System.currentTimeMillis() - startedAtMillis
            );

            if (!isValidDataResponse(response)) {
                logSyncFailure(failureEvent, operation, "HTTP " + response.code(), null);
                return outcomeForHttpCode(response.code());
            }

            RoomAvailabilityResponse data = response.body().getData();
            List<RoomAvailabilityGroup> groups = data.hasGroups()
                    ? NullSafeCollections.copyWithoutNulls(data.getGroups())
                    : new ArrayList<>();
            writeJsonCache(cacheKey, groups);
            AppDiagnostics.logEvent(
                    "background_sync_requester_availability_success"
                            + " prefix=" + safePrefix
                            + " month=" + month
                            + " year=" + year
                            + " groupCount=" + groups.size()
            );
            return SyncOutcome.success();
        } catch (IOException exception) {
            return handleException(failureEvent, operation, exception, true);
        } catch (RuntimeException exception) {
            return handleException(failureEvent, operation, exception, false);
        }
    }

    private void saveRooms(List<RoomItem> rooms) {
        List<RoomItem> safeRooms = RoomInventory.visibleRooms(rooms);
        long refreshedAtMillis = System.currentTimeMillis();

        database.runInTransaction(() -> {
            roomDao.clearRooms();
            roomDao.insertRooms(RoomMapper.toEntityList(safeRooms));
            cacheEntryDao.upsert(new CacheEntryEntity(
                    RoomRepository.ROOM_CACHE_KEY,
                    "{}",
                    refreshedAtMillis
            ));
        });

        RoomMemoryCache.setRooms(safeRooms);
        AppDiagnostics.logCacheWrite(RoomRepository.ROOM_CACHE_KEY);
    }

    private <T> void writeJsonCache(String cacheKey, T value) {
        String payloadJson = gson.toJson(value);
        cacheEntryDao.upsert(new CacheEntryEntity(
                cacheKey,
                payloadJson != null ? payloadJson : "",
                System.currentTimeMillis()
        ));
        AppDiagnostics.logCacheWrite(cacheKey);
    }

    private <T> boolean isValidPageResponse(
            Response<ApiResponse<PaginatedData<T>>> response
    ) {
        return response.isSuccessful()
                && response.body() != null
                && response.body().isSuccess()
                && response.body().getData() != null
                && response.body().getData().getResults() != null;
    }

    private <T> boolean isValidDataResponse(Response<ApiResponse<T>> response) {
        return response.isSuccessful()
                && response.body() != null
                && response.body().isSuccess()
                && response.body().getData() != null;
    }

    private void logSyncFailure(
            String eventName,
            String operation,
            String message,
            Throwable exception
    ) {
        AppDiagnostics.logEvent(eventName + " message=" + safe(message));
        AppDiagnostics.logApiFailure(operation, message, exception);
    }

    private SyncOutcome handleException(
            String eventName,
            String operation,
            Throwable exception,
            boolean retryable
    ) {
        logSyncFailure(
                eventName,
                operation,
                exception.getClass().getSimpleName(),
                exception
        );
        return retryable ? SyncOutcome.retryableFailure() : SyncOutcome.permanentFailure();
    }

    private SyncOutcome outcomeForHttpCode(int httpCode) {
        return isRetryableHttpCode(httpCode)
                ? SyncOutcome.retryableFailure()
                : SyncOutcome.permanentFailure();
    }

    private boolean isRetryableHttpCode(int httpCode) {
        return httpCode == 408 || httpCode == 429 || httpCode >= 500;
    }

    static String bookingPageOneCacheKey(String status) {
        return BookingRepository.firstPageCacheKey(null, null, null, status);
    }

    static String calendarAvailabilityCacheKey(Calendar calendar) {
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);
        return AvailabilityRepository.calendarAvailabilityCacheKey(month, year);
    }

    private String safe(String value) {
        return value != null ? value.trim() : "";
    }

    private static final class SyncSummary {
        private int successCount;
        private int retryableFailureCount;
        private int permanentFailureCount;

        private void add(SyncOutcome outcome) {
            if (outcome == null) {
                permanentFailureCount++;
                return;
            }

            if (outcome.success) {
                successCount++;
            } else if (outcome.retryable) {
                retryableFailureCount++;
            } else {
                permanentFailureCount++;
            }
        }
    }

    private static final class SyncOutcome {
        private final boolean success;
        private final boolean retryable;

        private SyncOutcome(boolean success, boolean retryable) {
            this.success = success;
            this.retryable = retryable;
        }

        private static SyncOutcome success() {
            return new SyncOutcome(true, false);
        }

        private static SyncOutcome retryableFailure() {
            return new SyncOutcome(false, true);
        }

        private static SyncOutcome permanentFailure() {
            return new SyncOutcome(false, false);
        }
    }
}
