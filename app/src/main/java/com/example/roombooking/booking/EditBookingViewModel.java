package com.example.roombooking.booking;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roombooking.model.booking.BookingActionData;
import com.example.roombooking.model.booking.BookingItem;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.model.room.RoomItem;
import com.example.roombooking.room.RoomRepository;
import com.example.roombooking.room.RoomResult;
import com.example.roombooking.utils.ApiErrorUtils;
import com.example.roombooking.utils.AppDiagnostics;
import com.example.roombooking.utils.InternetErrorBanner;
import com.example.roombooking.utils.NullSafeCollections;
import com.example.roombooking.utils.UiEvent;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditBookingViewModel extends ViewModel {

    private static final String MESSAGE_UPDATE_FAILED = "Update failed.";
    private static final String MESSAGE_LOAD_ROOMS_FAILED =
            "Rooms could not be loaded. Please try again.";

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final SimpleDateFormat apiDateTimeFormat =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());

    private final MutableLiveData<EditBookingFormState> formStateLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<List<RoomItem>> roomsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> savingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<UiEvent<String>> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<String>> toastLiveData = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<Boolean>> networkBannerLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<UiEvent<EditBookingValidationResult>> validationLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<UiEvent<EditBookingResult>> resultLiveData =
            new MutableLiveData<>();

    private int bookingId = -1;
    private String bookingStatus = "";
    private boolean initialized = false;
    private boolean roomRefreshRequested = false;
    private Call<ApiResponse<BookingActionData>> updateBookingCall;

    public EditBookingViewModel(
            BookingRepository bookingRepository,
            RoomRepository roomRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
    }

    public LiveData<EditBookingFormState> getFormStateLiveData() {
        return formStateLiveData;
    }

    public LiveData<List<RoomItem>> getRoomsLiveData() {
        return roomsLiveData;
    }

    public LiveData<Boolean> getSavingLiveData() {
        return savingLiveData;
    }

    public LiveData<UiEvent<String>> getErrorLiveData() {
        return errorLiveData;
    }

    public LiveData<UiEvent<String>> getToastLiveData() {
        return toastLiveData;
    }

    public LiveData<UiEvent<Boolean>> getNetworkBannerLiveData() {
        return networkBannerLiveData;
    }

    public LiveData<UiEvent<EditBookingValidationResult>> getValidationLiveData() {
        return validationLiveData;
    }

    public LiveData<UiEvent<EditBookingResult>> getResultLiveData() {
        return resultLiveData;
    }

    public void initialize(BookingItem bookingItem) {
        if (initialized || bookingItem == null) {
            return;
        }

        initialized = true;
        bookingId = bookingItem.getId();
        bookingStatus = bookingItem.getStatus();
        formStateLiveData.setValue(
                EditBookingFormMapper.fromBookingItem(bookingItem, apiDateTimeFormat)
        );
    }

    public void loadRooms() {
        roomRepository.getRooms(result -> {
            handleRoomsResult(result, true);

            if (result.isSuccess() && result.isFromCache()) {
                refreshRoomsFromApi();
            }
        });
    }

    private void refreshRoomsFromApi() {
        if (roomRefreshRequested) {
            return;
        }

        roomRefreshRequested = true;
        roomRepository.forceRefresh(result -> handleRoomsResult(result, false));
    }

    private void handleRoomsResult(RoomResult result, boolean showLoadError) {
        if (result.isSuccess() && result.getRooms() != null) {
            networkBannerLiveData.setValue(new UiEvent<>(false));
            roomsLiveData.setValue(NullSafeCollections.copyWithoutNulls(result.getRooms()));
            return;
        }

        if (InternetErrorBanner.isNetworkErrorMessage(result.getErrorMessage())) {
            networkBannerLiveData.setValue(new UiEvent<>(true));
        }

        if (showLoadError) {
            errorLiveData.setValue(new UiEvent<>(MESSAGE_LOAD_ROOMS_FAILED));
        }
    }

    public Calendar getArrivalCalendar() {
        EditBookingFormState state = currentState();
        return EditBookingFormMapper.calendarFromMillis(state.getArrivalAtMillis());
    }

    public Calendar getDepartureCalendar() {
        EditBookingFormState state = currentState();
        return EditBookingFormMapper.calendarFromMillis(state.getDepartureAtMillis());
    }

    public boolean isSaving() {
        return Boolean.TRUE.equals(savingLiveData.getValue());
    }

    public void updateArrivalDateTime(Calendar arrival) {
        EditBookingFormState state = currentState();
        Calendar departure = EditBookingFormMapper.calendarFromMillis(state.getDepartureAtMillis());
        EditBookingFormMapper.ensureDepartureAfterArrival(arrival, departure);
        updateDateTimes(state, arrival, departure);
    }

    public boolean updateDepartureDateTime(Calendar departure) {
        EditBookingFormState state = currentState();
        Calendar arrival = EditBookingFormMapper.calendarFromMillis(state.getArrivalAtMillis());
        boolean adjusted = !departure.getTime().after(arrival.getTime());
        EditBookingFormMapper.ensureDepartureAfterArrival(arrival, departure);
        updateDateTimes(state, arrival, departure);
        return !adjusted;
    }

    public void save(EditBookingFormState formState) {
        if (updateBookingCall != null || isSaving()) {
            return;
        }

        EditBookingFormState state = formState.copy();
        EditBookingValidationResult validationResult = EditBookingFormMapper.validate(state);
        if (!validationResult.isValid()) {
            validationLiveData.setValue(new UiEvent<>(validationResult));
            return;
        }

        formStateLiveData.setValue(state.copy());
        savingLiveData.setValue(true);

        Call<ApiResponse<BookingActionData>> call = bookingRepository.updateBooking(
                bookingId,
                EditBookingFormMapper.toUpdateRequest(state)
        );
        updateBookingCall = call;
        call.enqueue(new Callback<ApiResponse<BookingActionData>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<BookingActionData>> call,
                    @NonNull Response<ApiResponse<BookingActionData>> response
            ) {
                if (call != updateBookingCall) return;
                updateBookingCall = null;
                savingLiveData.setValue(false);
                networkBannerLiveData.setValue(new UiEvent<>(false));

                if (!response.isSuccessful() || response.body() == null) {
                    String message = ApiErrorUtils.messageFromResponse(
                            response,
                            MESSAGE_UPDATE_FAILED
                    );
                    handleUpdateFailure(message, null);
                    return;
                }

                ApiResponse<BookingActionData> apiResponse = response.body();
                if (!apiResponse.isSuccess()) {
                    String message = ApiErrorUtils.messageFromApiResponse(
                            apiResponse,
                            MESSAGE_UPDATE_FAILED
                    );
                    handleUpdateFailure(message, null);
                    return;
                }

                BookingActionData data = apiResponse.getData();
                if (data != null && data.getStatus() != null) {
                    bookingStatus = data.getStatus();
                }

                resultLiveData.setValue(new UiEvent<>(new EditBookingResult(
                        bookingId,
                        bookingStatus,
                        state.getArrivalAt(),
                        state.getDepartureAt()
                )));
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<BookingActionData>> call,
                    @NonNull Throwable t
            ) {
                if (call != updateBookingCall) return;
                updateBookingCall = null;
                savingLiveData.setValue(false);
                if (!call.isCanceled()) {
                    networkBannerLiveData.setValue(new UiEvent<>(true));
                    handleUpdateFailure(ApiErrorUtils.networkMessage(), t);
                }
            }
        });
    }

    private void handleUpdateFailure(String message, Throwable throwable) {
        AppDiagnostics.logBookingMutationFailure(
                "update",
                bookingId,
                message,
                throwable
        );
        errorLiveData.setValue(new UiEvent<>(
                EditBookingFormMapper.makeFriendlyMessage(message)
        ));
    }

    private void updateDateTimes(
            EditBookingFormState state,
            Calendar arrival,
            Calendar departure
    ) {
        EditBookingFormMapper.applyDateTimes(
                state,
                arrival,
                departure,
                apiDateTimeFormat
        );
        formStateLiveData.setValue(state.copy());
    }

    private EditBookingFormState currentState() {
        EditBookingFormState state = formStateLiveData.getValue();
        return state != null ? state.copy() : new EditBookingFormState();
    }

    @Override
    protected void onCleared() {
        if (updateBookingCall != null && !updateBookingCall.isCanceled()) {
            updateBookingCall.cancel();
        }
        updateBookingCall = null;
        super.onCleared();
    }
}
