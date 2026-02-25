package com.example.p2proombooking;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG_RTC = "RTC";
    private static final String TAG_SYNC = "SYNC";
    private static final String WS_URL  = "ws://10.50.42.114:8080/ws";

    private SessionManager session;
    private AppDatabase db;

    private SignallingClient signalling;
    private WebRtcPeer peer;
    private SyncProtocol syncProtocol;

    private String myUserId;
    private String connectedPeerUserId;

    private TextView tvSyncStatus;
    private SwitchCompat swShowCanceled;
    private BookingAdapter adapter;
    private RecyclerView rv;

    private final Map<String, List<JSONObject>> pendingIce = new HashMap<>();
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private LiveData<List<BookingEntity>> liveSource;
    private SyncBus.Listener syncBusListener;

    // Connection state
    private volatile boolean iceConnected = false;
    private volatile boolean dcOpen = false;

    // IMPORTANT: to avoid killing first-time setup, only treat stale if we once had a working DC
    private volatile boolean everDcOpen = false;

    // if local update happens while DC not ready, sync later
    private volatile boolean localDirty = false;

    // Reconnect backoff
    private volatile boolean reconnectScheduled = false;
    private ConnectivityManager cm;
    private ConnectivityManager.NetworkCallback netCb;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int reconnectAttempt = 0;
    private Button btnSyncNow;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        session = new SessionManager(this);
        db = AppDatabase.getInstance(this);

        myUserId = session.getActiveUserId();
        if (myUserId == null) {
            goToAuthAndClearBackstack();
            return;
        }


        TextView tvUserInfo = findViewById(R.id.tvUserInfo);
        tvSyncStatus = findViewById(R.id.tvSyncStatus);
        swShowCanceled = findViewById(R.id.swShowCanceled);

        Button btnNewBooking = findViewById(R.id.btnNewBooking);
        Button btnLogout = findViewById(R.id.btnLogout);
        btnSyncNow = findViewById(R.id.btnSyncNow);

        String deviceId = session.getOrCreateDeviceId();
        tvUserInfo.setText(
                "Name: " + session.getDisplayName()
                        + "\nUserId: " + myUserId
                        + "\nDevice: " + deviceId
        );
        tvSyncStatus.setText("Signal: Connecting...");

        // RecyclerView
        rv = findViewById(R.id.rvBookings);
        adapter = new BookingAdapter(

                // Cancel
                booking -> io.execute(() -> {
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
                }),

                // Edit / Resolve
                booking -> {
                    Intent i;
                    if (BookingConstants.STATUS_CONFLICTED.equalsIgnoreCase(booking.status)) {
                        i = new Intent(this, ResolveConflictActivity.class);
                        i.putExtra(ResolveConflictActivity.EXTRA_BOOKING_ID, booking.bookingId);
                    } else {
                        i = new Intent(this, EditBookingActivity.class);
                        i.putExtra(EditBookingActivity.EXTRA_BOOKING_ID, booking.bookingId);
                    }
                    startActivity(i);
                }
        );

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        // LiveData observe
        swShowCanceled.setOnCheckedChangeListener((btn, checked) -> observeBookings(checked));
        observeBookings(false);

        btnNewBooking.setOnClickListener(v ->
                startActivity(new Intent(this, CreateBookingActivity.class))
        );

        btnLogout.setOnClickListener(v -> {
            session.logout();
            goToAuthAndClearBackstack();
        });

        btnSyncNow.setOnClickListener(v -> {
            // 1) If DC is open, just poke sync immediately
            if (syncProtocol != null && peer != null && dcOpen && peer.isDataChannelOpen()) {
                tvSyncStatus.setText("Sync: forcing now…");
                syncProtocol.sendPoke();
                localDirty = false;
                Toast.makeText(this, "Sync triggered", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2) If signalling is connected but WebRTC/DC isn't ready yet, try to connect to a peer
            if (signalling != null && signalling.isConnected()) {
                tvSyncStatus.setText("Sync: connecting to peer…");
                if (connectedPeerUserId != null) {
                    connectToPeerIfNeeded(connectedPeerUserId);
                } else {
                    // no known peer yet, still try signalling reconnect to fetch peers/join state
                    scheduleReconnectNow();
                }
                Toast.makeText(this, "Trying to connect…", Toast.LENGTH_SHORT).show();
                return;
            }

            // 3) Signalling not connected -> reconnect signalling
            tvSyncStatus.setText("Signal: reconnecting (manual)...");
            scheduleReconnectNow();
            Toast.makeText(this, "Reconnecting…", Toast.LENGTH_SHORT).show();
        });


        // SyncBus listener
        syncBusListener = new SyncBus.Listener() {
            @Override public void onLocalDbChanged() {}

            @Override
            public void onLocalChange() {
                localDirty = true;
                Log.d(TAG_SYNC, "Local change -> dirty=true, protocol=" + (syncProtocol != null));

                // if protocol ready, poke immediately
                if (syncProtocol != null && dcOpen && peer != null && peer.isDataChannelOpen()) {
                    syncProtocol.sendPoke();
                    localDirty = false;
                    Log.d(TAG_SYNC, "Poke sent -> dirty=false");
                } else {
                    Log.d(TAG_SYNC, "DC not ready -> will sync on next DC OPEN");
                }
            }

            @Override
            public void onRemoteChange() {
                Log.d(TAG_SYNC, "Remote change applied (LiveData will refresh)");
            }
        };
        SyncBus.addListener(syncBusListener);




        cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        netCb = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                runOnUiThread(() -> tvSyncStatus.setText("Network: available ✅"));
                scheduleReconnectNow();
            }

            @Override
            public void onLost(Network network) {
                runOnUiThread(() -> tvSyncStatus.setText("Network: lost ⚠"));
                // optional: close current stuff
                // safeClosePeer();
            }
        };

        cm.registerDefaultNetworkCallback(netCb);




        // Start signalling
        startSignalling(WS_URL);
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
        // If signalling is dead, try
        if (signalling == null || !signalling.isConnected()) {
            scheduleReconnectNow();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cm != null && netCb != null) {
            try { cm.unregisterNetworkCallback(netCb); } catch (Exception ignored) {}
            netCb = null;
        }

        if (syncBusListener != null) {
            SyncBus.removeListener(syncBusListener);
            syncBusListener = null;
        }

        io.shutdownNow();
        safeClosePeer();
        syncProtocol = null;
    }

    // -----------------------------
    // SAFE close + state reset
    // -----------------------------
    private void safeClosePeer() {
        try { if (syncProtocol != null) syncProtocol.close(); } catch (Exception ignored) {}
        syncProtocol = null;

        try { if (peer != null) peer.close(); } catch (Exception ignored) {}
        peer = null;

        connectedPeerUserId = null;
        pendingIce.clear();

        iceConnected = false;
        dcOpen = false;
        reconnectScheduled = false;
    }

    private void goToAuthAndClearBackstack() {
        Intent i = new Intent(this, AuthActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    // -----------------------------
    // Reconnect helper (single-shot)
    // -----------------------------
    private void scheduleReconnect(String reason) {
        if (reconnectScheduled) return;
        reconnectScheduled = true;

        final String target = connectedPeerUserId;

        Log.w(TAG_RTC, "scheduleReconnect reason=" + reason + " target=" + target);

        // Close current peer safely
        safeClosePeer();

        // Try reconnect a bit later
        main.postDelayed(() -> {
            reconnectScheduled = false;
            if (signalling != null && target != null) {
                connectToPeerIfNeeded(target);
            }
        }, 900);
    }

    // -----------------------------
    // Signalling + WebRTC wiring
    // -----------------------------
    private void startSignalling(String wsUrl) {
        signalling = new SignallingClient(wsUrl, myUserId, new SignallingClient.Listener() {

            @Override
            public void onJoined(JSONArray peers) {
                runOnUiThread(() ->
                        tvSyncStatus.setText("Signal: Joined. Peers=" + (peers == null ? 0 : peers.length()))
                );
                if (peers != null && peers.length() > 0) {
                    connectToPeerIfNeeded(peers.optString(0, null));
                }
            }

            @Override
            public void onPeerJoined(String userId) {
                runOnUiThread(() -> tvSyncStatus.setText("Signal: Peer joined"));
                connectToPeerIfNeeded(userId);
            }

            @Override
            public void onPeerLeft(String userId) {
                runOnUiThread(() -> tvSyncStatus.setText("Signal: Peer left"));
                if (connectedPeerUserId != null && connectedPeerUserId.equalsIgnoreCase(userId)) {
                    scheduleReconnect("peer-left");
                }
            }

            @Override
            public void onOffer(String from, String sdp) {
                runOnUiThread(() -> tvSyncStatus.setText("Signal: Offer"));
                ensurePeer(from);
                if (peer != null) {
                    peer.onRemoteOffer(sdp);
                    flushPendingIce(from);
                }
            }

            @Override
            public void onAnswer(String from, String sdp) {
                runOnUiThread(() -> tvSyncStatus.setText("Signal: Answer"));
                if (peer != null) {
                    peer.onRemoteAnswer(sdp);
                    flushPendingIce(from);
                }
            }

            @Override
            public void onIce(String from, JSONObject cand) {
                if (cand == null) return;

                // If we don't yet know peer/connected id, queue
                if (peer == null || connectedPeerUserId == null) {
                    queueIce(from, cand);
                    return;
                }

                // Only accept ICE for current connection
                if (!from.equalsIgnoreCase(connectedPeerUserId)) return;

                peer.onRemoteIce(
                        cand.optString("sdpMid", "0"),
                        cand.optInt("sdpMLineIndex", 0),
                        cand.optString("candidate", "")
                );
            }

            @Override
            public void onError(String err) {
                runOnUiThread(() -> tvSyncStatus.setText("Signal/WebRTC error: " + err));
                Log.e(TAG_RTC, "signalling error=" + err);

                // don't instantly close on generic errors; only reconnect for hard failures
                if (err != null && err.toLowerCase().contains("closed")) {
                    scheduleReconnect("signalling-closed");
                }
                scheduleReconnectWithBackoff();
            }



        });

        signalling.connect();
    }

    private void connectToPeerIfNeeded(String otherUserId) {
        if (otherUserId == null || otherUserId.trim().isEmpty()) return;
        if (otherUserId.equalsIgnoreCase(myUserId)) return;

        // Already have an active, usable connection?
        if (peer != null && iceConnected && dcOpen && peer.isDataChannelOpen()) {
            return;
        }

        // IMPORTANT CHANGE:
        // Do NOT treat "not usable" as stale during initial negotiation.
        // Only treat stale if we previously had a working DC (everDcOpen == true).
        if (peer != null && everDcOpen) {
            boolean usable = iceConnected && dcOpen && peer.isDataChannelOpen();
            if (!usable) {
                Log.w(TAG_RTC, "Stale peer (had DC before) -> reconnect");
                scheduleReconnect("stale-peer");
            } else {
                return;
            }
        }

        ensurePeer(otherUserId);

        boolean iAmCaller = myUserId.compareToIgnoreCase(otherUserId) < 0;
        Log.d(TAG_RTC, "tieBreak my=" + myUserId + " other=" + otherUserId + " caller=" + iAmCaller);

        if (iAmCaller) {
            runOnUiThread(() -> tvSyncStatus.setText("WebRTC: creating offer"));
            if (peer != null) peer.startAsCaller();
        } else {
            runOnUiThread(() -> tvSyncStatus.setText("WebRTC: waiting for offer"));
        }
    }

    private void ensurePeer(String otherUserId) {
        if (peer != null) return;

        connectedPeerUserId = otherUserId;

        peer = new WebRtcPeer(this, otherUserId, signalling, new WebRtcPeer.Listener() {

            @Override
            public void onIceConnected() {
                iceConnected = true;
                runOnUiThread(() -> tvSyncStatus.setText("WebRTC: ICE Connected ✅"));
            }

            @Override
            public void onIceDisconnected() {
                // IMPORTANT: do not flap during initial setup
                // If we never had DC open before, ignore transient disconnects (offer/answer timing)
                if (!everDcOpen) {
                    Log.w(TAG_RTC, "ICE disconnected during initial setup -> ignoring");
                    return;
                }

                runOnUiThread(() -> {
                    tvSyncStatus.setText("WebRTC: ICE Disconnected ⚠");
                    scheduleReconnect("ice-disconnected");
                });
            }

            @Override
            public void onIceFailed() {
                runOnUiThread(() -> {
                    tvSyncStatus.setText("WebRTC: ICE Failed ❌");
                    scheduleReconnect("ice-failed");
                });
            }

            @Override
            public void onDataChannelOpen() {
                dcOpen = true;
                everDcOpen = true;

                if (syncProtocol == null) {
                    syncProtocol = new SyncProtocol(HomeActivity.this, otherUserId, peer);
                }
                syncProtocol.onDataChannelOpen();

                // If local changes happened while offline, sync immediately
                if (localDirty) {
                    syncProtocol.sendPoke();
                    localDirty = false;
                    Log.d(TAG_SYNC, "DC open -> flushed localDirty via poke");
                }

                runOnUiThread(() -> tvSyncStatus.setText("DC: OPEN ✅ Sync started"));
            }

            @Override
            public void onDataChannelClosed() {
                // If we never had DC open, let negotiation continue; don't auto-reconnect.
                if (!everDcOpen) {
                    Log.w(TAG_RTC, "DC closed during initial setup -> ignoring");
                    return;
                }

                runOnUiThread(() -> {
                    tvSyncStatus.setText("DC: CLOSED ⚠");
                    scheduleReconnect("dc-closed");
                });
            }

            @Override
            public void onDataChannelMessage(String text) {
                if (syncProtocol != null) syncProtocol.onMessage(text);
            }

            @Override
            public void onError(String err) {
                Log.e(TAG_RTC, "peer err=" + err);
                // do not close here; most errors are already handled by ICE/DC state changes
            }
        });

        peer.createPeerConnection();
        flushPendingIce(otherUserId);
    }

    private void queueIce(String from, JSONObject cand) {
        List<JSONObject> q = pendingIce.get(from);
        if (q == null) q = new ArrayList<>();
        q.add(cand);
        pendingIce.put(from, q);
    }

    private void flushPendingIce(String from) {
        if (peer == null) return;
        List<JSONObject> q = pendingIce.remove(from);
        if (q == null || q.isEmpty()) return;

        for (JSONObject cand : q) {
            peer.onRemoteIce(
                    cand.optString("sdpMid", "0"),
                    cand.optInt("sdpMLineIndex", 0),
                    cand.optString("candidate", "")
            );
        }
    }


    private void scheduleReconnectNow() {
        reconnectAttempt = 0;
        scheduleReconnectWithBackoff();
    }

    private void scheduleReconnectWithBackoff() {
        if (reconnectScheduled) return;
        reconnectScheduled = true;

        long delay = Math.min(8000, 500L * (1L << Math.min(reconnectAttempt, 4))); // 0.5s,1s,2s,4s,8s max
        reconnectAttempt++;

        mainHandler.postDelayed(() -> {
            reconnectScheduled = false;
            tryReconnect();
        }, delay);
    }

    private void tryReconnect() {
        // if already connected, do nothing
        if (signalling != null && signalling.isConnected()) return;

        runOnUiThread(() -> tvSyncStatus.setText("Signal: reconnecting... (" + reconnectAttempt + ")"));

        // reset peer state (important)
        safeClosePeer();
        syncProtocol = null;

        if (signalling == null) {
            startSignalling(WS_URL);
            return;
        }

        signalling.reconnect();
    }


}