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
    private static final String WS_URL  = "ws://10.50.49.9:8080/ws";

    private SessionManager session;
    private AppDatabase db;

    private SignallingClient signalling;
    private WebRtcPeer peer;
    private SyncProtocol syncProtocol;

    private String myUserId;
    private String connectedPeerUserId;

    private SwitchCompat swShowCanceled;
    private BookingAdapter adapter;
    private RecyclerView rv;

    // Status views
    private TextView tvNetStatus;
    private TextView tvSignalStatus;
    private TextView tvWebRtcStatus;
    private TextView tvDcStatus;
    private TextView tvSyncInfo;

    private final Map<String, List<JSONObject>> pendingIce = new HashMap<>();
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private LiveData<List<BookingEntity>> liveSource;
    private SyncBus.Listener syncBusListener;

    // Connection state
    private volatile boolean iceConnected = false;
    private volatile boolean dcOpen = false;
    private volatile boolean everDcOpen = false;  // only treat stale if DC once opened
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

        db.bookingDao()
                .observeAllIncludingCanceled()
                .observe(this, bookings -> {

                    if (syncProtocol != null) {
                        syncProtocol.sendPoke();
                    }

                });
        TextView tvUserInfo = findViewById(R.id.tvUserInfo);

        // status widgets
        tvNetStatus = findViewById(R.id.tvNetStatus);
        tvSignalStatus = findViewById(R.id.tvSignalStatus);
        tvWebRtcStatus = findViewById(R.id.tvWebRtcStatus);
        tvDcStatus = findViewById(R.id.tvDcStatus);
        tvSyncInfo = findViewById(R.id.tvSyncInfo);

        tvNetStatus.setText("Network: checking…");
        tvSignalStatus.setText("Signalling: connecting…");
        tvWebRtcStatus.setText("WebRTC: -");
        tvDcStatus.setText("DataChannel: -");
        tvSyncInfo.setText("Sync: idle");

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
                tvSyncInfo.setText("Sync: forcing now…");
                syncProtocol.sendPoke();
                localDirty = false;
                Toast.makeText(this, "Sync triggered", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2) If signalling is connected but WebRTC/DC isn't ready yet, try to connect to a peer
            if (signalling != null && signalling.isConnected()) {
                tvSyncInfo.setText("Sync: connecting to peer…");

                if (connectedPeerUserId != null) {
                    connectToPeerIfNeeded(connectedPeerUserId);
                } else {
                    tvSignalStatus.setText("Signalling: fetching peers…");
                    signalling.refreshPeers();   // ✅ correct with your FastAPI server
                }

                Toast.makeText(this, "Trying to connect…", Toast.LENGTH_SHORT).show();
                return;
            }

            // 3) Signalling not connected -> reconnect signalling
            tvSignalStatus.setText("Signalling: reconnecting (manual)…");
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
                runOnUiThread(() -> tvNetStatus.setText("Network: available ✅"));
                scheduleReconnectNow();
            }

            @Override
            public void onLost(Network network) {
                runOnUiThread(() -> tvNetStatus.setText("Network: lost ⚠"));
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
    private void safeClosePeer(boolean resetUi) {
        // close protocol first (it may have threads/executor)
        try { if (syncProtocol != null) syncProtocol.close(); } catch (Exception ignored) {}
        syncProtocol = null;

        // close peer connection
        try { if (peer != null) peer.close(); } catch (Exception ignored) {}
        peer = null;

        // reset ids + queues
        connectedPeerUserId = null;
        pendingIce.clear();

        // reset state flags
        iceConnected = false;
        dcOpen = false;

        // IMPORTANT: don't reset everDcOpen (your logic relies on it)

        if (resetUi) {
            resetStatusUi("peer closed");
        }
    }

    private void safeClosePeer() {
        safeClosePeer(true);
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

        runOnUiThread(() -> {
            tvWebRtcStatus.setText("WebRTC: reconnecting…");
            tvDcStatus.setText("DataChannel: reconnecting…");
            tvSyncInfo.setText("Sync: waiting…");
        });

        // close without overwriting UI
        safeClosePeer(false);

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
                        tvSignalStatus.setText("Signalling: joined ✅ peers=" + (peers == null ? 0 : peers.length()))
                );
                if (peers != null && peers.length() > 0) {
                    connectToPeerIfNeeded(peers.optString(0, null));
                }
            }

            @Override
            public void onPeerJoined(String userId) {
                runOnUiThread(() -> tvSignalStatus.setText("Signalling: peer joined ✅"));
                connectToPeerIfNeeded(userId);
            }

            @Override
            public void onPeerLeft(String userId) {
                runOnUiThread(() -> tvSignalStatus.setText("Signalling: peer left ⚠"));
                if (connectedPeerUserId != null && connectedPeerUserId.equalsIgnoreCase(userId)) {
                    scheduleReconnect("peer-left");
                }
            }

            @Override
            public void onOffer(String from, String sdp) {
                runOnUiThread(() -> tvSignalStatus.setText("Signalling: offer received"));
                ensurePeer(from);
                if (peer != null) {
                    peer.onRemoteOffer(sdp);
                    flushPendingIce(from);
                }
            }

            @Override
            public void onAnswer(String from, String sdp) {
                runOnUiThread(() -> tvSignalStatus.setText("Signalling: answer received"));
                if (peer != null) {
                    peer.onRemoteAnswer(sdp);
                    flushPendingIce(from);
                }
            }

            @Override
            public void onIce(String from, JSONObject cand) {
                if (cand == null) return;

                if (peer == null || connectedPeerUserId == null) {
                    queueIce(from, cand);
                    return;
                }

                if (!from.equalsIgnoreCase(connectedPeerUserId)) return;

                peer.onRemoteIce(
                        cand.optString("sdpMid", "0"),
                        cand.optInt("sdpMLineIndex", 0),
                        cand.optString("candidate", "")
                );
            }

            @Override
            public void onError(String err) {
                runOnUiThread(() -> tvSignalStatus.setText("Signalling: error ❌ " + err));
                Log.e(TAG_RTC, "signalling error=" + err);

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

        if (peer != null && iceConnected && dcOpen && peer.isDataChannelOpen()) {
            return;
        }

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
            runOnUiThread(() -> tvWebRtcStatus.setText("WebRTC: creating offer…"));
            if (peer != null) peer.startAsCaller();
        } else {
            runOnUiThread(() -> tvWebRtcStatus.setText("WebRTC: waiting for offer…"));
        }
    }

    private void ensurePeer(String otherUserId) {
        if (peer != null) return;

        connectedPeerUserId = otherUserId;

        peer = new WebRtcPeer(this, otherUserId, signalling, new WebRtcPeer.Listener() {

            @Override
            public void onIceConnected() {
                iceConnected = true;
                runOnUiThread(() -> tvWebRtcStatus.setText("WebRTC: ICE connected ✅"));
            }

            @Override
            public void onIceDisconnected() {
                if (!everDcOpen) {
                    Log.w(TAG_RTC, "ICE disconnected during initial setup -> ignoring");
                    return;
                }

                runOnUiThread(() -> {
                    tvWebRtcStatus.setText("WebRTC: ICE disconnected ⚠");
                    scheduleReconnect("ice-disconnected");
                });
            }

            @Override
            public void onIceFailed() {
                runOnUiThread(() -> {
                    tvWebRtcStatus.setText("WebRTC: ICE failed ❌");
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

                if (localDirty) {
                    syncProtocol.sendPoke();
                    localDirty = false;
                    runOnUiThread(() -> tvSyncInfo.setText("Sync: flushed local changes ✅"));
                } else {
                    runOnUiThread(() -> tvSyncInfo.setText("Sync: ready ✅"));
                }

                runOnUiThread(() -> tvDcStatus.setText("DataChannel: OPEN ✅"));
            }

            @Override
            public void onDataChannelClosed() {
                if (!everDcOpen) {
                    Log.w(TAG_RTC, "DC closed during initial setup -> ignoring");
                    return;
                }

                runOnUiThread(() -> {
                    tvDcStatus.setText("DataChannel: CLOSED ⚠");
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

        long delay = Math.min(8000, 500L * (1L << Math.min(reconnectAttempt, 4)));
        reconnectAttempt++;

        mainHandler.postDelayed(() -> {
            reconnectScheduled = false;
            tryReconnect();
        }, delay);
    }

    private void tryReconnect() {
        if (signalling != null && signalling.isConnected()) return;

        runOnUiThread(() -> {
            tvSignalStatus.setText("Signalling: reconnecting… (" + reconnectAttempt + ")");
            tvWebRtcStatus.setText("WebRTC: -");
            tvDcStatus.setText("DataChannel: -");
            tvSyncInfo.setText("Sync: idle");
        });

        // close without overwriting the UI we just set
        safeClosePeer(false);

        if (signalling == null) {
            startSignalling(WS_URL);
            return;
        }

        signalling.reconnect();
    }

    private void resetStatusUi(String reason) {
        runOnUiThread(() -> {
            // keep network text as-is (it’s managed by NetworkCallback)
            tvSignalStatus.setText("Signalling: -");
            tvWebRtcStatus.setText("WebRTC: -");
            tvDcStatus.setText("DataChannel: -");
            tvSyncInfo.setText(reason == null ? "Sync: idle" : ("Sync: idle (" + reason + ")"));
        });
    }
}