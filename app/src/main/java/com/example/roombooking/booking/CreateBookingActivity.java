package com.example.roombooking.booking;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextWatcher;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.roombooking.R;
import com.example.roombooking.admin.BookingRequestDecisionRequest;
import com.example.roombooking.auth.AuthSessionGuard;
import com.example.roombooking.api.RetrofitClient;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.model.room.RoomItem;
import com.example.roombooking.requester.BookingRequestItem;
import com.example.roombooking.room.RoomRepository;
import com.example.roombooking.utils.ApiErrorUtils;
import com.example.roombooking.utils.NullSafeCollections;
import com.example.roombooking.utils.EdgeToEdgeUtils;
import com.example.roombooking.utils.AppToolbarMenu;
import com.example.roombooking.utils.InternetErrorBanner;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateBookingActivity extends AppCompatActivity {

    private static final String EXTRA_BOOKING_CREATED = "booking_created";

    public static final String EXTRA_ROOM_ID = "room_id";
    public static final String EXTRA_ROOM_NAME = "room_name";
    public static final String EXTRA_ARRIVAL_DATE = "arrival_date";
    public static final String EXTRA_DEPARTURE_DATE = "departure_date";
    public static final String EXTRA_IS_PARTIAL_ROOM = "is_partial_room";
    public static final String EXTRA_AVAILABLE_FROM_DATE = "available_from_date";
    public static final String EXTRA_AVAILABLE_FROM_TIME = "available_from_time";
    public static final String EXTRA_BOOKING_REQUEST_ID = "booking_request_id";
    public static final String EXTRA_VISITOR_NAME = "visitor_name";
    public static final String EXTRA_VISITOR_DESIGNATION = "visitor_designation";
    public static final String EXTRA_VISITOR_ORGANISATION = "visitor_organisation";
    public static final String EXTRA_VISITOR_GENDER = "visitor_gender";
    public static final String EXTRA_VISITOR_MOBILE = "visitor_mobile";
    public static final String EXTRA_VISITOR_EMAIL = "visitor_email";
    public static final String EXTRA_VISITOR_CATEGORY = "visitor_category";
    public static final String EXTRA_PURPOSE_OF_VISIT = "purpose_of_visit";
    public static final String EXTRA_REQUESTOR_NAME = "requestor_name";
    public static final String EXTRA_REQUESTOR_DESIGNATION = "requestor_designation";
    public static final String EXTRA_REQUESTOR_DEPARTMENT = "requestor_department";
    public static final String EXTRA_REQUESTOR_MOBILE = "requestor_mobile";
    public static final String EXTRA_ATTENDER_REQUIRED = "attender_required";
    public static final String EXTRA_ATTENDER_COUNT_PER_DAY = "attender_count_per_day";
    public static final String EXTRA_ATTENDER_GENERAL_SHIFT = "attender_general_shift";
    public static final String EXTRA_ATTENDER_MORNING_SHIFT = "attender_morning_shift";
    public static final String EXTRA_ATTENDER_DAY_SHIFT = "attender_day_shift";

    private EditText etVisitorName;
    private EditText etVisitorDesignation;
    private EditText etVisitorOrganisation;
    private Spinner spinnerGender;
    private EditText etVisitorMobile;
    private EditText etVisitorEmail;

    private EditText etArrivalDT;
    private EditText etDepartureDT;
    private EditText etPurpose;

    private EditText etRequestorName;
    private EditText etRequestorDesignation;
    private EditText etRequestorDepartment;
    private EditText etRequestorMobile;

    private EditText etLogisticsName;
    private EditText etLogisticsDesignation;
    private EditText etLogisticsMobile;

    private Spinner spinnerRoom;
    private TextView tvMessage;
    private TextView tvPartialRoomInfo;
    private ScrollView scrollCreateBooking;

    private Button btnCreateBooking;
    private Button btnFillDummy;
    private Button btnSendBackBooking;
    private Button btnDeleteBookingRequest;
    private ImageButton btnBack;

    private RadioGroup rgVisitorCategory;
    private RadioGroup rgRoomChargesStatus;
    private RadioGroup rgAttenderChargesStatus;
    private CheckBox cbBudgetHeadName;
    private CheckBox cbBudgetHeadDepartmentName;
    private CheckBox cbBudgetHeadProjectCode;
    private EditText etRoomChargesAmount;
    private EditText etAttenderChargesAmount;
    private EditText etBudgetHeadName;
    private EditText etBudgetHeadDepartmentName;
    private EditText etBudgetHeadProjectCode;
    private CheckBox cbAttenderRequired;
    private TextView tvSelectShiftLabel;
    private EditText etAttenderCount;
    private CheckBox cbGeneralShift;
    private CheckBox cbMorningShift;
    private CheckBox cbDayShift;

    private CreateBookingViewModel viewModel;
    private CreateBookingFormState currentFormState;
    private int bookingRequestId = -1;
    private boolean bookingRequestApprovalMode = false;
    private boolean approvalRequestInFlight = false;
    private boolean suppressBudgetHeadFocus = false;
    private Call<ApiResponse<BookingRequestItem>> approveRequestCall;
    private Call<ApiResponse<BookingRequestItem>> rejectRequestCall;
    private Call<ApiResponse<BookingRequestItem>> sendBackRequestCall;
    private Call<ApiResponse<BookingRequestItem>> deleteRequestCall;
    private final Random random = new Random();

    private final SimpleDateFormat displayFormat =
            new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    private RoomSpinnerAdapter roomAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_booking);
        if (!AuthSessionGuard.ensureAdmin(this)) {
            return;
        }

        EdgeToEdgeUtils.applySystemBarInsets(this, findViewById(R.id.rootView));

        initDependencies();
        bindViews();
        setupScrollInsets();
        AppToolbarMenu.setup(this, findViewById(R.id.appToolbar));
        configureBookingRequestApprovalMode();
        setupRoomSpinner();
        setupGenderSpinner();
        setupListeners();
        setupBackPressHandler();
        observeViewModel();

        viewModel.initialize(readInitialDataFromIntent());
        applyBookingRequestPrefillFromIntent();
        viewModel.loadRooms();
    }

    private void initDependencies() {
        BookingRepository bookingRepository = new BookingRepository(getApplicationContext());
        RoomRepository roomRepository = new RoomRepository(getApplicationContext());
        CreateBookingViewModelFactory factory = new CreateBookingViewModelFactory(
                bookingRepository,
                roomRepository
        );
        viewModel = new ViewModelProvider(this, factory).get(CreateBookingViewModel.class);
    }

    private void bindViews() {
        etVisitorName = findViewById(R.id.etVisitorName);
        etVisitorDesignation = findViewById(R.id.etVisitorDesignation);
        etVisitorOrganisation = findViewById(R.id.etVisitorOrganisation);
        spinnerGender = findViewById(R.id.spinnerGender);
        etVisitorMobile = findViewById(R.id.etVisitorMobile);
        etVisitorEmail = findViewById(R.id.etVisitorEmail);

        etArrivalDT = findViewById(R.id.etArrivalDT);
        etDepartureDT = findViewById(R.id.etDepartureDT);
        etPurpose = findViewById(R.id.etPurpose);

        etRequestorName = findViewById(R.id.etRequestorName);
        etRequestorDesignation = findViewById(R.id.etRequestorDesignation);
        etRequestorDepartment = findViewById(R.id.etRequestorDepartment);
        etRequestorMobile = findViewById(R.id.etRequestorMobile);

        etLogisticsName = findViewById(R.id.etLogisticsName);
        etLogisticsDesignation = findViewById(R.id.etLogisticsDesignation);
        etLogisticsMobile = findViewById(R.id.etLogisticsMobile);

        spinnerRoom = findViewById(R.id.spinnerRoom);
        tvMessage = findViewById(R.id.tvMessage);
        tvPartialRoomInfo = findViewById(R.id.tvPartialRoomInfo);
        scrollCreateBooking = findViewById(R.id.scrollCreateBooking);

        btnCreateBooking = findViewById(R.id.btnCreateBooking);
        btnFillDummy = findViewById(R.id.btnFillDummy);
        btnSendBackBooking = findViewById(R.id.btnSendBackBooking);
        btnDeleteBookingRequest = findViewById(R.id.btnDeleteBookingRequest);
        btnBack = findViewById(R.id.btnBack);

        rgVisitorCategory = findViewById(R.id.rgVisitorCategory);
        rgRoomChargesStatus = findViewById(R.id.rgRoomChargesStatus);
        rgAttenderChargesStatus = findViewById(R.id.rgAttenderChargesStatus);
        cbBudgetHeadName = findViewById(R.id.cbBudgetHeadName);
        cbBudgetHeadDepartmentName = findViewById(R.id.cbBudgetHeadDepartmentName);
        cbBudgetHeadProjectCode = findViewById(R.id.cbBudgetHeadProjectCode);
        etRoomChargesAmount = findViewById(R.id.etRoomChargesAmount);
        etAttenderChargesAmount = findViewById(R.id.etAttenderChargesAmount);
        etBudgetHeadName = findViewById(R.id.etBudgetHeadName);
        etBudgetHeadDepartmentName = findViewById(R.id.etBudgetHeadDepartmentName);
        etBudgetHeadProjectCode = findViewById(R.id.etBudgetHeadProjectCode);

        cbAttenderRequired = findViewById(R.id.cbAttenderRequired);
        etAttenderCount = findViewById(R.id.etAttenderCount);
        tvSelectShiftLabel = findViewById(R.id.tvSelectShiftLabel);
        cbGeneralShift = findViewById(R.id.cbGeneralShift);
        cbMorningShift = findViewById(R.id.cbMorningShift);
        cbDayShift = findViewById(R.id.cbDayShift);
    }

    private void setupScrollInsets() {
        if (scrollCreateBooking == null) {
            return;
        }

        int initialLeft = scrollCreateBooking.getPaddingLeft();
        int initialTop = scrollCreateBooking.getPaddingTop();
        int initialRight = scrollCreateBooking.getPaddingRight();
        int initialBottom = scrollCreateBooking.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(scrollCreateBooking, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

            view.setPadding(
                    initialLeft,
                    initialTop,
                    initialRight,
                    initialBottom + Math.max(systemBars.bottom, ime.bottom)
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(scrollCreateBooking);
    }

    private void setupRoomSpinner() {
        roomAdapter = new RoomSpinnerAdapter(this, new ArrayList<>());
        spinnerRoom.setAdapter(roomAdapter);
    }

    private void setupGenderSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.gender_options,
                android.R.layout.simple_spinner_item
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(adapter);
    }

    private void setupListeners() {
        etArrivalDT.setOnClickListener(v -> {
            Calendar selectedDateTime = viewModel.getArrivalCalendar();
            pickDateOrTime(
                    selectedDateTime,
                    () -> viewModel.updateArrivalDateTime(selectedDateTime)
            );
        });

        etDepartureDT.setOnClickListener(v -> {
            Calendar selectedDateTime = viewModel.getDepartureCalendar();
            pickDateOrTime(
                    selectedDateTime,
                    () -> {
                        if (!viewModel.updateDepartureDateTime(selectedDateTime)) {
                            showError("Departure must be after arrival.");
                        }
                    }
            );
        });

        btnCreateBooking.setOnClickListener(v -> submitBooking());
        if (bookingRequestApprovalMode) {
            btnCreateBooking.setText("Approve");
            btnFillDummy.setText("Reject");
            btnFillDummy.setOnClickListener(v -> showRejectBookingRequestDialog());
            btnSendBackBooking.setVisibility(View.VISIBLE);
            btnSendBackBooking.setOnClickListener(v -> showSendBackBookingRequestDialog());
            btnDeleteBookingRequest.setVisibility(View.VISIBLE);
            btnDeleteBookingRequest.setOnClickListener(v -> showDeleteBookingRequestDialog());
        } else {
            btnSendBackBooking.setVisibility(View.GONE);
            btnDeleteBookingRequest.setVisibility(View.GONE);
            btnFillDummy.setOnClickListener(v -> fillRandomBookingData());
        }
        btnBack.setOnClickListener(v -> handleBackPress());

        setupChargeAmountListener(
                rgRoomChargesStatus,
                etRoomChargesAmount,
                R.id.rbRoomChargesYes
        );
        setupChargeAmountListener(
                rgAttenderChargesStatus,
                etAttenderChargesAmount,
                R.id.rbAttenderChargesYes
        );

        setupClearRadioAction(R.id.btnClearVisitorCategory, rgVisitorCategory);
        setupBudgetHeadFocusControls();
        setupAttenderRequirementControls();
    }

    private void observeViewModel() {
        viewModel.getFormStateLiveData().observe(this, state -> {
            if (state == null) return;

            currentFormState = state.copy();
            refreshDateTimeFields(currentFormState);
            showPartialRoomInfoIfNeeded(currentFormState);
            updateRoomSelectionState();
        });

        viewModel.getRoomsLiveData().observe(this, rooms -> {
            InternetErrorBanner.hide(CreateBookingActivity.this);
            bindRoomsToSpinner(rooms);
            preselectRoomIfAvailable();
        });

        viewModel.getCreatingLiveData().observe(this, creating ->
                setLoading(Boolean.TRUE.equals(creating))
        );

        viewModel.getNetworkBannerLiveData().observe(this, event -> {
            if (event == null) return;

            Boolean shouldShow = event.getContentIfNotHandled();
            if (shouldShow == null) return;

            if (shouldShow) {
                InternetErrorBanner.show(this);
            } else {
                InternetErrorBanner.hide(this);
            }
        });

        viewModel.getErrorLiveData().observe(this, event -> {
            if (event == null) return;

            String message = event.getContentIfNotHandled();
            if (!isBlank(message)) {
                showError(message);
            }
        });

        viewModel.getValidationLiveData().observe(this, event -> {
            if (event == null) return;

            CreateBookingValidationResult result = event.getContentIfNotHandled();
            if (result != null && !result.isValid()) {
                handleValidationError(result);
            }
        });

        viewModel.getResultLiveData().observe(this, event -> {
            if (event == null) return;

            CreateBookingResult result = event.getContentIfNotHandled();
            if (result != null) {
                handleCreateSuccess(result);
            }
        });
    }

    private void configureBookingRequestApprovalMode() {
        Intent intent = getIntent();
        bookingRequestId = intent != null
                ? intent.getIntExtra(EXTRA_BOOKING_REQUEST_ID, -1)
                : -1;
        bookingRequestApprovalMode = bookingRequestId > 0;
    }

    private CreateBookingInitialData readInitialDataFromIntent() {
        Intent intent = getIntent();

        if (intent == null) {
            return new CreateBookingInitialData(null, "", "", "", false, "", "");
        }

        Integer roomId = null;
        if (intent.hasExtra(EXTRA_ROOM_ID)) {
            int rawRoomId = intent.getIntExtra(EXTRA_ROOM_ID, -1);
            roomId = rawRoomId != -1 ? rawRoomId : null;
        }

        return new CreateBookingInitialData(
                roomId,
                intent.getStringExtra(EXTRA_ROOM_NAME),
                intent.getStringExtra(EXTRA_ARRIVAL_DATE),
                intent.getStringExtra(EXTRA_DEPARTURE_DATE),
                intent.getBooleanExtra(EXTRA_IS_PARTIAL_ROOM, false),
                intent.getStringExtra(EXTRA_AVAILABLE_FROM_DATE),
                intent.getStringExtra(EXTRA_AVAILABLE_FROM_TIME)
        );
    }

    private void showPartialRoomInfoIfNeeded(CreateBookingFormState state) {
        if (state == null || !state.isPartialRoom()) {
            tvPartialRoomInfo.setVisibility(View.GONE);
            return;
        }

        tvPartialRoomInfo.setVisibility(View.VISIBLE);
        tvPartialRoomInfo.setText(
                "This room is partially available.\nAvailable from: "
                        + safeText(state.getAvailableFromDate())
                        + ", "
                        + safeText(state.getAvailableFromTime())
        );
    }

    private void bindRoomsToSpinner(List<RoomItem> rooms) {
        List<RoomSpinnerEntry> entries = RoomSpinnerEntries.build(
                NullSafeCollections.copyWithoutNulls(rooms)
        );
        roomAdapter.clear();
        roomAdapter.addAll(entries);
        roomAdapter.notifyDataSetChanged();
    }

    private void preselectRoomIfAvailable() {
        if (currentFormState == null || currentFormState.getRoomId() == null) {
            updateRoomSelectionState();
            return;
        }

        int selectedRoomId = currentFormState.getRoomId();
        for (int i = 0; i < roomAdapter.getCount(); i++) {
            RoomSpinnerEntry entry = roomAdapter.getItem(i);
            RoomItem roomItem = entry != null ? entry.getRoom() : null;

            if (roomItem != null && roomItem.getId() == selectedRoomId) {
                spinnerRoom.setSelection(i);
                updateRoomSelectionState();
                return;
            }
        }

        if (!isBlank(currentFormState.getPreselectedRoomName())) {
            showMessage("Selected room: " + currentFormState.getPreselectedRoomName());
        }
        updateRoomSelectionState();
    }

    private void updateRoomSelectionState() {
        boolean hasPreselectedRoom = currentFormState != null
                && currentFormState.hasPreselectedRoom()
                && !bookingRequestApprovalMode;
        spinnerRoom.setEnabled(!hasPreselectedRoom);
        spinnerRoom.setClickable(!hasPreselectedRoom);
        spinnerRoom.setFocusable(!hasPreselectedRoom);
        spinnerRoom.setAlpha(hasPreselectedRoom ? 0.65f : 1.0f);
    }

    private void applyBookingRequestPrefillFromIntent() {
        if (!bookingRequestApprovalMode) {
            return;
        }

        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        etVisitorName.setText(intent.getStringExtra(EXTRA_VISITOR_NAME));
        etVisitorDesignation.setText(intent.getStringExtra(EXTRA_VISITOR_DESIGNATION));
        etVisitorOrganisation.setText(intent.getStringExtra(EXTRA_VISITOR_ORGANISATION));
        etVisitorMobile.setText(intent.getStringExtra(EXTRA_VISITOR_MOBILE));
        etVisitorEmail.setText(intent.getStringExtra(EXTRA_VISITOR_EMAIL));
        etPurpose.setText(intent.getStringExtra(EXTRA_PURPOSE_OF_VISIT));

        etRequestorName.setText(intent.getStringExtra(EXTRA_REQUESTOR_NAME));
        etRequestorDesignation.setText(intent.getStringExtra(EXTRA_REQUESTOR_DESIGNATION));
        etRequestorDepartment.setText(intent.getStringExtra(EXTRA_REQUESTOR_DEPARTMENT));
        etRequestorMobile.setText(intent.getStringExtra(EXTRA_REQUESTOR_MOBILE));

        setGenderSelection(intent.getStringExtra(EXTRA_VISITOR_GENDER));
        setVisitorCategorySelection(intent.getStringExtra(EXTRA_VISITOR_CATEGORY));

        boolean attenderRequired = intent.getBooleanExtra(EXTRA_ATTENDER_REQUIRED, false);
        cbAttenderRequired.setChecked(attenderRequired);
        if (attenderRequired) {
            int count = intent.getIntExtra(EXTRA_ATTENDER_COUNT_PER_DAY, 0);
            etAttenderCount.setText(count > 0 ? String.valueOf(count) : "");
            cbGeneralShift.setChecked(intent.getBooleanExtra(EXTRA_ATTENDER_GENERAL_SHIFT, false));
            cbMorningShift.setChecked(intent.getBooleanExtra(EXTRA_ATTENDER_MORNING_SHIFT, false));
            cbDayShift.setChecked(intent.getBooleanExtra(EXTRA_ATTENDER_DAY_SHIFT, false));
            updateAttenderControlsState();
        }

        showMessage("Review requester details before creating or rejecting this booking.");
    }

    private void setGenderSelection(String gender) {
        if (isBlank(gender) || spinnerGender == null) {
            return;
        }

        for (int i = 0; i < spinnerGender.getCount(); i++) {
            Object item = spinnerGender.getItemAtPosition(i);
            if (item != null && gender.equalsIgnoreCase(item.toString())) {
                spinnerGender.setSelection(i);
                return;
            }
        }
    }

    private void setVisitorCategorySelection(String visitorCategory) {
        if ("institute_guest".equalsIgnoreCase(visitorCategory)) {
            rgVisitorCategory.check(R.id.rbInstituteGuest);
        } else if ("conference_workshop_guest".equalsIgnoreCase(visitorCategory)) {
            rgVisitorCategory.check(R.id.rbConferenceGuest);
        } else if ("other_guest".equalsIgnoreCase(visitorCategory)) {
            rgVisitorCategory.check(R.id.rbOtherGuest);
        }
    }

    private Integer getSelectedRoomId() {
        if (currentFormState != null
                && currentFormState.hasPreselectedRoom()
                && currentFormState.getRoomId() != null) {
            return currentFormState.getRoomId();
        }

        int position = spinnerRoom.getSelectedItemPosition();

        RoomSpinnerEntry entry = roomAdapter.getItem(position);
        if (entry == null || entry.getRoom() == null) {
            return null;
        }
        return entry.getRoom().getId();
    }

    private String getSelectedGender() {
        int position = spinnerGender.getSelectedItemPosition();

        if (position == 0) {
            return null;
        }

        return spinnerGender.getSelectedItem().toString();
    }

    private String getSelectedVisitorCategory() {
        int checkedId = rgVisitorCategory.getCheckedRadioButtonId();

        if (checkedId == R.id.rbInstituteGuest) {
            return "institute_guest";
        }

        if (checkedId == R.id.rbConferenceGuest) {
            return "conference_workshop_guest";
        }

        if (checkedId == R.id.rbOtherGuest) {
            return "other_guest";
        }

        return "";
    }

    private int getAttenderCount() {
        String countText = getText(etAttenderCount);

        if (countText.isEmpty()) {
            return 0;
        }

        try {
            return Integer.parseInt(countText);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void refreshDateTimeFields(CreateBookingFormState state) {
        etArrivalDT.setText(displayFormat.format(new java.util.Date(state.getArrivalAtMillis())));
        etDepartureDT.setText(displayFormat.format(new java.util.Date(state.getDepartureAtMillis())));
    }

    private void pickDateOrTime(Calendar target, Runnable onDone) {
        if (currentFormState != null && currentFormState.hasPreselectedDateRange()) {
            showTimePicker(target, onDone);
            return;
        }

        pickDateTime(target, onDone);
    }

    private void pickDateTime(Calendar target, Runnable onDone) {
        int year = target.get(Calendar.YEAR);
        int month = target.get(Calendar.MONTH);
        int day = target.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    target.set(Calendar.YEAR, selectedYear);
                    target.set(Calendar.MONTH, selectedMonth);
                    target.set(Calendar.DAY_OF_MONTH, selectedDay);

                    showTimePicker(target, onDone);
                },
                year,
                month,
                day
        );

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void showTimePicker(Calendar target, Runnable onDone) {
        int hour = target.get(Calendar.HOUR_OF_DAY);
        int minute = target.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (timeView, selectedHour, selectedMinute) -> {
                    target.set(Calendar.HOUR_OF_DAY, selectedHour);
                    target.set(Calendar.MINUTE, selectedMinute);
                    target.set(Calendar.SECOND, 0);
                    target.set(Calendar.MILLISECOND, 0);

                    onDone.run();
                },
                hour,
                minute,
                true
        );

        timePickerDialog.show();
    }

    private void setupAttenderRequirementControls() {
        cbAttenderRequired.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                etAttenderCount.setText("");
                etAttenderCount.setError(null);
                clearAttenderShifts();
            }

            updateAttenderControlsState();
        });

        etAttenderCount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action required.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No action required.
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateAttenderControlsState();
            }
        });

        updateAttenderControlsState();
    }

    private void updateAttenderControlsState() {
        boolean attenderRequired = cbAttenderRequired.isChecked();

        setViewEnabled(etAttenderCount, attenderRequired);

        boolean validAttenderCount = attenderRequired && getAttenderCount() > 0;

        setShiftControlsEnabled(validAttenderCount);

        if (!validAttenderCount) {
            clearAttenderShifts();
        }
    }

    private void setShiftControlsEnabled(boolean enabled) {
        setViewEnabled(cbGeneralShift, enabled);
        setViewEnabled(cbMorningShift, enabled);
        setViewEnabled(cbDayShift, enabled);

        if (tvSelectShiftLabel != null) {
            setViewEnabled(tvSelectShiftLabel, enabled);
        }
    }

    private void setViewEnabled(View view, boolean enabled) {
        if (view == null) {
            return;
        }

        view.setEnabled(enabled);
        view.setAlpha(enabled ? 1.0f : 0.45f);
    }

    private void clearAttenderShifts() {
        cbGeneralShift.setChecked(false);
        cbMorningShift.setChecked(false);
        cbDayShift.setChecked(false);
    }

    private void submitBooking() {
        if (isBusy()) {
            return;
        }

        tvMessage.setVisibility(View.GONE);

        if (bookingRequestApprovalMode) {
            approveBookingRequestFromForm();
            return;
        }

        viewModel.create(collectFormData());
    }

    @Override
    protected void onResume() {
        super.onResume();
        AuthSessionGuard.ensureAdmin(this);
    }

    private void handleBackPress() {
        if (isBusy()) {
            showMessage("Please wait for the booking request to finish.");
            return;
        }
        finish();
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPress();
            }
        });
    }

    private CreateBookingFormState collectFormData() {
        CreateBookingFormState data = currentFormState != null
                ? currentFormState.copy()
                : new CreateBookingFormState();

        data.setVisitorName(getText(etVisitorName));
        data.setVisitorDesignation(getText(etVisitorDesignation));
        data.setVisitorOrganisation(getText(etVisitorOrganisation));
        data.setVisitorGender(getSelectedGender());
        data.setVisitorMobile(getText(etVisitorMobile));
        data.setVisitorEmail(getText(etVisitorEmail));

        data.setPurpose(getText(etPurpose));

        data.setRequestorName(getText(etRequestorName));
        data.setRequestorDesignation(getText(etRequestorDesignation));
        data.setRequestorDepartment(getText(etRequestorDepartment));
        data.setRequestorMobile(getText(etRequestorMobile));

        data.setLogisticsName(getText(etLogisticsName));
        data.setLogisticsDesignation(getText(etLogisticsDesignation));
        data.setLogisticsMobile(getText(etLogisticsMobile));

        data.setRoomId(getSelectedRoomId());
        data.setVisitorCategory(getSelectedVisitorCategory());

        data.setAttenderRequired(cbAttenderRequired.isChecked());
        data.setAttenderCountPerDay(getAttenderCount());
        data.setAttenderGeneralShift(cbGeneralShift.isChecked());
        data.setAttenderMorningShift(cbMorningShift.isChecked());
        data.setAttenderDayShift(cbDayShift.isChecked());
        data.setRoomChargesStatus(getChargeStatus(
                rgRoomChargesStatus,
                R.id.rbRoomChargesYes,
                R.id.rbRoomChargesWaived
        ));
        data.setAttenderChargesStatus(getChargeStatus(
                rgAttenderChargesStatus,
                R.id.rbAttenderChargesYes,
                R.id.rbAttenderChargesWaived
        ));
        data.setRoomChargesAmount("yes".equals(data.getRoomChargesStatus())
                ? getText(etRoomChargesAmount)
                : "0");
        data.setAttenderChargesAmount("yes".equals(data.getAttenderChargesStatus())
                ? getText(etAttenderChargesAmount)
                : "0");

        data.setBudgetHeadType("");
        data.setBudgetHeadValue("");
        data.setBudgetHeadName(getBudgetHeadText(cbBudgetHeadName, etBudgetHeadName));
        data.setBudgetHeadDepartmentName(
                getBudgetHeadText(cbBudgetHeadDepartmentName, etBudgetHeadDepartmentName)
        );
        data.setBudgetHeadProjectCode(
                getBudgetHeadText(cbBudgetHeadProjectCode, etBudgetHeadProjectCode)
        );

        return data;
    }

    private String getBudgetHeadText(CheckBox checkbox, EditText field) {
        return checkbox != null && checkbox.isChecked() ? getText(field) : "";
    }

    private void handleValidationError(CreateBookingValidationResult result) {
        if (CreateBookingFormState.FIELD_ROOM_CHARGES_AMOUNT.equals(result.getField())) {
            etRoomChargesAmount.setError("Room charges amount is required.");
            focusAndShowKeyboard(etRoomChargesAmount);
            showError(result.getMessage());
            return;
        }

        if (CreateBookingFormState.FIELD_ATTENDER_CHARGES_AMOUNT.equals(result.getField())) {
            etAttenderChargesAmount.setError("Attender charges amount is required.");
            focusAndShowKeyboard(etAttenderChargesAmount);
            showError(result.getMessage());
            return;
        }

        showError(result.getMessage());
    }

    private void handleCreateSuccess(CreateBookingResult result) {
        sendBookingCreatedResult();
        showMessage(result.getMessage());
        finish();
    }

    private Map<String, Object> toApprovalPayload(CreateBookingFormState data) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("room", data.getRoomId());
        payload.put("arrival_at", data.getArrivalAt());
        payload.put("departure_at", data.getDepartureAt());
        payload.put("visitor_name", data.getVisitorName());
        payload.put("visitor_designation", data.getVisitorDesignation());
        payload.put("visitor_organisation", data.getVisitorOrganisation());
        payload.put("visitor_gender", data.getVisitorGender());
        payload.put("visitor_mobile", data.getVisitorMobile());
        payload.put("visitor_email", data.getVisitorEmail());
        payload.put("purpose_of_visit", data.getPurpose());
        payload.put("visitor_category", data.getVisitorCategory());
        payload.put("attender_required", data.isAttenderRequired());
        payload.put("attender_count_per_day", data.getAttenderCountPerDay());
        payload.put("attender_general_shift", data.isAttenderGeneralShift());
        payload.put("attender_morning_shift", data.isAttenderMorningShift());
        payload.put("attender_day_shift", data.isAttenderDayShift());
        payload.put("room_charges_status", data.getRoomChargesStatus());
        payload.put("attender_charges_status", data.getAttenderChargesStatus());
        payload.put("room_charges_amount", data.getRoomChargesAmount());
        payload.put("attender_charges_amount", data.getAttenderChargesAmount());
        payload.put("budget_head_type", data.getBudgetHeadType());
        payload.put("budget_head_value", data.getBudgetHeadValue());
        payload.put("budget_head_name", data.getBudgetHeadName());
        payload.put("budget_head_department_name", data.getBudgetHeadDepartmentName());
        payload.put("budget_head_project_code", data.getBudgetHeadProjectCode());
        payload.put("requestor_name", data.getRequestorName());
        payload.put("requestor_designation", data.getRequestorDesignation());
        payload.put("requestor_department", data.getRequestorDepartment());
        payload.put("requestor_mobile", data.getRequestorMobile());
        payload.put("logistics_name", data.getLogisticsName());
        payload.put("logistics_designation", data.getLogisticsDesignation());
        payload.put("logistics_mobile", data.getLogisticsMobile());
        payload.put("remarks", "");
        return payload;
    }

    private void approveBookingRequestFromForm() {
        CreateBookingFormState state = collectFormData();
        CreateBookingValidationResult validationResult = CreateBookingFormMapper.validate(state);
        if (!validationResult.isValid()) {
            handleValidationError(validationResult);
            return;
        }

        setLoading(true);
        approvalRequestInFlight = true;
        approveRequestCall = RetrofitClient.getApiService(getApplicationContext())
                .approveBookingRequestFromForm(
                        bookingRequestId,
                        toApprovalPayload(state)
                );
        approveRequestCall.enqueue(new Callback<ApiResponse<BookingRequestItem>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<BookingRequestItem>> call,
                    @NonNull Response<ApiResponse<BookingRequestItem>> response
            ) {
                if (call != approveRequestCall) return;
                approveRequestCall = null;
                approvalRequestInFlight = false;
                setLoading(false);

                if (!response.isSuccessful()
                        || response.body() == null
                        || !response.body().isSuccess()) {
                    showError(ApiErrorUtils.messageFromResponse(
                            response,
                            "Booking request could not be approved."
                    ));
                    return;
                }

                new BookingRepository(getApplicationContext()).clearFirstPageCaches();
                new BookingRepository(getApplicationContext()).clearAvailabilityCachesForBookingMutation();
                sendBookingCreatedResult();
                showMessage("Booking created successfully.");
                finish();
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<BookingRequestItem>> call,
                    @NonNull Throwable t
            ) {
                if (call != approveRequestCall) return;
                approveRequestCall = null;
                approvalRequestInFlight = false;
                setLoading(false);
                if (!call.isCanceled()) {
                    showError(ApiErrorUtils.messageFromThrowable(t));
                }
            }
        });
    }

    private void showRejectBookingRequestDialog() {
        if (isBusy()) {
            return;
        }

        EditText remarks = new EditText(this);
        remarks.setHint("Remarks (optional)");
        remarks.setSingleLine(false);
        remarks.setMinLines(2);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Reject Booking Request?")
                .setView(remarks)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton("Reject", (dialog, which) -> rejectBookingRequest(getText(remarks)))
                .show();
    }

    private void rejectBookingRequest(String remarks) {
        setLoading(true);
        approvalRequestInFlight = true;
        rejectRequestCall = RetrofitClient.getApiService(getApplicationContext())
                .rejectBookingRequest(
                        bookingRequestId,
                        new BookingRequestDecisionRequest(null, remarks)
                );
        rejectRequestCall.enqueue(new Callback<ApiResponse<BookingRequestItem>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<BookingRequestItem>> call,
                    @NonNull Response<ApiResponse<BookingRequestItem>> response
            ) {
                if (call != rejectRequestCall) return;
                rejectRequestCall = null;
                approvalRequestInFlight = false;
                setLoading(false);

                if (!response.isSuccessful()
                        || response.body() == null
                        || !response.body().isSuccess()) {
                    showError(ApiErrorUtils.messageFromResponse(
                            response,
                            "Booking request could not be rejected."
                    ));
                    return;
                }

                sendBookingCreatedResult();
                showMessage("Booking request rejected.");
                finish();
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<BookingRequestItem>> call,
                    @NonNull Throwable t
            ) {
                if (call != rejectRequestCall) return;
                rejectRequestCall = null;
                approvalRequestInFlight = false;
                setLoading(false);
                if (!call.isCanceled()) {
                    showError(ApiErrorUtils.messageFromThrowable(t));
                }
            }
        });
    }

    private void showSendBackBookingRequestDialog() {
        if (isBusy()) {
            return;
        }

        EditText remarks = new EditText(this);
        remarks.setHint("Explain what needs to be corrected");
        remarks.setSingleLine(false);
        remarks.setMinLines(3);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Send Back for Correction")
                .setView(remarks)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton("Send Back", null)
                .create();
        dialog.setOnShowListener(shownDialog -> dialog
                .getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String value = getText(remarks);
                    if (value.isEmpty()) {
                        remarks.setError("Remarks are required.");
                        return;
                    }
                    dialog.dismiss();
                    sendBackBookingRequest(value);
                }));
        dialog.show();
    }

    private void sendBackBookingRequest(String remarks) {
        setLoading(true);
        approvalRequestInFlight = true;
        sendBackRequestCall = RetrofitClient.getApiService(getApplicationContext())
                .sendBackBookingRequest(
                        bookingRequestId,
                        new BookingRequestDecisionRequest(null, remarks)
                );
        sendBackRequestCall.enqueue(new Callback<ApiResponse<BookingRequestItem>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<BookingRequestItem>> call,
                    @NonNull Response<ApiResponse<BookingRequestItem>> response
            ) {
                if (call != sendBackRequestCall) return;
                sendBackRequestCall = null;
                approvalRequestInFlight = false;
                setLoading(false);

                if (!response.isSuccessful()
                        || response.body() == null
                        || !response.body().isSuccess()) {
                    showError(ApiErrorUtils.messageFromResponse(
                            response,
                            "Booking request could not be sent back for correction."
                    ));
                    return;
                }

                sendBookingCreatedResult();
                showMessage("Request sent back for correction.");
                finish();
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<BookingRequestItem>> call,
                    @NonNull Throwable t
            ) {
                if (call != sendBackRequestCall) return;
                sendBackRequestCall = null;
                approvalRequestInFlight = false;
                setLoading(false);
                if (!call.isCanceled()) {
                    showError(ApiErrorUtils.messageFromThrowable(t));
                }
            }
        });
    }

    private void showDeleteBookingRequestDialog() {
        if (isBusy()) {
            return;
        }

        TextView message = new TextView(this);
        message.setText("Are you sure you want to delete this booking request?");
        message.setTextColor(getColor(R.color.detail_text_primary));
        message.setTextSize(14);
        message.setPadding(0, 0, 0, getResources().getDimensionPixelSize(R.dimen.space_10));

        EditText remarks = new EditText(this);
        remarks.setHint("Remarks");
        remarks.setSingleLine(false);
        remarks.setMinLines(2);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int horizontalPadding = getResources().getDimensionPixelSize(R.dimen.space_20);
        content.setPadding(horizontalPadding, getResources().getDimensionPixelSize(R.dimen.space_6), horizontalPadding, 0);
        content.addView(message);
        content.addView(remarks);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Request")
                .setView(content)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton("Delete", (dialog, which) -> deleteBookingRequest(getText(remarks)))
                .show();
    }

    private void deleteBookingRequest(String remarks) {
        setLoading(true);
        approvalRequestInFlight = true;
        deleteRequestCall = RetrofitClient.getApiService(getApplicationContext())
                .deleteAdminBookingRequest(
                        bookingRequestId,
                        new BookingRequestDecisionRequest(null, remarks)
                );
        deleteRequestCall.enqueue(new Callback<ApiResponse<BookingRequestItem>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<BookingRequestItem>> call,
                    @NonNull Response<ApiResponse<BookingRequestItem>> response
            ) {
                if (call != deleteRequestCall) return;
                deleteRequestCall = null;
                approvalRequestInFlight = false;
                setLoading(false);

                if (!response.isSuccessful()
                        || response.body() == null
                        || !response.body().isSuccess()) {
                    showError(ApiErrorUtils.messageFromResponse(
                            response,
                            "Booking request could not be deleted."
                    ));
                    return;
                }

                sendBookingCreatedResult();
                showMessage("Booking request deleted successfully.");
                finish();
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<BookingRequestItem>> call,
                    @NonNull Throwable t
            ) {
                if (call != deleteRequestCall) return;
                deleteRequestCall = null;
                approvalRequestInFlight = false;
                setLoading(false);
                if (!call.isCanceled()) {
                    showError(ApiErrorUtils.messageFromThrowable(t));
                }
            }
        });
    }

    private void setupChargeAmountListener(RadioGroup group, EditText amountField, int yesId) {
        group.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            boolean enabled = checkedId == yesId;
            amountField.setEnabled(enabled);

            if (enabled) {
                focusAndShowKeyboard(amountField);
            } else {
                amountField.setText("");
                amountField.setError(null);
                amountField.clearFocus();
                hideKeyboard(amountField);
            }
        });

        View yesButton = group.findViewById(yesId);
        if (yesButton != null) {
            yesButton.setOnClickListener(v -> {
                if (group.getCheckedRadioButtonId() == yesId && amountField.isEnabled()) {
                    focusAndShowKeyboard(amountField);
                }
            });
        }
    }

    private void setupClearRadioAction(int clearButtonId, RadioGroup group) {
        View clearButton = findViewById(clearButtonId);
        if (clearButton == null || group == null) {
            return;
        }

        clearButton.setOnClickListener(v -> group.clearCheck());
    }

    private void setupBudgetHeadFocusControls() {
        setupBudgetHeadOption(cbBudgetHeadName, etBudgetHeadName);
        setupBudgetHeadOption(cbBudgetHeadDepartmentName, etBudgetHeadDepartmentName);
        setupBudgetHeadOption(cbBudgetHeadProjectCode, etBudgetHeadProjectCode);

        View clearButton = findViewById(R.id.btnClearBudgetHeadFocus);
        if (clearButton != null) {
            clearButton.setOnClickListener(v -> {
                clearBudgetHeadOption(cbBudgetHeadName, etBudgetHeadName);
                clearBudgetHeadOption(cbBudgetHeadDepartmentName, etBudgetHeadDepartmentName);
                clearBudgetHeadOption(cbBudgetHeadProjectCode, etBudgetHeadProjectCode);
                hideKeyboard(v);
            });
        }
    }

    private void setupBudgetHeadOption(CheckBox checkbox, EditText field) {
        if (checkbox == null || field == null) {
            return;
        }

        updateBudgetHeadFieldVisibility(checkbox, field);
        checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateBudgetHeadFieldVisibility(checkbox, field);
            if (isChecked) {
                if (!suppressBudgetHeadFocus) {
                    focusAndShowKeyboard(field);
                }
            } else {
                field.setText("");
                field.clearFocus();
            }
        });
    }

    private void clearBudgetHeadOption(CheckBox checkbox, EditText field) {
        if (checkbox != null) {
            checkbox.setChecked(false);
        }
        if (field != null) {
            field.setText("");
            field.clearFocus();
            field.setVisibility(View.GONE);
        }
    }

    private void setBudgetHeadOptionFromValue(CheckBox checkbox, EditText field, String value) {
        if (field == null) {
            return;
        }

        field.setText(safeText(value));
        suppressBudgetHeadFocus = true;
        if (checkbox != null) {
            checkbox.setChecked(!isBlank(value));
        }
        suppressBudgetHeadFocus = false;
        updateBudgetHeadFieldVisibility(checkbox, field);
    }

    private void updateBudgetHeadFieldVisibility(CheckBox checkbox, EditText field) {
        if (field == null) {
            return;
        }

        field.setVisibility(checkbox != null && checkbox.isChecked() ? View.VISIBLE : View.GONE);
    }

    private void focusAndShowKeyboard(EditText field) {
        field.requestFocus();
        field.setSelection(field.getText().length());
        field.post(() -> {
            InputMethodManager inputMethodManager =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null) {
                inputMethodManager.showSoftInput(field, InputMethodManager.SHOW_IMPLICIT);
            }
            scrollFieldIntoView(field);
            field.postDelayed(() -> scrollFieldIntoView(field), 300);
        });
    }

    private void scrollFieldIntoView(EditText field) {
        if (scrollCreateBooking == null) {
            return;
        }

        Rect fieldRect = new Rect();
        field.getDrawingRect(fieldRect);
        scrollCreateBooking.offsetDescendantRectToMyCoords(field, fieldRect);

        int viewportTop = scrollCreateBooking.getScrollY() + scrollCreateBooking.getPaddingTop();
        int viewportBottom = scrollCreateBooking.getScrollY()
                + scrollCreateBooking.getHeight()
                - scrollCreateBooking.getPaddingBottom();
        int spacing = getResources().getDimensionPixelSize(R.dimen.space_24);

        if (fieldRect.bottom + spacing > viewportBottom) {
            int scrollY = fieldRect.bottom
                    + spacing
                    - scrollCreateBooking.getHeight()
                    + scrollCreateBooking.getPaddingBottom();
            scrollCreateBooking.smoothScrollTo(0, Math.max(0, scrollY));
            return;
        }

        if (fieldRect.top - spacing < viewportTop) {
            scrollCreateBooking.smoothScrollTo(
                    0,
                    Math.max(0, fieldRect.top - spacing - scrollCreateBooking.getPaddingTop())
            );
        }
    }

    private void hideKeyboard(View field) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(field.getWindowToken(), 0);
        }
    }

    private String getChargeStatus(RadioGroup group, int yesId, int waivedId) {
        int checkedId = group.getCheckedRadioButtonId();

        if (checkedId == yesId) {
            return "yes";
        }

        if (checkedId == waivedId) {
            return "waived_off";
        }

        return "no";
    }

    private void sendBookingCreatedResult() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_BOOKING_CREATED, true);
        setResult(RESULT_OK, resultIntent);
    }

    private void setLoading(boolean loading) {
        btnCreateBooking.setEnabled(!loading);
        btnCreateBooking.setAlpha(loading ? 0.65f : 1.0f);
        btnCreateBooking.setText(loading ? "Please wait..." : (bookingRequestApprovalMode ? "Approve" : "Create Booking"));
        if (bookingRequestApprovalMode && btnFillDummy != null) {
            btnFillDummy.setEnabled(!loading);
            btnFillDummy.setAlpha(loading ? 0.65f : 1.0f);
        }
        if (bookingRequestApprovalMode && btnSendBackBooking != null) {
            btnSendBackBooking.setEnabled(!loading);
            btnSendBackBooking.setAlpha(loading ? 0.65f : 1.0f);
        }
        if (bookingRequestApprovalMode && btnDeleteBookingRequest != null) {
            btnDeleteBookingRequest.setEnabled(!loading);
            btnDeleteBookingRequest.setAlpha(loading ? 0.65f : 1.0f);
        }
    }

    private void showError(String message) {
        tvMessage.setVisibility(View.VISIBLE);
        tvMessage.setText(CreateBookingFormMapper.makeFriendlyMessage(message));
    }

    private void showMessage(String message) {
        tvMessage.setVisibility(View.VISIBLE);
        tvMessage.setText(message);
    }

    private String getText(EditText editText) {
        return editText.getText() != null
                ? editText.getText().toString().trim()
                : "";
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isBusy() {
        return viewModel.isCreating() || approvalRequestInFlight;
    }

    private void fillRandomBookingData() {
        String[] names = {
                "Amit Sharma", "Neha Verma", "Rohit Singh", "Priya Patel", "Ankit Jain",
                "Suresh Yadav", "Pooja Gupta", "Rahul Mehta", "Sneha Reddy", "Vikas Kumar",
                "Deepak Mishra", "Anjali Singh", "Karan Malhotra", "Ritika Kapoor", "Manish Tiwari",
                "Nikhil Agarwal", "Swati Saxena", "Aditya Joshi", "Meena Iyer", "Rajesh Khanna",
                "Arjun Nair", "Kavita Desai", "Tarun Bansal", "Divya Choudhary", "Harsh Vardhan",
                "Ramesh Pawar", "Komal Shah", "Varun Arora", "Ishita Ghosh", "Yash Thakur"
        };

        String[] designations = {
                "Engineer", "Professor", "Manager", "Analyst", "Researcher",
                "Consultant", "Director", "Coordinator", "Developer", "Architect",
                "Scientist", "Assistant Professor", "HR Manager", "Team Lead", "Intern",
                "Project Manager", "Data Engineer", "Security Analyst", "DevOps Engineer", "QA Engineer",
                "Business Analyst", "Software Engineer", "Technical Lead", "System Admin", "Support Engineer",
                "Network Engineer", "AI Engineer", "Cloud Engineer", "Product Manager", "Operations Manager"
        };

        String[] organisations = {
                "IIT Bhilai", "Google", "Microsoft", "Amazon", "Infosys",
                "TCS", "Wipro", "DRDO", "ISRO", "Accenture",
                "Capgemini", "HCL", "Cognizant", "Flipkart", "Zomato",
                "Swiggy", "Paytm", "PhonePe", "Reliance", "Adani Group",
                "L&T", "Tech Mahindra", "Dell", "HP", "IBM",
                "Oracle", "SAP", "Nvidia", "Intel", "Qualcomm"
        };

        String[] addresses = {
                "Raipur", "Delhi", "Mumbai", "Bangalore", "Hyderabad",
                "Pune", "Chennai", "Kolkata", "Ahmedabad", "Jaipur",
                "Lucknow", "Bhopal", "Indore", "Patna", "Nagpur",
                "Chandigarh", "Surat", "Noida", "Gurgaon", "Kanpur",
                "Ranchi", "Dehradun", "Shimla", "Goa", "Trivandrum",
                "Coimbatore", "Mysore", "Vizag", "Jodhpur", "Udaipur"
        };

        String[] purposes = {
                "Meeting", "Research", "Interview", "Project Discussion", "Audit",
                "Seminar", "Workshop", "Training", "Conference", "Inspection",
                "Client Visit", "Technical Review", "Demo Session", "Collaboration", "Consultation",
                "Presentation", "Evaluation", "Recruitment", "Documentation", "Field Visit",
                "Testing", "Deployment", "Maintenance", "Support", "Planning",
                "Strategy Meeting", "Budget Discussion", "Compliance Check", "Review Meeting", "Site Visit"
        };

        String[] departments = {
                "CSE", "ECE", "ME", "Civil", "Admin",
                "HR", "Finance", "IT", "Operations", "Marketing",
                "Sales", "Legal", "Security", "R&D", "Production",
                "Logistics", "Procurement", "Quality", "Design", "Support",
                "Analytics", "Cloud", "AI/ML", "Networking", "Embedded",
                "Automation", "Testing", "DevOps", "Infrastructure", "Consulting"
        };

        int index = random.nextInt(names.length);
        String visitorName = names[index];

        etVisitorName.setText(visitorName);
        etVisitorDesignation.setText(randomValue(designations));
        etVisitorOrganisation.setText(randomValue(organisations));
        spinnerGender.setSelection(getRandomGenderPosition());
        etVisitorMobile.setText(generateRandomMobile());
        etVisitorEmail.setText(generateRandomEmail(visitorName));

        etPurpose.setText(randomValue(purposes));

        etRequestorName.setText("Rishabh");
        etRequestorDesignation.setText(randomValue(designations));
        etRequestorDepartment.setText(randomValue(departments));
        etRequestorMobile.setText(generateRandomMobile());
        setBudgetHeadOptionFromValue(cbBudgetHeadName, etBudgetHeadName, "Project Travel");
        setBudgetHeadOptionFromValue(
                cbBudgetHeadDepartmentName,
                etBudgetHeadDepartmentName,
                randomValue(departments)
        );
        setBudgetHeadOptionFromValue(
                cbBudgetHeadProjectCode,
                etBudgetHeadProjectCode,
                "PRJ-" + (1000 + random.nextInt(9000))
        );

        etLogisticsName.setText("Logistics " + (random.nextInt(100) + 1));
        etLogisticsDesignation.setText(randomValue(designations));
        etLogisticsMobile.setText(generateRandomMobile());

        showMessage("Random booking data generated 🎯");
    }

    private int getRandomGenderPosition() {
        if (spinnerGender.getCount() <= 1) {
            return 0;
        }

        return 1 + random.nextInt(spinnerGender.getCount() - 1);
    }

    private String randomValue(String[] values) {
        return values[random.nextInt(values.length)];
    }

    private String generateRandomMobile() {
        return "9" + (100000000 + random.nextInt(900000000));
    }

    private String generateRandomEmail(String name) {
        String cleanName = name.toLowerCase(Locale.ROOT).replace(" ", ".");
        return cleanName + random.nextInt(1000) + "@test.com";
    }

    @Override
    protected void onDestroy() {
        if (approveRequestCall != null && !approveRequestCall.isCanceled()) {
            approveRequestCall.cancel();
        }
        if (rejectRequestCall != null && !rejectRequestCall.isCanceled()) {
            rejectRequestCall.cancel();
        }
        if (sendBackRequestCall != null && !sendBackRequestCall.isCanceled()) {
            sendBackRequestCall.cancel();
        }
        if (deleteRequestCall != null && !deleteRequestCall.isCanceled()) {
            deleteRequestCall.cancel();
        }
        approveRequestCall = null;
        rejectRequestCall = null;
        sendBackRequestCall = null;
        deleteRequestCall = null;
        super.onDestroy();
    }

}
