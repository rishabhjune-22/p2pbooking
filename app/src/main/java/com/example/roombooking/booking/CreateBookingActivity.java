package com.example.roombooking.booking;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.roombooking.R;
import com.example.roombooking.model.booking.BookingActionData;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.model.room.RoomItem;
import com.example.roombooking.room.RoomRepository;
import com.example.roombooking.security.CryptoManager;
import com.example.roombooking.security.EncryptedBookingPayload;
import com.example.roombooking.security.KeystoreBackedCryptoSessionManager;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import javax.crypto.SecretKey;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateBookingActivity extends AppCompatActivity {

    private static final String EXTRA_BOOKING_CREATED = "booking_created";

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
    private Button btnCreateBooking;

    private BookingRepository bookingRepository;
    private RoomRepository roomRepository;
    private final Gson gson = new Gson();
    private Button btnFillDummy;
    private final Calendar arrivalCal = Calendar.getInstance();
    private final Calendar departureCal = Calendar.getInstance();

    private final SimpleDateFormat displayFmt =
            new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    private final SimpleDateFormat apiDateTimeFmt =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());

    private final List<RoomItem> roomList = new ArrayList<>();
    private ArrayAdapter<String> roomAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_booking);

        bookingRepository = new BookingRepository(getApplicationContext());
        roomRepository = new RoomRepository(getApplicationContext());
        bindViews();
        setupRoomSpinner();
        setupGenderSpinner();

        departureCal.setTimeInMillis(arrivalCal.getTimeInMillis() + 60 * 60 * 1000L);
        refreshDateTimeFields();
        loadRooms();

        etArrivalDT.setOnClickListener(v -> pickDateTime(arrivalCal, () -> {
            if (departureCal.getTimeInMillis() <= arrivalCal.getTimeInMillis()) {
                departureCal.setTimeInMillis(arrivalCal.getTimeInMillis() + 60 * 60 * 1000L);
            }
            refreshDateTimeFields();
        }));

        etDepartureDT.setOnClickListener(v -> pickDateTime(departureCal, () -> {
            if (departureCal.getTimeInMillis() <= arrivalCal.getTimeInMillis()) {
                showError("Departure must be after arrival.");
                departureCal.setTimeInMillis(arrivalCal.getTimeInMillis() + 60 * 60 * 1000L);
            }
            refreshDateTimeFields();
        }));

        btnCreateBooking.setOnClickListener(v -> submitBooking());
        btnFillDummy.setOnClickListener(v -> getRandomBookingData());
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
        btnCreateBooking = findViewById(R.id.btnCreateBooking);
        btnFillDummy = findViewById(R.id.btnFillDummy);

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

    private void loadRooms() {
        roomRepository.getRooms(result -> {
            if (result.isSuccess() && result.getRooms() != null) {
                roomList.clear();
                roomList.addAll(result.getRooms());
                bindRoomsToSpinner();
            } else if (result.getErrorMessage() != null) {
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

    private Integer getSelectedRoomId() {
        int position = spinnerRoom.getSelectedItemPosition();
        if (position <= 0) {
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

    private void refreshDateTimeFields() {
        etArrivalDT.setText(displayFmt.format(arrivalCal.getTime()));
        etDepartureDT.setText(displayFmt.format(departureCal.getTime()));
    }

    private void pickDateTime(Calendar target, Runnable onDone) {
        int year = target.get(Calendar.YEAR);
        int month = target.get(Calendar.MONTH);
        int day = target.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, y, m, d) -> {
            target.set(Calendar.YEAR, y);
            target.set(Calendar.MONTH, m);
            target.set(Calendar.DAY_OF_MONTH, d);

            int hour = target.get(Calendar.HOUR_OF_DAY);
            int minute = target.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (timeView, h, min) -> {
                target.set(Calendar.HOUR_OF_DAY, h);
                target.set(Calendar.MINUTE, min);
                target.set(Calendar.SECOND, 0);
                target.set(Calendar.MILLISECOND, 0);
                onDone.run();
            }, hour, minute, true);

            timePickerDialog.show();
        }, year, month, day);

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void submitBooking() {
        tvMessage.setVisibility(View.GONE);

        String visitorName = getText(etVisitorName);
        String visitorDesignation = getText(etVisitorDesignation);
        String visitorOrganisation = getText(etVisitorOrganisation);
        String visitorGender = getSelectedGender();
        String visitorAddress = getText(etVisitorAddress);
        String visitorMobile = getText(etVisitorMobile);
        String visitorEmail = getText(etVisitorEmail);
        String purpose = getText(etPurpose);

        String requesteeName = getText(etRequesteeName);
        String requesteeDesignation = getText(etRequesteeDesignation);
        String requesteeDepartment = getText(etRequesteeDepartment);
        String requesteeMobile = getText(etRequesteeMobile);

        String logisticsName = getText(etLogisticsName);
        String logisticsDesignation = getText(etLogisticsDesignation);
        String logisticsMobile = getText(etLogisticsMobile);

        Integer roomId = getSelectedRoomId();

        String arrivalAt = apiDateTimeFmt.format(arrivalCal.getTime());
        String departureAt = apiDateTimeFmt.format(departureCal.getTime());

        if (!validateInputs(
                visitorName,
                visitorDesignation,
                visitorOrganisation,
                visitorGender,
                visitorAddress,
                visitorMobile,
                visitorEmail,
                arrivalAt,
                departureAt,
                purpose,
                requesteeName,
                requesteeDesignation,
                requesteeDepartment,
                requesteeMobile,
                logisticsName,
                logisticsDesignation,
                logisticsMobile,
                roomId
        )) {
            return;
        }

        try {
            EncryptedBookingPayload payload = new EncryptedBookingPayload(
                    visitorName,
                    visitorDesignation,
                    visitorOrganisation,
                    visitorGender,
                    visitorAddress,
                    visitorMobile,
                    visitorEmail,
                    purpose
            );

            String payloadJson = gson.toJson(payload);

            KeystoreBackedCryptoSessionManager cryptoSessionManager =
                    KeystoreBackedCryptoSessionManager.getInstance(getApplicationContext());

            SecretKey dek = cryptoSessionManager.getDek();
            if (dek == null) {
                showError("Encryption key not available. Please unlock again.");
                return;
            }

            CryptoManager cryptoManager = new CryptoManager();
            CryptoManager.EncryptionResult encryptionResult =
                    cryptoManager.encryptPayload(payloadJson, dek);

            BookingCreateRequest request = new BookingCreateRequest(
                    roomId,
                    arrivalAt,
                    departureAt,
                    encryptionResult.getCiphertextBase64(),
                    encryptionResult.getNonceBase64(),
                    1,
                    requesteeName,
                    requesteeDesignation,
                    requesteeDepartment,
                    requesteeMobile,
                    logisticsName,
                    logisticsDesignation,
                    logisticsMobile
            );

            setLoading(true);

            bookingRepository.createBooking(request).enqueue(new Callback<ApiResponse<BookingActionData>>() {
                @Override
                public void onResponse(
                        @NonNull Call<ApiResponse<BookingActionData>> call,
                        @NonNull Response<ApiResponse<BookingActionData>> response
                ) {
                    setLoading(false);

                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<BookingActionData> apiResponse = response.body();

                        if (!apiResponse.isSuccess()) {
                            showError(apiResponse.getFirstErrorMessage());
                            return;
                        }

                        Intent resultIntent = new Intent();
                        resultIntent.putExtra(EXTRA_BOOKING_CREATED, true);
                        setResult(RESULT_OK, resultIntent);

                        showMessage(apiResponse.getMessage());
                        finish();
                        return;
                    }

                    showError(extractErrorMessage(response));
                }

                @Override
                public void onFailure(
                        @NonNull Call<ApiResponse<BookingActionData>> call,
                        @NonNull Throwable t
                ) {
                    setLoading(false);
                    showError("Please check your internet connection.");
                }
            });

        } catch (Exception e) {
            showError("Encryption failed.");
        }
    }

    private boolean validateInputs(
            String visitorName,
            String visitorDesignation,
            String visitorOrganisation,
            String visitorGender,
            String visitorAddress,
            String visitorMobile,
            String visitorEmail,
            String arrivalAt,
            String departureAt,
            String purpose,
            String requesteeName,
            String requesteeDesignation,
            String requesteeDepartment,
            String requesteeMobile,
            String logisticsName,
            String logisticsDesignation,
            String logisticsMobile,
            Integer roomId
    ) {
        if (TextUtils.isEmpty(visitorName)
                || TextUtils.isEmpty(visitorDesignation)
                || TextUtils.isEmpty(visitorOrganisation)
                || TextUtils.isEmpty(visitorGender)
                || TextUtils.isEmpty(visitorAddress)
                || TextUtils.isEmpty(visitorMobile)
                || TextUtils.isEmpty(visitorEmail)
                || TextUtils.isEmpty(arrivalAt)
                || TextUtils.isEmpty(departureAt)
                || TextUtils.isEmpty(purpose)
                || TextUtils.isEmpty(requesteeName)
                || TextUtils.isEmpty(requesteeDesignation)
                || TextUtils.isEmpty(requesteeDepartment)
                || TextUtils.isEmpty(requesteeMobile)
                || TextUtils.isEmpty(logisticsName)
                || TextUtils.isEmpty(logisticsDesignation)
                || TextUtils.isEmpty(logisticsMobile)) {
            showError("Please fill all fields.");
            return false;
        }

        if (roomId == null) {
            showError("Please select a room.");
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(visitorEmail).matches()) {
            showError("Enter a valid visitor email.");
            return false;
        }

        if (!visitorMobile.matches("\\d{10}")) {
            showError("Visitor mobile must be 10 digits.");
            return false;
        }

        if (!requesteeMobile.matches("\\d{10}")) {
            showError("Requestee mobile must be 10 digits.");
            return false;
        }

        if (!logisticsMobile.matches("\\d{10}")) {
            showError("Logistics mobile must be 10 digits.");
            return false;
        }

        if (!departureCal.getTime().after(arrivalCal.getTime())) {
            showError("Departure date/time must be after arrival date/time.");
            return false;
        }

        return true;
    }

    private void setLoading(boolean loading) {
        btnCreateBooking.setEnabled(!loading);
        btnCreateBooking.setText(loading ? "Please wait..." : "Create Booking");
    }

    private void showError(String message) {
        tvMessage.setVisibility(View.VISIBLE);
        tvMessage.setText(message);
    }

    private void showMessage(String message) {
        tvMessage.setVisibility(View.VISIBLE);
        tvMessage.setText(message);
    }

    private String getText(EditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private <T> String extractErrorMessage(Response<ApiResponse<T>> response) {
        try {
            if (response.errorBody() != null) {
                String errorJson = response.errorBody().string();
                ApiResponse<?> errorResponse = gson.fromJson(errorJson, ApiResponse.class);

                if (errorResponse != null) {
                    String firstError = errorResponse.getFirstErrorMessage();
                    if (firstError != null && !firstError.trim().isEmpty()) {
                        return firstError;
                    }

                    if (errorResponse.getMessage() != null && !errorResponse.getMessage().trim().isEmpty()) {
                        return errorResponse.getMessage();
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return "Booking creation failed.";
    }

    private final Random random = new Random();

    private void getRandomBookingData() {

        String[] names = {
                "Amit Sharma","Neha Verma","Rohit Singh","Priya Patel","Ankit Jain",
                "Suresh Yadav","Pooja Gupta","Rahul Mehta","Sneha Reddy","Vikas Kumar",
                "Deepak Mishra","Anjali Singh","Karan Malhotra","Ritika Kapoor","Manish Tiwari",
                "Nikhil Agarwal","Swati Saxena","Aditya Joshi","Meena Iyer","Rajesh Khanna",
                "Arjun Nair","Kavita Desai","Tarun Bansal","Divya Choudhary","Harsh Vardhan",
                "Ramesh Pawar","Komal Shah","Varun Arora","Ishita Ghosh","Yash Thakur"
        };

        String[] designations = {
                "Engineer","Professor","Manager","Analyst","Researcher",
                "Consultant","Director","Coordinator","Developer","Architect",
                "Scientist","Assistant Professor","HR Manager","Team Lead","Intern",
                "Project Manager","Data Engineer","Security Analyst","DevOps Engineer","QA Engineer",
                "Business Analyst","Software Engineer","Technical Lead","System Admin","Support Engineer",
                "Network Engineer","AI Engineer","Cloud Engineer","Product Manager","Operations Manager"
        };

        String[] organisations = {
                "IIT Bhilai","Google","Microsoft","Amazon","Infosys",
                "TCS","Wipro","DRDO","ISRO","Accenture",
                "Capgemini","HCL","Cognizant","Flipkart","Zomato",
                "Swiggy","Paytm","PhonePe","Reliance","Adani Group",
                "L&T","Tech Mahindra","Dell","HP","IBM",
                "Oracle","SAP","Nvidia","Intel","Qualcomm"
        };

        String[] addresses = {
                "Raipur","Delhi","Mumbai","Bangalore","Hyderabad",
                "Pune","Chennai","Kolkata","Ahmedabad","Jaipur",
                "Lucknow","Bhopal","Indore","Patna","Nagpur",
                "Chandigarh","Surat","Noida","Gurgaon","Kanpur",
                "Ranchi","Dehradun","Shimla","Goa","Trivandrum",
                "Coimbatore","Mysore","Vizag","Jodhpur","Udaipur"
        };

        String[] purposes = {
                "Meeting","Research","Interview","Project Discussion","Audit",
                "Seminar","Workshop","Training","Conference","Inspection",
                "Client Visit","Technical Review","Demo Session","Collaboration","Consultation",
                "Presentation","Evaluation","Recruitment","Documentation","Field Visit",
                "Testing","Deployment","Maintenance","Support","Planning",
                "Strategy Meeting","Budget Discussion","Compliance Check","Review Meeting","Site Visit"
        };

        String[] departments = {
                "CSE","ECE","ME","Civil","Admin",
                "HR","Finance","IT","Operations","Marketing",
                "Sales","Legal","Security","R&D","Production",
                "Logistics","Procurement","Quality","Design","Support",
                "Analytics","Cloud","AI/ML","Networking","Embedded",
                "Automation","Testing","DevOps","Infrastructure","Consulting"
        };

        int index = random.nextInt(names.length);

        // Visitor Info
        etVisitorName.setText(names[index]);
        etVisitorDesignation.setText(designations[random.nextInt(designations.length)]);
        etVisitorOrganisation.setText(organisations[random.nextInt(organisations.length)]);
        spinnerGender.setSelection(random.nextInt(spinnerGender.getCount()));
        etVisitorAddress.setText(addresses[random.nextInt(addresses.length)]);
        etVisitorMobile.setText(generateRandomMobile());
        etVisitorEmail.setText(generateRandomEmail(names[index]));

        // Purpose
        etPurpose.setText(purposes[random.nextInt(purposes.length)]);

        // Requestee (you can also randomize if needed)
        etRequesteeName.setText("Rishabh");
        etRequesteeDesignation.setText(designations[random.nextInt(designations.length)]);
        etRequesteeDepartment.setText(departments[random.nextInt(departments.length)]);
        etRequesteeMobile.setText(generateRandomMobile());

        // Logistics
        etLogisticsName.setText("Logistics " + (random.nextInt(100) + 1));
        etLogisticsDesignation.setText(designations[random.nextInt(designations.length)]);
        etLogisticsMobile.setText(generateRandomMobile());

        // Room selection
        if (spinnerRoom.getCount() > 1) {
            spinnerRoom.setSelection(random.nextInt(spinnerRoom.getCount()));
        }

        showMessage("Random booking data generated 🎯");
    }
    private String generateRandomMobile() {
        return "9" + (100000000 + random.nextInt(900000000));
    }

    private String generateRandomEmail(String name) {
        String cleanName = name.toLowerCase().replace(" ", ".");
        return cleanName + random.nextInt(1000) + "@test.com";
    }
}