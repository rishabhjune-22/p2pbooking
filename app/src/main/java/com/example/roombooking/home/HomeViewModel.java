package com.example.roombooking.home;

import android.util.Log;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roombooking.booking.BookingRepository;
import com.example.roombooking.cache.CachePolicy;
import com.example.roombooking.cache.CacheReadResult;
import com.example.roombooking.model.booking.BookingActionData;
import com.example.roombooking.model.booking.BookingItem;
import com.example.roombooking.model.booking.BookingStatus;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.model.common.PaginatedData;
import com.example.roombooking.utils.ApiErrorUtils;
import com.example.roombooking.utils.AppDiagnostics;
import com.example.roombooking.utils.NullSafeCollections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeViewModel extends ViewModel {

    private static final String TAG = "HomeViewModel";
    private static final int FIRST_PAGE = 1;

    private static final String MESSAGE_EMPTY_BOOKINGS =
            "No bookings yet. Tap + to create one.";
    private static final String MESSAGE_LOAD_FAILED =
            "Failed to load bookings.";
    private static final String MESSAGE_DELETE_FAILED =
            "Delete failed.";
    private static final String MESSAGE_NETWORK_ERROR =
            "Please check your internet connection.";
    private static final String MESSAGE_STALE_BOOKINGS =
            "Could not update latest bookings. Showing last loaded data.";
    private static final int MAX_FIRST_PAGE_NETWORK_RETRIES = 0;
    private static final long FIRST_PAGE_RETRY_DELAY_MS = 700L;
    private static final Object CACHE_LOCK = new Object();
    private static final Map<String, CachedBookingPage> firstPageCache = new HashMap<>();

    private final BookingRepository bookingRepository;
    private final Handler retryHandler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<List<BookingItem>> bookingsLiveData =
            new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<Boolean> fullScreenLoadingLiveData =
            new MutableLiveData<>(false);

    private final MutableLiveData<Boolean> paginationLoadingLiveData =
            new MutableLiveData<>(false);

    private final MutableLiveData<Boolean> swipeRefreshingLiveData =
            new MutableLiveData<>(false);

    private final MutableLiveData<String> messageLiveData =
            new MutableLiveData<>("");

    private final MutableLiveData<String> toastLiveData =
            new MutableLiveData<>();

    private int currentPage = FIRST_PAGE;

    private boolean isLoading = false;
    private boolean isLastPage = false;

    private Call<ApiResponse<PaginatedData<BookingItem>>> bookingsCall;
    private final Map<Integer, Call<ApiResponse<BookingActionData>>> deleteCalls =
            new HashMap<>();
    private Runnable pendingBookingsRetry;
    private int firstPageNetworkRetryCount = 0;
    private int cacheLoadGeneration = 0;

    private String filterPrefix = null;
    private String filterArrivalFrom = null;
    private String filterDepartureTo = null;
    private String filterStatus = BookingStatus.ACTIVE;

    public HomeViewModel(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    private static final class CachedBookingPage {
        private final List<BookingItem> bookings;
        private final long updatedAtMillis;

        private CachedBookingPage(List<BookingItem> bookings, long updatedAtMillis) {
            this.bookings = NullSafeCollections.copyWithoutNulls(bookings);
            this.updatedAtMillis = updatedAtMillis;
        }
    }

    public LiveData<List<BookingItem>> getBookingsLiveData() {
        return bookingsLiveData;
    }

    public LiveData<Boolean> getFullScreenLoadingLiveData() {
        return fullScreenLoadingLiveData;
    }

    public LiveData<Boolean> getPaginationLoadingLiveData() {
        return paginationLoadingLiveData;
    }

    public LiveData<Boolean> getSwipeRefreshingLiveData() {
        return swipeRefreshingLiveData;
    }

    public LiveData<String> getMessageLiveData() {
        return messageLiveData;
    }

    public LiveData<String> getToastLiveData() {
        return toastLiveData;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public boolean isLastPage() {
        return isLastPage;
    }

    public void loadInitialBookings() {
        abortBookingsRequest();
        resetPagination();

        long actionStartedAtMillis = System.currentTimeMillis();
        String cacheKey = firstPageCacheKey();
        CachedBookingPage memoryPage = getCachedBookingsForCurrentFilter();
        if (memoryPage != null) {
            showCachedFirstPage(memoryPage.bookings, "memory", actionStartedAtMillis);
            if (isFresh(memoryPage.updatedAtMillis, CachePolicy.BOOKING_PAGE_ONE_TTL_MS)) {
                return;
            }

            fetchBookings(FIRST_PAGE, false, true, actionStartedAtMillis);
            return;
        }

        int generation = ++cacheLoadGeneration;
        if (!hasVisibleBookings()) {
            fullScreenLoadingLiveData.setValue(true);
            messageLiveData.setValue("");
        }

        bookingRepository.getCachedFirstPage(cacheKey, result -> {
            if (generation != cacheLoadGeneration || !cacheKey.equals(firstPageCacheKey())) {
                return;
            }

            handleCachedFirstPageResult(result, actionStartedAtMillis);
        });
    }

    public void refreshBookings() {
        cacheLoadGeneration++;
        abortBookingsRequest();
        resetPagination();
        swipeRefreshingLiveData.setValue(true);
        fetchBookings(FIRST_PAGE, false, false);
    }

    public void invalidateBookingPageOneCacheForMutation() {
        clearFirstPageCaches();
    }

    public void loadNextPage() {
        if (isLoading || isLastPage || bookingsCall != null) {
            logSkippedRequest(currentPage + 1);
            return;
        }

        fetchBookings(currentPage + 1, false, false);
    }

    public void applyFilter(
            String prefix,
            String arrivalFrom,
            String departureTo,
            String status
    ) {
        filterPrefix = isBlank(prefix) ? null : prefix.trim();
        filterArrivalFrom = arrivalFrom;
        filterDepartureTo = departureTo;
        filterStatus = status != null ? status : BookingStatus.ACTIVE;

        loadInitialBookings();
    }

    public void deleteBooking(BookingItem bookingItem) {
        if (bookingItem == null) return;

        int bookingId = bookingItem.getId();
        if (deleteCalls.containsKey(bookingId)) {
            toastLiveData.setValue("Deletion is already in progress.");
            return;
        }

        Call<ApiResponse<BookingActionData>> request = bookingRepository.deleteBooking(bookingId);
        deleteCalls.put(bookingId, request);
        request.enqueue(new Callback<ApiResponse<BookingActionData>>() {

            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<BookingActionData>> call,
                    @NonNull Response<ApiResponse<BookingActionData>> response
            ) {
                if (!isCurrentDeleteCall(bookingId, call)) return;
                deleteCalls.remove(bookingId);

                if (!response.isSuccessful() || response.body() == null) {
                    String message = ApiErrorUtils.messageFromResponse(
                            response,
                            MESSAGE_DELETE_FAILED
                    );
                    AppDiagnostics.logBookingMutationFailure("delete", bookingId, message);
                    toastLiveData.setValue(message);
                    return;
                }

                ApiResponse<BookingActionData> apiResponse = response.body();

                if (!apiResponse.isSuccess() || apiResponse.getData() == null) {
                    String message = ApiErrorUtils.messageFromApiResponse(
                            apiResponse,
                            MESSAGE_DELETE_FAILED
                    );
                    AppDiagnostics.logBookingMutationFailure("delete", bookingId, message);
                    toastLiveData.setValue(message);
                    return;
                }

                toastLiveData.setValue(apiResponse.getSafeMessage());
                removeBookingById(bookingId);
                refreshBookingsAfterBookingChange();
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<BookingActionData>> call,
                    @NonNull Throwable t
            ) {
                if (!isCurrentDeleteCall(bookingId, call)) return;
                deleteCalls.remove(bookingId);
                if (!call.isCanceled()) {
                    AppDiagnostics.logBookingMutationFailure(
                            "delete",
                            bookingId,
                            MESSAGE_NETWORK_ERROR,
                            t
                    );
                    toastLiveData.setValue(ApiErrorUtils.networkMessage());
                }
            }
        });
    }

    public void updateBookingById(
            int bookingId,
            String updatedStatus,
            String arrivalAt,
            String departureAt
    ) {
        List<BookingItem> currentList = bookingsLiveData.getValue();

        if (currentList == null || currentList.isEmpty()) return;

        List<BookingItem> updatedList = new ArrayList<>(currentList);

        for (int index = 0; index < updatedList.size(); index++) {
            BookingItem item = updatedList.get(index);
            if (item.getId() == bookingId) {
                if (updatedStatus != null
                        && !updatedStatus.equalsIgnoreCase(filterStatus)) {
                    updatedList.remove(index);
                    break;
                }
                updateBookingFields(
                        item,
                        updatedStatus,
                        arrivalAt,
                        departureAt
                );
                break;
            }
        }

        bookingsLiveData.setValue(updatedList);
        clearFirstPageCaches();
    }

    public void removeBookingById(int bookingId) {
        List<BookingItem> currentList = bookingsLiveData.getValue();

        if (currentList == null || currentList.isEmpty()) return;

        List<BookingItem> updatedList = new ArrayList<>(currentList);
        boolean removed = false;

        for (int index = 0; index < updatedList.size(); index++) {
            if (updatedList.get(index).getId() == bookingId) {
                updatedList.remove(index);
                removed = true;
                break;
            }
        }

        if (!removed) return;

        bookingsLiveData.setValue(updatedList);
        clearFirstPageCaches();
        if (updatedList.isEmpty()) {
            messageLiveData.setValue(MESSAGE_EMPTY_BOOKINGS);
        }
    }

    private void fetchBookings(
            int page,
            boolean showFullScreenLoader,
            boolean quietFailure
    ) {
        fetchBookings(page, showFullScreenLoader, quietFailure, System.currentTimeMillis());
    }

    private void fetchBookings(
            int page,
            boolean showFullScreenLoader,
            boolean quietFailure,
            long actionStartedAtMillis
    ) {
        if (isLoading || bookingsCall != null) {
            logSkippedRequest(page);
            return;
        }

        logRequestStart(page);
        String cacheKey = firstPageCacheKey();
        long requestStartedAtMillis = System.currentTimeMillis();
        AppDiagnostics.logNetworkStart("booking_list_page_" + page, cacheKey);
        isLoading = true;
        showLoaderForRequest(page, showFullScreenLoader);

        Call<ApiResponse<PaginatedData<BookingItem>>> request = bookingRepository.getBookings(
                page,
                filterPrefix,
                filterArrivalFrom,
                filterDepartureTo,
                filterStatus
        );
        bookingsCall = request;
        request.enqueue(new Callback<ApiResponse<PaginatedData<BookingItem>>>() {

            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<PaginatedData<BookingItem>>> call,
                    @NonNull Response<ApiResponse<PaginatedData<BookingItem>>> response
            ) {
                if (call.isCanceled()) {
                    clearCurrentBookingsCall(call);
                    return;
                }
                if (!isCurrentBookingsCall(call)) return;
                bookingsCall = null;

                if (!response.isSuccessful() || response.body() == null) {
                    if (shouldRetryFirstPageRequest(page, response.code())) {
                        scheduleFirstPageRetry(showFullScreenLoader, quietFailure);
                        logRequestResponse(page, response.code());
                        logNetworkResponse(page, response.code(), requestStartedAtMillis);
                        return;
                    }

                    hideAllLoaders();
                    handleUnsuccessfulBookingsResponse(page, response, quietFailure);
                    logRequestResponse(page, response.code());
                    logNetworkResponse(page, response.code(), requestStartedAtMillis);
                    return;
                }

                ApiResponse<PaginatedData<BookingItem>> apiResponse = response.body();

                if (!apiResponse.isSuccess() || apiResponse.getData() == null) {
                    hideAllLoaders();
                    messageLiveData.setValue(ApiErrorUtils.messageFromApiResponse(
                            apiResponse,
                            MESSAGE_LOAD_FAILED
                    ));
                    logRequestResponse(page, response.code());
                    logNetworkResponse(page, response.code(), requestStartedAtMillis);
                    return;
                }

                hideAllLoaders();
                handleBookingsPage(page, apiResponse.getData());
                AppDiagnostics.logUiUpdated(
                        "booking_list_page_" + page,
                        "network",
                        System.currentTimeMillis() - actionStartedAtMillis
                );
                logRequestResponse(page, response.code());
                logNetworkResponse(page, response.code(), requestStartedAtMillis);
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<PaginatedData<BookingItem>>> call,
                    @NonNull Throwable t
            ) {
                if (call.isCanceled()) {
                    clearCurrentBookingsCall(call);
                    return;
                }
                if (!isCurrentBookingsCall(call)) return;
                bookingsCall = null;
                logRequestFailure(page, t);
                logNetworkResponse(page, 0, requestStartedAtMillis);

                if (shouldRetryFirstPageRequest(page)) {
                    scheduleFirstPageRetry(showFullScreenLoader, quietFailure);
                    return;
                }

                hideAllLoaders();
                if (quietFailure && hasVisibleBookings()) {
                    toastLiveData.setValue(MESSAGE_STALE_BOOKINGS);
                    return;
                }

                AppDiagnostics.logApiFailure("booking_list", MESSAGE_NETWORK_ERROR, t);
                messageLiveData.setValue(ApiErrorUtils.networkMessage());
            }
        });
    }

    private boolean isCurrentBookingsCall(
            Call<ApiResponse<PaginatedData<BookingItem>>> call
    ) {
        return call == bookingsCall;
    }

    private void clearCurrentBookingsCall(
            Call<ApiResponse<PaginatedData<BookingItem>>> call
    ) {
        if (isCurrentBookingsCall(call)) {
            bookingsCall = null;
        }
    }

    private boolean isCurrentDeleteCall(
            int bookingId,
            Call<ApiResponse<BookingActionData>> call
    ) {
        return call == deleteCalls.get(bookingId);
    }

    private void abortBookingsRequest() {
        cancelPendingBookingsRetry();
        if (bookingsCall != null && !bookingsCall.isCanceled()) {
            bookingsCall.cancel();
        }
        bookingsCall = null;
        hideAllLoaders();
    }

    private void showLoaderForRequest(int page, boolean showFullScreenLoader) {
        if (showFullScreenLoader) {
            fullScreenLoadingLiveData.setValue(true);
            messageLiveData.setValue("");
            return;
        }

        if (page > FIRST_PAGE) {
            paginationLoadingLiveData.setValue(true);
        }
    }

    private void hideAllLoaders() {
        isLoading = false;
        fullScreenLoadingLiveData.setValue(false);
        paginationLoadingLiveData.setValue(false);
        swipeRefreshingLiveData.setValue(false);
    }

    private void handleUnsuccessfulBookingsResponse(
            int page,
            Response<ApiResponse<PaginatedData<BookingItem>>> response,
            boolean quietFailure
    ) {
        int httpCode = response.code();

        if (httpCode == 404 && page > FIRST_PAGE) {
            isLastPage = true;
            return;
        }

        if (quietFailure && hasVisibleBookings()) {
            toastLiveData.setValue(MESSAGE_STALE_BOOKINGS);
            return;
        }

        if (httpCode == 429) {
            messageLiveData.setValue(ApiErrorUtils.messageFromResponse(
                    response,
                    ApiErrorUtils.rateLimitMessage()
            ));
            return;
        }

        String message = ApiErrorUtils.messageFromResponse(response, MESSAGE_LOAD_FAILED);
        AppDiagnostics.logApiFailure("booking_list", message, null);
        messageLiveData.setValue(message);
    }

    private void handleBookingsPage(
            int page,
            PaginatedData<BookingItem> paginatedData
    ) {
        List<BookingItem> results = NullSafeCollections.copyWithoutNulls(
                paginatedData.getResults()
        );

        if (page == FIRST_PAGE) {
            handleFirstPageResults(results);
            cacheFirstPageForCurrentFilter(results);
        } else {
            appendNextPageResults(results);
            messageLiveData.setValue("");
        }

        currentPage = page;
        isLastPage = !paginatedData.hasNextPage()
                || results.isEmpty();

    }

    private void handleFirstPageResults(List<BookingItem> results) {
        if (results.isEmpty()) {
            bookingsLiveData.setValue(new ArrayList<>());
            messageLiveData.setValue(MESSAGE_EMPTY_BOOKINGS);
            return;
        }

        bookingsLiveData.setValue(results);
        messageLiveData.setValue("");
    }

    private void appendNextPageResults(List<BookingItem> results) {
        List<BookingItem> currentList = bookingsLiveData.getValue();

        if (currentList == null) {
            currentList = new ArrayList<>();
        }

        List<BookingItem> updatedList = new ArrayList<>(currentList);

        updatedList.addAll(results);

        bookingsLiveData.setValue(updatedList);
    }

    private void resetPagination() {
        currentPage = FIRST_PAGE;
        isLastPage = false;
        isLoading = false;
        firstPageNetworkRetryCount = 0;
    }

    private void refreshBookingsAfterBookingChange() {
        abortBookingsRequest();
        resetPagination();
        clearFirstPageCaches();
        swipeRefreshingLiveData.setValue(true);
        fetchBookings(FIRST_PAGE, false, false);
    }

    private boolean shouldRetryFirstPageRequest(int page) {
        return page == FIRST_PAGE && firstPageNetworkRetryCount < MAX_FIRST_PAGE_NETWORK_RETRIES;
    }

    private boolean shouldRetryFirstPageRequest(int page, int httpCode) {
        return shouldRetryFirstPageRequest(page)
                && (httpCode == 500 || httpCode == 502 || httpCode == 503 || httpCode == 504);
    }

    private void scheduleFirstPageRetry(
            boolean showFullScreenLoader,
            boolean quietFailure
    ) {
        isLoading = false;
        firstPageNetworkRetryCount++;
        cancelPendingBookingsRetry();

        long delayMillis = FIRST_PAGE_RETRY_DELAY_MS * firstPageNetworkRetryCount;
        pendingBookingsRetry = () -> {
            pendingBookingsRetry = null;
            fetchBookings(FIRST_PAGE, showFullScreenLoader, quietFailure);
        };
        retryHandler.postDelayed(pendingBookingsRetry, delayMillis);
    }

    private void cancelPendingBookingsRetry() {
        if (pendingBookingsRetry != null) {
            retryHandler.removeCallbacks(pendingBookingsRetry);
            pendingBookingsRetry = null;
        }
    }

    private boolean hasVisibleBookings() {
        List<BookingItem> currentList = bookingsLiveData.getValue();
        return currentList != null && !currentList.isEmpty();
    }

    private void cacheFirstPageForCurrentFilter(List<BookingItem> results) {
        String cacheKey = firstPageCacheKey();
        long updatedAtMillis = System.currentTimeMillis();
        List<BookingItem> cachedBookings = NullSafeCollections.copyWithoutNulls(results);
        cacheFirstPageInMemory(cacheKey, cachedBookings, updatedAtMillis);
        bookingRepository.saveCachedFirstPage(cacheKey, cachedBookings);
    }

    private void cacheFirstPageInMemory(
            String cacheKey,
            List<BookingItem> results,
            long updatedAtMillis
    ) {
        synchronized (CACHE_LOCK) {
            firstPageCache.put(cacheKey, new CachedBookingPage(results, updatedAtMillis));
        }
    }

    private void clearFirstPageCaches() {
        synchronized (CACHE_LOCK) {
            firstPageCache.clear();
        }
        bookingRepository.clearFirstPageCaches();
    }

    private CachedBookingPage getCachedBookingsForCurrentFilter() {
        synchronized (CACHE_LOCK) {
            CachedBookingPage cachedPage = firstPageCache.get(firstPageCacheKey());
            if (cachedPage == null) {
                return null;
            }

            return new CachedBookingPage(
                    cachedPage.bookings,
                    cachedPage.updatedAtMillis
            );
        }
    }

    private void handleCachedFirstPageResult(
            CacheReadResult<List<BookingItem>> result,
            long actionStartedAtMillis
    ) {
        if (result.isHit()) {
            List<BookingItem> cachedBookings = NullSafeCollections.copyWithoutNulls(
                    result.getValue()
            );
            cacheFirstPageInMemory(
                    result.getKey(),
                    cachedBookings,
                    result.getUpdatedAtMillis()
            );
            showCachedFirstPage(cachedBookings, "disk", actionStartedAtMillis);

            if (result.isFresh()) {
                return;
            }

            fetchBookings(FIRST_PAGE, false, true, actionStartedAtMillis);
            return;
        }

        fetchBookings(FIRST_PAGE, !hasVisibleBookings(), false, actionStartedAtMillis);
    }

    private void showCachedFirstPage(
            List<BookingItem> bookings,
            String source,
            long actionStartedAtMillis
    ) {
        hideAllLoaders();
        handleFirstPageResults(NullSafeCollections.copyWithoutNulls(bookings));
        AppDiagnostics.logUiUpdated(
                "booking_list_page_1",
                "cache_" + source,
                System.currentTimeMillis() - actionStartedAtMillis
        );
    }

    private boolean isFresh(long updatedAtMillis, long ttlMillis) {
        return CachePolicy.isFresh(updatedAtMillis, ttlMillis, System.currentTimeMillis());
    }

    private String filterKey() {
        return safeFilterValue(filterPrefix)
                + "|"
                + safeFilterValue(filterArrivalFrom)
                + "|"
                + safeFilterValue(filterDepartureTo)
                + "|"
                + safeFilterValue(filterStatus);
    }

    private String firstPageCacheKey() {
        return BookingRepository.firstPageCacheKey(
                filterPrefix,
                filterArrivalFrom,
                filterDepartureTo,
                filterStatus
        );
    }

    private void logNetworkResponse(
            int page,
            int httpCode,
            long requestStartedAtMillis
    ) {
        AppDiagnostics.logNetworkResponse(
                "booking_list_page_" + page,
                firstPageCacheKey(),
                httpCode,
                System.currentTimeMillis() - requestStartedAtMillis
        );
    }

    private String safeFilterValue(String value) {
        return value == null ? "" : value.trim();
    }

    private void logRequestStart(int page) {
        Log.d(TAG, "booking-list request start page=" + page
                + " status=" + filterStatus
                + " isLoading=" + isLoading
                + " isLastPage=" + isLastPage
                + " activeCall=" + (bookingsCall != null));
    }

    private void logSkippedRequest(int page) {
        Log.d(TAG, "booking-list request skipped page=" + page
                + " status=" + filterStatus
                + " isLoading=" + isLoading
                + " isLastPage=" + isLastPage
                + " activeCall=" + (bookingsCall != null));
    }

    private void logRequestResponse(int page, int httpCode) {
        Log.d(TAG, "booking-list response page=" + page
                + " code=" + httpCode
                + " finalIsLoading=" + isLoading
                + " finalIsLastPage=" + isLastPage);
    }

    private void logRequestFailure(int page, Throwable t) {
        Log.d(TAG, "booking-list failure page=" + page
                + " error=" + t.getClass().getSimpleName()
                + ": " + t.getMessage()
                + " finalIsLoading=" + isLoading
                + " finalIsLastPage=" + isLastPage);
    }

    private void updateBookingFields(
            BookingItem item,
            String updatedStatus,
            String arrivalAt,
            String departureAt
    ) {
        if (updatedStatus != null) {
            item.setStatus(updatedStatus);
        }

        if (arrivalAt != null) {
            item.setArrivalAt(arrivalAt);
        }

        if (departureAt != null) {
            item.setDepartureAt(departureAt);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    protected void onCleared() {
        abortBookingsRequest();
        Set<Call<ApiResponse<BookingActionData>>> calls =
                new HashSet<>(deleteCalls.values());
        deleteCalls.clear();
        for (Call<?> call : calls) {
            if (call != null && !call.isCanceled()) {
                call.cancel();
            }
        }
        super.onCleared();
    }
}
