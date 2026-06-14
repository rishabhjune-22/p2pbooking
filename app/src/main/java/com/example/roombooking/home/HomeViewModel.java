package com.example.roombooking.home;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roombooking.booking.BookingCancelRequest;
import com.example.roombooking.booking.BookingRepository;
import com.example.roombooking.model.booking.BookingActionData;
import com.example.roombooking.model.booking.BookingItem;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.model.common.PaginatedData;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeViewModel extends ViewModel {

    private static final int FIRST_PAGE = 1;

    private static final String DEFAULT_STATUS = "active";

    private static final String MESSAGE_EMPTY_BOOKINGS =
            "No bookings yet. Tap + to create one.";
    private static final String MESSAGE_LOAD_FAILED =
            "Failed to load bookings.";
    private static final String MESSAGE_CANCEL_FAILED =
            "Cancel failed.";
    private static final String MESSAGE_NETWORK_ERROR =
            "Please check your internet connection.";

    private final BookingRepository bookingRepository;

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

    private String filterPrefix = null;
    private String filterArrivalFrom = null;
    private String filterDepartureTo = null;
    private String filterStatus = DEFAULT_STATUS;

    public HomeViewModel(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
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
        if (isLoading) return;

        resetPagination();
        fetchBookings(FIRST_PAGE, true);
    }

    public void refreshBookings() {
        if (isLoading) return;

        resetPagination();
        swipeRefreshingLiveData.setValue(true);
        fetchBookings(FIRST_PAGE, false);
    }

    public void loadNextPage() {
        if (isLoading || isLastPage) return;

        fetchBookings(currentPage + 1, false);
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
        filterStatus = status != null ? status : DEFAULT_STATUS;

        loadInitialBookings();
    }

    public void cancelBooking(BookingItem bookingItem, String reason) {
        if (bookingItem == null) return;

        bookingRepository.cancelBooking(
                bookingItem.getId(),
                new BookingCancelRequest(reason)
        ).enqueue(new Callback<ApiResponse<BookingActionData>>() {

            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<BookingActionData>> call,
                    @NonNull Response<ApiResponse<BookingActionData>> response
            ) {
                if (!response.isSuccessful() || response.body() == null) {
                    toastLiveData.setValue(MESSAGE_CANCEL_FAILED);
                    return;
                }

                ApiResponse<BookingActionData> apiResponse = response.body();

                if (!apiResponse.isSuccess() || apiResponse.getData() == null) {
                    toastLiveData.setValue(apiResponse.getFirstErrorMessage());
                    return;
                }

                toastLiveData.setValue(apiResponse.getSafeMessage());
                refreshBookings();
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<BookingActionData>> call,
                    @NonNull Throwable t
            ) {
                toastLiveData.setValue(MESSAGE_NETWORK_ERROR);
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

        for (BookingItem item : updatedList) {
            if (item.getId() == bookingId) {
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
    }

    private void fetchBookings(
            int page,
            boolean showFullScreenLoader
    ) {
        isLoading = true;
        showLoaderForRequest(page, showFullScreenLoader);

        bookingRepository.getBookings(
                page,
                filterPrefix,
                filterArrivalFrom,
                filterDepartureTo,
                filterStatus
        ).enqueue(new Callback<ApiResponse<PaginatedData<BookingItem>>>() {

            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<PaginatedData<BookingItem>>> call,
                    @NonNull Response<ApiResponse<PaginatedData<BookingItem>>> response
            ) {
                hideAllLoaders();

                if (!response.isSuccessful() || response.body() == null) {
                    messageLiveData.setValue(MESSAGE_LOAD_FAILED);
                    return;
                }

                ApiResponse<PaginatedData<BookingItem>> apiResponse = response.body();

                if (!apiResponse.isSuccess() || apiResponse.getData() == null) {
                    messageLiveData.setValue(apiResponse.getFirstErrorMessage());
                    return;
                }

                handleBookingsPage(page, apiResponse.getData());
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<PaginatedData<BookingItem>>> call,
                    @NonNull Throwable t
            ) {
                hideAllLoaders();
                messageLiveData.setValue(MESSAGE_NETWORK_ERROR);
            }
        });
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

    private void handleBookingsPage(
            int page,
            PaginatedData<BookingItem> paginatedData
    ) {
        List<BookingItem> results = paginatedData.getResults();

        if (page == FIRST_PAGE) {
            handleFirstPageResults(results);
        } else {
            appendNextPageResults(results);
        }

        currentPage = page;
        isLastPage = !paginatedData.hasNextPage()
                || results == null
                || results.isEmpty();

        messageLiveData.setValue("");
    }

    private void handleFirstPageResults(List<BookingItem> results) {
        if (results == null || results.isEmpty()) {
            bookingsLiveData.setValue(new ArrayList<>());
            messageLiveData.setValue(MESSAGE_EMPTY_BOOKINGS);
            return;
        }

        bookingsLiveData.setValue(new ArrayList<>(results));
    }

    private void appendNextPageResults(List<BookingItem> results) {
        List<BookingItem> currentList = bookingsLiveData.getValue();

        if (currentList == null) {
            currentList = new ArrayList<>();
        }

        List<BookingItem> updatedList = new ArrayList<>(currentList);

        if (results != null) {
            updatedList.addAll(results);
        }

        bookingsLiveData.setValue(updatedList);
    }

    private void resetPagination() {
        currentPage = FIRST_PAGE;
        isLastPage = false;
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
}
