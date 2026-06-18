package com.example.roombooking.booking;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
import android.text.Editable;
import android.text.TextWatcher;
import com.example.roombooking.R;
import com.example.roombooking.model.booking.BookingItem;
import com.example.roombooking.model.room.RoomItem;
import com.example.roombooking.room.RoomRepository;
import com.example.roombooking.utils.NullSafeCollections;
import com.example.roombooking.utils.EdgeToEdgeUtils;
import com.example.roombooking.utils.AppToolbarMenu;
import com.example.roombooking.utils.InternetErrorBanner;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class EditBookingActivity extends AppCompatActivity {

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
    private EditText etVisitorAddress;
    private EditText etVisitorMobile;
    private EditText etVisitorEmail;
    private EditText etPurpose;

    private EditText etArrivalAt;
    private EditText etDepartureAt;

    private RadioGroup rgVisitorCategory;
    private RadioGroup rgRoomChargesStatus;
    private RadioGroup rgAttenderChargesStatus;
    private RadioGroup rgBudgetHeadType;
    private EditText etRoomChargesAmount;
    private EditText etAttenderChargesAmount;
    private EditText etBudgetHeadValue;
    private TextView tvSelectShiftLabel;
    private CheckBox cbAttenderRequired;
    private EditText etAttenderCount;
    private CheckBox cbGeneralShift;
    private CheckBox cbMorningShift;
    private CheckBox cbDayShift;

    private EditText etRequestorName;
    private EditText etRequestorDesignation;
    private EditText etRequestorDepartment;
    private EditText etRequestorMobile;

    private EditText etLogisticsName;
    private EditText etLogisticsDesignation;
    private EditText etLogisticsMobile;

    private TextView tvMessage;
    private ScrollView scrollViewEditBooking;
    private AppCompatButton btnSaveBooking;

    private EditBookingViewModel viewModel;
    private EditBookingFormState currentFormState;
    private BookingItem bookingItem;
    private boolean formBound = false;

    private final SimpleDateFormat displayFormat =
            new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    private RoomSpinnerAdapter roomAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_booking);
        EdgeToEdgeUtils.applySystemBarInsets(this, findViewById(R.id.rootView));

        initDependencies();
        bindViews();
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
        etVisitorAddress = findViewById(R.id.etVisitorAddress);
        etVisitorMobile = findViewById(R.id.etVisitorMobile);
        etVisitorEmail = findViewById(R.id.etVisitorEmail);
        etPurpose = findViewById(R.id.etPurpose);

        etArrivalAt = findViewById(R.id.etArrivalAt);
        etDepartureAt = findViewById(R.id.etDepartureAt);

        rgVisitorCategory = findViewById(R.id.rgVisitorCategory);
        rgRoomChargesStatus = findViewById(R.id.rgRoomChargesStatus);
        rgAttenderChargesStatus = findViewById(R.id.rgAttenderChargesStatus);
        rgBudgetHeadType = findViewById(R.id.rgBudgetHeadType);
        etRoomChargesAmount = findViewById(R.id.etRoomChargesAmount);
        etAttenderChargesAmount = findViewById(R.id.etAttenderChargesAmount);
        etBudgetHeadValue = findViewById(R.id.etBudgetHeadValue);

        cbAttenderRequired = findViewById(R.id.cbAttenderRequired);
        etAttenderCount = findViewById(R.id.etAttenderCount);
        cbGeneralShift = findViewById(R.id.cbGeneralShift);
        cbMorningShift = findViewById(R.id.cbMorningShift);
        cbDayShift = findViewById(R.id.cbDayShift);

        etRequestorName = findViewById(R.id.etRequestorName);
        etRequestorDesignation = findViewById(R.id.etRequestorDesignation);
        etRequestorDepartment = findViewById(R.id.etRequestorDepartment);
        etRequestorMobile = findViewById(R.id.etRequestorMobile);

        etLogisticsName = findViewById(R.id.etLogisticsName);
        etLogisticsDesignation = findViewById(R.id.etLogisticsDesignation);
        etLogisticsMobile = findViewById(R.id.etLogisticsMobile);

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
        setupAttenderRequirementControls();
        setupBudgetHeadControls();
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
        etVisitorAddress.setText(safe(state.getVisitorAddress()));
        etVisitorMobile.setText(safe(state.getVisitorMobile()));
        etVisitorEmail.setText(safe(state.getVisitorEmail()));
        etPurpose.setText(safe(state.getPurpose()));

        selectGender(state.getVisitorGender());
        selectVisitorCategory(state.getVisitorCategory());

        cbAttenderRequired.setChecked(state.isAttenderRequired());
        etAttenderCount.setText(String.valueOf(state.getAttenderCountPerDay()));
        cbGeneralShift.setChecked(state.isAttenderGeneralShift());
        cbMorningShift.setChecked(state.isAttenderMorningShift());
        cbDayShift.setChecked(state.isAttenderDayShift());
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
        etAttenderChargesAmount.setText(
                "yes".equalsIgnoreCase(state.getAttenderChargesStatus())
                        ? safe(state.getAttenderChargesAmount())
                        : ""
        );
        selectBudgetHeadType(state.getBudgetHeadType());
        etBudgetHeadValue.setText(safe(state.getBudgetHeadValue()));

        etRequestorName.setText(safe(state.getRequestorName()));
        etRequestorDesignation.setText(safe(state.getRequestorDesignation()));
        etRequestorDepartment.setText(safe(state.getRequestorDepartment()));
        etRequestorMobile.setText(safe(state.getRequestorMobile()));

        etLogisticsName.setText(safe(state.getLogisticsName()));
        etLogisticsDesignation.setText(safe(state.getLogisticsDesignation()));
        etLogisticsMobile.setText(safe(state.getLogisticsMobile()));

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
                spinnerRoom.setSelection(i);
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

    private void setupBudgetHeadControls() {
        rgBudgetHeadType.setOnCheckedChangeListener((group, checkedId) -> {
            updateBudgetHeadHint(getSelectedBudgetHeadType());
            if (formBound && checkedId != -1) {
                focusAndShowKeyboard(etBudgetHeadValue);
            }
        });
        setupBudgetHeadRadioClick(R.id.rbBudgetIndividual);
        setupBudgetHeadRadioClick(R.id.rbBudgetInstitute);
        setupBudgetHeadRadioClick(R.id.rbBudgetProject);
        updateBudgetHeadHint(getSelectedBudgetHeadType());
    }

    private void setupBudgetHeadRadioClick(int radioButtonId) {
        View radioButton = rgBudgetHeadType.findViewById(radioButtonId);
        if (radioButton != null) {
            radioButton.setOnClickListener(v -> {
                if (formBound && rgBudgetHeadType.getCheckedRadioButtonId() == radioButtonId) {
                    updateBudgetHeadHint(getSelectedBudgetHeadType());
                    focusAndShowKeyboard(etBudgetHeadValue);
                }
            });
        }
    }

    private void selectBudgetHeadType(String budgetHeadType) {
        if (EditBookingFormState.BUDGET_HEAD_INSTITUTE.equalsIgnoreCase(budgetHeadType)) {
            rgBudgetHeadType.check(R.id.rbBudgetInstitute);
        } else if (EditBookingFormState.BUDGET_HEAD_PROJECT.equalsIgnoreCase(budgetHeadType)) {
            rgBudgetHeadType.check(R.id.rbBudgetProject);
        } else if (EditBookingFormState.BUDGET_HEAD_INDIVIDUAL.equalsIgnoreCase(budgetHeadType)) {
            rgBudgetHeadType.check(R.id.rbBudgetIndividual);
        } else {
            rgBudgetHeadType.clearCheck();
        }
        updateBudgetHeadHint(getSelectedBudgetHeadType());
    }

    private String getSelectedBudgetHeadType() {
        int checkedId = rgBudgetHeadType.getCheckedRadioButtonId();

        if (checkedId == R.id.rbBudgetInstitute) {
            return EditBookingFormState.BUDGET_HEAD_INSTITUTE;
        }

        if (checkedId == R.id.rbBudgetProject) {
            return EditBookingFormState.BUDGET_HEAD_PROJECT;
        }

        return "";
    }

    private void updateBudgetHeadHint(String budgetHeadType) {
        if (EditBookingFormState.BUDGET_HEAD_INSTITUTE.equals(budgetHeadType)) {
            etBudgetHeadValue.setHint("Department Name");
            return;
        }

        if (EditBookingFormState.BUDGET_HEAD_PROJECT.equals(budgetHeadType)) {
            etBudgetHeadValue.setHint("Project Code");
            return;
        }

        etBudgetHeadValue.setHint("Budget Head Value");
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
        setViewEnabled(tvSelectShiftLabel, enabled);
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
        data.setVisitorAddress(getText(etVisitorAddress));
        data.setVisitorMobile(getText(etVisitorMobile));
        data.setVisitorEmail(getText(etVisitorEmail));
        data.setPurpose(getText(etPurpose));

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

        data.setBudgetHeadType(getSelectedBudgetHeadType());
        data.setBudgetHeadValue(getText(etBudgetHeadValue));

        data.setRequestorName(getText(etRequestorName));
        data.setRequestorDesignation(getText(etRequestorDesignation));
        data.setRequestorDepartment(getText(etRequestorDepartment));
        data.setRequestorMobile(getText(etRequestorMobile));

        data.setLogisticsName(getText(etLogisticsName));
        data.setLogisticsDesignation(getText(etLogisticsDesignation));
        data.setLogisticsMobile(getText(etLogisticsMobile));

        return data;
    }

    private Integer getSelectedRoomId() {
        int position = spinnerRoom.getSelectedItemPosition();

        RoomSpinnerEntry entry = roomAdapter.getItem(position);
        if (entry == null || entry.getRoom() == null) {
            return currentFormState != null ? currentFormState.getRoomId() : null;
        }
        return entry.getRoom().getId();
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

        if (EditBookingFormState.FIELD_BUDGET_HEAD_VALUE.equals(result.getField())) {
            etBudgetHeadValue.setError("Budget head value is required.");
            focusAndShowKeyboard(etBudgetHeadValue);
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
            boolean enabled = checkedId == yesId;
            amountField.setEnabled(enabled);

            if (enabled) {
                if (formBound) {
                    focusAndShowKeyboard(amountField);
                }
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
                if (formBound
                        && group.getCheckedRadioButtonId() == yesId
                        && amountField.isEnabled()) {
                    focusAndShowKeyboard(amountField);
                }
            });
        }
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

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
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
