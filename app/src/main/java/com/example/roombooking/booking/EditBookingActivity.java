package com.example.roombooking.booking;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.roombooking.R;
import com.example.roombooking.api.RetrofitClient;
import com.example.roombooking.model.booking.BookingActionData;
import com.example.roombooking.model.booking.BookingItem;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.security.CryptoManager;
import com.example.roombooking.security.EncryptedBookingPayload;
import com.example.roombooking.security.KeystoreBackedCryptoSessionManager;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import javax.crypto.SecretKey;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditBookingActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_DATA = "booking_data";

    private static final String EXTRA_UPDATED_BOOKING_ID = "updated_booking_id";
    private static final String EXTRA_UPDATED_STATUS = "updated_status";
    private static final String EXTRA_ARRIVAL_AT = "arrival_at";
    private static final String EXTRA_DEPARTURE_AT = "departure_at";

    private EditText etVisitorName;
    private EditText etVisitorMobile;
    private EditText etPurpose;
    private EditText etArrivalAt;
    private EditText etDepartureAt;
    private Button btnSaveBooking;

    private BookingItem bookingItem;
    private final Gson gson = new Gson();

    private final Calendar arrivalCal = Calendar.getInstance();
    private final Calendar departureCal = Calendar.getInstance();

    private final SimpleDateFormat displayFmt =
            new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    private final SimpleDateFormat apiDateTimeFmt =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());

    private String originalVisitorDesignation = "";
    private String originalVisitorOrganisation = "";
    private String originalVisitorGender = "";
    private String originalVisitorAddress = "";
    private String originalVisitorEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_booking);

        bindViews();
        bookingItem = getBookingFromIntent();

        if (bookingItem == null) {
            showToast("No booking details found.");
            finish();
            return;
        }

        bindBookingData();

        etArrivalAt.setOnClickListener(v -> pickDateTime(arrivalCal, etArrivalAt));
        etDepartureAt.setOnClickListener(v -> pickDateTime(departureCal, etDepartureAt));
        btnSaveBooking.setOnClickListener(v -> saveBooking());
    }

    private void bindViews() {
        etVisitorName = findViewById(R.id.etVisitorName);
        etVisitorMobile = findViewById(R.id.etVisitorMobile);
        etPurpose = findViewById(R.id.etPurpose);
        etArrivalAt = findViewById(R.id.etArrivalAt);
        etDepartureAt = findViewById(R.id.etDepartureAt);
        btnSaveBooking = findViewById(R.id.btnSaveBooking);
    }

    private BookingItem getBookingFromIntent() {
        Intent intent = getIntent();
        if (intent == null) {
            return null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return intent.getParcelableExtra(EXTRA_BOOKING_DATA, BookingItem.class);
        } else {
            return intent.getParcelableExtra(EXTRA_BOOKING_DATA);
        }
    }

    private void bindBookingData() {
        if (!bookingItem.canDecrypt() || !bookingItem.hasEncryptedPayload()) {
            showToast("You are not allowed to edit this booking.");
            finish();
            return;
        }

        try {
            KeystoreBackedCryptoSessionManager sessionManager =
                    KeystoreBackedCryptoSessionManager.getInstance(getApplicationContext());

            SecretKey dek = sessionManager.getDek();
            if (dek == null) {
                showToast("Encryption key not available. Please unlock again.");
                finish();
                return;
            }

            CryptoManager cryptoManager = new CryptoManager();
            String decryptedJson = cryptoManager.decryptPayload(
                    bookingItem.getEncryptedPayload(),
                    bookingItem.getPayloadNonce(),
                    dek
            );

            EncryptedBookingPayload payload =
                    EncryptedBookingPayload.fromJson(decryptedJson, gson);

            if (payload == null) {
                showToast("Invalid encrypted booking data.");
                finish();
                return;
            }

            originalVisitorDesignation = safe(payload.getVisitorDesignation());
            originalVisitorOrganisation = safe(payload.getVisitorOrganisation());
            originalVisitorGender = safe(payload.getVisitorGender());
            originalVisitorAddress = safe(payload.getVisitorAddress());
            originalVisitorEmail = safe(payload.getVisitorEmail());

            etVisitorName.setText(safe(payload.getVisitorName()));
            etVisitorMobile.setText(safe(payload.getVisitorMobile()));
            etPurpose.setText(safe(payload.getPurposeOfVisit()));

            parseCalendarFromApiString(bookingItem.getArrivalAt(), arrivalCal);
            parseCalendarFromApiString(bookingItem.getDepartureAt(), departureCal);

            etArrivalAt.setText(displayFmt.format(arrivalCal.getTime()));
            etDepartureAt.setText(displayFmt.format(departureCal.getTime()));

        } catch (Exception e) {
            showToast("Failed to decrypt booking data.");
            finish();
        }
    }

    private void saveBooking() {
        String visitorName = getText(etVisitorName);
        String visitorMobile = getText(etVisitorMobile);
        String purpose = getText(etPurpose);
        String arrivalAt = apiDateTimeFmt.format(arrivalCal.getTime());
        String departureAt = apiDateTimeFmt.format(departureCal.getTime());

        if (TextUtils.isEmpty(visitorName)
                || TextUtils.isEmpty(visitorMobile)
                || TextUtils.isEmpty(purpose)) {
            showToast("Please fill all fields.");
            return;
        }

        if (!visitorMobile.matches("\\d{10}")) {
            showToast("Visitor mobile must be 10 digits.");
            return;
        }

        if (!departureCal.getTime().after(arrivalCal.getTime())) {
            showToast("Departure date/time must be after arrival date/time.");
            return;
        }

        try {
            KeystoreBackedCryptoSessionManager sessionManager =
                    KeystoreBackedCryptoSessionManager.getInstance(getApplicationContext());

            SecretKey dek = sessionManager.getDek();
            if (dek == null) {
                showToast("Encryption key not available. Please unlock again.");
                return;
            }

            EncryptedBookingPayload payload = new EncryptedBookingPayload(
                    visitorName,
                    originalVisitorDesignation,
                    originalVisitorOrganisation,
                    originalVisitorGender,
                    originalVisitorAddress,
                    visitorMobile,
                    originalVisitorEmail,
                    purpose
            );

            CryptoManager cryptoManager = new CryptoManager();
            CryptoManager.EncryptionResult encryptionResult =
                    cryptoManager.encryptPayload(payload.toJson(gson), dek);

            btnSaveBooking.setEnabled(false);
            btnSaveBooking.setText("Saving...");

            BookingUpdateRequest request = new BookingUpdateRequest(
                    bookingItem.getRoom(),
                    arrivalAt,
                    departureAt,
                    encryptionResult.getCiphertextBase64(),
                    encryptionResult.getNonceBase64(),
                    1,
                    bookingItem.getRequesteeName(),
                    bookingItem.getRequesteeDesignation(),
                    bookingItem.getRequesteeDepartment(),
                    bookingItem.getRequesteeMobile(),
                    bookingItem.getLogisticsName(),
                    bookingItem.getLogisticsDesignation(),
                    bookingItem.getLogisticsMobile()
            );

            RetrofitClient.getApiService(this)
                    .updateBooking(bookingItem.getId(), request)
                    .enqueue(new Callback<ApiResponse<BookingActionData>>() {
                        @Override
                        public void onResponse(
                                @NonNull Call<ApiResponse<BookingActionData>> call,
                                @NonNull Response<ApiResponse<BookingActionData>> response
                        ) {
                            btnSaveBooking.setEnabled(true);
                            btnSaveBooking.setText("Save Booking");

                            if (response.isSuccessful() && response.body() != null) {
                                ApiResponse<BookingActionData> apiResponse = response.body();

                                if (!apiResponse.isSuccess()) {
                                    showToast(apiResponse.getFirstErrorMessage());
                                    return;
                                }

                                bookingItem.setArrivalAt(arrivalAt);
                                bookingItem.setDepartureAt(departureAt);
                                bookingItem.setEncryptedPayload(encryptionResult.getCiphertextBase64());
                                bookingItem.setPayloadNonce(encryptionResult.getNonceBase64());
                                bookingItem.setPayloadVersion(1);

                                Intent resultIntent = new Intent();
                                resultIntent.putExtra(EXTRA_UPDATED_BOOKING_ID, bookingItem.getId());
                                resultIntent.putExtra(EXTRA_UPDATED_STATUS, bookingItem.getStatus());
                                resultIntent.putExtra(EXTRA_ARRIVAL_AT, arrivalAt);
                                resultIntent.putExtra(EXTRA_DEPARTURE_AT, departureAt);
                                setResult(RESULT_OK, resultIntent);
                                finish();
                                return;
                            }

                            showToast(extractErrorMessage(response));
                        }

                        @Override
                        public void onFailure(
                                @NonNull Call<ApiResponse<BookingActionData>> call,
                                @NonNull Throwable t
                        ) {
                            btnSaveBooking.setEnabled(true);
                            btnSaveBooking.setText("Save Booking");
                            showToast("Please check your internet connection.");
                        }
                    });

        } catch (Exception e) {
            btnSaveBooking.setEnabled(true);
            btnSaveBooking.setText("Save Booking");
            showToast("Encryption failed.");
        }
    }

    private void pickDateTime(Calendar target, EditText targetEditText) {
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
                targetEditText.setText(displayFmt.format(target.getTime()));
            }, hour, minute, true);

            timePickerDialog.show();
        }, year, month, day);

        datePickerDialog.show();
    }

    private void parseCalendarFromApiString(String value, Calendar cal) {
        try {
            if (value != null && !value.trim().isEmpty()) {
                cal.setTime(apiDateTimeFmt.parse(value));
            }
        } catch (Exception ignored) {
        }
    }

    private String getText(EditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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

        return "Update failed.";
    }
}