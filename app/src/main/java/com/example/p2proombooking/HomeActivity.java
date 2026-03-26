package com.example.p2proombooking;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HomeActivity extends BaseActivity {

    private static final String WS_URL = "ws://10.50.27.112:8080/ws";

    private SessionManager session;
    private AppDatabase db;
    private MeshManager meshManager;

    private SwitchCompat swShowCanceled;
    private BookingAdapter adapter;
    private RecyclerView rv;

    private TextView tvNetStatus;
    private TextView tvSignalStatus;
    private TextView tvWebRtcStatus;
    private TextView tvDcStatus;
    private TextView tvSyncInfo;
    private TextView tvDrawerUserInfo;
    private DrawerLayout drawerLayout;

    private LiveData<List<BookingEntity>> liveSource;
    private SyncBus.Listener syncBusListener;

    private ConnectivityManager cm;
    private ConnectivityManager.NetworkCallback netCb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        session = new SessionManager(this);
        db = AppDatabase.getInstance(this);

        String myUserId = session.getActiveUserId();
        if (myUserId == null) {
            goToAuthAndClearBackstack();
            return;
        }

        drawerLayout = findViewById(R.id.drawerLayout);
        TextView tvUserInfo = findViewById(R.id.tvUserInfo);
        tvDrawerUserInfo = findViewById(R.id.tvDrawerUserInfo);
        tvNetStatus = findViewById(R.id.tvNetStatus);
        tvSignalStatus = findViewById(R.id.tvSignalStatus);
        tvWebRtcStatus = findViewById(R.id.tvWebRtcStatus);
        tvDcStatus = findViewById(R.id.tvDcStatus);
        tvSyncInfo = findViewById(R.id.tvSyncInfo);

        ImageButton btnProfile = findViewById(R.id.btnProfile);
        View ivDrawerProfile = findViewById(R.id.ivDrawerProfile);
        swShowCanceled = findViewById(R.id.swShowCanceled);
        Button btnNewBooking = findViewById(R.id.btnNewBooking);
        Button btnAbout = findViewById(R.id.btnAbout);
        Button btnDrawerLogout = findViewById(R.id.btnDrawerLogout);
        Button btnSyncNow = findViewById(R.id.btnSyncNow);

        tvNetStatus.setText("Network: checking…");
        tvSignalStatus.setText("Signalling: connecting…");
        tvWebRtcStatus.setText("WebRTC: -");
        tvDcStatus.setText("DataChannel: -");
        tvSyncInfo.setText("Sync: idle");

        String deviceId = session.getOrCreateDeviceId();
        String userInfoText =
                "Name: " + session.getDisplayName()
                        + "\nUserId: " + myUserId
                        + "\nDevice: " + deviceId;
        tvUserInfo.setText(userInfoText);
        tvDrawerUserInfo.setText(userInfoText);
        btnProfile.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));
        ivDrawerProfile.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.END));

        meshManager = new MeshManager(this, myUserId, WS_URL, new MeshManager.Listener() {
            @Override
            public void onSignalStatusChanged(String text) {
                runOnUiThread(() -> tvSignalStatus.setText(text));
            }

            @Override
            public void onWebRtcStatusChanged(String text) {
                runOnUiThread(() -> tvWebRtcStatus.setText(text));
            }

            @Override
            public void onDataChannelStatusChanged(String text) {
                runOnUiThread(() -> tvDcStatus.setText(text));
            }

            @Override
            public void onSyncStatusChanged(String text) {
                runOnUiThread(() -> tvSyncInfo.setText(text));
            }
        });

        setupRecycler();
        setupButtons(btnNewBooking, btnAbout, btnDrawerLogout, btnSyncNow);
        setupSyncBus();
        setupNetworkCallback();

        meshManager.start();
    }
    private void setupRecycler() {
        rv = findViewById(R.id.rvBookings);

        adapter = new BookingAdapter(
                booking -> new Thread(() -> {
                    String canceledBy = session.getActiveUserId();
                    if (canceledBy == null) {
                        runOnUiThread(this::goToAuthAndClearBackstack);
                        return;
                    }

                    db.bookingDao().cancelBooking(
                            booking.bookingId,
                            canceledBy,
                            System.currentTimeMillis()
                    );

                    SyncBus.notifyLocalChange();

                    runOnUiThread(() ->
                            Toast.makeText(this, "Booking canceled", Toast.LENGTH_SHORT).show()
                    );
                }).start(),

                booking -> {
                    Intent i = new Intent(this, EditBookingActivity.class);
                    i.putExtra(EditBookingActivity.EXTRA_BOOKING_ID, booking.bookingId);
                    startActivity(i);
                }
        );

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        swShowCanceled.setOnCheckedChangeListener((btn, checked) -> observeBookings(checked));
        observeBookings(false);
    }

    private void setupButtons(Button btnNewBooking, Button btnAbout, Button btnDrawerLogout, Button btnSyncNow) {
        btnNewBooking.setOnClickListener(v ->
                startActivity(new Intent(this, CreateBookingActivity.class))
        );

        btnAbout.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("About")
                .setMessage("P2P Room Booking\n\nManage bookings, sync peers, and access your account from the profile panel.")
                .setPositiveButton("OK", null)
                .show());

        View.OnClickListener logoutClickListener = v -> {
            session.logout();
            goToAuthAndClearBackstack();
        };

        btnDrawerLogout.setOnClickListener(logoutClickListener);

        btnSyncNow.setOnClickListener(v -> {
            meshManager.syncNow();
            Toast.makeText(this, "Sync requested", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupSyncBus() {
        syncBusListener = new SyncBus.Listener() {
            @Override
            public void onLocalDbChanged() {
            }

            @Override
            public void onLocalChange() {
                meshManager.onLocalChange();
            }

            @Override
            public void onRemoteChange() {
            }
        };
        SyncBus.addListener(syncBusListener);
    }

    private void setupNetworkCallback() {
        cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        netCb = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                runOnUiThread(() -> tvNetStatus.setText("Network: available ✅"));
                meshManager.onNetworkAvailable();
            }

            @Override
            public void onLost(Network network) {
                runOnUiThread(() -> tvNetStatus.setText("Network: lost ⚠"));
                meshManager.onNetworkLost();
            }
        };
        cm.registerDefaultNetworkCallback(netCb);
    }

    private void observeBookings(boolean showCanceled) {
        if (liveSource != null) liveSource.removeObservers(this);

        liveSource = showCanceled
                ? db.bookingDao().observeAllIncludingCanceled()
                : db.bookingDao().observeAllActive();

        liveSource.observe(this, adapter::submit);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (meshManager != null) {
            meshManager.onNetworkAvailable();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (cm != null && netCb != null) {
            try {
                cm.unregisterNetworkCallback(netCb);
            } catch (Exception ignored) {
            }
            netCb = null;
        }

        if (syncBusListener != null) {
            SyncBus.removeListener(syncBusListener);
            syncBusListener = null;
        }

        if (meshManager != null) {
            meshManager.stop();
        }
    }

    private void goToAuthAndClearBackstack() {
        Intent i = new Intent(this, AuthActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
            return;
        }
        super.onBackPressed();
    }
}
