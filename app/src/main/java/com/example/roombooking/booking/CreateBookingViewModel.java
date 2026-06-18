package com.example.roombooking.booking;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roombooking.common.LocalUserManager;
import com.example.roombooking.model.booking.BookingActionData;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.model.room.RoomItem;
import com.example.roombooking.room.RoomRepository;
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

public class CreateBookingViewModel extends ViewModel {

    private static final String MESSAGE_CREATE_FAILED = "Booking creation failed.";
    private static final String MESSAGE_LOAD_ROOMS_FAILED =
            "Rooms could not be loaded. Please try again.";

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final LocalUserManager localUserManager;
    private final SimpleDateFormat apiDateTimeFormat =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());

    private final MutableLiveData<CreateBookingFormState> formStateLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<List<RoomItem>> roomsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> creatingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<UiEvent<String>> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<Boolean>> networkBannerLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<UiEvent<CreateBookingValidationResult>> validationLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<UiEvent<CreateBookingResult>> resultLiveData =
            new MutableLiveData<>();

    private boolean initialized = false;
    private Call<ApiResponse<BookingActionData>> createBookingCall;

    public CreateBookingViewModel(
            BookingRepository bookingRepository,
            RoomRepository roomRepository,
            LocalUserManager localUserManager
    ) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.localUserManager = localUserManager;
    }

    public LiveData<CreateBookingFormState> getFormStateLiveData() {
        return formStateLiveData;
    }

    public LiveData<List<RoomItem>> getRoomsLiveData() {
        return roomsLiveData;
    }

    public LiveData<Boolean> getCreatingLiveData() {
        return creatingLiveData;
    }

    public LiveData<UiEvent<String>> getErrorLiveData() {
        return errorLiveData;
    }

    public LiveData<UiEvent<Boolean>> getNetworkBannerLiveData() {
        return networkBannerLiveData;
    }

    public LiveData<UiEvent<CreateBookingValidationResult>> getValidationLiveData() {
        return validationLiveData;
    }

    public LiveData<UiEvent<CreateBookingResult>> getResultLiveData() {
        return resultLiveData;
    }

    public void initialize(CreateBookingInitialData initialData) {
        if (initialized) {
            return;
        }

        initialized = true;
        CreateBookingFormState state = CreateBookingFormMapper.defaultState(
                initialData,
                apiDateTimeFormat
        );
        state.setCreatedByName(localUserManager.getUserName());
        formStateLiveData.setValue(state);
    }

    public void loadRooms() {
        roomRepository.getRooms(result -> {
            if (result.isSuccess() && result.getRooms() != null) {
                networkBannerLiveData.setValue(new UiEvent<>(false));
                roomsLiveData.setValue(NullSafeCollections.copyWithoutNulls(result.getRooms()));
                return;
            }

            if (InternetErrorBanner.isNetworkErrorMessage(result.getErrorMessage())) {
                networkBannerLiveData.setValue(new UiEvent<>(true));
            }
            errorLiveData.setValue(new UiEvent<>(MESSAGE_LOAD_ROOMS_FAILED));
        });
    }

    public Calendar getArrivalCalendar() {
        CreateBookingFormState state = currentState();
        return CreateBookingFormMapper.calendarFromMillis(state.getArrivalAtMillis());
    }

    public Calendar getDepartureCalendar() {
        CreateBookingFormState state = currentState();
        return CreateBookingFormMapper.calendarFromMillis(state.getDepartureAtMillis());
    }

    public boolean isCreating() {
        return Boolean.TRUE.equals(creatingLiveData.getValue());
    }

    public void updateArrivalDateTime(Calendar arrival) {
        CreateBookingFormState state = currentState();
        Calendar departure = CreateBookingFormMapper.calendarFromMillis(
                state.getDepartureAtMillis()
        );
        CreateBookingFormMapper.ensureDepartureAfterArrival(arrival, departure);
        updateDateTimes(state, arrival, departure);
    }

    public boolean updateDepartureDateTime(Calendar departure) {
        CreateBookingFormState state = currentState();
        Calendar arrival = CreateBookingFormMapper.calendarFromMillis(
                state.getArrivalAtMillis()
        );
        boolean adjusted = !departure.getTime().after(arrival.getTime());
        CreateBookingFormMapper.ensureDepartureAfterArrival(arrival, departure);
        updateDateTimes(state, arrival, departure);
        return !adjusted;
    }

    public void create(CreateBookingFormState formState) {
        if (createBookingCall != null || isCreating()) {
            return;
        }

        CreateBookingFormState state = formState.copy();
        state.setCreatedByName(localUserManager.getUserName());

        CreateBookingValidationResult validationResult =
                CreateBookingFormMapper.validate(state);
        if (!validationResult.isValid()) {
            validationLiveData.setValue(new UiEvent<>(validationResult));
            return;
        }

        formStateLiveData.setValue(state.copy());
        creatingLiveData.setValue(true);

        Call<ApiResponse<BookingActionData>> call = bookingRepository.createBooking(
                CreateBookingFormMapper.toCreateRequest(state)
        );
        createBookingCall = call;
        call.enqueue(new Callback<ApiResponse<BookingActionData>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<BookingActionData>> call,
                    @NonNull Response<ApiResponse<BookingActionData>> response
            ) {
                if (call != createBookingCall) return;
                createBookingCall = null;
                creatingLiveData.setValue(false);
                networkBannerLiveData.setValue(new UiEvent<>(false));

                if (!response.isSuccessful() || response.body() == null) {
                    String message = ApiErrorUtils.messageFromResponse(
                            response,
                            MESSAGE_CREATE_FAILED
                    );
                    handleCreateFailure(message, null);
                    return;
                }

                ApiResponse<BookingActionData> apiResponse = response.body();
                if (!apiResponse.isSuccess()) {
                    String message = ApiErrorUtils.messageFromApiResponse(
                            apiResponse,
                            MESSAGE_CREATE_FAILED
                    );
                    handleCreateFailure(message, null);
                    return;
                }

                resultLiveData.setValue(new UiEvent<>(
                        new CreateBookingResult(apiResponse.getSafeMessage())
                ));
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<BookingActionData>> call,
                    @NonNull Throwable t
            ) {
                if (call != createBookingCall) return;
                createBookingCall = null;
                creatingLiveData.setValue(false);
                if (!call.isCanceled()) {
                    networkBannerLiveData.setValue(new UiEvent<>(true));
                    handleCreateFailure(ApiErrorUtils.networkMessage(), t);
                }
            }
        });
    }

    private void handleCreateFailure(String message, Throwable throwable) {
        AppDiagnostics.logBookingMutationFailure(
                "create",
                null,
                message,
                throwable
        );
        errorLiveData.setValue(new UiEvent<>(
                CreateBookingFormMapper.makeFriendlyMessage(message)
        ));
    }

    private void updateDateTimes(
            CreateBookingFormState state,
            Calendar arrival,
            Calendar departure
    ) {
        CreateBookingFormMapper.applyDateTimes(
                state,
                arrival,
                departure,
                apiDateTimeFormat
        );
        formStateLiveData.setValue(state.copy());
    }

    private CreateBookingFormState currentState() {
        CreateBookingFormState state = formStateLiveData.getValue();
        return state != null ? state.copy() : new CreateBookingFormState();
    }

    @Override
    protected void onCleared() {
        if (createBookingCall != null && !createBookingCall.isCanceled()) {
            createBookingCall.cancel();
        }
        createBookingCall = null;
        super.onCleared();
    }
}
