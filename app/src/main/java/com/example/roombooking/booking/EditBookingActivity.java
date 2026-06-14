package com.example.roombooking.booking;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.TimeZone;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import android.text.Editable;
import android.text.TextWatcher;
import com.example.roombooking.R;
import com.example.roombooking.model.booking.BookingActionData;
import com.example.roombooking.model.booking.BookingItem;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.model.room.RoomItem;
import com.example.roombooking.room.RoomRepository;
import com.example.roombooking.utils.EdgeToEdgeUtils;
import com.example.roombooking.utils.AppToolbarMenu;
import com.example.roombooking.utils.InternetErrorBanner;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditBookingActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_DATA = "booking_data";

    private static final String EXTRA_UPDATED_BOOKING_ID = "updated_booking_id";
    private static final String EXTRA_UPDATED_STATUS = "updated_status";
    private static final String EXTRA_ARRIVAL_AT = "arrival_at";
    private static final String EXTRA_DEPARTURE_AT = "departure_at";

    private static final long ONE_HOUR_MILLIS = 60 * 60 * 1000L;

    private static final Pattern BOOKED_CONFLICT_PATTERN = Pattern.compile(
            "(.+?) is already booked from (.+?) to (.+?)\\.",
            Pattern.CASE_INSENSITIVE
    );

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
    private EditText etRoomChargesAmount;
    private EditText etAttenderChargesAmount;
    private TextView tvSelectShiftLabel;
    private CheckBox cbAttenderRequired;
    private EditText etAttenderCount;
    private CheckBox cbGeneralShift;
    private CheckBox cbMorningShift;
    private CheckBox cbDayShift;
    private CheckBox cbNightShift;

    private EditText etRequesteeName;
    private EditText etRequesteeDesignation;
    private EditText etRequesteeDepartment;
    private EditText etRequesteeMobile;

    private EditText etLogisticsName;
    private EditText etLogisticsDesignation;
    private EditText etLogisticsMobile;

    private TextView tvMessage;
    private AppCompatButton btnSaveBooking;

    private BookingRepository bookingRepository;
    private RoomRepository roomRepository;
    private BookingItem bookingItem;

    private final Gson gson = new Gson();

    private final Calendar arrivalCal = Calendar.getInstance();
    private final Calendar departureCal = Calendar.getInstance();

    private final SimpleDateFormat displayFormat =
            new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    private final SimpleDateFormat apiDateTimeFormat =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());

    private final SimpleDateFormat backendDateTimeFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ssXXX", Locale.getDefault());

    private final List<RoomItem> roomList = new ArrayList<>();
    private ArrayAdapter<String> roomAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_booking);
        backendDateTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        EdgeToEdgeUtils.applySystemBarInsets(this, findViewById(R.id.rootView));

        initDependencies();
        bindViews();
        AppToolbarMenu.setup(this, findViewById(R.id.appToolbar));
        setupRoomSpinner();
        setupGenderSpinner();
        setupListeners();

        bookingItem = getBookingFromIntent();

        if (bookingItem == null) {
            showToast("No booking details found.");
            finish();
            return;
        }

        bindBookingData();
        loadRooms();
    }

    private void initDependencies() {
        bookingRepository = new BookingRepository(getApplicationContext());
        roomRepository = new RoomRepository(getApplicationContext());
    }

    private void bindViews() {
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
        etRoomChargesAmount = findViewById(R.id.etRoomChargesAmount);
        etAttenderChargesAmount = findViewById(R.id.etAttenderChargesAmount);

        cbAttenderRequired = findViewById(R.id.cbAttenderRequired);
        etAttenderCount = findViewById(R.id.etAttenderCount);
        cbGeneralShift = findViewById(R.id.cbGeneralShift);
        cbMorningShift = findViewById(R.id.cbMorningShift);
        cbDayShift = findViewById(R.id.cbDayShift);
        cbNightShift = findViewById(R.id.cbNightShift);

        etRequesteeName = findViewById(R.id.etRequesteeName);
        etRequesteeDesignation = findViewById(R.id.etRequesteeDesignation);
        etRequesteeDepartment = findViewById(R.id.etRequesteeDepartment);
        etRequesteeMobile = findViewById(R.id.etRequesteeMobile);

        etLogisticsName = findViewById(R.id.etLogisticsName);
        etLogisticsDesignation = findViewById(R.id.etLogisticsDesignation);
        etLogisticsMobile = findViewById(R.id.etLogisticsMobile);

        tvMessage = findViewById(R.id.tvMessage);
        btnSaveBooking = findViewById(R.id.btnSaveBooking);
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

    private void setupListeners() {
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        etArrivalAt.setOnClickListener(v ->
                pickDateTime(arrivalCal, () -> {
                    ensureDepartureAfterArrival();
                    refreshDateTimeFields();
                })
        );

        etDepartureAt.setOnClickListener(v ->
                pickDateTime(departureCal, () -> {
                    if (!departureCal.getTime().after(arrivalCal.getTime())) {
                        showError("Departure must be after arrival.");
                        ensureDepartureAfterArrival();
                    }

                    refreshDateTimeFields();
                })
        );

        btnSaveBooking.setOnClickListener(v -> saveBooking());
        setupChargeAmountListener(rgRoomChargesStatus, etRoomChargesAmount);
        setupChargeAmountListener(rgAttenderChargesStatus, etAttenderChargesAmount);
        setupAttenderRequirementControls();
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

    private void bindBookingData() {
        etVisitorName.setText(safe(bookingItem.getVisitorName()));
        etVisitorDesignation.setText(safe(bookingItem.getVisitorDesignation()));
        etVisitorOrganisation.setText(safe(bookingItem.getVisitorOrganisation()));
        etVisitorAddress.setText(safe(bookingItem.getVisitorAddress()));
        etVisitorMobile.setText(safe(bookingItem.getVisitorMobile()));
        etVisitorEmail.setText(safe(bookingItem.getVisitorEmail()));
        etPurpose.setText(safe(bookingItem.getPurposeOfVisit()));

        selectGender(bookingItem.getVisitorGender());
        selectVisitorCategory(bookingItem.getVisitorCategory());

        cbAttenderRequired.setChecked(bookingItem.isAttenderRequired());
        etAttenderCount.setText(String.valueOf(bookingItem.getAttenderCountPerDay()));
        cbGeneralShift.setChecked(bookingItem.isAttenderGeneralShift());
        cbMorningShift.setChecked(bookingItem.isAttenderMorningShift());
        cbDayShift.setChecked(bookingItem.isAttenderDayShift());
        cbNightShift.setChecked(bookingItem.isAttenderNightShift());
        selectChargeStatus(
                rgRoomChargesStatus,
                bookingItem.getRoomChargesStatus(),
                R.id.rbRoomChargesYes,
                R.id.rbRoomChargesNo,
                R.id.rbRoomChargesWaived
        );
        selectChargeStatus(
                rgAttenderChargesStatus,
                bookingItem.getAttenderChargesStatus(),
                R.id.rbAttenderChargesYes,
                R.id.rbAttenderChargesNo,
                R.id.rbAttenderChargesWaived
        );
        etRoomChargesAmount.setText(
                "yes".equalsIgnoreCase(bookingItem.getRoomChargesStatus())
                        ? safe(bookingItem.getRoomChargesAmount())
                        : ""
        );
        etAttenderChargesAmount.setText(
                "yes".equalsIgnoreCase(bookingItem.getAttenderChargesStatus())
                        ? safe(bookingItem.getAttenderChargesAmount())
                        : ""
        );

        etRequesteeName.setText(safe(bookingItem.getRequesteeName()));
        etRequesteeDesignation.setText(safe(bookingItem.getRequesteeDesignation()));
        etRequesteeDepartment.setText(safe(bookingItem.getRequesteeDepartment()));
        etRequesteeMobile.setText(safe(bookingItem.getRequesteeMobile()));

        etLogisticsName.setText(safe(bookingItem.getLogisticsName()));
        etLogisticsDesignation.setText(safe(bookingItem.getLogisticsDesignation()));
        etLogisticsMobile.setText(safe(bookingItem.getLogisticsMobile()));

        parseCalendarFromApiString(bookingItem.getArrivalAt(), arrivalCal);
        parseCalendarFromApiString(bookingItem.getDepartureAt(), departureCal);

        ensureDepartureAfterArrival();
        refreshDateTimeFields();
    }

    private void loadRooms() {
        roomRepository.getRooms(result -> {
            if (result.isSuccess() && result.getRooms() != null) {
                InternetErrorBanner.hide(EditBookingActivity.this);
                roomList.clear();
                roomList.addAll(result.getRooms());

                bindRoomsToSpinner();
                preselectCurrentRoom();
                return;
            }

            if (InternetErrorBanner.isNetworkErrorMessage(result.getErrorMessage())) {
                InternetErrorBanner.show(EditBookingActivity.this);
            }
            showError("Rooms could not be loaded. Please try again.");
        });
    }

    private void bindRoomsToSpinner() {
        List<String> roomNames = new ArrayList<>();
        roomNames.add("Select Room");

        for (RoomItem roomItem : roomList) {
            roomNames.add(roomItem.getSafeRoomName());
        }

        roomAdapter.clear();
        roomAdapter.addAll(roomNames);
        roomAdapter.notifyDataSetChanged();
    }

    private void preselectCurrentRoom() {
        int currentRoomId = bookingItem.getRoom();

        for (int i = 0; i < roomList.size(); i++) {
            RoomItem roomItem = roomList.get(i);

            if (roomItem.getId() == currentRoomId) {
                spinnerRoom.setSelection(i + 1);
                return;
            }
        }
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
        cbNightShift.setChecked(false);
    }
    private void saveBooking() {
        hideMessage();

        EditBookingFormData formData = collectFormData();

        if (!validateInputs(formData)) {
            return;
        }

        BookingUpdateRequest request = createUpdateRequest(formData);

        setSavingState(true);

        bookingRepository.updateBooking(
                bookingItem.getId(),
                request
        ).enqueue(new Callback<ApiResponse<BookingActionData>>() {

            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<BookingActionData>> call,
                    @NonNull Response<ApiResponse<BookingActionData>> response
            ) {
                InternetErrorBanner.hide(EditBookingActivity.this);
                setSavingState(false);

                if (!response.isSuccessful() || response.body() == null) {
                    showError(extractErrorMessage(response));
                    return;
                }

                ApiResponse<BookingActionData> apiResponse = response.body();

                if (!apiResponse.isSuccess()) {
                    showError(apiResponse.getFirstErrorMessage());
                    return;
                }

                handleUpdateSuccess(formData, apiResponse);
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<BookingActionData>> call,
                    @NonNull Throwable t
            ) {
                setSavingState(false);
                InternetErrorBanner.show(EditBookingActivity.this);
                showError("Please check your internet connection.");
            }
        });
    }

    private EditBookingFormData collectFormData() {
        EditBookingFormData data = new EditBookingFormData();

        data.roomId = getSelectedRoomId();

        data.arrivalAt = apiDateTimeFormat.format(arrivalCal.getTime());
        data.departureAt = apiDateTimeFormat.format(departureCal.getTime());

        data.visitorName = getText(etVisitorName);
        data.visitorDesignation = getText(etVisitorDesignation);
        data.visitorOrganisation = getText(etVisitorOrganisation);
        data.visitorGender = getSelectedGender();
        data.visitorAddress = getText(etVisitorAddress);
        data.visitorMobile = getText(etVisitorMobile);
        data.visitorEmail = getText(etVisitorEmail);
        data.purpose = getText(etPurpose);

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

        data.requesteeName = getText(etRequesteeName);
        data.requesteeDesignation = getText(etRequesteeDesignation);
        data.requesteeDepartment = getText(etRequesteeDepartment);
        data.requesteeMobile = getText(etRequesteeMobile);

        data.logisticsName = getText(etLogisticsName);
        data.logisticsDesignation = getText(etLogisticsDesignation);
        data.logisticsMobile = getText(etLogisticsMobile);

        return data;
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

    private boolean validateInputs(EditBookingFormData data) {
        if (data.roomId == null) {
            showError("Please select a room.");
            return false;
        }

        if (TextUtils.isEmpty(data.visitorName)
                || TextUtils.isEmpty(data.visitorGender)
                || TextUtils.isEmpty(data.arrivalAt)
                || TextUtils.isEmpty(data.departureAt)) {

            showError("Please fill all required fields.");
            return false;
        }

        if (!TextUtils.isEmpty(data.visitorMobile)
                && !data.visitorMobile.matches("\\d{10}")) {
            showError("Visitor mobile must be 10 digits.");
            return false;
        }

        if (!TextUtils.isEmpty(data.requesteeMobile)
                && !data.requesteeMobile.matches("\\d{10}")) {
            showError("Requestee mobile must be 10 digits.");
            return false;
        }

        if (!TextUtils.isEmpty(data.logisticsMobile)
                && !data.logisticsMobile.matches("\\d{10}")) {
            showError("Logistics mobile must be 10 digits.");
            return false;
        }

        if (!departureCal.getTime().after(arrivalCal.getTime())) {
            showError("Departure must be after arrival.");
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

    private BookingUpdateRequest createUpdateRequest(EditBookingFormData data) {
        return new BookingUpdateRequest(
                data.roomId,
                data.arrivalAt,
                data.departureAt,

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

    private void handleUpdateSuccess(
            EditBookingFormData formData,
            ApiResponse<BookingActionData> apiResponse
    ) {
        bookingItem.setArrivalAt(formData.arrivalAt);
        bookingItem.setDepartureAt(formData.departureAt);

        BookingActionData data = apiResponse.getData();

        if (data != null && data.getStatus() != null) {
            bookingItem.setStatus(data.getStatus());
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_UPDATED_BOOKING_ID, bookingItem.getId());
        resultIntent.putExtra(EXTRA_UPDATED_STATUS, bookingItem.getStatus());
        resultIntent.putExtra(EXTRA_ARRIVAL_AT, formData.arrivalAt);
        resultIntent.putExtra(EXTRA_DEPARTURE_AT, formData.departureAt);

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

    private void parseCalendarFromApiString(String value, Calendar calendar) {
        try {
            if (value == null || value.trim().isEmpty()) {
                return;
            }

            Date date = apiDateTimeFormat.parse(value);

            if (date != null) {
                calendar.setTime(date);
            }

        } catch (Exception ignored) {
            // Keep current default date/time.
        }
    }

    private void ensureDepartureAfterArrival() {
        if (!departureCal.getTime().after(arrivalCal.getTime())) {
            departureCal.setTimeInMillis(arrivalCal.getTimeInMillis() + ONE_HOUR_MILLIS);
        }
    }

    private void refreshDateTimeFields() {
        etArrivalAt.setText(displayFormat.format(arrivalCal.getTime()));
        etDepartureAt.setText(displayFormat.format(departureCal.getTime()));
    }

    private void setSavingState(boolean saving) {
        btnSaveBooking.setEnabled(!saving);
        btnSaveBooking.setText(saving ? "Saving..." : "Save Booking");
    }

    private void showError(String message) {
        showMessage(makeFriendlyMessage(message), true);
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

    private String makeFriendlyMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "Something went wrong. Please try again.";
        }

        String conflictMessage = buildFriendlyConflictMessage(message);

        if (!conflictMessage.isEmpty()) {
            return conflictMessage;
        }

        String lowerMessage = message.toLowerCase(Locale.ROOT).trim();

        if (lowerMessage.contains("cooling period")) {
            return buildFriendlyCoolingMessage(message);
        }

        if (lowerMessage.contains("unavailable")) {
            return buildFriendlyUnavailableMessage(message);
        }

        if (lowerMessage.contains("arrival")
                && lowerMessage.contains("departure")) {
            return "Please check the arrival and departure date/time. Departure must be after arrival.";
        }

        if (lowerMessage.contains("mobile")
                || lowerMessage.contains("phone")) {
            return "Please enter a valid 10-digit mobile number.";
        }

        if (lowerMessage.contains("required")
                || lowerMessage.contains("blank")
                || lowerMessage.contains("empty")
                || lowerMessage.contains("null")) {
            return "Please fill all required details before saving the booking.";
        }

        if (lowerMessage.contains("network")
                || lowerMessage.contains("internet")
                || lowerMessage.contains("timeout")
                || lowerMessage.contains("failed to connect")) {
            return "Internet connection seems slow or unavailable. Please check your connection and try again.";
        }

        if (lowerMessage.contains("failed")) {
            return "Booking could not be updated. Please check the details and try again.";
        }

        return message.trim();
    }

    private String buildFriendlyConflictMessage(String message) {
        Matcher matcher = BOOKED_CONFLICT_PATTERN.matcher(message.trim());

        if (!matcher.find()) {
            return "";
        }

        String roomName = matcher.group(1).trim();
        String startTime = formatBackendDateTime(matcher.group(2).trim());
        String endTime = formatBackendDateTime(matcher.group(3).trim());

        return roomName
                + " is already booked from "
                + startTime
                + " to "
                + endTime
                + ". Please choose another room or change the timing.";
    }

    private String buildFriendlyCoolingMessage(String message) {
        String lowerMessage = message.toLowerCase(Locale.ROOT);

        if (!lowerMessage.contains("booked after")) {
            return "This room is in cooling period after a previous booking. Please choose a later time or another room.";
        }

        int index = lowerMessage.indexOf("booked after");
        String dateTimeText = message.substring(index + "booked after".length()).replace(".", "").trim();
        String availableAfter = formatBackendDateTime(dateTimeText);

        return "This room is in cooling period after a previous booking. It can be booked after "
                + availableAfter
                + ".";
    }

    private String buildFriendlyUnavailableMessage(String message) {
        return message
                .replace("+00:00", "")
                .replace(" 00:00:00", "")
                .trim();
    }

    private String formatBackendDateTime(String value) {
        try {
            Date date = backendDateTimeFormat.parse(value);

            if (date != null) {
                return displayFormat.format(date);
            }

        } catch (Exception ignored) {
            // Return original value below.
        }

        return value;
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

    private <T> String extractErrorMessage(Response<ApiResponse<T>> response) {
        try {
            if (response.errorBody() == null) {
                return "Update failed.";
            }

            String errorJson = response.errorBody().string();
            ApiResponse<?> errorResponse = gson.fromJson(errorJson, ApiResponse.class);

            if (errorResponse == null) {
                return "Update failed.";
            }

            String message = errorResponse.getSafeMessage();

            if (message != null && !message.trim().isEmpty()) {
                return message;
            }

            String firstError = errorResponse.getFirstErrorMessage();

            if (firstError != null && !firstError.trim().isEmpty()) {
                return firstError;
            }

        } catch (Exception ignored) {
            // Return default message below.
        }

        return "Update failed.";
    }

    private static class EditBookingFormData {
        private Integer roomId;

        private String arrivalAt;
        private String departureAt;

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
