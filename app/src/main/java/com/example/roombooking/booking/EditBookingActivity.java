package com.example.roombooking.booking;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import com.example.roombooking.R;
import com.example.roombooking.auth.AuthSessionGuard;
import com.example.roombooking.model.booking.BookingItem;
import com.example.roombooking.model.room.RoomItem;
import com.example.roombooking.room.RoomRepository;
import com.example.roombooking.utils.NullSafeCollections;
import com.example.roombooking.utils.EdgeToEdgeUtils;
import com.example.roombooking.utils.AppToolbarMenu;
import com.example.roombooking.utils.DateTimeUtils;
import com.example.roombooking.utils.RequiredMarkStyler;
import com.example.roombooking.utils.InternetErrorBanner;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class EditBookingActivity extends AppCompatActivity {

    private static final int ATTENDER_CHARGE_PER_SHIFT = 850;
    private static final long ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L;
    private static final int GAMMA_ATTACHED_ROOM_RATE = 1500;
    private static final int GAMMA_NON_ATTACHED_ROOM_RATE = 1300;
    private static final int BETA_ATTACHED_ROOM_RATE = 1000;
    private static final int BETA_NON_ATTACHED_ROOM_RATE = 800;
    private static final int FOREIGN_GAMMA_ATTACHED_ROOM_RATE = 2000;
    private static final int FOREIGN_GAMMA_NON_ATTACHED_ROOM_RATE = 1800;
    private static final int FOREIGN_BETA_ATTACHED_ROOM_RATE = 1500;
    private static final int FOREIGN_BETA_NON_ATTACHED_ROOM_RATE = 1300;

    public static final String EXTRA_BOOKING_DATA = "booking_data";

    private static final String EXTRA_UPDATED_BOOKING_ID = "updated_booking_id";
    private static final String EXTRA_UPDATED_STATUS = "updated_status";
    private static final String EXTRA_ARRIVAL_AT = "arrival_at";
    private static final String EXTRA_DEPARTURE_AT = "departure_at";

    private Spinner spinnerRoom;
    private Spinner spinnerGender;

    private EditText etVisitorName;
    private EditText etVisitorDesignation;
    private EditText etVisitorOrganisation;
    private EditText etVisitorMobile;
    private EditText etVisitorEmail;
    private EditText etPurpose;
    private EditText etRemarks;

    private EditText etArrivalAt;
    private EditText etDepartureAt;

    private RadioGroup rgVisitorCategory;
    private RadioGroup rgVisitorNationality;
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
    private TextView tvSelectShiftLabel;
    private CheckBox cbAttenderRequired;
    private CheckBox cbMorningShift;
    private RadioGroup rgMorningShiftChargeability;
    private CheckBox cbEveningShift;

    private EditText etRequestorName;
    private EditText etRequestorDesignation;
    private EditText etRequestorDepartment;
    private EditText etRequestorMobile;

    private EditText etLogisticsName;
    private EditText etLogisticsDesignation;
    private EditText etLogisticsMobile;
    private CheckBox cbLogisticsSameAsRequestor;

    private TextView tvMessage;
    private ScrollView scrollViewEditBooking;
    private AppCompatButton btnSaveBooking;

    private EditBookingViewModel viewModel;
    private EditBookingFormState currentFormState;
    private BookingItem bookingItem;
    private boolean formBound = false;
    private boolean suppressBudgetHeadFocus = false;
    private boolean suppressLogisticsSameAsRequestorChange = false;
    private boolean suppressRoomChargeAutoStatus = false;

    private final SimpleDateFormat displayFormat =
            DateTimeUtils.newDisplayDateTimeFormat();

    private RoomSpinnerAdapter roomAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_booking);
        if (!AuthSessionGuard.ensureAdmin(this)) {
            return;
        }
        EdgeToEdgeUtils.applySystemBarInsets(this, findViewById(R.id.rootView));

        initDependencies();
        bindViews();
        RequiredMarkStyler.applyTo(findViewById(R.id.rootView));
        setupScrollInsets();
        AppToolbarMenu.setup(this, findViewById(R.id.appToolbar));
        setupRoomSpinner();
        setupGenderSpinner();
        setupListeners();
        setupBackPressHandler();
        observeViewModel();

        bookingItem = getBookingFromIntent();

        if (bookingItem == null) {
            showToast("No booking details found.");
            finish();
            return;
        }

        viewModel.initialize(bookingItem);
        viewModel.loadRooms();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AuthSessionGuard.ensureAdmin(this);
    }

    private void initDependencies() {
        BookingRepository bookingRepository = new BookingRepository(getApplicationContext());
        RoomRepository roomRepository = new RoomRepository(getApplicationContext());
        EditBookingViewModelFactory factory =
                new EditBookingViewModelFactory(bookingRepository, roomRepository);
        viewModel = new ViewModelProvider(this, factory).get(EditBookingViewModel.class);
    }

    private void bindViews() {
        scrollViewEditBooking = findViewById(R.id.scrollViewEditBooking);
        spinnerRoom = findViewById(R.id.spinnerRoom);
        spinnerGender = findViewById(R.id.spinnerGender);
        tvSelectShiftLabel = findViewById(R.id.tvSelectShiftLabel);
        etVisitorName = findViewById(R.id.etVisitorName);
        etVisitorDesignation = findViewById(R.id.etVisitorDesignation);
        etVisitorOrganisation = findViewById(R.id.etVisitorOrganisation);
        etVisitorMobile = findViewById(R.id.etVisitorMobile);
        etVisitorEmail = findViewById(R.id.etVisitorEmail);
        etPurpose = findViewById(R.id.etPurpose);
        etRemarks = findViewById(R.id.etRemarks);

        etArrivalAt = findViewById(R.id.etArrivalAt);
        etDepartureAt = findViewById(R.id.etDepartureAt);

        rgVisitorCategory = findViewById(R.id.rgVisitorCategory);
        rgVisitorNationality = findViewById(R.id.rgVisitorNationality);
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
        cbMorningShift = findViewById(R.id.cbMorningShift);
        rgMorningShiftChargeability = findViewById(R.id.rgMorningShiftChargeability);
        cbEveningShift = findViewById(R.id.cbEveningShift);

        etRequestorName = findViewById(R.id.etRequestorName);
        etRequestorDesignation = findViewById(R.id.etRequestorDesignation);
        etRequestorDepartment = findViewById(R.id.etRequestorDepartment);
        etRequestorMobile = findViewById(R.id.etRequestorMobile);

        etLogisticsName = findViewById(R.id.etLogisticsName);
        etLogisticsDesignation = findViewById(R.id.etLogisticsDesignation);
        etLogisticsMobile = findViewById(R.id.etLogisticsMobile);
        cbLogisticsSameAsRequestor = findViewById(R.id.cbLogisticsSameAsRequestor);

        tvMessage = findViewById(R.id.tvMessage);
        btnSaveBooking = findViewById(R.id.btnSaveBooking);
    }

    private void setupScrollInsets() {
        if (scrollViewEditBooking == null) {
            return;
        }

        int initialLeft = scrollViewEditBooking.getPaddingLeft();
        int initialTop = scrollViewEditBooking.getPaddingTop();
        int initialRight = scrollViewEditBooking.getPaddingRight();
        int initialBottom = scrollViewEditBooking.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(scrollViewEditBooking, (view, insets) -> {
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

        ViewCompat.requestApplyInsets(scrollViewEditBooking);
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
        findViewById(R.id.ivBack).setOnClickListener(v -> handleBackPress());

        etArrivalAt.setOnClickListener(v ->
                pickDateTime(
                        viewModel.getArrivalCalendar(),
                        viewModel::updateArrivalDateTime
                )
        );

        etDepartureAt.setOnClickListener(v ->
                pickDateTime(
                        viewModel.getDepartureCalendar(),
                        selectedDateTime -> {
                            if (!viewModel.updateDepartureDateTime(selectedDateTime)) {
                                showError("Departure must be after arrival.");
                            }
                        }
                )
        );

        spinnerRoom.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                syncCalculatedRoomCharges(!suppressRoomChargeAutoStatus);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                syncCalculatedRoomCharges(false);
            }
        });

        btnSaveBooking.setOnClickListener(v -> saveBooking());
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
        setupClearRadioAction(R.id.btnClearVisitorNationality, rgVisitorNationality);
        rgVisitorNationality.setOnCheckedChangeListener(
                (group, checkedId) -> syncCalculatedRoomCharges(false)
        );
        setupBudgetHeadFocusControls();
        setupAttenderRequirementControls();
        setupLogisticsSameAsRequestorControls();
    }

    private void observeViewModel() {
        viewModel.getFormStateLiveData().observe(this, state -> {
            if (state == null) return;

            currentFormState = state.copy();

            if (!formBound) {
                bindBookingData(currentFormState);
                formBound = true;
                return;
            }

            refreshDateTimeFields(currentFormState);
            syncCalculatedRoomCharges(false);
            syncCalculatedAttenderCharges(false);
        });

        viewModel.getRoomsLiveData().observe(this, rooms -> {
            InternetErrorBanner.hide(EditBookingActivity.this);
            bindRoomsToSpinner(rooms);
            preselectCurrentRoom();
        });

        viewModel.getSavingLiveData().observe(this, saving ->
                setSavingState(Boolean.TRUE.equals(saving))
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
            if (message != null && !message.trim().isEmpty()) {
                showError(message);
            }
        });

        viewModel.getValidationLiveData().observe(this, event -> {
            if (event == null) return;

            EditBookingValidationResult result = event.getContentIfNotHandled();
            if (result != null && !result.isValid()) {
                handleValidationError(result);
            }
        });

        viewModel.getToastLiveData().observe(this, event -> {
            if (event == null) return;

            String message = event.getContentIfNotHandled();
            if (message != null && !message.trim().isEmpty()) {
                showToast(message);
            }
        });

        viewModel.getResultLiveData().observe(this, event -> {
            if (event == null) return;

            EditBookingResult result = event.getContentIfNotHandled();
            if (result != null) {
                handleUpdateSuccess(result);
            }
        });
    }

    private BookingItem getBookingFromIntent() {
        Intent intent = getIntent();

        if (intent == null) {
            return null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return intent.getParcelableExtra(EXTRA_BOOKING_DATA, BookingItem.class);
        }

        return intent.getParcelableExtra(EXTRA_BOOKING_DATA);
    }

    private void bindBookingData(EditBookingFormState state) {
        etVisitorName.setText(safe(state.getVisitorName()));
        etVisitorDesignation.setText(safe(state.getVisitorDesignation()));
        etVisitorOrganisation.setText(safe(state.getVisitorOrganisation()));
        etVisitorMobile.setText(safe(state.getVisitorMobile()));
        etVisitorEmail.setText(safe(state.getVisitorEmail()));
        etPurpose.setText(safe(state.getPurpose()));
        etRemarks.setText(safe(state.getRemarks()));

        selectGender(state.getVisitorGender());
        selectVisitorNationality(state.getVisitorNationality());
        selectVisitorCategory(state.getVisitorCategory());

        cbAttenderRequired.setChecked(state.isAttenderRequired());
        cbMorningShift.setChecked(state.isAttenderMorningShift());
        rgMorningShiftChargeability.check(
                state.isAttenderMorningChargeable()
                        ? R.id.rbMorningShiftChargeable
                        : R.id.rbMorningShiftNonChargeable
        );
        cbEveningShift.setChecked(state.isAttenderEveningShift());
        selectChargeStatus(
                rgRoomChargesStatus,
                state.getRoomChargesStatus(),
                R.id.rbRoomChargesYes,
                R.id.rbRoomChargesNo,
                R.id.rbRoomChargesWaived
        );
        selectChargeStatus(
                rgAttenderChargesStatus,
                state.getAttenderChargesStatus(),
                R.id.rbAttenderChargesYes,
                R.id.rbAttenderChargesNo,
                R.id.rbAttenderChargesWaived
        );
        etRoomChargesAmount.setText(
                "yes".equalsIgnoreCase(state.getRoomChargesStatus())
                        ? safe(state.getRoomChargesAmount())
                        : ""
        );
        syncCalculatedRoomCharges(false);
        etAttenderChargesAmount.setText(
                "yes".equalsIgnoreCase(state.getAttenderChargesStatus())
                        ? safe(state.getAttenderChargesAmount())
                        : ""
        );
        syncCalculatedAttenderCharges(false);
        setBudgetHeadOptionFromValue(
                cbBudgetHeadName,
                etBudgetHeadName,
                state.getBudgetHeadName()
        );
        setBudgetHeadOptionFromValue(
                cbBudgetHeadDepartmentName,
                etBudgetHeadDepartmentName,
                state.getBudgetHeadDepartmentName()
        );
        setBudgetHeadOptionFromValue(
                cbBudgetHeadProjectCode,
                etBudgetHeadProjectCode,
                state.getBudgetHeadProjectCode()
        );

        etRequestorName.setText(safe(state.getRequestorName()));
        etRequestorDesignation.setText(safe(state.getRequestorDesignation()));
        etRequestorDepartment.setText(safe(state.getRequestorDepartment()));
        etRequestorMobile.setText(safe(state.getRequestorMobile()));

        etLogisticsName.setText(safe(state.getLogisticsName()));
        etLogisticsDesignation.setText(safe(state.getLogisticsDesignation()));
        etLogisticsMobile.setText(safe(state.getLogisticsMobile()));
        initializeLogisticsSameAsRequestorState();

        refreshDateTimeFields(state);
    }

    private void bindRoomsToSpinner(List<RoomItem> rooms) {
        List<RoomItem> safeRooms = NullSafeCollections.copyWithoutNulls(rooms);
        List<RoomSpinnerEntry> entries = RoomSpinnerEntries.build(safeRooms);
        addCurrentRoomFallbackIfMissing(entries, safeRooms);

        roomAdapter.clear();
        roomAdapter.addAll(entries);
        roomAdapter.notifyDataSetChanged();
    }

    private void preselectCurrentRoom() {
        if (currentFormState == null || currentFormState.getRoomId() == null) {
            return;
        }

        int currentRoomId = currentFormState.getRoomId();

        for (int i = 0; i < roomAdapter.getCount(); i++) {
            RoomSpinnerEntry entry = roomAdapter.getItem(i);
            RoomItem roomItem = entry != null ? entry.getRoom() : null;

            if (roomItem != null && roomItem.getId() == currentRoomId) {
                suppressRoomChargeAutoStatus = true;
                spinnerRoom.setSelection(i);
                suppressRoomChargeAutoStatus = false;
                syncCalculatedRoomCharges(false);
                return;
            }
        }
    }

    private void addCurrentRoomFallbackIfMissing(
            List<RoomSpinnerEntry> entries,
            List<RoomItem> rooms
    ) {
        RoomItem fallbackRoom = buildCurrentRoomFallbackIfMissing(rooms);

        if (fallbackRoom == null) {
            return;
        }

        int insertPosition = entries.isEmpty() ? 0 : 1;
        entries.add(insertPosition, RoomSpinnerEntry.room(fallbackRoom));
    }

    private RoomItem buildCurrentRoomFallbackIfMissing(List<RoomItem> rooms) {
        if (currentFormState == null || currentFormState.getRoomId() == null) {
            return null;
        }

        int currentRoomId = currentFormState.getRoomId();

        for (RoomItem room : rooms) {
            if (room != null && room.getId() == currentRoomId) {
                return null;
            }
        }

        String roomName = bookingItem != null ? safe(bookingItem.getRoomName()) : "";
        if (roomName.isEmpty()) {
            roomName = "Current Room";
        }

        RoomItem fallbackRoom = new RoomItem();
        fallbackRoom.setId(currentRoomId);
        fallbackRoom.setNumber(roomName);
        fallbackRoom.setRoomName(roomName);
        fallbackRoom.setSelectionLabel(roomName);

        return fallbackRoom;
    }

    private void selectGender(String gender) {
        if (gender == null) return;

        for (int i = 0; i < spinnerGender.getCount(); i++) {
            Object item = spinnerGender.getItemAtPosition(i);

            if (item != null && gender.equalsIgnoreCase(item.toString())) {
                spinnerGender.setSelection(i);
                return;
            }
        }
    }

    private void selectVisitorCategory(String category) {
        if (category == null) return;

        if ("institute_guest".equalsIgnoreCase(category)) {
            rgVisitorCategory.check(R.id.rbInstituteGuest);
            return;
        }

        if ("conference_workshop_guest".equalsIgnoreCase(category)) {
            rgVisitorCategory.check(R.id.rbConferenceGuest);
            return;
        }

        if ("other_guest".equalsIgnoreCase(category)) {
            rgVisitorCategory.check(R.id.rbOtherGuest);
        }
    }

    private void selectVisitorNationality(String nationality) {
        if ("foreigner".equalsIgnoreCase(nationality)) {
            rgVisitorNationality.check(R.id.rbVisitorForeigner);
        } else if ("indian".equalsIgnoreCase(nationality)) {
            rgVisitorNationality.check(R.id.rbVisitorIndian);
        } else {
            rgVisitorNationality.clearCheck();
        }
    }

    private void setupAttenderRequirementControls() {
        cbAttenderRequired.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                clearAttenderShifts();
            }

            updateAttenderControlsState();
            syncCalculatedAttenderCharges(true);
        });
        cbMorningShift.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateAttenderControlsState();
            syncCalculatedAttenderCharges(true);
        });
        cbEveningShift.setOnCheckedChangeListener((buttonView, isChecked) -> syncCalculatedAttenderCharges(true));
        rgMorningShiftChargeability.setOnCheckedChangeListener((group, checkedId) -> syncCalculatedAttenderCharges(true));

        updateAttenderControlsState();
        syncCalculatedAttenderCharges(true);
    }

    private void updateAttenderControlsState() {
        boolean attenderRequired = cbAttenderRequired.isChecked();
        setShiftControlsEnabled(attenderRequired);
        if (!attenderRequired) {
            clearAttenderShifts();
        }
    }

    private void setShiftControlsEnabled(boolean enabled) {
        setViewEnabled(cbMorningShift, enabled);
        setViewEnabled(cbEveningShift, enabled);
        setMorningChargeabilityEnabled(enabled && cbMorningShift.isChecked());
        setViewEnabled(tvSelectShiftLabel, enabled);
    }

    private void setMorningChargeabilityEnabled(boolean enabled) {
        setViewEnabled(rgMorningShiftChargeability, enabled);
        for (int index = 0; index < rgMorningShiftChargeability.getChildCount(); index++) {
            setViewEnabled(rgMorningShiftChargeability.getChildAt(index), enabled);
        }
    }

    private void setViewEnabled(View view, boolean enabled) {
        if (view == null) {
            return;
        }

        view.setEnabled(enabled);
        view.setAlpha(enabled ? 1.0f : 0.45f);
    }

    private void setupLogisticsSameAsRequestorControls() {
        if (cbLogisticsSameAsRequestor == null) {
            return;
        }

        cbLogisticsSameAsRequestor.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressLogisticsSameAsRequestorChange) {
                updateLogisticsFieldsEnabled();
                return;
            }
            if (isChecked) {
                copyRequestorToLogistics();
            } else {
                clearLogisticsFields();
            }
            updateLogisticsFieldsEnabled();
        });

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No-op.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (cbLogisticsSameAsRequestor.isChecked()) {
                    copyRequestorToLogistics();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No-op.
            }
        };
        etRequestorName.addTextChangedListener(watcher);
        etRequestorDesignation.addTextChangedListener(watcher);
        etRequestorMobile.addTextChangedListener(watcher);

        initializeLogisticsSameAsRequestorState();
    }

    private void initializeLogisticsSameAsRequestorState() {
        if (cbLogisticsSameAsRequestor == null) {
            return;
        }
        boolean sameAsRequestor =
                !isBlank(getText(etRequestorName) + getText(etRequestorDesignation) + getText(etRequestorMobile))
                        && getText(etRequestorName).equals(getText(etLogisticsName))
                        && getText(etRequestorDesignation).equals(getText(etLogisticsDesignation))
                        && getText(etRequestorMobile).equals(getText(etLogisticsMobile));
        suppressLogisticsSameAsRequestorChange = true;
        try {
            cbLogisticsSameAsRequestor.setChecked(sameAsRequestor);
            updateLogisticsFieldsEnabled();
        } finally {
            suppressLogisticsSameAsRequestorChange = false;
        }
    }

    private void copyRequestorToLogistics() {
        etLogisticsName.setText(getText(etRequestorName));
        etLogisticsDesignation.setText(getText(etRequestorDesignation));
        etLogisticsMobile.setText(getText(etRequestorMobile));
    }

    private void clearLogisticsFields() {
        etLogisticsName.setText("");
        etLogisticsDesignation.setText("");
        etLogisticsMobile.setText("");
    }

    private void updateLogisticsFieldsEnabled() {
        boolean enabled = cbLogisticsSameAsRequestor == null
                || !cbLogisticsSameAsRequestor.isChecked();
        setViewEnabled(etLogisticsName, enabled);
        setViewEnabled(etLogisticsDesignation, enabled);
        setViewEnabled(etLogisticsMobile, enabled);
    }

    private boolean isMorningShiftChargeable() {
        return rgMorningShiftChargeability.getCheckedRadioButtonId() != R.id.rbMorningShiftNonChargeable;
    }

    private int calculatedAttenderChargesAmount() {
        if (!cbAttenderRequired.isChecked()) {
            return 0;
        }
        int chargeableShiftCount = 0;
        if (cbMorningShift.isChecked() && isMorningShiftChargeable()) {
            chargeableShiftCount += 1;
        }
        if (cbEveningShift.isChecked()) {
            chargeableShiftCount += 1;
        }
        return chargeableShiftCount * ATTENDER_CHARGE_PER_SHIFT * inclusiveStayDays();
    }

    private int inclusiveStayDays() {
        if (currentFormState == null) {
            return 1;
        }
        Calendar arrival = EditBookingFormMapper.calendarFromMillis(currentFormState.getArrivalAtMillis());
        Calendar departure = EditBookingFormMapper.calendarFromMillis(currentFormState.getDepartureAtMillis());
        clearTimeOfDay(arrival);
        clearTimeOfDay(departure);
        long nights = Math.max(0L, (departure.getTimeInMillis() - arrival.getTimeInMillis()) / ONE_DAY_MILLIS);
        return (int) Math.max(nights + 1L, 1L);
    }

    private void clearTimeOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private Integer roomChargeRate(RoomItem room) {
        if (room == null) {
            return null;
        }
        String prefix = room.getSafePrefix();
        boolean foreignVisitor = EditBookingFormState.VISITOR_NATIONALITY_FOREIGNER.equals(
                getSelectedVisitorNationality()
        );
        if ("Gamma".equalsIgnoreCase(prefix)) {
            if (foreignVisitor) {
                return room.hasAttachedBath()
                        ? FOREIGN_GAMMA_ATTACHED_ROOM_RATE
                        : FOREIGN_GAMMA_NON_ATTACHED_ROOM_RATE;
            }
            return room.hasAttachedBath()
                    ? GAMMA_ATTACHED_ROOM_RATE
                    : GAMMA_NON_ATTACHED_ROOM_RATE;
        }
        if ("Beta".equalsIgnoreCase(prefix)) {
            if (foreignVisitor) {
                return room.hasAttachedBath()
                        ? FOREIGN_BETA_ATTACHED_ROOM_RATE
                        : FOREIGN_BETA_NON_ATTACHED_ROOM_RATE;
            }
            return room.hasAttachedBath()
                    ? BETA_ATTACHED_ROOM_RATE
                    : BETA_NON_ATTACHED_ROOM_RATE;
        }
        return null;
    }

    private Integer calculatedRoomChargesAmount() {
        Integer rate = roomChargeRate(getSelectedRoomItem());
        return rate == null ? null : rate * inclusiveStayDays();
    }

    private void syncCalculatedRoomCharges(boolean autoStatus) {
        Integer amount = calculatedRoomChargesAmount();
        if (autoStatus && amount != null) {
            rgRoomChargesStatus.check(R.id.rbRoomChargesYes);
        }

        boolean chargesReceived = rgRoomChargesStatus.getCheckedRadioButtonId() == R.id.rbRoomChargesYes;
        if (!chargesReceived) {
            updateChargeAmountField(rgRoomChargesStatus, etRoomChargesAmount, R.id.rbRoomChargesYes);
            setRoomChargesAmountEditable(true);
            return;
        }

        etRoomChargesAmount.setEnabled(true);
        setRoomChargesAmountEditable(true);
        if (amount != null) {
            etRoomChargesAmount.setText(String.valueOf(amount));
            etRoomChargesAmount.setError(null);
        }
    }

    private void setRoomChargesAmountEditable(boolean editable) {
        etRoomChargesAmount.setFocusable(editable);
        etRoomChargesAmount.setFocusableInTouchMode(editable);
        etRoomChargesAmount.setCursorVisible(editable);
    }

    private void syncCalculatedAttenderCharges(boolean autoStatus) {
        int amount = calculatedAttenderChargesAmount();
        if (autoStatus) {
            if (amount > 0) {
                rgAttenderChargesStatus.check(R.id.rbAttenderChargesYes);
            } else if (rgAttenderChargesStatus.getCheckedRadioButtonId() == R.id.rbAttenderChargesYes) {
                rgAttenderChargesStatus.check(R.id.rbAttenderChargesNo);
            }
        }
        updateChargeAmountField(rgAttenderChargesStatus, etAttenderChargesAmount, R.id.rbAttenderChargesYes);
        if (rgAttenderChargesStatus.getCheckedRadioButtonId() == R.id.rbAttenderChargesYes) {
            etAttenderChargesAmount.setText(amount > 0 ? String.valueOf(amount) : "");
        }
    }

    private void clearAttenderShifts() {
        cbMorningShift.setChecked(false);
        rgMorningShiftChargeability.check(R.id.rbMorningShiftChargeable);
        cbEveningShift.setChecked(false);
    }

    private void saveBooking() {
        if (viewModel.isSaving()) {
            return;
        }

        hideMessage();
        viewModel.save(collectFormData());
    }

    private void handleBackPress() {
        if (viewModel.isSaving()) {
            showMessage("Please wait for the update request to finish.", false);
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

    private EditBookingFormState collectFormData() {
        EditBookingFormState data = currentFormState != null
                ? currentFormState.copy()
                : new EditBookingFormState();

        data.setRoomId(getSelectedRoomId());

        data.setVisitorName(getText(etVisitorName));
        data.setVisitorDesignation(getText(etVisitorDesignation));
        data.setVisitorOrganisation(getText(etVisitorOrganisation));
        data.setVisitorGender(getSelectedGender());
        data.setVisitorNationality(getSelectedVisitorNationality());
        data.setVisitorMobile(getText(etVisitorMobile));
        data.setVisitorEmail(getText(etVisitorEmail));
        data.setPurpose(getText(etPurpose));
        data.setRemarks(getText(etRemarks));

        data.setVisitorCategory(getSelectedVisitorCategory());

        data.setAttenderRequired(cbAttenderRequired.isChecked());
        data.setAttenderMorningShift(cbMorningShift.isChecked());
        data.setAttenderMorningChargeable(cbMorningShift.isChecked() && isMorningShiftChargeable());
        data.setAttenderEveningShift(cbEveningShift.isChecked());
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
        String roomChargesAmount = "0";
        if ("yes".equals(data.getRoomChargesStatus())) {
            roomChargesAmount = getText(etRoomChargesAmount);
        }
        data.setRoomChargesAmount(roomChargesAmount);
        data.setAttenderChargesAmount("yes".equals(data.getAttenderChargesStatus())
                ? getText(etAttenderChargesAmount)
                : "0");

        data.setBudgetHeadName(getBudgetHeadText(cbBudgetHeadName, etBudgetHeadName));
        data.setBudgetHeadDepartmentName(
                getBudgetHeadText(cbBudgetHeadDepartmentName, etBudgetHeadDepartmentName)
        );
        data.setBudgetHeadProjectCode(
                getBudgetHeadText(cbBudgetHeadProjectCode, etBudgetHeadProjectCode)
        );

        data.setRequestorName(getText(etRequestorName));
        data.setRequestorDesignation(getText(etRequestorDesignation));
        data.setRequestorDepartment(getText(etRequestorDepartment));
        data.setRequestorMobile(getText(etRequestorMobile));

        data.setLogisticsName(getText(etLogisticsName));
        data.setLogisticsDesignation(getText(etLogisticsDesignation));
        data.setLogisticsMobile(getText(etLogisticsMobile));

        return data;
    }

    private String getBudgetHeadText(CheckBox checkbox, EditText field) {
        return checkbox != null && checkbox.isChecked() ? getText(field) : "";
    }

    private Integer getSelectedRoomId() {
        RoomItem selectedRoom = getSelectedRoomItem();
        if (selectedRoom != null) {
            return selectedRoom.getId();
        }
        return currentFormState != null ? currentFormState.getRoomId() : null;
    }

    private RoomItem getSelectedRoomItem() {
        int position = spinnerRoom.getSelectedItemPosition();

        if (roomAdapter == null || position < 0 || position >= roomAdapter.getCount()) {
            return null;
        }

        RoomSpinnerEntry entry = roomAdapter.getItem(position);
        if (entry == null || entry.getRoom() == null) {
            return null;
        }
        return entry.getRoom();
    }

    private String getSelectedGender() {
        int position = spinnerGender.getSelectedItemPosition();

        if (position == 0 || spinnerGender.getSelectedItem() == null) {
            return "";
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

    private String getSelectedVisitorNationality() {
        int checkedId = rgVisitorNationality.getCheckedRadioButtonId();
        if (checkedId == R.id.rbVisitorForeigner) {
            return EditBookingFormState.VISITOR_NATIONALITY_FOREIGNER;
        }
        if (checkedId == R.id.rbVisitorIndian) {
            return EditBookingFormState.VISITOR_NATIONALITY_INDIAN;
        }
        return "";
    }

    private void handleValidationError(EditBookingValidationResult result) {
        if (EditBookingFormState.FIELD_ROOM_CHARGES_AMOUNT.equals(result.getField())) {
            etRoomChargesAmount.setError("Room charges amount is required.");
            focusAndShowKeyboard(etRoomChargesAmount);
            showError(result.getMessage());
            return;
        }

        if (EditBookingFormState.FIELD_ATTENDER_CHARGES_AMOUNT.equals(result.getField())) {
            etAttenderChargesAmount.setError("Attender charges amount is required.");
            focusAndShowKeyboard(etAttenderChargesAmount);
            showError(result.getMessage());
            return;
        }

        showError(result.getMessage());
    }

    private void handleUpdateSuccess(EditBookingResult result) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_UPDATED_BOOKING_ID, result.getBookingId());
        resultIntent.putExtra(EXTRA_UPDATED_STATUS, result.getUpdatedStatus());
        resultIntent.putExtra(EXTRA_ARRIVAL_AT, result.getArrivalAt());
        resultIntent.putExtra(EXTRA_DEPARTURE_AT, result.getDepartureAt());

        setResult(RESULT_OK, resultIntent);
        showToast("Booking updated successfully.");
        finish();
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

    private void setupChargeAmountListener(RadioGroup group, EditText amountField, int yesId) {
        group.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            if (group == rgAttenderChargesStatus) {
                syncCalculatedAttenderCharges(false);
            } else if (group == rgRoomChargesStatus) {
                syncCalculatedRoomCharges(false);
            } else {
                updateChargeAmountField(group, amountField, yesId);
            }
        });
        if (group == rgRoomChargesStatus) {
            syncCalculatedRoomCharges(false);
        } else {
            updateChargeAmountField(group, amountField, yesId);
        }

        View yesButton = group.findViewById(yesId);
        if (yesButton != null) {
            yesButton.setOnClickListener(v -> {
                if (formBound
                        && group.getCheckedRadioButtonId() == yesId
                        && amountField.isEnabled()
                        && amountField.isFocusable()) {
                    focusAndShowKeyboard(amountField);
                }
            });
        }
    }

    private boolean updateChargeAmountField(RadioGroup group, EditText amountField, int yesId) {
        boolean enabled = group.getCheckedRadioButtonId() == yesId;
        amountField.setEnabled(enabled);

        if (!enabled) {
            amountField.setText("");
            amountField.setError(null);
            amountField.clearFocus();
            hideKeyboard(amountField);
        }

        return enabled;
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

        field.setText(safe(value));
        suppressBudgetHeadFocus = true;
        if (checkbox != null) {
            checkbox.setChecked(!safe(value).trim().isEmpty());
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
        if (scrollViewEditBooking == null) {
            return;
        }

        Rect fieldRect = new Rect();
        field.getDrawingRect(fieldRect);
        scrollViewEditBooking.offsetDescendantRectToMyCoords(field, fieldRect);

        int viewportTop = scrollViewEditBooking.getScrollY() + scrollViewEditBooking.getPaddingTop();
        int viewportBottom = scrollViewEditBooking.getScrollY()
                + scrollViewEditBooking.getHeight()
                - scrollViewEditBooking.getPaddingBottom();
        int spacing = getResources().getDimensionPixelSize(R.dimen.space_24);

        if (fieldRect.bottom + spacing > viewportBottom) {
            int scrollY = fieldRect.bottom
                    + spacing
                    - scrollViewEditBooking.getHeight()
                    + scrollViewEditBooking.getPaddingBottom();
            scrollViewEditBooking.smoothScrollTo(0, Math.max(0, scrollY));
            return;
        }

        if (fieldRect.top - spacing < viewportTop) {
            scrollViewEditBooking.smoothScrollTo(
                    0,
                    Math.max(0, fieldRect.top - spacing - scrollViewEditBooking.getPaddingTop())
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

    private void selectChargeStatus(
            RadioGroup group,
            String status,
            int yesId,
            int noId,
            int waivedId
    ) {
        if ("yes".equalsIgnoreCase(status)) {
            group.check(yesId);
        } else if ("waived_off".equalsIgnoreCase(status)) {
            group.check(waivedId);
        } else {
            group.check(noId);
        }
    }

    private void pickDateTime(Calendar target, DateTimeSelectionListener listener) {
        int year = target.get(Calendar.YEAR);
        int month = target.get(Calendar.MONTH);
        int day = target.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    target.set(Calendar.YEAR, selectedYear);
                    target.set(Calendar.MONTH, selectedMonth);
                    target.set(Calendar.DAY_OF_MONTH, selectedDay);

                    showTimePicker(target, listener);
                },
                year,
                month,
                day
        );

        datePickerDialog.show();
    }

    private void showTimePicker(Calendar target, DateTimeSelectionListener listener) {
        int hour = target.get(Calendar.HOUR_OF_DAY);
        int minute = target.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (timeView, selectedHour, selectedMinute) -> {
                    target.set(Calendar.HOUR_OF_DAY, selectedHour);
                    target.set(Calendar.MINUTE, selectedMinute);
                    target.set(Calendar.SECOND, 0);
                    target.set(Calendar.MILLISECOND, 0);

                    listener.onSelected(target);
                },
                hour,
                minute,
                true
        );

        timePickerDialog.show();
    }

    private void refreshDateTimeFields(EditBookingFormState state) {
        etArrivalAt.setText(displayFormat.format(new java.util.Date(state.getArrivalAtMillis())));
        etDepartureAt.setText(displayFormat.format(new java.util.Date(state.getDepartureAtMillis())));
    }

    private void setSavingState(boolean saving) {
        btnSaveBooking.setEnabled(!saving);
        btnSaveBooking.setAlpha(saving ? 0.65f : 1.0f);
        btnSaveBooking.setText(saving ? "Saving..." : "Save Booking");
    }

    private void showError(String message) {
        showMessage(EditBookingFormMapper.makeFriendlyMessage(message), true);
    }

    private void showMessage(String message, boolean isError) {
        if (tvMessage == null) return;

        if (message == null || message.trim().isEmpty()) {
            tvMessage.setVisibility(View.GONE);
            return;
        }

        tvMessage.setVisibility(View.VISIBLE);
        tvMessage.setText(message.trim());
        tvMessage.setTextColor(getColor(isError ? R.color.error_red : R.color.success_green));
    }

    private void hideMessage() {
        if (tvMessage != null) {
            tvMessage.setVisibility(View.GONE);
        }
    }

    private String getText(EditText editText) {
        return editText.getText() != null
                ? editText.getText().toString().trim()
                : "";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void showToast(String message) {
        if (message == null || message.trim().isEmpty()) {
            message = "Something went wrong.";
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private interface DateTimeSelectionListener {
        void onSelected(Calendar selectedDateTime);
    }
}
