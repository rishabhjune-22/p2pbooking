package com.example.roombooking.requester;

import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roombooking.R;
import com.example.roombooking.api.RetrofitClient;
import com.example.roombooking.auth.AuthSessionGuard;
import com.example.roombooking.auth.AuthSessionManager;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.model.room.RoomPrefix;
import com.example.roombooking.utils.ApiErrorUtils;
import com.example.roombooking.utils.AppToolbarMenu;
import com.example.roombooking.utils.DateTimeUtils;
import com.example.roombooking.utils.EdgeToEdgeUtils;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RequesterRequestBookingActivity extends AppCompatActivity {

    public static final String EXTRA_ARRIVAL_AT = "arrival_at";
    public static final String EXTRA_DEPARTURE_AT = "departure_at";
    public static final String EXTRA_PREFERRED_PREFIX = "preferred_prefix";
    public static final String EXTRA_PREFERRED_ROOM_ID = "preferred_room_id";
    public static final String EXTRA_PREFERRED_ROOM_NAME = "preferred_room_name";
    public static final String EXTRA_BOOKING_REQUEST_ID = "booking_request_id";
    public static final String EXTRA_EDIT_MODE = "edit_mode";
    public static final String EXTRA_RESUBMIT_MODE = "resubmit_mode";
    public static final String EXTRA_VISITOR_NAME = "visitor_name";
    public static final String EXTRA_VISITOR_DESIGNATION = "visitor_designation";
    public static final String EXTRA_VISITOR_ORGANISATION = "visitor_organisation";
    public static final String EXTRA_VISITOR_GENDER = "visitor_gender";
    public static final String EXTRA_VISITOR_MOBILE = "visitor_mobile";
    public static final String EXTRA_VISITOR_EMAIL = "visitor_email";
    public static final String EXTRA_VISITOR_CATEGORY = "visitor_category";
    public static final String EXTRA_PURPOSE_OF_VISIT = "purpose_of_visit";
    public static final String EXTRA_BUDGET_HEAD_TYPE = "budget_head_type";
    public static final String EXTRA_BUDGET_HEAD_VALUE = "budget_head_value";
    public static final String EXTRA_BUDGET_HEAD_NAME = "budget_head_name";
    public static final String EXTRA_BUDGET_HEAD_DEPARTMENT_NAME = "budget_head_department_name";
    public static final String EXTRA_BUDGET_HEAD_PROJECT_CODE = "budget_head_project_code";
    public static final String EXTRA_ATTENDER_REQUIRED = "attender_required";
    public static final String EXTRA_ATTENDER_GENERAL_SHIFT = "attender_general_shift";
    public static final String EXTRA_ATTENDER_MORNING_SHIFT = "attender_morning_shift";
    public static final String EXTRA_ATTENDER_DAY_SHIFT = "attender_day_shift";
    public static final String EXTRA_REQUESTOR_NAME = "requestor_name";
    public static final String EXTRA_REQUESTOR_DESIGNATION = "requestor_designation";
    public static final String EXTRA_REQUESTOR_DEPARTMENT = "requestor_department";
    public static final String EXTRA_REQUESTOR_MOBILE = "requestor_mobile";
    public static final String EXTRA_REQUESTOR_EMAIL = "requestor_email";

    private static final long ONE_HOUR_MILLIS = 60L * 60L * 1000L;

    private final SimpleDateFormat apiDateTimeFormat =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
    private final SimpleDateFormat displayFormat =
            new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    private MaterialToolbar toolbar;
    private ImageButton btnBack;
    private ScrollView scrollView;
    private EditText etSelectedArrival;
    private EditText etSelectedDeparture;
    private EditText etSelectedRoom;
    private Spinner spinnerPreferredPrefix;
    private EditText etVisitorName;
    private EditText etVisitorDesignation;
    private EditText etVisitorOrganisation;
    private Spinner spinnerGender;
    private EditText etVisitorMobile;
    private EditText etVisitorEmail;
    private RadioGroup rgVisitorCategory;
    private TextView btnClearVisitorCategory;
    private EditText etPurpose;
    private CheckBox cbBudgetHeadName;
    private CheckBox cbBudgetHeadDepartmentName;
    private CheckBox cbBudgetHeadProjectCode;
    private EditText etBudgetHeadName;
    private EditText etBudgetHeadDepartmentName;
    private EditText etBudgetHeadProjectCode;
    private TextView btnClearBudgetHeadFocus;
    private CheckBox cbAttenderRequired;
    private TextView tvSelectShiftLabel;
    private CheckBox cbGeneralShift;
    private CheckBox cbMorningShift;
    private CheckBox cbDayShift;
    private EditText etRequestorName;
    private EditText etRequestorDesignation;
    private EditText etRequestorDepartment;
    private EditText etRequestorMobile;
    private EditText etRequestorEmail;
    private TextView tvError;
    private AppCompatButton btnSubmit;

    private final Calendar arrivalCalendar = Calendar.getInstance();
    private final Calendar departureCalendar = Calendar.getInstance();
    private String selectedPrefixValue = RoomPrefix.DELTA;
    private String preferredRoomName = "";
    private Integer preferredRoomId = null;
    private boolean editMode = false;
    private boolean resubmitMode = false;
    private boolean suppressBudgetHeadFocus = false;
    private int bookingRequestId = -1;
    private Call<ApiResponse<BookingRequestItem>> submitCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requester_request_booking);
        if (!AuthSessionGuard.ensureRequester(this)) {
            return;
        }

        EdgeToEdgeUtils.applySystemBarInsets(this, findViewById(R.id.rootView));
        bindViews();
        setupScrollInsets();
        setupToolbar();
        setupScheduleFields();
        setupSpinners();
        setupListeners();
        setupFocusScrolling();
        prefill();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.appToolbar);
        btnBack = findViewById(R.id.btnBack);
        scrollView = findViewById(R.id.scrollRequesterRequest);
        etSelectedArrival = findViewById(R.id.etSelectedArrival);
        etSelectedDeparture = findViewById(R.id.etSelectedDeparture);
        etSelectedRoom = findViewById(R.id.etSelectedRoom);
        spinnerPreferredPrefix = findViewById(R.id.spinnerPreferredPrefix);
        etVisitorName = findViewById(R.id.etVisitorName);
        etVisitorDesignation = findViewById(R.id.etVisitorDesignation);
        etVisitorOrganisation = findViewById(R.id.etVisitorOrganisation);
        spinnerGender = findViewById(R.id.spinnerGender);
        etVisitorMobile = findViewById(R.id.etVisitorMobile);
        etVisitorEmail = findViewById(R.id.etVisitorEmail);
        rgVisitorCategory = findViewById(R.id.rgVisitorCategory);
        btnClearVisitorCategory = findViewById(R.id.btnClearVisitorCategory);
        etPurpose = findViewById(R.id.etPurpose);
        cbBudgetHeadName = findViewById(R.id.cbBudgetHeadName);
        cbBudgetHeadDepartmentName = findViewById(R.id.cbBudgetHeadDepartmentName);
        cbBudgetHeadProjectCode = findViewById(R.id.cbBudgetHeadProjectCode);
        etBudgetHeadName = findViewById(R.id.etBudgetHeadName);
        etBudgetHeadDepartmentName = findViewById(R.id.etBudgetHeadDepartmentName);
        etBudgetHeadProjectCode = findViewById(R.id.etBudgetHeadProjectCode);
        btnClearBudgetHeadFocus = findViewById(R.id.btnClearBudgetHeadFocus);
        cbAttenderRequired = findViewById(R.id.cbAttenderRequired);
        tvSelectShiftLabel = findViewById(R.id.tvSelectShiftLabel);
        cbGeneralShift = findViewById(R.id.cbGeneralShift);
        cbMorningShift = findViewById(R.id.cbMorningShift);
        cbDayShift = findViewById(R.id.cbDayShift);
        etRequestorName = findViewById(R.id.etRequestorName);
        etRequestorDesignation = findViewById(R.id.etRequestorDesignation);
        etRequestorDepartment = findViewById(R.id.etRequestorDepartment);
        etRequestorMobile = findViewById(R.id.etRequestorMobile);
        etRequestorEmail = findViewById(R.id.etRequestorEmail);
        tvError = findViewById(R.id.tvMessage);
        btnSubmit = findViewById(R.id.btnSubmitRequest);
    }

    private void setupToolbar() {
        btnBack.setOnClickListener(v -> finish());
        AppToolbarMenu.setupRequester(this, toolbar);
    }

    private void setupScheduleFields() {
        setTimePickerField(etSelectedArrival);
        setTimePickerField(etSelectedDeparture);
        setFixedValueField(etSelectedRoom);

        etSelectedArrival.setOnClickListener(v ->
                showTimePicker(arrivalCalendar, () -> {
                    refreshSelectedScheduleSummary();
                    validateSelectedSchedule();
                })
        );
        etSelectedDeparture.setOnClickListener(v ->
                showTimePicker(departureCalendar, () -> {
                    refreshSelectedScheduleSummary();
                    validateSelectedSchedule();
                })
        );
    }

    private void setTimePickerField(EditText field) {
        field.setKeyListener(null);
        field.setCursorVisible(false);
        field.setFocusable(false);
        field.setClickable(true);
        field.setLongClickable(false);
    }

    private void setFixedValueField(EditText field) {
        field.setKeyListener(null);
        field.setCursorVisible(false);
        field.setFocusable(false);
        field.setClickable(false);
        field.setLongClickable(false);
    }

    private void setupScrollInsets() {
        int initialLeft = scrollView.getPaddingLeft();
        int initialTop = scrollView.getPaddingTop();
        int initialRight = scrollView.getPaddingRight();
        int initialBottom = scrollView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(scrollView, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomInset = Math.max(systemBars.bottom, ime.bottom);
            view.setPadding(
                    initialLeft,
                    initialTop,
                    initialRight,
                    initialBottom + bottomInset
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(scrollView);
    }

    private void setupSpinners() {
        ArrayAdapter<String> prefixAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                RoomPrefix.displayOrder()
        );
        prefixAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPreferredPrefix.setAdapter(prefixAdapter);
        spinnerPreferredPrefix.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Object selected = parent.getItemAtPosition(position);
                if (selected != null) {
                    selectedPrefixValue = selected.toString();
                    refreshSelectedScheduleSummary();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Keep the current prefix.
            }
        });

        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.gender_options,
                android.R.layout.simple_spinner_item
        );
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(genderAdapter);
    }

    private void setupListeners() {
        btnClearVisitorCategory.setOnClickListener(v -> rgVisitorCategory.clearCheck());

        cbAttenderRequired.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                clearAttenderShifts();
            }
            updateAttenderControlsState();
        });

        etRequestorEmail.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard(v);
                submit();
                return true;
            }
            return false;
        });

        btnSubmit.setOnClickListener(v -> submit());
        setupBudgetHeadControls();
        updateAttenderControlsState();
    }

    private void setupFocusScrolling() {
        View[] views = {
                etVisitorName,
                etVisitorDesignation,
                etVisitorOrganisation,
                etVisitorMobile,
                etVisitorEmail,
                etPurpose,
                etBudgetHeadName,
                etBudgetHeadDepartmentName,
                etBudgetHeadProjectCode,
                etRequestorName,
                etRequestorDesignation,
                etRequestorDepartment,
                etRequestorMobile,
                etRequestorEmail
        };

        for (View view : views) {
            view.setOnFocusChangeListener((focusedView, hasFocus) -> {
                if (hasFocus) {
                    scrollToView(focusedView);
                }
            });
        }
    }

    private void prefill() {
        AuthSessionManager sessionManager = new AuthSessionManager(getApplicationContext());
        editMode = getIntent().getBooleanExtra(EXTRA_EDIT_MODE, false);
        resubmitMode = getIntent().getBooleanExtra(EXTRA_RESUBMIT_MODE, false);
        bookingRequestId = getIntent().getIntExtra(EXTRA_BOOKING_REQUEST_ID, -1);
        if (resubmitMode) {
            toolbar.setTitle("Edit & Resubmit");
            btnSubmit.setText("Resubmit Request");
        } else if (editMode) {
            toolbar.setTitle("Edit Request");
            btnSubmit.setText("Save Changes");
        }

        initializeDateCalendars(
                getIntent().getStringExtra(EXTRA_ARRIVAL_AT),
                getIntent().getStringExtra(EXTRA_DEPARTURE_AT)
        );
        selectPreferredPrefix(getIntent().getStringExtra(EXTRA_PREFERRED_PREFIX));
        preferredRoomId = readPreferredRoomId();
        preferredRoomName = safe(getIntent().getStringExtra(EXTRA_PREFERRED_ROOM_NAME));
        if (editMode) {
            prefillExistingRequest();
        } else {
            etRequestorName.setText(sessionManager.getUserName());
            etRequestorEmail.setText(sessionManager.getUserEmail());
            etRequestorDesignation.setText(sessionManager.getUserDesignation());
            etRequestorDepartment.setText(sessionManager.getUserDepartment());
            etRequestorMobile.setText(sessionManager.getUserMobile());
        }
        refreshSelectedScheduleSummary();
    }

    private void prefillExistingRequest() {
        etVisitorName.setText(getIntent().getStringExtra(EXTRA_VISITOR_NAME));
        etVisitorDesignation.setText(getIntent().getStringExtra(EXTRA_VISITOR_DESIGNATION));
        etVisitorOrganisation.setText(getIntent().getStringExtra(EXTRA_VISITOR_ORGANISATION));
        etVisitorMobile.setText(getIntent().getStringExtra(EXTRA_VISITOR_MOBILE));
        etVisitorEmail.setText(getIntent().getStringExtra(EXTRA_VISITOR_EMAIL));
        etPurpose.setText(getIntent().getStringExtra(EXTRA_PURPOSE_OF_VISIT));
        prefillBudgetHeadFields();

        etRequestorName.setText(getIntent().getStringExtra(EXTRA_REQUESTOR_NAME));
        etRequestorDesignation.setText(getIntent().getStringExtra(EXTRA_REQUESTOR_DESIGNATION));
        etRequestorDepartment.setText(getIntent().getStringExtra(EXTRA_REQUESTOR_DEPARTMENT));
        etRequestorMobile.setText(getIntent().getStringExtra(EXTRA_REQUESTOR_MOBILE));
        etRequestorEmail.setText(getIntent().getStringExtra(EXTRA_REQUESTOR_EMAIL));

        setGenderSelection(getIntent().getStringExtra(EXTRA_VISITOR_GENDER));
        setVisitorCategorySelection(getIntent().getStringExtra(EXTRA_VISITOR_CATEGORY));

        boolean attenderRequired = getIntent().getBooleanExtra(EXTRA_ATTENDER_REQUIRED, false);
        cbAttenderRequired.setChecked(attenderRequired);
        if (attenderRequired) {
            cbGeneralShift.setChecked(getIntent().getBooleanExtra(EXTRA_ATTENDER_GENERAL_SHIFT, false));
            cbMorningShift.setChecked(getIntent().getBooleanExtra(EXTRA_ATTENDER_MORNING_SHIFT, false));
            cbDayShift.setChecked(getIntent().getBooleanExtra(EXTRA_ATTENDER_DAY_SHIFT, false));
            updateAttenderControlsState();
        }
    }

    private void prefillBudgetHeadFields() {
        String name = safe(getIntent().getStringExtra(EXTRA_BUDGET_HEAD_NAME));
        String departmentName = safe(getIntent().getStringExtra(EXTRA_BUDGET_HEAD_DEPARTMENT_NAME));
        String projectCode = safe(getIntent().getStringExtra(EXTRA_BUDGET_HEAD_PROJECT_CODE));
        String type = safe(getIntent().getStringExtra(EXTRA_BUDGET_HEAD_TYPE));
        String value = safe(getIntent().getStringExtra(EXTRA_BUDGET_HEAD_VALUE));

        if (name.isEmpty() && departmentName.isEmpty() && projectCode.isEmpty() && !value.isEmpty()) {
            if ("institute_head".equalsIgnoreCase(type)) {
                departmentName = value;
            } else if ("project_head".equalsIgnoreCase(type)) {
                projectCode = value;
            } else {
                name = value;
            }
        }

        setBudgetHeadOptionFromValue(cbBudgetHeadName, etBudgetHeadName, name);
        setBudgetHeadOptionFromValue(cbBudgetHeadDepartmentName, etBudgetHeadDepartmentName, departmentName);
        setBudgetHeadOptionFromValue(cbBudgetHeadProjectCode, etBudgetHeadProjectCode, projectCode);
    }

    private void initializeDateCalendars(String arrivalAt, String departureAt) {
        Calendar now = Calendar.getInstance();
        arrivalCalendar.setTimeInMillis(now.getTimeInMillis());
        arrivalCalendar.set(Calendar.SECOND, 0);
        arrivalCalendar.set(Calendar.MILLISECOND, 0);

        departureCalendar.setTimeInMillis(arrivalCalendar.getTimeInMillis() + ONE_HOUR_MILLIS);

        Date parsedArrival = parseApiDate(arrivalAt);
        Date parsedDeparture = parseApiDate(departureAt);
        if (parsedArrival != null) {
            arrivalCalendar.setTime(parsedArrival);
        }
        if (parsedDeparture != null) {
            departureCalendar.setTime(parsedDeparture);
        }
        ensureDepartureAfterArrival();
    }

    private Date parseApiDate(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return apiDateTimeFormat.parse(value);
        } catch (ParseException ignored) {
            return null;
        }
    }

    private void selectPreferredPrefix(String prefix) {
        String cleanPrefix = safe(prefix);
        selectedPrefixValue = cleanPrefix.isEmpty() ? RoomPrefix.DELTA : cleanPrefix;
        for (int i = 0; i < spinnerPreferredPrefix.getCount(); i++) {
            Object value = spinnerPreferredPrefix.getItemAtPosition(i);
            if (value != null && value.toString().equalsIgnoreCase(selectedPrefixValue)) {
                spinnerPreferredPrefix.setSelection(i);
                return;
            }
        }
        spinnerPreferredPrefix.setSelection(0);
        selectedPrefixValue = spinnerPreferredPrefix.getSelectedItem() != null
                ? spinnerPreferredPrefix.getSelectedItem().toString()
                : RoomPrefix.DELTA;
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

    private void ensureDepartureAfterArrival() {
        if (!departureCalendar.getTime().after(arrivalCalendar.getTime())) {
            departureCalendar.setTimeInMillis(arrivalCalendar.getTimeInMillis() + ONE_HOUR_MILLIS);
        }
    }

    private void refreshSelectedScheduleSummary() {
        etSelectedArrival.setText(formatScheduleDateTime(arrivalCalendar));
        etSelectedDeparture.setText(formatScheduleDateTime(departureCalendar));
        etSelectedRoom.setText(formatSelectedRoom());
    }

    private String formatScheduleDateTime(Calendar calendar) {
        String formatted = DateTimeUtils.formatUtcToLocal(
                apiDateTimeFormat.format(calendar.getTime())
        );
        return isBlank(formatted) ? displayFormat.format(calendar.getTime()) : formatted;
    }

    private String formatSelectedRoom() {
        String prefix = selectedPrefix();
        String room = safe(preferredRoomName);
        if (room.isEmpty()) {
            return prefix + " - No specific room selected";
        }
        if (room.toLowerCase(Locale.getDefault()).startsWith(prefix.toLowerCase(Locale.getDefault()))) {
            return room;
        }
        return prefix + " - " + room;
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

    private boolean validateSelectedSchedule() {
        if (!departureCalendar.getTime().after(arrivalCalendar.getTime())) {
            showError("Departure must be after arrival.");
            scrollToView(etSelectedDeparture);
            return false;
        }

        hideError();
        return true;
    }

    private void submit() {
        if (submitCall != null) {
            return;
        }

        String visitorName = text(etVisitorName);
        String visitorEmail = text(etVisitorEmail);
        String requestorEmail = text(etRequestorEmail);

        if (!validateSelectedSchedule()) {
            return;
        }
        if (visitorName.isEmpty()) {
            showError("Visitor name is required.");
            etVisitorName.requestFocus();
            return;
        }
        if (!visitorEmail.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(visitorEmail).matches()) {
            showError("Enter a valid visitor email address.");
            etVisitorEmail.requestFocus();
            return;
        }
        if (!requestorEmail.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(requestorEmail).matches()) {
            showError("Enter a valid requester email address.");
            etRequestorEmail.requestFocus();
            return;
        }

        boolean attenderRequired = cbAttenderRequired.isChecked();
        if (attenderRequired
                && !cbGeneralShift.isChecked()
                && !cbMorningShift.isChecked()
                && !cbDayShift.isChecked()) {
            showError("Please select at least one attender shift.");
            scrollToView(tvSelectShiftLabel);
            return;
        }

        hideError();
        if (editMode && bookingRequestId <= 0) {
            showError("Booking request could not be opened for editing.");
            return;
        }

        setLoading(true);
        BookingRequestCreateRequest request = buildRequest(
                visitorName,
                visitorEmail,
                requestorEmail,
                attenderRequired
        );

        submitCall = editMode
                ? RetrofitClient.getApiService(getApplicationContext())
                        .updateRequesterBookingRequest(bookingRequestId, request)
                : RetrofitClient.getApiService(getApplicationContext())
                        .createRequesterBookingRequest(request);
        submitCall.enqueue(new Callback<ApiResponse<BookingRequestItem>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<BookingRequestItem>> call,
                    @NonNull Response<ApiResponse<BookingRequestItem>> response
            ) {
                if (call != submitCall) return;
                submitCall = null;
                setLoading(false);

                if (!response.isSuccessful()
                        || response.body() == null
                        || !response.body().isSuccess()) {
                    showError(ApiErrorUtils.messageFromResponse(
                            response,
                            resubmitMode
                                    ? "Booking request could not be resubmitted."
                                    : editMode
                                    ? "Booking request could not be updated."
                                    : "Booking request could not be submitted."
                    ));
                    return;
                }

                Toast.makeText(
                        RequesterRequestBookingActivity.this,
                        resubmitMode
                                ? "Request resubmitted successfully."
                                : editMode
                                ? "Booking request updated."
                                : "Your booking request has been submitted for admin approval.",
                        Toast.LENGTH_LONG
                ).show();
                finish();
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<BookingRequestItem>> call,
                    @NonNull Throwable t
            ) {
                if (call != submitCall) return;
                submitCall = null;
                setLoading(false);
                if (!call.isCanceled()) {
                    showError(ApiErrorUtils.messageFromThrowable(t));
                }
            }
        });
    }

    private BookingRequestCreateRequest buildRequest(
            String visitorName,
            String visitorEmail,
            String requestorEmail,
            boolean attenderRequired
    ) {
        return new BookingRequestCreateRequest(
                apiDateTimeFormat.format(arrivalCalendar.getTime()),
                apiDateTimeFormat.format(departureCalendar.getTime()),
                selectedPrefix(),
                preferredRoomId,
                "",
                visitorName,
                text(etVisitorDesignation),
                text(etVisitorOrganisation),
                selectedGender(),
                text(etVisitorMobile),
                visitorEmail,
                selectedVisitorCategory(),
                text(etPurpose),
                "",
                "",
                getBudgetHeadText(cbBudgetHeadName, etBudgetHeadName),
                getBudgetHeadText(cbBudgetHeadDepartmentName, etBudgetHeadDepartmentName),
                getBudgetHeadText(cbBudgetHeadProjectCode, etBudgetHeadProjectCode),
                attenderRequired,
                attenderRequired && cbGeneralShift.isChecked(),
                attenderRequired && cbMorningShift.isChecked(),
                attenderRequired && cbDayShift.isChecked(),
                text(etRequestorName),
                text(etRequestorDesignation),
                text(etRequestorDepartment),
                text(etRequestorMobile),
                requestorEmail
        );
    }

    private void setupBudgetHeadControls() {
        setupBudgetHeadOption(cbBudgetHeadName, etBudgetHeadName);
        setupBudgetHeadOption(cbBudgetHeadDepartmentName, etBudgetHeadDepartmentName);
        setupBudgetHeadOption(cbBudgetHeadProjectCode, etBudgetHeadProjectCode);

        if (btnClearBudgetHeadFocus != null) {
            btnClearBudgetHeadFocus.setOnClickListener(v -> {
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

    private void updateBudgetHeadFieldVisibility(CheckBox checkbox, EditText field) {
        if (checkbox == null || field == null) {
            return;
        }
        field.setVisibility(checkbox.isChecked() ? View.VISIBLE : View.GONE);
    }

    private void setBudgetHeadOptionFromValue(CheckBox checkbox, EditText field, String value) {
        if (checkbox == null || field == null) {
            return;
        }
        suppressBudgetHeadFocus = true;
        try {
            String cleanValue = safe(value);
            checkbox.setChecked(!cleanValue.isEmpty());
            field.setText(cleanValue);
            updateBudgetHeadFieldVisibility(checkbox, field);
        } finally {
            suppressBudgetHeadFocus = false;
        }
    }

    private String getBudgetHeadText(CheckBox checkbox, EditText field) {
        if (checkbox == null || field == null || !checkbox.isChecked()) {
            return "";
        }
        return text(field);
    }

    private void updateAttenderControlsState() {
        boolean attenderRequired = cbAttenderRequired.isChecked();
        setViewEnabled(tvSelectShiftLabel, attenderRequired);
        setViewEnabled(cbGeneralShift, attenderRequired);
        setViewEnabled(cbMorningShift, attenderRequired);
        setViewEnabled(cbDayShift, attenderRequired);
        if (!attenderRequired) {
            clearAttenderShifts();
        }
    }

    private void setViewEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        view.setAlpha(enabled ? 1.0f : 0.45f);
    }

    private void clearAttenderShifts() {
        cbGeneralShift.setChecked(false);
        cbMorningShift.setChecked(false);
        cbDayShift.setChecked(false);
    }

    private String selectedPrefix() {
        return selectedPrefixValue;
    }

    private Integer readPreferredRoomId() {
        int roomId = getIntent().getIntExtra(EXTRA_PREFERRED_ROOM_ID, 0);
        return roomId > 0 ? roomId : null;
    }

    private String selectedGender() {
        int position = spinnerGender.getSelectedItemPosition();
        if (position <= 0 || spinnerGender.getSelectedItem() == null) {
            return "";
        }
        return spinnerGender.getSelectedItem().toString();
    }

    private String selectedVisitorCategory() {
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

    private void setLoading(boolean loading) {
        btnSubmit.setEnabled(!loading);
        if (loading) {
            btnSubmit.setText(resubmitMode ? "Resubmitting..." : editMode ? "Saving..." : "Submitting...");
        } else {
            btnSubmit.setText(resubmitMode ? "Resubmit Request" : editMode ? "Save Changes" : "Submit Request");
        }
    }

    private void showError(String message) {
        tvError.setText(TextUtils.isEmpty(message) ? "Something went wrong." : message);
        tvError.setVisibility(View.VISIBLE);
        scrollToView(tvError);
    }

    private void hideError() {
        tvError.setText("");
        tvError.setVisibility(View.GONE);
    }

    private void scrollToView(final View view) {
        if (scrollView == null || view == null) return;
        view.postDelayed(() -> {
            Rect rect = new Rect();
            view.getDrawingRect(rect);
            scrollView.offsetDescendantRectToMyCoords(view, rect);
            int targetY = Math.max(0, rect.bottom - (scrollView.getHeight() / 2));
            scrollView.smoothScrollTo(0, targetY);
        }, 220);
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        view.clearFocus();
    }

    private void focusAndShowKeyboard(EditText field) {
        if (field == null) {
            return;
        }
        field.requestFocus();
        field.setSelection(field.getText() != null ? field.getText().length() : 0);
        field.postDelayed(() -> {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(field, InputMethodManager.SHOW_IMPLICIT);
            }
            scrollToView(field);
        }, 150);
    }

    private String text(EditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private static String safe(String value) {
        return value != null ? value.trim() : "";
    }

    private static boolean isBlank(String value) {
        return TextUtils.isEmpty(safe(value));
    }

    @Override
    protected void onDestroy() {
        if (submitCall != null && !submitCall.isCanceled()) {
            submitCall.cancel();
        }
        submitCall = null;
        super.onDestroy();
    }
}
