package com.example.p2proombooking;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class EditBookingActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_ID = "booking_id";

    private Spinner spRooms;
    private EditText etStartDT, etEndDT;
    private TextView tvMsg;
    private Button btnUpdate;

    private final List<RoomEntity> rooms = new ArrayList<>();
    private final List<String> roomNames = new ArrayList<>();

    private final Calendar startCal = Calendar.getInstance();
    private final Calendar endCal = Calendar.getInstance();

    private final SimpleDateFormat displayFmt =
            new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    private AppDatabase db;
    private BookingEntity booking;
    private String bookingId;

    private volatile boolean saving = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_booking);

        db = AppDatabase.getInstance(this);

        spRooms = findViewById(R.id.spRooms);
        etStartDT = findViewById(R.id.etStartDateTime);
        etEndDT = findViewById(R.id.etEndDateTime);
        tvMsg = findViewById(R.id.tvMsg);
        btnUpdate = findViewById(R.id.btnUpdate);

        bookingId = getIntent().getStringExtra(EXTRA_BOOKING_ID);
        if (bookingId == null || bookingId.trim().isEmpty()) {
            tvMsg.setText("Invalid booking.");
            finish();
            return;
        }

        etStartDT.setOnClickListener(v -> pickDateTime(startCal, () -> {
            if (endCal.getTimeInMillis() <= startCal.getTimeInMillis()) {
                endCal.setTimeInMillis(startCal.getTimeInMillis() + 60 * 60 * 1000L);
            }
            refreshDateTimeFields();
        }));

        etEndDT.setOnClickListener(v -> pickDateTime(endCal, this::refreshDateTimeFields));

        btnUpdate.setOnClickListener(v -> {
            if (!saving) {
                updateBooking();
            }
        });

        loadData();
    }

    private void loadData() {
        new Thread(() -> {
            try {
                BookingEntity found = db.bookingDao().getById(bookingId);
                List<RoomEntity> roomList = db.roomDao().getAll();

                if (roomList == null || roomList.isEmpty()) {
                    db.roomDao().insertAll(RoomSeed.buildDefaultRooms());
                    roomList = db.roomDao().getAll();
                }

                booking = found;
                rooms.clear();
                if (roomList != null) {
                    rooms.addAll(roomList);
                }

                runOnUiThread(() -> {
                    if (booking == null) {
                        tvMsg.setText("Booking not found.");
                        finish();
                        return;
                    }

                    setupRoomSpinner();
                    bindBookingToUi();
                });

            } catch (Exception e) {
                runOnUiThread(() -> tvMsg.setText("Load failed: " + e.getMessage()));
            }
        }).start();
    }

    private void setupRoomSpinner() {
        roomNames.clear();
        for (RoomEntity r : rooms) {
            roomNames.add(r.displayName);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                roomNames
        );
        spRooms.setAdapter(adapter);
    }

    private void bindBookingToUi() {
        startCal.setTimeInMillis(booking.startUtc);
        endCal.setTimeInMillis(booking.endUtc);
        refreshDateTimeFields();

        int selectedIndex = 0;
        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).roomId.equals(booking.roomId)) {
                selectedIndex = i;
                break;
            }
        }
        spRooms.setSelection(selectedIndex);
    }

    private void updateBooking() {
        if (booking == null) {
            tvMsg.setText("Booking not loaded.");
            return;
        }

        if (rooms.isEmpty()) {
            tvMsg.setText("No rooms found.");
            return;
        }

        long startUtc = startCal.getTimeInMillis();
        long endUtc = endCal.getTimeInMillis();

        if (endUtc <= startUtc) {
            tvMsg.setText("End time must be greater than start time.");
            return;
        }

        int selectedPos = spRooms.getSelectedItemPosition();
        if (selectedPos < 0 || selectedPos >= rooms.size()) {
            tvMsg.setText("Please select a valid room.");
            return;
        }

        String selectedRoomId = rooms.get(selectedPos).roomId;

        saving = true;
        btnUpdate.setEnabled(false);
        tvMsg.setText("Updating booking...");

        new Thread(() -> {
            try {
                List<BookingEntity> overlaps = db.bookingDao().findOverlapsExcluding(
                        selectedRoomId,
                        booking.bookingId,
                        startUtc,
                        endUtc
                );

                if (overlaps != null && !overlaps.isEmpty()) {
                    runOnUiThread(() -> {
                        saving = false;
                        btnUpdate.setEnabled(true);
                        tvMsg.setText("⚠ Room already booked in this time range.");
                    });
                    return;
                }

                long now = System.currentTimeMillis();

                booking.roomId = selectedRoomId;
                booking.startUtc = startUtc;
                booking.endUtc = endUtc;

                // if it was conflicted and user is editing manually, move it back to active
                booking.status = BookingConstants.STATUS_ACTIVE;

                booking.updatedAt = now;
                booking.version = booking.version + 1;
                booking.syncFlag = BookingConstants.SYNC_PENDING;

                db.bookingDao().update(booking);

                // If conflict entry exists for this booking, clear it after successful manual edit
                db.bookingConflictDao().deleteById(booking.bookingId);

                SyncBus.notifyLocalChange();

                runOnUiThread(() -> {
                    saving = false;
                    btnUpdate.setEnabled(true);
                    tvMsg.setText("Booking updated successfully ✅");
                    finish();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    saving = false;
                    btnUpdate.setEnabled(true);
                    tvMsg.setText("Update failed: " + e.getMessage());
                });
            }
        }).start();
    }

    private void refreshDateTimeFields() {
        etStartDT.setText(displayFmt.format(startCal.getTime()));
        etEndDT.setText(displayFmt.format(endCal.getTime()));
    }

    private void pickDateTime(Calendar target, Runnable onDone) {
        int y = target.get(Calendar.YEAR);
        int m = target.get(Calendar.MONTH);
        int d = target.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dp = new DatePickerDialog(this, (DatePicker view, int year, int month, int dayOfMonth) -> {
            target.set(Calendar.YEAR, year);
            target.set(Calendar.MONTH, month);
            target.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            int hh = target.get(Calendar.HOUR_OF_DAY);
            int mm = target.get(Calendar.MINUTE);

            TimePickerDialog tp = new TimePickerDialog(this, (TimePicker tview, int hourOfDay, int minute) -> {
                target.set(Calendar.HOUR_OF_DAY, hourOfDay);
                target.set(Calendar.MINUTE, minute);
                target.set(Calendar.SECOND, 0);
                target.set(Calendar.MILLISECOND, 0);
                onDone.run();
            }, hh, mm, false);

            tp.show();
        }, y, m, d);

        dp.show();
    }
}