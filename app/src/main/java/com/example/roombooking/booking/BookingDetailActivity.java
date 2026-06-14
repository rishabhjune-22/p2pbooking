package com.example.roombooking.booking;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.roombooking.R;
import com.example.roombooking.model.booking.BookingActionData;
import com.example.roombooking.model.booking.BookingItem;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.utils.DateTimeUtils;
import com.example.roombooking.utils.EdgeToEdgeUtils;
import com.example.roombooking.utils.AppToolbarMenu;
import com.example.roombooking.utils.InternetErrorBanner;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingDetailActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_DATA = "booking_data";

    private static final String EXTRA_UPDATED_BOOKING_ID = "updated_booking_id";
    private static final String EXTRA_UPDATED_STATUS = "updated_status";
    private static final String EXTRA_ARRIVAL_AT = "arrival_at";
    private static final String EXTRA_DEPARTURE_AT = "departure_at";

    private static final String STATUS_CANCELLED = "cancelled";
    private static final String STATUS_EXPIRED = "expired";

    private ImageButton btnBack;

    private TextView tvBookingId;
    private TextView tvRoomName;

    private TextView tvVisitorDetails;
    private TextView tvVisitDetails;
    private TextView tvVisitorCategoryDetails;
    private TextView tvRoomChargesDetails;
    private TextView tvAttenderDetails;
    private TextView tvRequesteeDetails;
    private TextView tvLogisticsDetails;
    private TextView tvStatusDetails;

    private AppCompatButton btnCancelBooking;
    private AppCompatButton btnEditBooking;
    private SwipeRefreshLayout swipeRefreshBookingDetail;

    private BookingRepository bookingRepository;
    private BookingItem bookingItem;
    private Call<ApiResponse<BookingItem>> refreshBookingCall;

    private final Gson gson = new Gson();

    private final ActivityResultLauncher<Intent> editBookingLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            handleEditBookingResult(result.getData());
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_detail);

        EdgeToEdgeUtils.applySystemBarInsets(this, findViewById(R.id.rootView));

        initDependencies();
        initViews();
        AppToolbarMenu.setup(this, findViewById(R.id.appToolbar));
        setupListeners();

        bookingItem = getBookingFromIntent();

        if (bookingItem == null) {
            showToast("No booking details found.");
            disableActionButtons();
            return;
        }

        renderBookingDetails();
        updateButtonState();
    }

    private void initDependencies() {
        bookingRepository = new BookingRepository(getApplicationContext());
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);

        tvBookingId = findViewById(R.id.tvBookingId);
        tvRoomName = findViewById(R.id.tvRoomName);

        tvVisitorDetails = findViewById(R.id.tvVisitorDetails);
        tvVisitDetails = findViewById(R.id.tvVisitDetails);
        tvVisitorCategoryDetails = findViewById(R.id.tvVisitorCategoryDetails);
        tvRoomChargesDetails = findViewById(R.id.tvRoomChargesDetails);
        tvAttenderDetails = findViewById(R.id.tvAttenderDetails);
        tvRequesteeDetails = findViewById(R.id.tvRequesteeDetails);
        tvLogisticsDetails = findViewById(R.id.tvLogisticsDetails);
        tvStatusDetails = findViewById(R.id.tvStatusDetails);

        btnCancelBooking = findViewById(R.id.btnCancelBooking);
        btnEditBooking = findViewById(R.id.btnEditBooking);
        swipeRefreshBookingDetail = findViewById(R.id.swipeRefreshBookingDetail);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnCancelBooking.setOnClickListener(v -> showCancelDialog());

        btnEditBooking.setOnClickListener(v -> openEditBookingScreen());

        swipeRefreshBookingDetail.setColorSchemeResources(
                R.color.primary,
                R.color.success_green,
                R.color.error_red
        );
        swipeRefreshBookingDetail.setOnRefreshListener(this::refreshBookingDetails);
    }

    private void refreshBookingDetails() {
        if (bookingItem == null) {
            swipeRefreshBookingDetail.setRefreshing(false);
            showToast("No booking details found.");
            return;
        }

        cancelRefreshCall();
        Call<ApiResponse<BookingItem>> request = bookingRepository.getBooking(bookingItem.getId());
        refreshBookingCall = request;
        request.enqueue(new Callback<ApiResponse<BookingItem>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<BookingItem>> call,
                    @NonNull Response<ApiResponse<BookingItem>> response
            ) {
                if (!isCurrentRefreshCall(call)) {
                    return;
                }

                refreshBookingCall = null;
                swipeRefreshBookingDetail.setRefreshing(false);
                InternetErrorBanner.hide(BookingDetailActivity.this);

                if (!response.isSuccessful() || response.body() == null) {
                    showToast(extractErrorMessage(response));
                    return;
                }

                ApiResponse<BookingItem> apiResponse = response.body();

                if (!apiResponse.isSuccess() || apiResponse.getData() == null) {
                    showToast(apiResponse.getFirstErrorMessage());
                    return;
                }

                bookingItem = apiResponse.getData();
                renderBookingDetails();
                updateButtonState();
                sendUpdatedResult();
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<BookingItem>> call,
                    @NonNull Throwable t
            ) {
                if (!isCurrentRefreshCall(call)) {
                    return;
                }

                refreshBookingCall = null;
                swipeRefreshBookingDetail.setRefreshing(false);

                if (!call.isCanceled()) {
                    InternetErrorBanner.show(BookingDetailActivity.this);
                    showToast("Please check your internet connection.");
                }
            }
        });
    }

    private boolean isCurrentRefreshCall(Call<ApiResponse<BookingItem>> call) {
        return call == refreshBookingCall && !isFinishing() && !isDestroyed();
    }

    private void cancelRefreshCall() {
        if (refreshBookingCall != null && !refreshBookingCall.isCanceled()) {
            refreshBookingCall.cancel();
        }
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

    private void openEditBookingScreen() {
        if (bookingItem == null) {
            showToast("No booking details found.");
            return;
        }

        if (isInactiveBooking()) {
            showToast("This booking cannot be edited.");
            return;
        }

        Intent intent = new Intent(BookingDetailActivity.this, EditBookingActivity.class);
        intent.putExtra(EXTRA_BOOKING_DATA, bookingItem);
        editBookingLauncher.launch(intent);
    }

    private void handleEditBookingResult(Intent data) {
        if (bookingItem == null) {
            return;
        }

        String updatedArrivalAt = data.getStringExtra(EXTRA_ARRIVAL_AT);
        String updatedDepartureAt = data.getStringExtra(EXTRA_DEPARTURE_AT);
        String updatedStatus = data.getStringExtra(EXTRA_UPDATED_STATUS);

        if (updatedArrivalAt != null) {
            bookingItem.setArrivalAt(updatedArrivalAt);
        }

        if (updatedDepartureAt != null) {
            bookingItem.setDepartureAt(updatedDepartureAt);
        }

        if (updatedStatus != null) {
            bookingItem.setStatus(updatedStatus);
        }

        renderBookingDetails();
        updateButtonState();
        sendUpdatedResult();

        showToast("Booking updated successfully");
        swipeRefreshBookingDetail.setRefreshing(true);
        refreshBookingDetails();
    }

    private void renderBookingDetails() {
        if (bookingItem == null) {
            return;
        }

        tvBookingId.setText("Booking ID: " + bookingItem.getId());
        tvRoomName.setText("Room Name: " + safe(bookingItem.getRoomName()));

        tvVisitorDetails.setText(buildVisitorDetails());
        tvVisitDetails.setText(buildVisitDetails());
        tvVisitorCategoryDetails.setText(buildVisitorCategoryDetails());
        tvRoomChargesDetails.setText(buildRoomChargesDetails());
        tvAttenderDetails.setText(buildAttenderDetails());
        tvRequesteeDetails.setText(buildRequesteeDetails());
        tvLogisticsDetails.setText(buildLogisticsDetails());
        tvStatusDetails.setText("Status: " + getDisplayStatus());
    }
    private String buildRoomChargesDetails() {
        return "Room charges received: "
                + getChargeStatusWithAmount(
                bookingItem.getRoomChargesStatus(),
                bookingItem.getRoomChargesAmount()
        );
    }
    private String buildVisitorDetails() {
        return "Name: " + safe(bookingItem.getVisitorName()) + "\n"
                + "Designation: " + safe(bookingItem.getVisitorDesignation()) + "\n"
                + "Organisation: " + safe(bookingItem.getVisitorOrganisation()) + "\n"
                + "Gender: " + safe(bookingItem.getVisitorGender()) + "\n"
                + "Address: " + safe(bookingItem.getVisitorAddress()) + "\n"
                + "Mobile: " + safe(bookingItem.getVisitorMobile()) + "\n"
                + "Email: " + safe(bookingItem.getVisitorEmail());
    }

    private String buildVisitDetails() {
        return "Arrival: " + DateTimeUtils.formatUtcToLocal(bookingItem.getArrivalAt()) + "\n"
                + "Departure: " + DateTimeUtils.formatUtcToLocal(bookingItem.getDepartureAt()) + "\n"
                + "Purpose: " + safe(bookingItem.getPurposeOfVisit());
    }

    private String buildVisitorCategoryDetails() {
        return "Category: " + getVisitorCategoryText(bookingItem.getVisitorCategory());
    }

    private String buildAttenderDetails() {
        return "Required: " + (bookingItem.isAttenderRequired() ? "Yes" : "No") + "\n"
                + "Count per day: " + bookingItem.getAttenderCountPerDay() + "\n"
                + "Shifts: " + getAttenderShiftText() + "\n"
                + "Attender charges received: "
                + getChargeStatusWithAmount(
                bookingItem.getAttenderChargesStatus(),
                bookingItem.getAttenderChargesAmount()
        );
    }

    private String getChargeStatusWithAmount(String status, String amount) {
        String statusText = getChargeStatusText(status);

        if (!"yes".equalsIgnoreCase(status)) {
            return statusText;
        }

        return statusText + " (Amount: " + safe(amount) + ")";
    }

    private String getChargeStatusText(String status) {
        if ("yes".equalsIgnoreCase(status)) {
            return "Yes";
        }

        if ("waived_off".equalsIgnoreCase(status)) {
            return "Waived Off";
        }

        return "No";
    }

    private String buildRequesteeDetails() {
        return "Name: " + safe(bookingItem.getRequesteeName()) + "\n"
                + "Designation: " + safe(bookingItem.getRequesteeDesignation()) + "\n"
                + "Department: " + safe(bookingItem.getRequesteeDepartment()) + "\n"
                + "Mobile: " + safe(bookingItem.getRequesteeMobile());
    }

    private String buildLogisticsDetails() {
        return "Name: " + safe(bookingItem.getLogisticsName()) + "\n"
                + "Designation: " + safe(bookingItem.getLogisticsDesignation()) + "\n"
                + "Mobile: " + safe(bookingItem.getLogisticsMobile());
    }

    private String getVisitorCategoryText(String category) {
        if (category == null) {
            return "N/A";
        }

        switch (category) {
            case "institute_guest":
                return "Institute Guest";

            case "conference_workshop_guest":
                return "Conference / Workshop Guest";

            case "other_guest":
                return "Other Guest";

            default:
                return safe(category);
        }
    }

    private String getAttenderShiftText() {
        List<String> shifts = new ArrayList<>();

        if (bookingItem.isAttenderGeneralShift()) {
            shifts.add("General Shift (9 AM - 5 PM)");
        }

        if (bookingItem.isAttenderMorningShift()) {
            shifts.add("Morning Shift (6 AM - 2 PM)");
        }

        if (bookingItem.isAttenderDayShift()) {
            shifts.add("Day Shift (2 PM - 10 PM)");
        }

        if (bookingItem.isAttenderNightShift()) {
            shifts.add("Night Shift (10 PM - 6 AM)");
        }

        if (shifts.isEmpty()) {
            return "None";
        }

        return TextUtils.join(", ", shifts);
    }

    private void updateButtonState() {
        if (bookingItem == null) {
            disableActionButtons();
            return;
        }

        if (isCancelled()) {
            btnCancelBooking.setEnabled(false);
            btnCancelBooking.setText("Already Cancelled");

            btnEditBooking.setEnabled(false);
            btnEditBooking.setText("Edit Disabled");
            return;
        }

        if (isExpired()) {
            btnCancelBooking.setEnabled(false);
            btnCancelBooking.setText("Expired");

            btnEditBooking.setEnabled(false);
            btnEditBooking.setText("Edit Disabled");
            return;
        }

        btnCancelBooking.setEnabled(true);
        btnCancelBooking.setText("Cancel Booking");

        btnEditBooking.setEnabled(true);
        btnEditBooking.setText("Edit Booking");
    }

    private void disableActionButtons() {
        btnCancelBooking.setEnabled(false);
        btnEditBooking.setEnabled(false);
    }

    private void showCancelDialog() {
        if (bookingItem == null) {
            showToast("No booking details found.");
            return;
        }

        if (isCancelled()) {
            showToast("Booking is already cancelled.");
            return;
        }

        if (isExpired()) {
            showToast("Expired booking cannot be cancelled.");
            return;
        }

        EditText input = new EditText(this);
        input.setHint("Enter cancellation reason (optional)");

        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle("Cancel Booking")
                .setMessage("Do you want to cancel this booking?")
                .setView(input)
                .setPositiveButton("Cancel Booking", (dialog, which) -> {
                    String reason = input.getText() != null
                            ? input.getText().toString().trim()
                            : "";

                    cancelBooking(reason);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void cancelBooking(String reason) {
        if (bookingItem == null) {
            return;
        }

        setCancellingState(true);

        BookingCancelRequest request = new BookingCancelRequest(reason);

        bookingRepository.cancelBooking(
                bookingItem.getId(),
                request
        ).enqueue(new Callback<ApiResponse<BookingActionData>>() {

            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<BookingActionData>> call,
                    @NonNull Response<ApiResponse<BookingActionData>> response
            ) {
                InternetErrorBanner.hide(BookingDetailActivity.this);
                if (!response.isSuccessful() || response.body() == null) {
                    resetCancelButton();
                    showToast(extractErrorMessage(response));
                    return;
                }

                ApiResponse<BookingActionData> apiResponse = response.body();

                if (!apiResponse.isSuccess() || apiResponse.getData() == null) {
                    resetCancelButton();
                    showToast(apiResponse.getFirstErrorMessage());
                    return;
                }

                handleCancelSuccess(apiResponse);
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<BookingActionData>> call,
                    @NonNull Throwable t
            ) {
                resetCancelButton();
                InternetErrorBanner.show(BookingDetailActivity.this);
                showToast("Please check your internet connection.");
            }
        });
    }

    private void handleCancelSuccess(ApiResponse<BookingActionData> apiResponse) {
        BookingActionData data = apiResponse.getData();

        if (data != null && data.getStatus() != null) {
            bookingItem.setStatus(data.getStatus());
        } else {
            bookingItem.setStatus(STATUS_CANCELLED);
        }

        renderBookingDetails();
        updateButtonState();
        sendUpdatedResult();

        showToast(apiResponse.getSafeMessage());
    }

    private void setCancellingState(boolean cancelling) {
        btnCancelBooking.setEnabled(!cancelling);
        btnCancelBooking.setText(cancelling ? "Cancelling..." : "Cancel Booking");
    }

    private void resetCancelButton() {
        if (isInactiveBooking()) {
            updateButtonState();
            return;
        }

        setCancellingState(false);
    }

    private void sendUpdatedResult() {
        if (bookingItem == null) {
            return;
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_UPDATED_BOOKING_ID, bookingItem.getId());
        resultIntent.putExtra(EXTRA_UPDATED_STATUS, bookingItem.getStatus());
        resultIntent.putExtra(EXTRA_ARRIVAL_AT, bookingItem.getArrivalAt());
        resultIntent.putExtra(EXTRA_DEPARTURE_AT, bookingItem.getDepartureAt());

        setResult(RESULT_OK, resultIntent);
    }

    private boolean isInactiveBooking() {
        return isCancelled() || isExpired();
    }

    private boolean isCancelled() {
        return bookingItem != null
                && STATUS_CANCELLED.equalsIgnoreCase(bookingItem.getStatus());
    }

    private boolean isExpired() {
        return bookingItem != null
                && STATUS_EXPIRED.equalsIgnoreCase(bookingItem.getStatus());
    }

    private String getDisplayStatus() {
        if (bookingItem == null || bookingItem.getStatus() == null) {
            return "Active";
        }

        String status = bookingItem.getStatus().trim();

        if (STATUS_CANCELLED.equalsIgnoreCase(status)) {
            return "Cancelled";
        }

        if (STATUS_EXPIRED.equalsIgnoreCase(status)) {
            return "Expired";
        }

        return "Active";
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty()
                ? "N/A"
                : value;
    }

    private void showToast(String message) {
        if (message == null || message.trim().isEmpty()) {
            message = "Something went wrong.";
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        cancelRefreshCall();
        super.onDestroy();
    }

    private <T> String extractErrorMessage(Response<ApiResponse<T>> response) {
        try {
            if (response.errorBody() == null) {
                return "Request failed. Code: " + response.code();
            }

            String errorJson = response.errorBody().string();
            ApiResponse<?> errorResponse = gson.fromJson(errorJson, ApiResponse.class);

            if (errorResponse == null) {
                return "Request failed. Code: " + response.code();
            }

            String firstError = errorResponse.getFirstErrorMessage();

            if (firstError != null && !firstError.trim().isEmpty()) {
                return firstError;
            }

            if (errorResponse.getMessage() != null
                    && !errorResponse.getMessage().trim().isEmpty()) {
                return errorResponse.getMessage();
            }

        } catch (Exception ignored) {
            // Return default error below.
        }

        return "Request failed. Code: " + response.code();
    }
}
