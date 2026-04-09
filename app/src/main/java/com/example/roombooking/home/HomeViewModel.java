package com.example.roombooking.home;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roombooking.auth.LogoutRequest;
import com.example.roombooking.auth.AuthRepository;
import com.example.roombooking.auth.SessionManager;
import com.example.roombooking.model.booking.BookingActionData;
import com.example.roombooking.booking.BookingCancelRequest;
import com.example.roombooking.booking.BookingRepository;
import com.example.roombooking.model.booking.BookingItem;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.model.common.PaginatedData;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeViewModel extends ViewModel {

    private final BookingRepository bookingRepository;
    private final AuthRepository authRepository;
    private final SessionManager sessionManager;
    private final Gson gson = new Gson();

    private final MutableLiveData<List<BookingItem>> bookingsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> fullScreenLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> paginationLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> swipeRefreshingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> messageLiveData = new MutableLiveData<>("");
    private final MutableLiveData<String> toastLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> logoutEventLiveData = new MutableLiveData<>(false);

    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private boolean isFirstLoad = true;

    public HomeViewModel(
            BookingRepository bookingRepository,
            AuthRepository authRepository,
            SessionManager sessionManager
    ) {
        this.bookingRepository = bookingRepository;
        this.authRepository = authRepository;
        this.sessionManager = sessionManager;
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

    public LiveData<Boolean> getLogoutEventLiveData() {
        return logoutEventLiveData;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public boolean isLastPage() {
        return isLastPage;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void loadInitialBookings() {
        if (isLoading) return;
        currentPage = 1;
        isLastPage = false;
        isFirstLoad = true;
        fetchBookings(1, true, false);
    }

    public void refreshBookings() {
        if (isLoading) return;
        currentPage = 1;
        isLastPage = false;
        isFirstLoad = false;
        swipeRefreshingLiveData.setValue(true);
        fetchBookings(1, false, true);
    }

    public void loadNextPage() {
        if (isLoading || isLastPage) return;
        fetchBookings(currentPage + 1, false, false);
    }

    private void fetchBookings(int page, boolean showFullScreenLoader, boolean isRefresh) {
        isLoading = true;

        if (showFullScreenLoader) {
            fullScreenLoadingLiveData.setValue(true);
            messageLiveData.setValue("");
        } else if (page > 1) {
            paginationLoadingLiveData.setValue(true);
        }

        bookingRepository.getBookings(page).enqueue(new Callback<ApiResponse<PaginatedData<BookingItem>>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<PaginatedData<BookingItem>>> call,
                    @NonNull Response<ApiResponse<PaginatedData<BookingItem>>> response
            ) {
                isLoading = false;
                fullScreenLoadingLiveData.setValue(false);
                paginationLoadingLiveData.setValue(false);
                swipeRefreshingLiveData.setValue(false);
                isFirstLoad = false;

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<PaginatedData<BookingItem>> apiResponse = response.body();

                    if (!apiResponse.isSuccess() || apiResponse.getData() == null) {
                        messageLiveData.setValue(apiResponse.getFirstErrorMessage());
                        return;
                    }

                    PaginatedData<BookingItem> paginatedData = apiResponse.getData();
                    List<BookingItem> results = paginatedData.getResults();

                    if (page == 1) {
                        if (results == null || results.isEmpty()) {
                            bookingsLiveData.setValue(new ArrayList<>());
                            messageLiveData.setValue("No bookings yet. Tap + to create one.");
                            return;
                        }
                        bookingsLiveData.setValue(new ArrayList<>(results));
                    } else {
                        List<BookingItem> currentList = bookingsLiveData.getValue();
                        if (currentList == null) currentList = new ArrayList<>();
                        currentList = new ArrayList<>(currentList);
                        if (results != null) {
                            currentList.addAll(results);
                        }
                        bookingsLiveData.setValue(currentList);
                    }

                    currentPage = page;
                    isLastPage = paginatedData.getNext() == null || results == null || results.isEmpty();
                    messageLiveData.setValue("");
                    return;
                }

                handleApiError(response);
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<PaginatedData<BookingItem>>> call,
                    @NonNull Throwable t
            ) {
                isLoading = false;
                fullScreenLoadingLiveData.setValue(false);
                paginationLoadingLiveData.setValue(false);
                swipeRefreshingLiveData.setValue(false);
                messageLiveData.setValue(getNetworkErrorMessage(t));
            }
        });
    }

    public void cancelBooking(BookingItem bookingItem, String reason) {
        bookingRepository.cancelBooking(
                bookingItem.getId(),
                new BookingCancelRequest(reason)
        ).enqueue(new Callback<ApiResponse<BookingActionData>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<BookingActionData>> call,
                    @NonNull Response<ApiResponse<BookingActionData>> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<BookingActionData> apiResponse = response.body();

                    if (!apiResponse.isSuccess() || apiResponse.getData() == null) {
                        toastLiveData.setValue(apiResponse.getFirstErrorMessage());
                        return;
                    }

                    BookingActionData data = apiResponse.getData();
                    updateBookingStatusById(data.getBookingId(), data.getStatus());
                    toastLiveData.setValue(apiResponse.getMessage());
                    return;
                }

                toastLiveData.setValue(extractErrorMessage(response));
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<BookingActionData>> call,
                    @NonNull Throwable t
            ) {
                toastLiveData.setValue(getNetworkErrorMessage(t));
            }
        });
    }

    public void performLogout() {
        String refreshToken = sessionManager.getRefreshToken();

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            sessionManager.logout();
            logoutEventLiveData.setValue(true);
            return;
        }

        authRepository.logout(new LogoutRequest(refreshToken))
                .enqueue(new Callback<ApiResponse<Object>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<ApiResponse<Object>> call,
                            @NonNull Response<ApiResponse<Object>> response
                    ) {
                        sessionManager.logout();
                        logoutEventLiveData.setValue(true);
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<ApiResponse<Object>> call,
                            @NonNull Throwable t
                    ) {
                        sessionManager.logout();
                        logoutEventLiveData.setValue(true);
                    }
                });
    }

    public void updateBookingById(
            int bookingId,
            String updatedStatus,
            String visitorName,
            String visitorMobile,
            String purpose,
            String arrivalDate,
            String arrivalTime,
            String departureDate,
            String departureTime
    ) {
        List<BookingItem> currentList = bookingsLiveData.getValue();
        if (currentList == null || currentList.isEmpty()) return;

        List<BookingItem> updatedList = new ArrayList<>(currentList);

        for (int i = 0; i < updatedList.size(); i++) {
            BookingItem item = updatedList.get(i);
            if (item.getId() == bookingId) {
                item.setStatus(updatedStatus);
                item.setVisitorName(visitorName);
                item.setVisitorMobile(visitorMobile);
                item.setPurposeOfVisit(purpose);
                item.setArrivalDate(arrivalDate);
                item.setArrivalTime(arrivalTime);
                item.setDepartureDate(departureDate);
                item.setDepartureTime(departureTime);
                break;
            }
        }

        bookingsLiveData.setValue(updatedList);
    }

    private void updateBookingStatusById(int bookingId, String status) {
        List<BookingItem> currentList = bookingsLiveData.getValue();
        if (currentList == null || currentList.isEmpty()) return;

        List<BookingItem> updatedList = new ArrayList<>(currentList);

        for (int i = 0; i < updatedList.size(); i++) {
            BookingItem item = updatedList.get(i);
            if (item.getId() == bookingId) {
                item.setStatus(status);
                break;
            }
        }

        bookingsLiveData.setValue(updatedList);
    }

    private <T> void handleApiError(Response<ApiResponse<T>> response) {
        if (response.code() == 401) {
            sessionManager.logout();
            messageLiveData.setValue("Session expired. Please login again.");
            logoutEventLiveData.setValue(true);
            return;
        }

        messageLiveData.setValue(extractErrorMessage(response));
    }

    private <T> String extractErrorMessage(Response<ApiResponse<T>> response) {
        try {
            if (response.errorBody() != null) {
                String errorJson = response.errorBody().string();
                ApiResponse<?> errorResponse = gson.fromJson(errorJson, ApiResponse.class);

                if (errorResponse != null) {
                    String firstError = errorResponse.getFirstErrorMessage();
                    if (firstError != null && !firstError.trim().isEmpty()) {
                        return firstError;
                    }

                    if (errorResponse.getMessage() != null && !errorResponse.getMessage().trim().isEmpty()) {
                        return errorResponse.getMessage();
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return "Request failed. Code: " + response.code();
    }

    private String getNetworkErrorMessage(Throwable throwable) {
        return "Please check your internet connection.";
    }
}