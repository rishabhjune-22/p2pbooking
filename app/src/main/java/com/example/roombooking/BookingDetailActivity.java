package com.example.roombooking;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.roombooking.api.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingDetailActivity extends AppCompatActivity {

    private TextView tvDetails;
    private Button btnCancelBooking;

    private BookingItem bookingItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_detail);

        tvDetails = findViewById(R.id.tvDetails);
        btnCancelBooking = findViewById(R.id.btnCancelBooking);

        bookingItem = (BookingItem) getIntent().getSerializableExtra("booking_data");

        if (bookingItem != null) {
            renderBookingDetails();
            updateCancelButtonState();

            btnCancelBooking.setOnClickListener(v -> showCancelDialog());
        } else {
            tvDetails.setText("No booking details found.");
            btnCancelBooking.setEnabled(false);
        }
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