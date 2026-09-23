package com.example.roombooking.booking;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roombooking.model.booking.BookingActionData;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.model.room.RoomPrefix;
import com.example.roombooking.utils.ApiErrorUtils;
import com.example.roombooking.utils.AppDiagnostics;
import com.example.roombooking.utils.DateTimeUtils;
import com.example.roombooking.utils.InternetErrorBanner;
import com.example.roombooking.utils.NullSafeCollections;
import com.example.roombooking.utils.UiEvent;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateBookingViewModel extends ViewModel {

    private static final String MESSAGE_CREATE_FAILED = "Booking creation failed.";
    private static final String MESSAGE_LOAD_ROOMS_FAILED =
            "Rooms could not be loaded. Please try again.";

    private final BookingRepository bookingRepository;
    private final AvailabilityRepository availabilityRepository;
    private final SimpleDateFormat apiDateTimeFormat =
            DateTimeUtils.newApiDateTimeFormat();

    private final MutableLiveData<CreateBookingFormState> formStateLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<List<AvailableRoomItem>> roomsLiveData =
            new MutableLiveData<>();
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
    private final List<Call<ApiResponse<AvailableRoomsRangeResponse>>> availableRoomsCalls =
            new ArrayList<>();
    private int availableRoomsLoadGeneration = 0;

    public CreateBookingViewModel(
            BookingRepository bookingRepository,
            AvailabilityRepository availabilityRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.availabilityRepository = availabilityRepository;
    }

    public LiveData<CreateBookingFormState> getFormStateLiveData() {
        return formStateLiveData;
    }

    public LiveData<List<AvailableRoomItem>> getRoomsLiveData() {
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
        formStateLiveData.setValue(state);
    }

    public void loadRooms() {
        CreateBookingFormState state = currentState();
        String arrivalDate = dateOnly(state.getArrivalAt());
        String departureDate = dateOnly(state.getDepartureAt());

        if (arrivalDate.isEmpty() || departureDate.isEmpty()) {
            roomsLiveData.setValue(new ArrayList<>());
            return;
        }

        cancelAvailableRoomsCalls();
        int generation = ++availableRoomsLoadGeneration;
        loadAvailableRoomsForPrefix(
                generation,
                RoomPrefix.displayOrder(),
                0,
                arrivalDate,
                departureDate,
                new ArrayList<>()
        );
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

    public void replaceDateTimes(Calendar arrival, Calendar departure) {
        CreateBookingFormState state = currentState();
        CreateBookingFormMapper.ensureDepartureAfterArrival(arrival, departure);
        updateDateTimes(state, arrival, departure);
    }

    public void create(CreateBookingFormState formState) {
        create(formState, false);
    }

    public void createAndGenerateMailTemplate(CreateBookingFormState formState) {
        create(formState, true);
    }

    private void create(CreateBookingFormState formState, boolean generateMailTemplate) {
        if (createBookingCall != null || isCreating()) {
            return;
        }

        CreateBookingFormState state = formState.copy();

        CreateBookingValidationResult validationResult =
                CreateBookingFormMapper.validate(state);
        if (!validationResult.isValid()) {
            validationLiveData.setValue(new UiEvent<>(validationResult));
            return;
        }

        formStateLiveData.setValue(state.copy());
        creatingLiveData.setValue(true);

        BookingCreateRequest request = CreateBookingFormMapper.toCreateRequest(state);
        Call<ApiResponse<BookingActionData>> call = generateMailTemplate
                ? bookingRepository.createBookingMailTemplate(request)
                : bookingRepository.createBooking(request);
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

                bookingRepository.clearFirstPageCaches();
                bookingRepository.clearAvailabilityCachesForBookingMutation();
                BookingActionData actionData = apiResponse.getData();
                String createdStatus = actionData != null ? actionData.getSafeStatus() : "";
                resultLiveData.setValue(new UiEvent<>(
                        new CreateBookingResult(
                                apiResponse.getSafeMessage(),
                                createdStatus,
                                actionData != null ? actionData.getMailTemplate() : null
                        )
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
        loadRooms();
    }

    private void loadAvailableRoomsForPrefix(
            int generation,
            List<String> prefixes,
            int prefixIndex,
            String arrivalDate,
            String departureDate,
            List<AvailableRoomItem> accumulatedRooms
    ) {
        if (generation != availableRoomsLoadGeneration) {
            return;
        }

        if (prefixIndex >= prefixes.size()) {
            networkBannerLiveData.setValue(new UiEvent<>(false));
            roomsLiveData.setValue(NullSafeCollections.copyWithoutNulls(accumulatedRooms));
            return;
        }

        String prefix = prefixes.get(prefixIndex);
        Call<ApiResponse<AvailableRoomsRangeResponse>> call =
                availabilityRepository.getAvailableRoomsByDateRange(
                        arrivalDate,
                        departureDate,
                        prefix
                );
        availableRoomsCalls.add(call);
        call.enqueue(new Callback<ApiResponse<AvailableRoomsRangeResponse>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<AvailableRoomsRangeResponse>> call,
                    @NonNull Response<ApiResponse<AvailableRoomsRangeResponse>> response
            ) {
                availableRoomsCalls.remove(call);
                if (generation != availableRoomsLoadGeneration) {
                    return;
                }

                if (!response.isSuccessful()
                        || response.body() == null
                        || !response.body().isSuccess()
                        || response.body().getData() == null) {
                    handleAvailableRoomsFailure(ApiErrorUtils.messageFromResponse(
                            response,
                            MESSAGE_LOAD_ROOMS_FAILED
                    ));
                    return;
                }

                AvailableRoomsRangeResponse data = response.body().getData();
                accumulatedRooms.addAll(NullSafeCollections.copyWithoutNulls(data.getRooms()));
                loadAvailableRoomsForPrefix(
                        generation,
                        prefixes,
                        prefixIndex + 1,
                        arrivalDate,
                        departureDate,
                        accumulatedRooms
                );
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<AvailableRoomsRangeResponse>> call,
                    @NonNull Throwable t
            ) {
                availableRoomsCalls.remove(call);
                if (generation != availableRoomsLoadGeneration || call.isCanceled()) {
                    return;
                }

                handleAvailableRoomsFailure(ApiErrorUtils.messageFromThrowable(t));
            }
        });
    }

    private void handleAvailableRoomsFailure(String message) {
        cancelAvailableRoomsCalls();
        roomsLiveData.setValue(new ArrayList<>());
        if (InternetErrorBanner.isNetworkErrorMessage(message)) {
            networkBannerLiveData.setValue(new UiEvent<>(true));
        }
        errorLiveData.setValue(new UiEvent<>(MESSAGE_LOAD_ROOMS_FAILED));
    }

    private void cancelAvailableRoomsCalls() {
        availableRoomsLoadGeneration++;
        List<Call<ApiResponse<AvailableRoomsRangeResponse>>> calls =
                new ArrayList<>(availableRoomsCalls);
        availableRoomsCalls.clear();
        for (Call<?> call : calls) {
            if (call != null && !call.isCanceled()) {
                call.cancel();
            }
        }
    }

    private String dateOnly(String value) {
        if (value == null || value.trim().length() < 10) {
            return "";
        }
        return value.trim().substring(0, 10);
    }

    private CreateBookingFormState currentState() {
        CreateBookingFormState state = formStateLiveData.getValue();
        return state != null ? state.copy() : new CreateBookingFormState();
    }

    @Override
    protected void onCleared() {
        cancelAvailableRoomsCalls();
        if (createBookingCall != null && !createBookingCall.isCanceled()) {
            createBookingCall.cancel();
        }
        createBookingCall = null;
        super.onCleared();
    }
}
