package com.example.p2proombooking;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

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

    private final Calendar startCal = Calendar.getInstance();
    private final Calendar endCal = Calendar.getInstance();
    private final SimpleDateFormat displayFmt =
            new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    private List<RoomEntity> rooms = new ArrayList<>();
    private final List<String> roomNames = new ArrayList<>();

    private BookingEntity booking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_booking); // reuse same XML

        spRooms = findViewById(R.id.spRooms);
        etStartDT = findViewById(R.id.etStartDateTime);
        etEndDT = findViewById(R.id.etEndDateTime);
        tvMsg = findViewById(R.id.tvMsg);
        Button btnSave = findViewById(R.id.btnSave);

        ((TextView)findViewById(R.id.tvTitle)).setText("Edit Booking");
        btnSave.setText("Save Changes");

        String bookingId = getIntent().getStringExtra(EXTRA_BOOKING_ID);
        if (bookingId == null) {
            tvMsg.setText("Missing booking id.");
            finish();
            return;
        }

        loadRooms();
        loadBooking(bookingId);
        if ("CANCELED".equalsIgnoreCase(booking.status)) {
            finish();
            return;
        }

        etStartDT.setOnClickListener(v ->
                CreateBookingUi.pickDateTime(this, startCal, () -> {
                    if (endCal.getTimeInMillis() <= startCal.getTimeInMillis()) {
                        endCal.setTimeInMillis(startCal.getTimeInMillis() + 60 * 60 * 1000L);
                    }
                    refreshDateTimeFields();
                })
        );

        etEndDT.setOnClickListener(v ->
                CreateBookingUi.pickDateTime(this, endCal, this::refreshDateTimeFields)
        );

        btnSave.setOnClickListener(v -> saveChanges());
    }

    private void loadRooms() {
        AppDatabase db = AppDatabase.getInstance(this);
        rooms = db.roomDao().getAll();

        if (rooms == null || rooms.isEmpty()) {
            db.roomDao().insertAll(RoomSeed.buildDefaultRooms());
            rooms = db.roomDao().getAll();
        }

        roomNames.clear();
        for (RoomEntity r : rooms) roomNames.add(r.displayName);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                roomNames
        );
        spRooms.setAdapter(adapter);
    }

    private void loadBooking(String bookingId) {
        booking = AppDatabase.getInstance(this).bookingDao().getById(bookingId);
        if (booking == null) {
            tvMsg.setText("Booking not found.");
            finish();
            return;
        }

        startCal.setTimeInMillis(booking.startUtc);
        endCal.setTimeInMillis(booking.endUtc);
        refreshDateTimeFields();

        // select room in spinner
        int idx = 0;
        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).roomId.equals(booking.roomId)) {
                idx = i;
                break;
            }
        }
        spRooms.setSelection(idx);
    }

    private void refreshDateTimeFields() {
        etStartDT.setText(displayFmt.format(startCal.getTime()));
        etEndDT.setText(displayFmt.format(endCal.getTime()));
    }

    private void saveChanges() {
        if (booking == null) return;

        long startUtc = startCal.getTimeInMillis();
        long endUtc = endCal.getTimeInMillis();
        if (endUtc <= startUtc) {
            tvMsg.setText("End must be after start.");
            return;
        }

        RoomEntity selected = rooms.get(spRooms.getSelectedItemPosition());
        String newRoomId = selected.roomId;

        AppDatabase db = AppDatabase.getInstance(this);

        // conflict check excluding current booking
        List<BookingEntity> overlaps = db.bookingDao()
                .findOverlapsExcluding(newRoomId, booking.bookingId, startUtc, endUtc);

        if (overlaps != null && !overlaps.isEmpty()) {
            tvMsg.setText("⚠ Conflict: room already booked in that time.");
            return;
        }

        // update entity
        booking.roomId = newRoomId;
        booking.startUtc = startUtc;
        booking.endUtc = endUtc;
        booking.updatedAt = System.currentTimeMillis();
        booking.version = booking.version + 1;
        booking.syncFlag = "PENDING_SYNC"; // ✅ NEW
        db.bookingDao().update(booking);
        SyncBus.notifyLocalChange();

        finish();
    }
}