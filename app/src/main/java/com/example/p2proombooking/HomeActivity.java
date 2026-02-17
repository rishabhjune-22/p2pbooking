package com.example.p2proombooking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.p2proombooking.AppDatabase;
import com.example.p2proombooking.BookingEntity;

import java.util.List;
import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        RecyclerView rv = findViewById(R.id.rvBookings);
        BookingAdapter adapter = new BookingAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

// load bookings
        loadBookings(adapter);



        session = new SessionManager(this);

        TextView tvUserInfo = findViewById(R.id.tvUserInfo);
        TextView tvSyncStatus = findViewById(R.id.tvSyncStatus);
        Button btnSyncNow = findViewById(R.id.btnSyncNow);
        Button btnNewBooking = findViewById(R.id.btnNewBooking);
        Button btnLogout = findViewById(R.id.btnLogout);

        btnNewBooking.setOnClickListener(v -> {
            startActivity(new Intent(this, CreateBookingActivity.class));
        });

        // If someone somehow opens Home without login, kick to Auth
        String userId = session.getLoggedInUserId();
        if (userId == null) {
            goToAuthAndClearBackstack();
            return;
        }

        // Show session info (demo)
        String deviceId = session.getOrCreateDeviceId();
        tvUserInfo.setText("Logged in as: " + userId + "\nDevice: " + deviceId);

        // Demo sync status (we’ll wire real WebRTC later)
        tvSyncStatus.setText("Sync Status: Offline (demo)");

        btnSyncNow.setOnClickListener(v ->
                Toast.makeText(this, "Sync will be added after local DB + bookings.", Toast.LENGTH_SHORT).show()
        );


        btnLogout.setOnClickListener(v -> {
            session.logout();
            goToAuthAndClearBackstack();
        });
    }

    private void goToAuthAndClearBackstack() {
        Intent i = new Intent(this, AuthActivity.class);
        // Clear the back stack so back won't return to Home
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void loadBookings(BookingAdapter adapter) {
        List<BookingEntity> list = AppDatabase.getInstance(this).bookingDao().getAll();
        adapter.submit(list);
    }

    @Override
    protected void onResume() {
        super.onResume();
        RecyclerView rv = findViewById(R.id.rvBookings);
        if (rv.getAdapter() instanceof BookingAdapter) {
            loadBookings((BookingAdapter) rv.getAdapter());
        }
    }
}