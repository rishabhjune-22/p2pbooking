package com.example.p2proombooking;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.widget.DatePicker;
import android.widget.TimePicker;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import androidx.appcompat.app.AppCompatActivity;

import com.example.p2proombooking.AppDatabase;
import com.example.p2proombooking.BookingDao;
import com.example.p2proombooking.BookingEntity;
import com.example.p2proombooking.RoomEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreateBookingActivity extends AppCompatActivity {

    private Spinner spRooms;
    private TextView tvMsg;

    private List<RoomEntity> rooms = new ArrayList<>();
    private final List<String> roomNames = new ArrayList<>();


    private EditText etStartDT, etEndDT;

    private final Calendar startCal = Calendar.getInstance();
    private final Calendar endCal = Calendar.getInstance();

    private final SimpleDateFormat displayFmt =
            new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_booking);

        spRooms = findViewById(R.id.spRooms);
        etStartDT = findViewById(R.id.etStartDateTime);
        etEndDT = findViewById(R.id.etEndDateTime);
        tvMsg = findViewById(R.id.tvMsg);

// default: end = start + 1 hour
        endCal.setTimeInMillis(startCal.getTimeInMillis() + 60 * 60 * 1000L);
        refreshDateTimeFields();

        etStartDT.setOnClickListener(v -> pickDateTime(startCal, () -> {
            // keep end >= start + 1 hour if user picks a later start
            if (endCal.getTimeInMillis() <= startCal.getTimeInMillis()) {
                endCal.setTimeInMillis(startCal.getTimeInMillis() + 60 * 60 * 1000L);
            }
            refreshDateTimeFields();
        }));

        etEndDT.setOnClickListener(v -> pickDateTime(endCal, this::refreshDateTimeFields));



        Button btnSave = findViewById(R.id.btnSave);

        loadRooms();

        btnSave.setOnClickListener(v -> saveBooking());
    }

    private void loadRooms() {
        AppDatabase db = AppDatabase.getInstance(this);

        rooms = db.roomDao().getAll();

        if (rooms == null || rooms.isEmpty()) {
            db.roomDao().insertAll(RoomSeed.buildDefaultRooms());
            rooms = db.roomDao().getAll();
        }

        roomNames.clear();
        for (RoomEntity r : rooms) {
            roomNames.add(r.displayName); // clean: "Delta 101A"
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                roomNames
        );
        spRooms.setAdapter(adapter);
    }

    private void saveBooking() {

        SessionManager sm = new SessionManager(this);
        String userId = sm.getActiveUserId();
        if (userId == null) {
            tvMsg.setText("Session expired. Please login again.");
            return;
        }

        if (rooms == null || rooms.isEmpty()) {
            tvMsg.setText("No rooms found.");
            return;
        }

        long startUtc = startCal.getTimeInMillis();
        long endUtc = endCal.getTimeInMillis();

        if (endUtc <= startUtc) {
            tvMsg.setText("End time must be greater than start time.");
            return;
        }

        RoomEntity selected = rooms.get(spRooms.getSelectedItemPosition());
        String roomId = selected.roomId;

        AppDatabase db = AppDatabase.getInstance(this);

        // ✅ move DB work off main thread for smoothness + no random UI delays
        new Thread(() -> {
            try {
                List<BookingEntity> overlaps = db.bookingDao().findOverlaps(roomId, startUtc, endUtc);
                if (overlaps != null && !overlaps.isEmpty()) {
                    runOnUiThread(() -> tvMsg.setText("⚠ Room already booked in this time range."));
                    return;
                }

                long now = System.currentTimeMillis();
                String deviceId = sm.getOrCreateDeviceId();

                BookingEntity b = new BookingEntity();
                b.bookingId = UUID.randomUUID().toString();
                b.roomId = roomId;
                b.startUtc = startUtc;
                b.endUtc = endUtc;

                b.createdByUserId = userId;
                b.createdByDeviceId = deviceId;

                b.createdAt = now;
                b.updatedAt = now;
                b.version = 1;

                b.status = BookingConstants.STATUS_ACTIVE;
                b.canceledByUserId = null;
                b.canceledAt = 0L;

                b.syncFlag = BookingConstants.SYNC_PENDING;
                b.deletedFlag = 0;

                db.bookingDao().insert(b);

                Log.d("SYNC", "Created booking -> SyncBus.notifyLocalChange()");
                SyncBus.notifyLocalChange(); // ✅ now this triggers poke reliably

                runOnUiThread(() -> {
                    tvMsg.setText("Booking created successfully ✅");
                    finish();
                });

            } catch (Exception e) {
                Log.e("SYNC", "saveBooking failed", e);
                runOnUiThread(() -> tvMsg.setText("Save failed: " + e.getMessage()));
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