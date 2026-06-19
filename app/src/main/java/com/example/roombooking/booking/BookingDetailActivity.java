package com.example.roombooking.booking;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.example.roombooking.model.booking.BookingStatus;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.utils.ApiErrorUtils;
import com.example.roombooking.utils.AppDiagnostics;
import com.example.roombooking.utils.DateTimeUtils;
import com.example.roombooking.utils.EdgeToEdgeUtils;
import com.example.roombooking.utils.AppToolbarMenu;
import com.example.roombooking.utils.InternetErrorBanner;

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
    private static final String EXTRA_BOOKING_DELETED = "booking_deleted";

    private ImageButton btnBack;

    private TextView tvBookingId;
    private TextView tvRoomName;

    private TextView tvVisitorDetails;
    private TextView tvVisitDetails;
    private TextView tvVisitorCategoryDetails;
    private TextView tvRoomChargesDetails;
    private TextView tvAttenderDetails;
    private TextView tvBudgetHeadDetails;
    private TextView tvRequestorDetails;
    private TextView tvLogisticsDetails;
    private TextView tvStatusDetails;

    private AppCompatButton btnDeleteBooking;
    private AppCompatButton btnEditBooking;
    private SwipeRefreshLayout swipeRefreshBookingDetail;

    private BookingRepository bookingRepository;
    private BookingItem bookingItem;
    private Call<ApiResponse<BookingItem>> refreshBookingCall;
    private Call<ApiResponse<BookingActionData>> deleteBookingCall;

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
        tvBudgetHeadDetails = findViewById(R.id.tvBudgetHeadDetails);
        tvRequestorDetails = findViewById(R.id.tvRequestorDetails);
        tvLogisticsDetails = findViewById(R.id.tvLogisticsDetails);
        tvStatusDetails = findViewById(R.id.tvStatusDetails);

        btnDeleteBooking = findViewById(R.id.btnDeleteBooking);
        btnEditBooking = findViewById(R.id.btnEditBooking);
        swipeRefreshBookingDetail = findViewById(R.id.swipeRefreshBookingDetail);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnDeleteBooking.setOnClickListener(v -> showDeleteDialog());

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
                    showToast(ApiErrorUtils.messageFromResponse(
                            response,
                            "Failed to load booking details."
                    ));
                    return;
                }

                ApiResponse<BookingItem> apiResponse = response.body();

                if (!apiResponse.isSuccess() || apiResponse.getData() == null) {
                    showToast(ApiErrorUtils.messageFromApiResponse(
                            apiResponse,
                            "Failed to load booking details."
                    ));
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
                    showToast(ApiErrorUtils.networkMessage());
                }
            }
        });
    }

    private boolean isCurrentRefreshCall(Call<ApiResponse<BookingItem>> call) {
        return call == refreshBookingCall && !isFinishing() && !isDestroyed();
    }

    private boolean canUpdateUi() {
        return !isFinishing() && !isDestroyed();
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
        tvBudgetHeadDetails.setText(buildBudgetHeadDetails());
        tvRequestorDetails.setText(buildRequestorDetails());
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

    private String buildRequestorDetails() {
        return "Name: " + safe(bookingItem.getRequestorName()) + "\n"
                + "Designation: " + safe(bookingItem.getRequestorDesignation()) + "\n"
                + "Department: " + safe(bookingItem.getRequestorDepartment()) + "\n"
                + "Mobile: " + safe(bookingItem.getRequestorMobile());
    }

    private String buildBudgetHeadDetails() {
        return "Budget Head Name: " + safe(resolveBudgetHeadName()) + "\n"
                + "Department Name: " + safe(resolveBudgetHeadDepartmentName()) + "\n"
                + "Project Code: " + safe(resolveBudgetHeadProjectCode());
    }

    private String resolveBudgetHeadName() {
        String value = bookingItem.getBudgetHeadName();
        if (!isBlank(value)) {
            return value;
        }

        if ("individual".equals(bookingItem.getBudgetHeadType())) {
            return bookingItem.getBudgetHeadValue();
        }

        return "";
    }

    private String resolveBudgetHeadDepartmentName() {
        String value = bookingItem.getBudgetHeadDepartmentName();
        if (!isBlank(value)) {
            return value;
        }

        if ("institute_head".equals(bookingItem.getBudgetHeadType())) {
            return bookingItem.getBudgetHeadValue();
        }

        return "";
    }

    private String resolveBudgetHeadProjectCode() {
        String value = bookingItem.getBudgetHeadProjectCode();
        if (!isBlank(value)) {
            return value;
        }

        if ("project_head".equals(bookingItem.getBudgetHeadType())) {
            return bookingItem.getBudgetHeadValue();
        }

        return "";
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
            shifts.add("Morning Shift (7 AM - 3 PM)");
        }

        if (bookingItem.isAttenderDayShift()) {
            shifts.add("Day Shift (3 PM - 11 PM)");
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

        if (isExpired()) {
            btnDeleteBooking.setEnabled(false);
            btnDeleteBooking.setText("Expired");

            btnEditBooking.setEnabled(false);
            btnEditBooking.setText("Edit Disabled");
            return;
        }

        btnDeleteBooking.setEnabled(true);
        btnDeleteBooking.setText("Delete Booking");

        btnEditBooking.setEnabled(true);
        btnEditBooking.setText("Edit Booking");
    }

    private void disableActionButtons() {
        btnDeleteBooking.setEnabled(false);
        btnEditBooking.setEnabled(false);
    }

    private void showDeleteDialog() {
        if (bookingItem == null) {
            showToast("No booking details found.");
            return;
        }

        if (isExpired()) {
            showToast("Expired booking cannot be deleted.");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Booking")
                .setMessage("Delete this booking permanently?")
                .setPositiveButton("Delete Booking", (dialog, which) -> deleteBooking())
                .setNegativeButton("Close", null)
                .show();
    }

    private void deleteBooking() {
        if (bookingItem == null || deleteBookingCall != null) {
            return;
        }

        setDeletingState(true);

        Call<ApiResponse<BookingActionData>> requestCall =
                bookingRepository.deleteBooking(bookingItem.getId());
        deleteBookingCall = requestCall;
        requestCall.enqueue(new Callback<ApiResponse<BookingActionData>>() {

            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<BookingActionData>> call,
                    @NonNull Response<ApiResponse<BookingActionData>> response
            ) {
                if (call != deleteBookingCall || !canUpdateUi()) return;
                deleteBookingCall = null;
                InternetErrorBanner.hide(BookingDetailActivity.this);
                if (!response.isSuccessful() || response.body() == null) {
                    resetDeleteButton();
                    String message = ApiErrorUtils.messageFromResponse(
                            response,
                            "Delete failed."
                    );
                    AppDiagnostics.logBookingMutationFailure(
                            "delete",
                            bookingItem.getId(),
                            message
                    );
                    showToast(message);
                    return;
                }

                ApiResponse<BookingActionData> apiResponse = response.body();

                if (!apiResponse.isSuccess() || apiResponse.getData() == null) {
                    resetDeleteButton();
                    String message = ApiErrorUtils.messageFromApiResponse(
                            apiResponse,
                            "Delete failed."
                    );
                    AppDiagnostics.logBookingMutationFailure(
                            "delete",
                            bookingItem.getId(),
                            message
                    );
                    showToast(message);
                    return;
                }

                handleDeleteSuccess(apiResponse);
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<BookingActionData>> call,
                    @NonNull Throwable t
            ) {
                if (call != deleteBookingCall || !canUpdateUi()) return;
                deleteBookingCall = null;
                resetDeleteButton();
                if (!call.isCanceled()) {
                    InternetErrorBanner.show(BookingDetailActivity.this);
                    String message = ApiErrorUtils.networkMessage();
                    AppDiagnostics.logBookingMutationFailure(
                            "delete",
                            bookingItem.getId(),
                            message,
                            t
                    );
                    showToast(message);
                }
            }
        });
    }

    private void handleDeleteSuccess(ApiResponse<BookingActionData> apiResponse) {
        bookingRepository.clearFirstPageCaches();
        bookingRepository.clearAvailabilityCachesForBookingMutation();
        sendDeletedResult();
        showToast(apiResponse.getSafeMessage());
        finish();
    }

    private void sendDeletedResult() {
        if (bookingItem == null) {
            return;
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_UPDATED_BOOKING_ID, bookingItem.getId());
        resultIntent.putExtra(EXTRA_BOOKING_DELETED, true);
        setResult(RESULT_OK, resultIntent);
    }

    private void setDeletingState(boolean deleting) {
        btnDeleteBooking.setEnabled(!deleting);
        btnDeleteBooking.setText(deleting ? "Deleting..." : "Delete Booking");
    }

    private void resetDeleteButton() {
        if (isInactiveBooking()) {
            updateButtonState();
            return;
        }

        setDeletingState(false);
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
        return isExpired();
    }

    private boolean isExpired() {
        return bookingItem != null
                && BookingStatus.isExpired(bookingItem.getStatus());
    }

    private String getDisplayStatus() {
        return bookingItem == null
                ? BookingStatus.displayName(null)
                : BookingStatus.displayName(bookingItem.getStatus());
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty()
                ? "N/A"
                : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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
        if (deleteBookingCall != null && !deleteBookingCall.isCanceled()) {
            deleteBookingCall.cancel();
        }
        deleteBookingCall = null;
        super.onDestroy();
    }

}
