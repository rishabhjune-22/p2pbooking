package com.example.roombooking;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.roombooking.api.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingDetailActivity extends AppCompatActivity {

    private TextView tvDetails;
    private Button btnCancelBooking;
    private Button btnEditBooking;

    private BookingItem bookingItem;

    private final ActivityResultLauncher<Intent> editBookingLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK
                        && result.getData() != null
                        && bookingItem != null) {

                    Intent data = result.getData();

                    bookingItem.setVisitor_name(data.getStringExtra("visitor_name"));
                    bookingItem.setVisitor_mobile(data.getStringExtra("visitor_mobile"));
                    bookingItem.setPurpose_of_visit(data.getStringExtra("purpose_of_visit"));
                    bookingItem.setArrival_date(data.getStringExtra("arrival_date"));
                    bookingItem.setArrival_time(data.getStringExtra("arrival_time"));
                    bookingItem.setDeparture_date(data.getStringExtra("departure_date"));
                    bookingItem.setDeparture_time(data.getStringExtra("departure_time"));

                    renderBookingDetails();

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("updated_booking_id", bookingItem.getId());
                    resultIntent.putExtra("updated_status", bookingItem.getStatus());
                    resultIntent.putExtra("visitor_name", bookingItem.getVisitor_name());
                    resultIntent.putExtra("visitor_mobile", bookingItem.getVisitor_mobile());
                    resultIntent.putExtra("purpose_of_visit", bookingItem.getPurpose_of_visit());
                    resultIntent.putExtra("arrival_date", bookingItem.getArrival_date());
                    resultIntent.putExtra("arrival_time", bookingItem.getArrival_time());
                    resultIntent.putExtra("departure_date", bookingItem.getDeparture_date());
                    resultIntent.putExtra("departure_time", bookingItem.getDeparture_time());
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

        bookingItem = (BookingItem) getIntent().getSerializableExtra("booking_data");

        if (bookingItem != null) {
            renderBookingDetails();
            updateCancelButtonState();

            btnEditBooking.setOnClickListener(v -> openEditScreen());
            btnCancelBooking.setOnClickListener(v -> showCancelDialog());
        } else {
            tvDetails.setText("No booking details found.");
            btnCancelBooking.setEnabled(false);
            btnEditBooking.setEnabled(false);
        }
    }

    private void openEditScreen() {
        if (bookingItem == null) return;

        if ("cancelled".equalsIgnoreCase(bookingItem.getStatus())) {
            Toast.makeText(this, "Cancelled booking cannot be edited", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, EditBookingActivity.class);
        intent.putExtra("booking_data", bookingItem);
        editBookingLauncher.launch(intent);
    }

    private void renderBookingDetails() {
        String details =
                "Booking ID: " + bookingItem.getId() + "\n\n" +
                        "Room Name: " + safe(bookingItem.getRoom_name()) + "\n" +
                        "Created By: " + safe(bookingItem.getCreated_by_username()) + "\n\n" +

                        "Visitor Name: " + safe(bookingItem.getVisitor_name()) + "\n" +
                        "Visitor Designation: " + safe(bookingItem.getVisitor_designation()) + "\n" +
                        "Visitor Organisation: " + safe(bookingItem.getVisitor_organisation()) + "\n" +
                        "Visitor Gender: " + safe(bookingItem.getVisitor_gender()) + "\n" +
                        "Visitor Address: " + safe(bookingItem.getVisitor_address()) + "\n" +
                        "Visitor Mobile: " + safe(bookingItem.getVisitor_mobile()) + "\n" +
                        "Visitor Email: " + safe(bookingItem.getVisitor_email()) + "\n\n" +

                        "Arrival Date: " + safe(bookingItem.getArrival_date()) + "\n" +
                        "Arrival Time: " + safe(bookingItem.getArrival_time()) + "\n" +
                        "Departure Date: " + safe(bookingItem.getDeparture_date()) + "\n" +
                        "Departure Time: " + safe(bookingItem.getDeparture_time()) + "\n\n" +

                        "Purpose: " + safe(bookingItem.getPurpose_of_visit()) + "\n\n" +

                        "Requestee Name: " + safe(bookingItem.getRequestee_name()) + "\n" +
                        "Requestee Designation: " + safe(bookingItem.getRequestee_designation()) + "\n" +
                        "Requestee Department: " + safe(bookingItem.getRequestee_department()) + "\n" +
                        "Requestee Mobile: " + safe(bookingItem.getRequestee_mobile()) + "\n\n" +

                        "Logistics Name: " + safe(bookingItem.getLogistics_name()) + "\n" +
                        "Logistics Designation: " + safe(bookingItem.getLogistics_designation()) + "\n" +
                        "Logistics Mobile: " + safe(bookingItem.getLogistics_mobile()) + "\n\n" +

                        "Status: " + safe(bookingItem.getStatus());

        tvDetails.setText(details);
    }

    private void updateCancelButtonState() {
        boolean alreadyCancelled = "cancelled".equalsIgnoreCase(bookingItem.getStatus());
        btnCancelBooking.setEnabled(!alreadyCancelled);
        btnCancelBooking.setText(alreadyCancelled ? "Already Cancelled" : "Cancel Booking");

        btnEditBooking.setEnabled(!alreadyCancelled);
        btnEditBooking.setText(alreadyCancelled ? "Edit Disabled" : "Edit Booking");
    }

    private void showCancelDialog() {
        if (bookingItem == null) return;

        if ("cancelled".equalsIgnoreCase(bookingItem.getStatus())) {
            Toast.makeText(this, "Booking is already cancelled", Toast.LENGTH_SHORT).show();
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
                .enqueue(new Callback<BookingCancelResponse>() {
                    @Override
                    public void onResponse(Call<BookingCancelResponse> call, Response<BookingCancelResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            BookingCancelResponse body = response.body();

                            bookingItem.setStatus(body.getStatus());
                            renderBookingDetails();
                            updateCancelButtonState();

                            Toast.makeText(BookingDetailActivity.this, body.getMessage(), Toast.LENGTH_SHORT).show();

                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("updated_booking_id", bookingItem.getId());
                            resultIntent.putExtra("updated_status", body.getStatus());
                            setResult(RESULT_OK, resultIntent);

                        } else if (response.code() == 400) {
                            btnCancelBooking.setEnabled(true);
                            btnCancelBooking.setText("Cancel Booking");
                            Toast.makeText(BookingDetailActivity.this, "Booking is already cancelled or invalid request", Toast.LENGTH_SHORT).show();

                        } else if (response.code() == 403) {
                            btnCancelBooking.setEnabled(true);
                            btnCancelBooking.setText("Cancel Booking");
                            Toast.makeText(BookingDetailActivity.this, "You can cancel only your own booking", Toast.LENGTH_SHORT).show();

                        } else if (response.code() == 404) {
                            btnCancelBooking.setEnabled(true);
                            btnCancelBooking.setText("Cancel Booking");
                            Toast.makeText(BookingDetailActivity.this, "Booking not found", Toast.LENGTH_SHORT).show();

                        } else {
                            btnCancelBooking.setEnabled(true);
                            btnCancelBooking.setText("Cancel Booking");
                            Toast.makeText(BookingDetailActivity.this, "Cancel failed. Code: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<BookingCancelResponse> call, Throwable t) {
                        btnCancelBooking.setEnabled(true);
                        btnCancelBooking.setText("Cancel Booking");
                        Toast.makeText(BookingDetailActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}