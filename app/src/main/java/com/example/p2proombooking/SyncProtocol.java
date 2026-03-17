package com.example.p2proombooking;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class SyncProtocol {

    private static final String TAG = "SYNC";

    private static final long MIN_REQUEST_GAP_MS = 600;
    private static final long POKE_COALESCE_MS = 250;

    // Keep only batch-level dedup memory
    private static final int MAX_SEEN_BATCH_IDS = 200;

    private final String remoteUserId;
    private final WebRtcPeer rtcPeer;

    private final BookingDao bookingDao;
    private final PeerStateDao peerStateDao;
    private final SyncEngine syncEngine;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private volatile long lastRequestAtMs = 0;
    private volatile boolean requestScheduled = false;

    // duplicate-batch protection only
    private final ArrayDeque<String> seenBatchOrder = new ArrayDeque<>();
    private final java.util.HashSet<String> seenBatchIds = new java.util.HashSet<>();

    public SyncProtocol(Context ctx, String remoteUserId, WebRtcPeer peer) {
        this.remoteUserId = remoteUserId;
        this.rtcPeer = peer;

        AppDatabase db = AppDatabase.getInstance(ctx.getApplicationContext());
        bookingDao = db.bookingDao();
        peerStateDao = db.peerStateDao();
        syncEngine = new SyncEngine(ctx.getApplicationContext());
    }

    public void onDataChannelOpen() {
        requestSyncDebounced(0);
    }

    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        try {
            io.shutdownNow();
        } catch (Exception ignored) {
        }
    }

    public void onMessage(String text) {
        if (text == null || closed.get()) return;

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

    public void sendPoke() {
        io.execute(() -> {
            if (closed.get()) return;

            if (!isDcReady()) {
                Log.d(TAG, "TX poke skipped (DC not open) to=" + remoteUserId);
                return;
            }

            try {
                JSONObject obj = new JSONObject();
                obj.put("type", "poke");
                rtcPeer.sendText(obj.toString());
                Log.d(TAG, "TX poke to=" + remoteUserId);
            } catch (Exception e) {
                Log.e(TAG, "sendPoke error", e);
            }
        });
    }

    private void requestSyncDebounced(long delayMs) {
        io.execute(() -> {
            if (closed.get()) return;
            if (requestScheduled) return;

            requestScheduled = true;

            try {
                if (delayMs > 0) {
                    Thread.sleep(delayMs);
                }

                long now = SystemClock.elapsedRealtime();
                long sinceLast = now - lastRequestAtMs;
                if (sinceLast < MIN_REQUEST_GAP_MS) {
                    Thread.sleep(MIN_REQUEST_GAP_MS - sinceLast);
                }
            } catch (InterruptedException ignored) {
                if (closed.get()) {
                    requestScheduled = false;
                    return;
                }
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

    private void handleSyncRequest(JSONObject msg) {
        if (closed.get()) return;

        if (!isDcReady()) {
            Log.d(TAG, "handleSyncRequest: DC not open -> skip response");
            return;
        }

        try {
            long since = msg.optLong("since", 0L);

            List<BookingEntity> changes = bookingDao.getChangesSince(since);
            if (changes == null || changes.isEmpty()) {
                changes = bookingDao.getPendingSync();
            }

            JSONArray arr = new JSONArray();
            long maxUpdated = 0L;

            if (changes != null) {
                for (BookingEntity b : changes) {
                    arr.put(BookingJsonMapper.toJson(b));
                    maxUpdated = Math.max(maxUpdated, b.updatedAt);
                }
            }

            JSONObject response = new JSONObject();
            response.put("type", "sync_batch");
            response.put("batchId", UUID.randomUUID().toString());
            response.put("items", arr);
            response.put("maxUpdatedAt", maxUpdated);
            response.put("serverTime", System.currentTimeMillis());

            rtcPeer.sendText(response.toString());

            Log.d(TAG, "TX sync_batch count=" + (changes == null ? 0 : changes.size())
                    + " since=" + since
                    + " maxUpdatedAt=" + maxUpdated);
        } catch (Exception e) {
            Log.e(TAG, "handleSyncRequest error", e);
        }
    }

    private void handleSyncBatch(JSONObject msg) {
        if (closed.get()) return;

        try {
            String batchId = msg.optString("batchId", "");
            long advertisedMaxUpdated = msg.optLong("maxUpdatedAt", 0L);

            JSONArray arr = msg.optJSONArray("items");
            if (arr == null) {
                Log.w(TAG, "sync_batch missing items");
                return;
            }

            // Duplicate batch? Do NOT apply again. But do ACK again.
            if (!batchId.isEmpty() && alreadySeenBatch(batchId)) {
                Log.d(TAG, "Duplicate batch ignored batchId=" + batchId);

                long ackMax = advertisedMaxUpdated;
                if (ackMax <= 0) {
                    PeerStateEntity state = peerStateDao.get(remoteUserId);
                    ackMax = (state != null) ? state.lastSyncedUpdatedAt : 0L;
                }

                sendAck(batchId, ackMax, true);
                return;
            }

            if (!batchId.isEmpty()) {
                rememberBatch(batchId);
            }

            long maxUpdated = 0L;

            for (int i = 0; i < arr.length(); i++) {
                BookingEntity remote = BookingJsonMapper.fromJson(arr.getJSONObject(i));
                syncEngine.applyRemoteBooking(remote);
                maxUpdated = Math.max(maxUpdated, remote.updatedAt);
            }

            if (maxUpdated <= 0) {
                maxUpdated = advertisedMaxUpdated;
            }

            Log.d(TAG, "Applied remote items=" + arr.length()
                    + " batchId=" + batchId
                    + " maxUpdatedAt=" + maxUpdated);

            SyncBus.notifyRemoteChange();

            sendAck(batchId, maxUpdated, false);

        } catch (Exception e) {
            Log.e(TAG, "handleSyncBatch error", e);
        }
    }

    private void sendAck(String batchId, long maxUpdated, boolean duplicateBatch) {
        if (!isDcReady()) {
            Log.d(TAG, "ACK skipped (DC not open)");
            return;
        }

        try {
            JSONObject ack = new JSONObject();
            ack.put("type", "sync_ack");
            ack.put("batchId", batchId == null ? JSONObject.NULL : batchId);
            ack.put("maxUpdatedAt", maxUpdated);

            rtcPeer.sendText(ack.toString());

            Log.d(TAG, "TX sync_ack maxUpdatedAt=" + maxUpdated
                    + " batchId=" + batchId
                    + " duplicateBatch=" + duplicateBatch);
        } catch (Exception e) {
            Log.e(TAG, "sendAck error", e);
        }
    }

    private void handleSyncAck(JSONObject msg) {
        if (closed.get()) return;

        try {
            String batchId = msg.optString("batchId", "");
            long max = msg.optLong("maxUpdatedAt", 0L);

            if (max <= 0) {
                Log.d(TAG, "ACK maxUpdatedAt<=0 ignored batchId=" + batchId);
                return;
            }

            PeerStateEntity state = peerStateDao.get(remoteUserId);
            long prev = (state != null) ? state.lastSyncedUpdatedAt : 0L;

            if (max < prev) {
                Log.w(TAG, "ACK out-of-order ignored prev=" + prev + " new=" + max + " batchId=" + batchId);
                return;
            }

            PeerStateEntity up = new PeerStateEntity();
            up.peerUserId = remoteUserId;
            up.lastSyncedUpdatedAt = max;
            peerStateDao.upsert(up);

            bookingDao.markSyncedUpTo(max, System.currentTimeMillis());

            Log.d(TAG, "ACK processed up to=" + max + " batchId=" + batchId);

        } catch (Exception e) {
            Log.e(TAG, "handleSyncAck error", e);
        }
    }

    private boolean alreadySeenBatch(String batchId) {
        synchronized (seenBatchIds) {
            return seenBatchIds.contains(batchId);
        }
    }

    private void rememberBatch(String batchId) {
        synchronized (seenBatchIds) {
            if (seenBatchIds.add(batchId)) {
                seenBatchOrder.addLast(batchId);

                while (seenBatchOrder.size() > MAX_SEEN_BATCH_IDS) {
                    String old = seenBatchOrder.removeFirst();
                    seenBatchIds.remove(old);
                }
            }
        }
    }

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