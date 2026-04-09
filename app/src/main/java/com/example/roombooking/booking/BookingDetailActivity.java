package com.example.roombooking.booking;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.example.roombooking.model.booking.BookingActionData;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.roombooking.R;
import com.example.roombooking.api.RetrofitClient;
import com.example.roombooking.model.booking.BookingItem;
import com.example.roombooking.model.common.ApiResponse;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingDetailActivity extends AppCompatActivity {

    private static final String EXTRA_BOOKING_DATA = "booking_data";
    private static final String EXTRA_UPDATED_BOOKING_ID = "updated_booking_id";
    private static final String EXTRA_UPDATED_STATUS = "updated_status";
    private static final String EXTRA_VISITOR_NAME = "visitor_name";
    private static final String EXTRA_VISITOR_MOBILE = "visitor_mobile";
    private static final String EXTRA_PURPOSE_OF_VISIT = "purpose_of_visit";
    private static final String EXTRA_ARRIVAL_DATE = "arrival_date";
    private static final String EXTRA_ARRIVAL_TIME = "arrival_time";
    private static final String EXTRA_DEPARTURE_DATE = "departure_date";
    private static final String EXTRA_DEPARTURE_TIME = "departure_time";

    private TextView tvDetails;
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

                    bookingItem.setVisitorName(data.getStringExtra(EXTRA_VISITOR_NAME));
                    bookingItem.setVisitorMobile(data.getStringExtra(EXTRA_VISITOR_MOBILE));
                    bookingItem.setPurposeOfVisit(data.getStringExtra(EXTRA_PURPOSE_OF_VISIT));
                    bookingItem.setArrivalDate(data.getStringExtra(EXTRA_ARRIVAL_DATE));
                    bookingItem.setArrivalTime(data.getStringExtra(EXTRA_ARRIVAL_TIME));
                    bookingItem.setDepartureDate(data.getStringExtra(EXTRA_DEPARTURE_DATE));
                    bookingItem.setDepartureTime(data.getStringExtra(EXTRA_DEPARTURE_TIME));

                    renderBookingDetails();

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(EXTRA_UPDATED_BOOKING_ID, bookingItem.getId());
                    resultIntent.putExtra(EXTRA_UPDATED_STATUS, bookingItem.getStatus());
                    resultIntent.putExtra(EXTRA_VISITOR_NAME, bookingItem.getVisitorName());
                    resultIntent.putExtra(EXTRA_VISITOR_MOBILE, bookingItem.getVisitorMobile());
                    resultIntent.putExtra(EXTRA_PURPOSE_OF_VISIT, bookingItem.getPurposeOfVisit());
                    resultIntent.putExtra(EXTRA_ARRIVAL_DATE, bookingItem.getArrivalDate());
                    resultIntent.putExtra(EXTRA_ARRIVAL_TIME, bookingItem.getArrivalTime());
                    resultIntent.putExtra(EXTRA_DEPARTURE_DATE, bookingItem.getDepartureDate());
                    resultIntent.putExtra(EXTRA_DEPARTURE_TIME, bookingItem.getDepartureTime());
                    setResult(RESULT_OK, resultIntent);

                    Toast.makeText(this, "Booking updated successfully", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_detail);

        tvDetails = findViewById(R.id.tvDetails);
        btnCancelBooking = findViewById(R.id.btnCancelBooking);
        btnEditBooking = findViewById(R.id.btnEditBooking);

        bookingItem = (BookingItem) getIntent().getSerializableExtra(EXTRA_BOOKING_DATA);

        if (bookingItem != null) {
            renderBookingDetails();
            updateButtonState();


            btnCancelBooking.setOnClickListener(v -> showCancelDialog());
        } else {
            tvDetails.setText("No booking details found.");
            btnCancelBooking.setEnabled(false);
            btnEditBooking.setEnabled(false);
        }
    }



    private void renderBookingDetails() {
        String details =
                "Booking ID: " + bookingItem.getId() + "\n\n" +
                        "Room Name: " + safe(bookingItem.getRoomName()) + "\n" +
                        "Created By: " + safe(bookingItem.getCreatedByUsername()) + "\n\n" +

                        "Visitor Name: " + safe(bookingItem.getVisitorName()) + "\n" +
                        "Visitor Designation: " + safe(bookingItem.getVisitorDesignation()) + "\n" +
                        "Visitor Organisation: " + safe(bookingItem.getVisitorOrganisation()) + "\n" +
                        "Visitor Gender: " + safe(bookingItem.getVisitorGender()) + "\n" +
                        "Visitor Address: " + safe(bookingItem.getVisitorAddress()) + "\n" +
                        "Visitor Mobile: " + safe(bookingItem.getVisitorMobile()) + "\n" +
                        "Visitor Email: " + safe(bookingItem.getVisitorEmail()) + "\n\n" +

                        "Arrival Date: " + safe(bookingItem.getArrivalDate()) + "\n" +
                        "Arrival Time: " + safe(bookingItem.getArrivalTime()) + "\n" +
                        "Departure Date: " + safe(bookingItem.getDepartureDate()) + "\n" +
                        "Departure Time: " + safe(bookingItem.getDepartureTime()) + "\n\n" +

                        "Purpose: " + safe(bookingItem.getPurposeOfVisit()) + "\n\n" +

                        "Requestee Name: " + safe(bookingItem.getRequesteeName()) + "\n" +
                        "Requestee Designation: " + safe(bookingItem.getRequesteeDesignation()) + "\n" +
                        "Requestee Department: " + safe(bookingItem.getRequesteeDepartment()) + "\n" +
                        "Requestee Mobile: " + safe(bookingItem.getRequesteeMobile()) + "\n\n" +

                        "Logistics Name: " + safe(bookingItem.getLogisticsName()) + "\n" +
                        "Logistics Designation: " + safe(bookingItem.getLogisticsDesignation()) + "\n" +
                        "Logistics Mobile: " + safe(bookingItem.getLogisticsMobile()) + "\n\n" +

                        "Status: " + safe(bookingItem.getStatus());

        tvDetails.setText(details);
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