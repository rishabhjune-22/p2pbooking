package com.example.roombooking.booking;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextWatcher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.roombooking.R;
import com.example.roombooking.common.LocalUserManager;
import com.example.roombooking.model.booking.BookingActionData;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.model.room.RoomItem;
import com.example.roombooking.room.RoomRepository;
import com.example.roombooking.utils.EdgeToEdgeUtils;
import com.example.roombooking.utils.AppToolbarMenu;
import com.example.roombooking.utils.DateTimeUtils;
import com.example.roombooking.utils.InternetErrorBanner;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
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

    private static final long ONE_HOUR_MILLIS = 60 * 60 * 1000L;

    private Integer preselectedRoomId = null;
    private String preselectedRoomName = null;

    private EditText etVisitorName;
    private EditText etVisitorDesignation;
    private EditText etVisitorOrganisation;
    private Spinner spinnerGender;
    private EditText etVisitorAddress;
    private EditText etVisitorMobile;
    private EditText etVisitorEmail;

    private EditText etArrivalDT;
    private EditText etDepartureDT;
    private EditText etPurpose;

    private EditText etRequesteeName;
    private EditText etRequesteeDesignation;
    private EditText etRequesteeDepartment;
    private EditText etRequesteeMobile;

    private EditText etLogisticsName;
    private EditText etLogisticsDesignation;
    private EditText etLogisticsMobile;

    private Spinner spinnerRoom;
    private TextView tvMessage;
    private TextView tvPartialRoomInfo;

    private Button btnCreateBooking;
    private Button btnFillDummy;
    private ImageButton btnBack;

    private RadioGroup rgVisitorCategory;
    private RadioGroup rgRoomChargesStatus;
    private RadioGroup rgAttenderChargesStatus;
    private EditText etRoomChargesAmount;
    private EditText etAttenderChargesAmount;
    private CheckBox cbAttenderRequired;
    private TextView tvSelectShiftLabel;
    private EditText etAttenderCount;
    private CheckBox cbGeneralShift;
    private CheckBox cbMorningShift;
    private CheckBox cbDayShift;
    private CheckBox cbNightShift;

    private BookingRepository bookingRepository;
    private RoomRepository roomRepository;
    private LocalUserManager localUserManager;

    private final Gson gson = new Gson();
    private final Random random = new Random();

    private final Calendar arrivalCal = Calendar.getInstance();
    private final Calendar departureCal = Calendar.getInstance();

    private final SimpleDateFormat displayFormat =
            new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    private final SimpleDateFormat apiDateTimeFormat =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());

    private boolean isPartialRoom = false;
    private String availableFromDate = null;
    private String availableFromTime = null;

    private final List<RoomItem> roomList = new ArrayList<>();
    private ArrayAdapter<String> roomAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_booking);

        EdgeToEdgeUtils.applySystemBarInsets(this, findViewById(R.id.rootView));

        initDependencies();
        bindViews();
        AppToolbarMenu.setup(this, findViewById(R.id.appToolbar));
        setupRoomSpinner();
        setupGenderSpinner();
        setupDefaultDateTimes();
        readIntentExtras();
        refreshDateTimeFields();
        setupListeners();
        loadRooms();
    }

    private void initDependencies() {
        bookingRepository = new BookingRepository(getApplicationContext());
        roomRepository = new RoomRepository(getApplicationContext());
        localUserManager = new LocalUserManager(getApplicationContext());
    }

    private void bindViews() {
        etVisitorName = findViewById(R.id.etVisitorName);
        etVisitorDesignation = findViewById(R.id.etVisitorDesignation);
        etVisitorOrganisation = findViewById(R.id.etVisitorOrganisation);
        spinnerGender = findViewById(R.id.spinnerGender);
        etVisitorAddress = findViewById(R.id.etVisitorAddress);
        etVisitorMobile = findViewById(R.id.etVisitorMobile);
        etVisitorEmail = findViewById(R.id.etVisitorEmail);

        etArrivalDT = findViewById(R.id.etArrivalDT);
        etDepartureDT = findViewById(R.id.etDepartureDT);
        etPurpose = findViewById(R.id.etPurpose);

        etRequesteeName = findViewById(R.id.etRequesteeName);
        etRequesteeDesignation = findViewById(R.id.etRequesteeDesignation);
        etRequesteeDepartment = findViewById(R.id.etRequesteeDepartment);
        etRequesteeMobile = findViewById(R.id.etRequesteeMobile);

        etLogisticsName = findViewById(R.id.etLogisticsName);
        etLogisticsDesignation = findViewById(R.id.etLogisticsDesignation);
        etLogisticsMobile = findViewById(R.id.etLogisticsMobile);

        spinnerRoom = findViewById(R.id.spinnerRoom);
        tvMessage = findViewById(R.id.tvMessage);
        tvPartialRoomInfo = findViewById(R.id.tvPartialRoomInfo);

        btnCreateBooking = findViewById(R.id.btnCreateBooking);
        btnFillDummy = findViewById(R.id.btnFillDummy);
        btnBack = findViewById(R.id.btnBack);

        rgVisitorCategory = findViewById(R.id.rgVisitorCategory);
        rgRoomChargesStatus = findViewById(R.id.rgRoomChargesStatus);
        rgAttenderChargesStatus = findViewById(R.id.rgAttenderChargesStatus);
        etRoomChargesAmount = findViewById(R.id.etRoomChargesAmount);
        etAttenderChargesAmount = findViewById(R.id.etAttenderChargesAmount);

        cbAttenderRequired = findViewById(R.id.cbAttenderRequired);
        etAttenderCount = findViewById(R.id.etAttenderCount);
        tvSelectShiftLabel = findViewById(R.id.tvSelectShiftLabel);
        cbGeneralShift = findViewById(R.id.cbGeneralShift);
        cbMorningShift = findViewById(R.id.cbMorningShift);
        cbDayShift = findViewById(R.id.cbDayShift);
        cbNightShift = findViewById(R.id.cbNightShift);
    }

    private void setupRoomSpinner() {
        roomAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new ArrayList<>()
        );

        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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

    private void setupDefaultDateTimes() {
        departureCal.setTimeInMillis(arrivalCal.getTimeInMillis() + ONE_HOUR_MILLIS);
    }

    private void setupListeners() {
        etArrivalDT.setOnClickListener(v ->
                pickDateTime(arrivalCal, () -> {
                    ensureDepartureAfterArrival();
                    refreshDateTimeFields();
                })
        );

        etDepartureDT.setOnClickListener(v ->
                pickDateTime(departureCal, () -> {
                    if (!departureCal.getTime().after(arrivalCal.getTime())) {
                        showError("Departure must be after arrival.");
                        departureCal.setTimeInMillis(arrivalCal.getTimeInMillis() + ONE_HOUR_MILLIS);
                    }

                    refreshDateTimeFields();
                })
        );

        btnCreateBooking.setOnClickListener(v -> submitBooking());
        btnFillDummy.setOnClickListener(v -> fillRandomBookingData());
        btnBack.setOnClickListener(v -> finish());

        setupChargeAmountListener(rgRoomChargesStatus, etRoomChargesAmount);
        setupChargeAmountListener(rgAttenderChargesStatus, etAttenderChargesAmount);

        setupAttenderRequirementControls();
    }

    private void readIntentExtras() {
        Intent intent = getIntent();

        if (intent == null) {
            return;
        }

        readPreselectedRoom(intent);
        readPreselectedDates(intent);
        readPartialRoomInfo(intent);

        ensureDepartureAfterArrival();
        showPartialRoomInfoIfNeeded();
    }

    private void readPreselectedRoom(Intent intent) {
        if (intent.hasExtra(EXTRA_ROOM_ID)) {
            int roomId = intent.getIntExtra(EXTRA_ROOM_ID, -1);
            preselectedRoomId = roomId != -1 ? roomId : null;
        }

        preselectedRoomName = intent.getStringExtra(EXTRA_ROOM_NAME);
    }

    private void readPreselectedDates(Intent intent) {
        String arrivalDate = intent.getStringExtra(EXTRA_ARRIVAL_DATE);
        String departureDate = intent.getStringExtra(EXTRA_DEPARTURE_DATE);

        if (!isBlank(arrivalDate)) {
            setCalendarDateOnly(arrivalCal, arrivalDate, 10, 0);
        }

        if (!isBlank(departureDate)) {
            setCalendarDateOnly(departureCal, departureDate, 10, 0);
        }
    }

    private void readPartialRoomInfo(Intent intent) {
        isPartialRoom = intent.getBooleanExtra(EXTRA_IS_PARTIAL_ROOM, false);
        availableFromDate = intent.getStringExtra(EXTRA_AVAILABLE_FROM_DATE);
        availableFromTime = intent.getStringExtra(EXTRA_AVAILABLE_FROM_TIME);
    }

    private void showPartialRoomInfoIfNeeded() {
        if (!isPartialRoom) {
            tvPartialRoomInfo.setVisibility(View.GONE);
            return;
        }

        tvPartialRoomInfo.setVisibility(View.VISIBLE);
        tvPartialRoomInfo.setText(
                "This room is partially available.\nAvailable from: "
                        + safeText(availableFromDate)
                        + ", "
                        + safeText(availableFromTime)
        );
    }

    private void setCalendarDateOnly(Calendar calendar, String date, int hour, int minute) {
        try {
            String[] parts = date.split("-");

            if (parts.length != 3) {
                return;
            }

            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1;
            int day = Integer.parseInt(parts[2]);

            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

        } catch (Exception ignored) {
            // Keep default calendar value.
        }
    }

    private void ensureDepartureAfterArrival() {
        if (!departureCal.getTime().after(arrivalCal.getTime())) {
            departureCal.setTimeInMillis(arrivalCal.getTimeInMillis() + ONE_HOUR_MILLIS);
        }
    }

    private void loadRooms() {
        roomRepository.getRooms(result -> {
            if (result.isSuccess() && result.getRooms() != null) {
                InternetErrorBanner.hide(CreateBookingActivity.this);
                roomList.clear();
                roomList.addAll(result.getRooms());

                bindRoomsToSpinner();
                preselectRoomIfAvailable();
                return;
            }

            if (result.getErrorMessage() != null) {
                if (InternetErrorBanner.isNetworkErrorMessage(result.getErrorMessage())) {
                    InternetErrorBanner.show(CreateBookingActivity.this);
                }
                showError(result.getErrorMessage());
            }
        });
    }

    private void bindRoomsToSpinner() {
        List<String> roomNames = new ArrayList<>();
        roomNames.add("Select Room");

        for (RoomItem roomItem : roomList) {
            roomNames.add(roomItem.getRoomName());
        }

        roomAdapter.clear();
        roomAdapter.addAll(roomNames);
        roomAdapter.notifyDataSetChanged();
    }

    private void preselectRoomIfAvailable() {
        if (preselectedRoomId == null) {
            return;
        }

        for (int i = 0; i < roomList.size(); i++) {
            RoomItem roomItem = roomList.get(i);

            if (roomItem.getId() == preselectedRoomId) {
                spinnerRoom.setSelection(i + 1);
                return;
            }
        }

        if (!isBlank(preselectedRoomName)) {
            showMessage("Selected room: " + preselectedRoomName);
        }
    }

    private Integer getSelectedRoomId() {
        int position = spinnerRoom.getSelectedItemPosition();

        if (position <= 0 || position - 1 >= roomList.size()) {
            return null;
        }

        return roomList.get(position - 1).getId();
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

    private void refreshDateTimeFields() {
        etArrivalDT.setText(displayFormat.format(arrivalCal.getTime()));
        etDepartureDT.setText(displayFormat.format(departureCal.getTime()));
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
        setViewEnabled(cbNightShift, enabled);

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
        cbNightShift.setChecked(false);
    }

    private void submitBooking() {
        tvMessage.setVisibility(View.GONE);

        BookingFormData formData = collectFormData();

        if (!validateInputs(formData)) {
            return;
        }

        BookingCreateRequest request = createBookingRequest(formData);

        setLoading(true);

        bookingRepository.createBooking(request).enqueue(new Callback<ApiResponse<BookingActionData>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<BookingActionData>> call,
                    @NonNull Response<ApiResponse<BookingActionData>> response
            ) {
                InternetErrorBanner.hide(CreateBookingActivity.this);
                setLoading(false);

                if (!response.isSuccessful() || response.body() == null) {
                    showError(extractErrorMessage(response));
                    return;
                }

                ApiResponse<BookingActionData> apiResponse = response.body();

                if (!apiResponse.isSuccess()) {
                    showError(apiResponse.getFirstErrorMessage());
                    return;
                }

                sendBookingCreatedResult();
                showMessage(apiResponse.getSafeMessage());
                finish();
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<BookingActionData>> call,
                    @NonNull Throwable t
            ) {
                setLoading(false);
                InternetErrorBanner.show(CreateBookingActivity.this);
                showError("Please check your internet connection.");
            }
        });
    }

    private BookingFormData collectFormData() {
        BookingFormData data = new BookingFormData();

        data.visitorName = getText(etVisitorName);
        data.visitorDesignation = getText(etVisitorDesignation);
        data.visitorOrganisation = getText(etVisitorOrganisation);
        data.visitorGender = getSelectedGender();
        data.visitorAddress = getText(etVisitorAddress);
        data.visitorMobile = getText(etVisitorMobile);
        data.visitorEmail = getText(etVisitorEmail);

        data.arrivalAt = apiDateTimeFormat.format(arrivalCal.getTime());
        data.departureAt = apiDateTimeFormat.format(departureCal.getTime());

        data.purpose = getText(etPurpose);
        data.createdByName = localUserManager.getUserName();

        data.requesteeName = getText(etRequesteeName);
        data.requesteeDesignation = getText(etRequesteeDesignation);
        data.requesteeDepartment = getText(etRequesteeDepartment);
        data.requesteeMobile = getText(etRequesteeMobile);

        data.logisticsName = getText(etLogisticsName);
        data.logisticsDesignation = getText(etLogisticsDesignation);
        data.logisticsMobile = getText(etLogisticsMobile);

        data.roomId = getSelectedRoomId();
        data.visitorCategory = getSelectedVisitorCategory();

        data.attenderRequired = cbAttenderRequired.isChecked();
        data.attenderCountPerDay = getAttenderCount();
        data.attenderGeneralShift = cbGeneralShift.isChecked();
        data.attenderMorningShift = cbMorningShift.isChecked();
        data.attenderDayShift = cbDayShift.isChecked();
        data.attenderNightShift = cbNightShift.isChecked();
        data.roomChargesStatus = getChargeStatus(
                rgRoomChargesStatus,
                R.id.rbRoomChargesYes,
                R.id.rbRoomChargesWaived
        );
        data.attenderChargesStatus = getChargeStatus(
                rgAttenderChargesStatus,
                R.id.rbAttenderChargesYes,
                R.id.rbAttenderChargesWaived
        );
        data.roomChargesAmount = "yes".equals(data.roomChargesStatus)
                ? getText(etRoomChargesAmount)
                : "0";
        data.attenderChargesAmount = "yes".equals(data.attenderChargesStatus)
                ? getText(etAttenderChargesAmount)
                : "0";

        return data;
    }

    private BookingCreateRequest createBookingRequest(BookingFormData data) {
        return new BookingCreateRequest(
                data.roomId,
                data.arrivalAt,
                data.departureAt,
                data.createdByName,

                data.visitorName,
                data.visitorDesignation,
                data.visitorOrganisation,
                data.visitorGender,
                data.visitorAddress,
                data.visitorMobile,
                data.visitorEmail,
                data.purpose,
                data.visitorCategory,
                data.attenderRequired,
                data.attenderCountPerDay,
                data.attenderGeneralShift,
                data.attenderMorningShift,
                data.attenderDayShift,
                data.attenderNightShift,
                data.roomChargesStatus,
                data.attenderChargesStatus,
                data.roomChargesAmount,
                data.attenderChargesAmount,

                data.requesteeName,
                data.requesteeDesignation,
                data.requesteeDepartment,
                data.requesteeMobile,

                data.logisticsName,
                data.logisticsDesignation,
                data.logisticsMobile
        );
    }

    private boolean validateInputs(BookingFormData data) {
        if (TextUtils.isEmpty(data.visitorName)
                || TextUtils.isEmpty(data.arrivalAt)
                || TextUtils.isEmpty(data.departureAt)) {

            showError("Please fill all required fields.");
            return false;
        }

        if (data.roomId == null) {
            showError("Please select a room.");
            return false;
        }

        if (!departureCal.getTime().after(arrivalCal.getTime())) {
            showError("Departure date/time must be after arrival date/time.");
            return false;
        }

        if (data.attenderRequired && data.attenderCountPerDay <= 0) {
            showError("Enter number of attenders required per day.");
            return false;
        }

        if (data.attenderRequired
                && !data.attenderGeneralShift
                && !data.attenderMorningShift
                && !data.attenderDayShift
                && !data.attenderNightShift) {

            showError("Please select at least one attender shift.");
            return false;
        }

        if ("yes".equals(data.roomChargesStatus)
                && !isPositiveAmount(data.roomChargesAmount)) {
            etRoomChargesAmount.setError("Room charges amount is required.");
            etRoomChargesAmount.requestFocus();
            showError("Enter room charges amount.");
            return false;
        }

        if ("yes".equals(data.attenderChargesStatus)
                && !isPositiveAmount(data.attenderChargesAmount)) {
            etAttenderChargesAmount.setError("Attender charges amount is required.");
            etAttenderChargesAmount.requestFocus();
            showError("Enter attender charges amount.");
            return false;
        }

        return true;
    }

    private void setupChargeAmountListener(RadioGroup group, EditText amountField) {
        group.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            boolean enabled = checkedId == R.id.rbRoomChargesYes
                    || checkedId == R.id.rbAttenderChargesYes;
            amountField.setEnabled(enabled);

            if (!enabled) {
                amountField.setText("");
                amountField.setError(null);
            }
        });
    }

    private boolean isPositiveAmount(String amount) {
        try {
            return !TextUtils.isEmpty(amount) && new BigDecimal(amount).signum() > 0;
        } catch (NumberFormatException exception) {
            return false;
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
        btnCreateBooking.setText(loading ? "Please wait..." : "Create Booking");
    }

    private void showError(String message) {
        tvMessage.setVisibility(View.VISIBLE);
        tvMessage.setText(DateTimeUtils.formatDateTimesInText(message));
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

    private <T> String extractErrorMessage(Response<ApiResponse<T>> response) {
        try {
            if (response.errorBody() == null) {
                return "Booking creation failed.";
            }

            String errorJson = response.errorBody().string();
            ApiResponse<?> errorResponse = gson.fromJson(errorJson, ApiResponse.class);

            if (errorResponse == null) {
                return "Booking creation failed.";
            }

            String firstError = errorResponse.getFirstErrorMessage();

            if (!isBlank(firstError)) {
                return firstError;
            }

            if (!isBlank(errorResponse.getMessage())) {
                return errorResponse.getMessage();
            }

        } catch (Exception ignored) {
            // Return default error below.
        }

        return "Booking creation failed.";
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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
        etVisitorAddress.setText(randomValue(addresses));
        etVisitorMobile.setText(generateRandomMobile());
        etVisitorEmail.setText(generateRandomEmail(visitorName));

        etPurpose.setText(randomValue(purposes));

        etRequesteeName.setText("Rishabh");
        etRequesteeDesignation.setText(randomValue(designations));
        etRequesteeDepartment.setText(randomValue(departments));
        etRequesteeMobile.setText(generateRandomMobile());

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

    private static class BookingFormData {
        private Integer roomId;

        private String arrivalAt;
        private String departureAt;
        private String createdByName;

        private String visitorName;
        private String visitorDesignation;
        private String visitorOrganisation;
        private String visitorGender;
        private String visitorAddress;
        private String visitorMobile;
        private String visitorEmail;
        private String purpose;
        private String visitorCategory;

        private boolean attenderRequired;
        private int attenderCountPerDay;
        private boolean attenderGeneralShift;
        private boolean attenderMorningShift;
        private boolean attenderDayShift;
        private boolean attenderNightShift;
        private String roomChargesStatus;
        private String attenderChargesStatus;
        private String roomChargesAmount;
        private String attenderChargesAmount;

        private String requesteeName;
        private String requesteeDesignation;
        private String requesteeDepartment;
        private String requesteeMobile;

        private String logisticsName;
        private String logisticsDesignation;
        private String logisticsMobile;
    }
}
