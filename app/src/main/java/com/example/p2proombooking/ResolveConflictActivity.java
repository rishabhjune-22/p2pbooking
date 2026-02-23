package com.example.p2proombooking;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ResolveConflictActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_ID = "booking_id";

    private final SimpleDateFormat fmt =
            new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resolve_conflict);

        String bookingId = getIntent().getStringExtra(EXTRA_BOOKING_ID);
        if (bookingId == null) { finish(); return; }

        AppDatabase db = AppDatabase.getInstance(this);
        BookingConflictEntity c = db.bookingConflictDao().getById(bookingId);
        BookingEntity local = db.bookingDao().getById(bookingId);

        if (c == null || local == null) { finish(); return; }

        TextView tvLocal = findViewById(R.id.tvLocal);
        TextView tvRemote = findViewById(R.id.tvRemote);
        Button btnKeepLocal = findViewById(R.id.btnKeepLocal);
        Button btnAcceptRemote = findViewById(R.id.btnAcceptRemote);

        tvLocal.setText(
                "LOCAL\nRoom: " + c.localRoomId +
                        "\nStart: " + fmt.format(new Date(c.localStartUtc)) +
                        "\nEnd: " + fmt.format(new Date(c.localEndUtc)) +
                        "\nStatus: " + c.localStatus +
                        "\nVersion: " + c.localVersion
        );

        tvRemote.setText(
                "REMOTE\nRoom: " + c.remoteRoomId +
                        "\nStart: " + fmt.format(new Date(c.remoteStartUtc)) +
                        "\nEnd: " + fmt.format(new Date(c.remoteEndUtc)) +
                        "\nStatus: " + c.remoteStatus +
                        "\nVersion: " + c.remoteVersion
        );

        // Keep Local: clear conflict + mark ACTIVE + pending sync
        btnKeepLocal.setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            local.status = BookingConstants.STATUS_ACTIVE;
            local.updatedAt = now;
            local.version = local.version + 1;
            local.syncFlag = BookingConstants.SYNC_PENDING;

            db.bookingDao().update(local);
            db.bookingConflictDao().deleteById(bookingId);
            finish();
        });

        // Accept Remote: overwrite local using remote snapshot
        btnAcceptRemote.setOnClickListener(v -> {
            long now = System.currentTimeMillis();

            local.roomId = c.remoteRoomId;
            local.startUtc = c.remoteStartUtc;
            local.endUtc = c.remoteEndUtc;
            local.status = c.remoteStatus == null ? BookingConstants.STATUS_ACTIVE : c.remoteStatus;

            local.updatedAt = now;
            local.version = local.version + 1;
            local.syncFlag = BookingConstants.SYNC_PENDING;

            db.bookingDao().update(local);
            db.bookingConflictDao().deleteById(bookingId);
            finish();
        });
    }
}
