package com.example.p2proombooking;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SyncProtocol (hardened)
 *
 * Fixes applied:
 * 1) Debounces sync_request to avoid request storms (poke spam / reconnect loops).
 * 2) Never sends on a closed / not-open DataChannel (prevents cascading failures).
 * 3) Adds close() to shutdown executor cleanly when peer is torn down.
 * 4) Coalesces multiple "poke" into a single sync_request.
 * 5) Safer ACK / state updates.
 */
public class SyncProtocol {

    private static final String TAG = "SYNC";

    // Debounce windows (tweak if needed)
    private static final long MIN_REQUEST_GAP_MS = 600;   // minimum gap between sync_request sends
    private static final long POKE_COALESCE_MS   = 250;   // coalesce multiple pokes quickly

    private final Context context;
    private final String remoteUserId;
    private final WebRtcPeer rtcPeer;

    private final BookingDao bookingDao;
    private final PeerStateDao peerStateDao;
    private final SyncEngine syncEngine;

    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private final AtomicBoolean closed = new AtomicBoolean(false);

    // Debounce/coalesce
    private volatile long lastRequestAtMs = 0;
    private volatile boolean requestScheduled = false;

    public SyncProtocol(Context ctx, String remoteUserId, WebRtcPeer peer) {
        this.context = ctx.getApplicationContext();
        this.remoteUserId = remoteUserId;
        this.rtcPeer = peer;

        AppDatabase db = AppDatabase.getInstance(ctx);
        bookingDao = db.bookingDao();
        peerStateDao = db.peerStateDao();
        syncEngine = new SyncEngine(ctx);
    }

    /** Call from HomeActivity when DC opens */
    public void onDataChannelOpen() {
        // First sync immediately (but still protected by isDcReady + debounce)
        requestSyncDebounced(0);
    }

