package com.example.roombooking.booking;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

import javax.crypto.SecretKey;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingDetailActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_DATA = "booking_data";

    private static final String EXTRA_UPDATED_BOOKING_ID = "updated_booking_id";
    private static final String EXTRA_UPDATED_STATUS = "updated_status";
    private static final String EXTRA_ARRIVAL_AT = "arrival_at";
    private static final String EXTRA_DEPARTURE_AT = "departure_at";

    private static final String MASK = "****";

    private TextView tvBookingId, tvRoomName, tvCreatedBy;
    private TextView tvVisitorDetails, tvVisitDetails, tvRequesteeDetails, tvLogisticsDetails, tvStatusDetails;
    private Button btnCancelBooking;
    private Button btnEditBooking;

    private BookingItem bookingItem;
    private final Gson gson = new Gson();

    private final ActivityResultLauncher<Intent> editBookingLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK
                        && result.getData() != null
                        && bookingItem != null) {

                    Intent data = result.getData();

                    String updatedArrivalAt = data.getStringExtra(EXTRA_ARRIVAL_AT);
                    String updatedDepartureAt = data.getStringExtra(EXTRA_DEPARTURE_AT);

                    if (updatedArrivalAt != null) {
                        bookingItem.setArrivalAt(updatedArrivalAt);
                    }

                    if (updatedDepartureAt != null) {
                        bookingItem.setDepartureAt(updatedDepartureAt);
                    }

                    renderBookingDetails();

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(EXTRA_UPDATED_BOOKING_ID, bookingItem.getId());
                    resultIntent.putExtra(EXTRA_UPDATED_STATUS, bookingItem.getStatus());
                    resultIntent.putExtra(EXTRA_ARRIVAL_AT, bookingItem.getArrivalAt());
                    resultIntent.putExtra(EXTRA_DEPARTURE_AT, bookingItem.getDepartureAt());
                    setResult(RESULT_OK, resultIntent);

                    Toast.makeText(this, "Booking updated successfully", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_detail);

        tvBookingId = findViewById(R.id.tvBookingId);
        tvRoomName = findViewById(R.id.tvRoomName);
        tvCreatedBy = findViewById(R.id.tvCreatedBy);
        tvVisitorDetails = findViewById(R.id.tvVisitorDetails);
        tvVisitDetails = findViewById(R.id.tvVisitDetails);
        tvRequesteeDetails = findViewById(R.id.tvRequesteeDetails);
        tvLogisticsDetails = findViewById(R.id.tvLogisticsDetails);
        tvStatusDetails = findViewById(R.id.tvStatusDetails);
        
        btnCancelBooking = findViewById(R.id.btnCancelBooking);
        btnEditBooking = findViewById(R.id.btnEditBooking);
        
        ImageView ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> finish());

        bookingItem = getBookingFromIntent();

        if (bookingItem != null) {
            renderBookingDetails();
            updateButtonState();

            btnCancelBooking.setOnClickListener(v -> showCancelDialog());

            btnEditBooking.setOnClickListener(v -> {
                Intent intent = new Intent(BookingDetailActivity.this, EditBookingActivity.class);
                intent.putExtra(EXTRA_BOOKING_DATA, bookingItem);
                editBookingLauncher.launch(intent);
            });
        } else {
            Toast.makeText(this, "No booking details found.", Toast.LENGTH_SHORT).show();
            btnCancelBooking.setEnabled(false);
            btnEditBooking.setEnabled(false);
        }
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

    private void renderBookingDetails() {
        String visitorName = MASK;
        String visitorDesignation = MASK;
        String visitorOrganisation = MASK;
        String visitorGender = MASK;
        String visitorAddress = MASK;
        String visitorMobile = MASK;
        String visitorEmail = MASK;
        String purpose = MASK;

        try {
            if (bookingItem.canDecrypt() && bookingItem.hasEncryptedPayload()) {
                KeystoreBackedCryptoSessionManager sessionManager =
                        KeystoreBackedCryptoSessionManager.getInstance(getApplicationContext());

                SecretKey dek = sessionManager.getDek();

                if (dek != null) {
                    CryptoManager cryptoManager = new CryptoManager();

                    String decryptedJson = cryptoManager.decryptPayload(
                            bookingItem.getEncryptedPayload(),
                            bookingItem.getPayloadNonce(),
                            dek
                    );

                    EncryptedBookingPayload payload =
                            EncryptedBookingPayload.fromJson(decryptedJson, gson);

                    if (payload != null) {
                        visitorName = safeOrMask(payload.getVisitorName());
                        visitorDesignation = safeOrMask(payload.getVisitorDesignation());
                        visitorOrganisation = safeOrMask(payload.getVisitorOrganisation());
                        visitorGender = safeOrMask(payload.getVisitorGender());
                        visitorAddress = safeOrMask(payload.getVisitorAddress());
                        visitorMobile = safeOrMask(payload.getVisitorMobile());
                        visitorEmail = safeOrMask(payload.getVisitorEmail());
                        purpose = safeOrMask(payload.getPurposeOfVisit());
                    }
                } else if (bookingItem.canDecrypt()) {
                    showToast("Please unlock to view details");
                }
            }
        } catch (Exception e) {
            showToast("Failed to decrypt booking data");
        }

        tvBookingId.setText("Booking ID: " + bookingItem.getId());
        tvRoomName.setText("Room Name: " + safe(bookingItem.getRoomName()));
        tvCreatedBy.setText("Created By: " + safe(bookingItem.getCreatedByUsername()));

        String visitorStr = "•  Visitor Name             :  " + visitorName + "\n" +
                            "•  Visitor Designation      :  " + visitorDesignation + "\n" +
                            "•  Visitor Organisation     :  " + visitorOrganisation + "\n" +
                            "•  Visitor Gender           :  " + visitorGender + "\n" +
                            "•  Visitor Address          :  " + visitorAddress + "\n" +
                            "•  Visitor Mobile           :  " + visitorMobile + "\n" +
                            "•  Visitor Email            :  " + visitorEmail;
        tvVisitorDetails.setText(visitorStr);

        String visitStr = "•  Arrival                  :  " + safe(bookingItem.getArrivalAt()) + "\n" +
                          "•  Departure                :  " + safe(bookingItem.getDepartureAt()) + "\n" +
                          "•  Purpose                  :  " + purpose;
        tvVisitDetails.setText(visitStr);

        String reqStr = "•  Requestee Name           :  " + safe(bookingItem.getRequesteeName()) + "\n" +
                        "•  Requestee Designation    :  " + safe(bookingItem.getRequesteeDesignation()) + "\n" +
                        "•  Requestee Department     :  " + safe(bookingItem.getRequesteeDepartment()) + "\n" +
                        "•  Requestee Mobile         :  " + safe(bookingItem.getRequesteeMobile());
        tvRequesteeDetails.setText(reqStr);

        String logStr = "•  Logistics Name           :  " + safe(bookingItem.getLogisticsName()) + "\n" +
                        "•  Logistics Designation    :  " + safe(bookingItem.getLogisticsDesignation()) + "\n" +
                        "•  Logistics Mobile         :  " + safe(bookingItem.getLogisticsMobile());
        tvLogisticsDetails.setText(logStr);

        String statusStr = "•  Status                   :  " + safe(bookingItem.getStatus());
        tvStatusDetails.setText(statusStr);
    }

    private void updateButtonState() {
        boolean alreadyCancelled = isCancelled();

        btnCancelBooking.setEnabled(!alreadyCancelled);
        btnCancelBooking.setText(alreadyCancelled ? "Already Cancelled" : "Cancel Booking");

        btnEditBooking.setEnabled(!alreadyCancelled);
        btnEditBooking.setText(alreadyCancelled ? "Edit Disabled" : "Edit Booking");
    }

    private void showCancelDialog() {
        if (bookingItem == null) {
            return;
        }

        if (isCancelled()) {
            showToast("Booking is already cancelled");
            return;
        }

        EditText input = new EditText(this);
        input.setHint("Enter cancellation reason (optional)");

        new AlertDialog.Builder(this)
                .setTitle("Cancel Booking")
                .setMessage("Do you want to cancel this booking?")
                .setView(input)
                .setPositiveButton("Cancel Booking", (dialog, which) -> {
                    String reason = input.getText().toString().trim();
                    cancelBooking(reason);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void cancelBooking(String reason) {
        btnCancelBooking.setEnabled(false);
        btnCancelBooking.setText("Cancelling...");

        BookingCancelRequest request = new BookingCancelRequest(reason);

        RetrofitClient.getApiService(this)
                .cancelBooking(bookingItem.getId(), request)
                .enqueue(new Callback<ApiResponse<BookingActionData>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<ApiResponse<BookingActionData>> call,
                            @NonNull Response<ApiResponse<BookingActionData>> response
                    ) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<BookingActionData> apiResponse = response.body();

                            if (!apiResponse.isSuccess() || apiResponse.getData() == null) {
                                resetCancelButton();
                                showToast(apiResponse.getFirstErrorMessage());
                                return;
                            }

                            BookingActionData data = apiResponse.getData();
                            bookingItem.setStatus(data.getStatus());

                            renderBookingDetails();
                            updateButtonState();

                            Intent resultIntent = new Intent();
                            resultIntent.putExtra(EXTRA_UPDATED_BOOKING_ID, bookingItem.getId());
                            resultIntent.putExtra(EXTRA_UPDATED_STATUS, bookingItem.getStatus());
                            resultIntent.putExtra(EXTRA_ARRIVAL_AT, bookingItem.getArrivalAt());
                            resultIntent.putExtra(EXTRA_DEPARTURE_AT, bookingItem.getDepartureAt());
                            setResult(RESULT_OK, resultIntent);

                            showToast(apiResponse.getMessage());
                            return;
                        }

                        resetCancelButton();
                        showToast(extractErrorMessage(response));
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<ApiResponse<BookingActionData>> call,
                            @NonNull Throwable t
                    ) {
                        resetCancelButton();
                        showToast(getNetworkErrorMessage(t));
                    }
                });
    }

    private boolean isCancelled() {
        return bookingItem != null && "cancelled".equalsIgnoreCase(bookingItem.getStatus());
    }

    private void resetCancelButton() {
        btnCancelBooking.setEnabled(true);
        btnCancelBooking.setText("Cancel Booking");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safeOrMask(String value) {
        if (value == null || value.trim().isEmpty()) {
            return MASK;
        }
        return value;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private String getNetworkErrorMessage(Throwable throwable) {
        return "Please check your internet connection.";
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

        return "Request failed. Code: " + response.code();
    }
}