    /** Call from HomeActivity when Activity/Peer is going away */
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        try { io.shutdownNow(); } catch (Exception ignored) {}
    }

    public void onMessage(String text) {
        if (text == null) return;

        Log.d(TAG, "RX raw=" + text);

        try {
            JSONObject msg = new JSONObject(text);
            String type = msg.optString("type", "");

            switch (type) {
                case "sync_request":
                    io.execute(() -> handleSyncRequest(msg));
                    break;

                case "sync_batch":
                    io.execute(() -> handleSyncBatch(msg));
                    break;

                case "sync_ack":
                    io.execute(() -> handleSyncAck(msg));
                    break;

                case "poke":
                    // IMPORTANT: don't immediately spam requestSync()
                    // Coalesce pokes and issue a single debounced sync_request.
                    Log.d(TAG, "RX poke -> debounced requestSync()");
                    requestSyncDebounced(POKE_COALESCE_MS);
                    break;

                default:
                    Log.w(TAG, "Unknown msg type=" + type);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "parse error", e);
        }
    }

    /**
     * Public "poke" send used by HomeActivity when local DB changes.
     * This only sends if DC is open.
     */
    public void sendPoke() {
        io.execute(() -> {
            if (closed.get()) return;
            if (!isDcReady()) {
                Log.d(TAG, "TX poke skipped (DC not open) to=" + remoteUserId);
                return;
            }

            try {
                rtcPeer.sendText("{\"type\":\"poke\"}");
                Log.d(TAG, "TX poke to=" + remoteUserId);
            } catch (Exception e) {
                Log.e(TAG, "sendPoke error", e);
            }
        });
    }

    // -----------------------------
    // Debounced sync request
    // -----------------------------

    private void requestSyncDebounced(long delayMs) {
        // Schedule on io thread to keep ordering consistent
        io.execute(() -> {
            if (closed.get()) return;

            // If already scheduled, don't schedule again (coalesce)
            if (requestScheduled) return;

            requestScheduled = true;

            // Simple delay by sleeping inside io thread (single-thread executor).
            // Because we coalesce, this is OK for your small app.
            if (delayMs > 0) {
                try { Thread.sleep(delayMs); } catch (InterruptedException ignored) {}
            }

            // Enforce minimum gap between requests
            long now = SystemClock.elapsedRealtime();
            long sinceLast = now - lastRequestAtMs;
            if (sinceLast < MIN_REQUEST_GAP_MS) {
                long extra = MIN_REQUEST_GAP_MS - sinceLast;
                try { Thread.sleep(extra); } catch (InterruptedException ignored) {}
            }

            requestScheduled = false;
            requestSyncInternal();
        });
    }

    private void requestSyncInternal() {
        if (closed.get()) return;

        if (!isDcReady()) {
            Log.d(TAG, "TX sync_request skipped (DC not open) to=" + remoteUserId);
            return;
        }

        try {
            PeerStateEntity state = peerStateDao.get(remoteUserId);
            long since = (state != null) ? state.lastSyncedUpdatedAt : 0L;

            JSONObject obj = new JSONObject();
            obj.put("type", "sync_request");
            obj.put("since", since);

            rtcPeer.sendText(obj.toString());
            lastRequestAtMs = SystemClock.elapsedRealtime();

            Log.d(TAG, "TX sync_request since=" + since + " to=" + remoteUserId);

        } catch (Exception e) {
            Log.e(TAG, "requestSync error", e);
        }
    }

    private boolean isDcReady() {
        try {
            return rtcPeer != null && rtcPeer.isDataChannelOpen();
        } catch (Exception e) {
            return false;
        }
    }

    // -----------------------------
    // Handlers
    // -----------------------------

    private void handleSyncRequest(JSONObject msg) {
        if (closed.get()) return;

        // If our DC is not open, don't try to reply (avoid exceptions during teardown)
        if (!isDcReady()) {
            Log.d(TAG, "handleSyncRequest: DC not open -> skip response");
            return;
        }

        try {
            long since = msg.optLong("since", 0L);

            // Prefer incremental changes. If none, you were using pendingSync fallback.
            List<BookingEntity> changes = bookingDao.getChangesSince(since);
            if (changes == null || changes.isEmpty()) {
                changes = bookingDao.getPendingSync();
            }

            JSONArray arr = new JSONArray();
            if (changes != null) {
                for (BookingEntity b : changes) {
                    arr.put(BookingJsonMapper.toJson(b));
                }
            }

            JSONObject response = new JSONObject();
            response.put("type", "sync_batch");
            response.put("items", arr);
            response.put("serverTime", System.currentTimeMillis());

            rtcPeer.sendText(response.toString());
            Log.d(TAG, "TX sync_batch count=" + (changes == null ? 0 : changes.size()) + " since=" + since);

        } catch (Exception e) {
            Log.e(TAG, "handleSyncRequest error", e);
        }
    }

    private void handleSyncBatch(JSONObject msg) {
        if (closed.get()) return;

        try {
            JSONArray arr = msg.optJSONArray("items");
            if (arr == null) return;

            long maxUpdated = 0;

            for (int i = 0; i < arr.length(); i++) {
                BookingEntity remote = BookingJsonMapper.fromJson(arr.getJSONObject(i));
                syncEngine.applyRemoteBooking(remote);
                maxUpdated = Math.max(maxUpdated, remote.updatedAt);
            }

            Log.d(TAG, "Applied remote items=" + arr.length());

            // Notify UI layer; LiveData will refresh anyway
            SyncBus.notifyRemoteChange();

            // ACK only if DC open
            if (isDcReady()) {
                JSONObject ack = new JSONObject();
                ack.put("type", "sync_ack");
                ack.put("maxUpdatedAt", maxUpdated);
                rtcPeer.sendText(ack.toString());
                Log.d(TAG, "TX sync_ack maxUpdatedAt=" + maxUpdated);
            } else {
                Log.d(TAG, "ACK skipped (DC not open)");
            }

        } catch (Exception e) {
            Log.e(TAG, "handleSyncBatch error", e);
        }
    }

    private void handleSyncAck(JSONObject msg) {
        if (closed.get()) return;

        try {
            long max = msg.optLong("maxUpdatedAt", 0L);

            // Only advance state if max is meaningful
            if (max <= 0) {
                Log.d(TAG, "ACK maxUpdatedAt<=0 ignored");
                return;
            }

            PeerStateEntity state = peerStateDao.get(remoteUserId);
            long prev = (state != null) ? state.lastSyncedUpdatedAt : 0L;

            if (max < prev) {
                // Ignore out-of-order ack
                Log.w(TAG, "ACK out-of-order ignored prev=" + prev + " new=" + max);
                return;
            }

            PeerStateEntity up = new PeerStateEntity();
            up.peerUserId = remoteUserId;
            up.lastSyncedUpdatedAt = max;
            peerStateDao.upsert(up);

            bookingDao.markSyncedUpTo(max, System.currentTimeMillis());
            Log.d(TAG, "ACK processed up to=" + max);

        } catch (Exception e) {
            Log.e(TAG, "handleSyncAck error", e);
        }
    }

    // -----------------------------
    // JSON Mapper
    // -----------------------------
    public static class BookingJsonMapper {

        public static JSONObject toJson(BookingEntity b) throws Exception {
            JSONObject o = new JSONObject();
            o.put("bookingId", b.bookingId);
            o.put("roomId", b.roomId);
            o.put("startUtc", b.startUtc);
            o.put("endUtc", b.endUtc);
            o.put("status", b.status);
            o.put("version", b.version);
            o.put("updatedAt", b.updatedAt);
            o.put("createdAt", b.createdAt);
            o.put("createdByUserId", b.createdByUserId);
            o.put("createdByDeviceId", b.createdByDeviceId);
            o.put("canceledByUserId", b.canceledByUserId == null ? JSONObject.NULL : b.canceledByUserId);
            o.put("canceledAt", b.canceledAt);
            o.put("deletedFlag", b.deletedFlag);
            return o;
        }

        public static BookingEntity fromJson(JSONObject o) throws Exception {
            BookingEntity b = new BookingEntity();
            b.bookingId = o.getString("bookingId");
            b.roomId = o.getString("roomId");
            b.startUtc = o.getLong("startUtc");
            b.endUtc = o.getLong("endUtc");
            b.status = o.getString("status");
            b.version = o.getInt("version");
            b.updatedAt = o.getLong("updatedAt");
            b.createdAt = o.optLong("createdAt", 0L);
            b.createdByUserId = o.getString("createdByUserId");
            b.createdByDeviceId = o.getString("createdByDeviceId");

            if (o.has("canceledByUserId") && !o.isNull("canceledByUserId")) {
                b.canceledByUserId = o.getString("canceledByUserId");
            }
            b.canceledAt = o.optLong("canceledAt", 0L);
            b.deletedFlag = o.optInt("deletedFlag", 0);

            b.syncFlag = BookingConstants.SYNCED;
            b.lastSyncedAt = System.currentTimeMillis();
            return b;
        }
    }
}