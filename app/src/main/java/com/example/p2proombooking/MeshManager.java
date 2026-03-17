package com.example.p2proombooking;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MeshManager {

    public interface Listener {
        void onSignalStatusChanged(String text);
        void onWebRtcStatusChanged(String text);
        void onDataChannelStatusChanged(String text);
        void onSyncStatusChanged(String text);
    }

    private static final String TAG_RTC = "RTC";
    private static final String TAG_SYNC = "SYNC";

    private static final long SDP_DUP_WINDOW_MS = 2000L;
    private static final long PEER_RECONNECT_DELAY_MS = 900L;

    private final Context context;
    private final String myUserId;
    private final String wsUrl;
    private final Listener listener;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private SignallingClient signalling;

    // Active peer objects
    private final Map<String, WebRtcPeer> peers = new HashMap<>();
    private final Map<String, SyncProtocol> protocols = new HashMap<>();
    private final Map<String, List<JSONObject>> pendingIce = new HashMap<>();

    // Status tracking
    private final Set<String> knownPeers = new HashSet<>();
    private final Set<String> iceConnectedPeers = new HashSet<>();
    private final Set<String> dcOpenPeers = new HashSet<>();
    private final Set<String> everDcOpenPeers = new HashSet<>();
    private final Set<String> reconnectingPeers = new HashSet<>();

    // Duplicate SDP suppression
    private final Map<String, Long> offerSeenAt = new HashMap<>();
    private final Map<String, Long> answerSeenAt = new HashMap<>();

    // Generation counter:
    // each recreated peer gets a higher generation;
    // stale callbacks from old peer instances are ignored.
    private final Map<String, Integer> peerGenerations = new HashMap<>();

    private boolean signallingReconnectScheduled = false;
    private boolean localDirty = false;
    private int signallingReconnectAttempt = 0;

    public MeshManager(Context context, String myUserId, String wsUrl, Listener listener) {
        this.context = context.getApplicationContext();
        this.myUserId = myUserId;
        this.wsUrl = wsUrl;
        this.listener = listener;
    }

    public void start() {
        startSignalling();
    }

    public void stop() {
        safeCloseAllPeers();

        if (signalling != null) {
            try {
                signalling.disconnect();
            } catch (Exception ignored) {
            }
        }

        updatePeerStatusUi();
    }

    public void onNetworkAvailable() {
        scheduleSignallingReconnectNow();
    }

    public void onNetworkLost() {
        if (listener != null) {
            listener.onSignalStatusChanged("Signalling: network lost");
        }
    }

    public void syncNow() {
        if (!protocols.isEmpty() && !dcOpenPeers.isEmpty()) {
            if (listener != null) {
                listener.onSyncStatusChanged("Sync: forcing now…");
            }
            broadcastPokeIfPossible();
            localDirty = false;
            return;
        }

        if (signalling != null && signalling.isConnected()) {
            if (listener != null) {
                listener.onSignalStatusChanged("Signalling: refreshing peers…");
            }
            signalling.refreshPeers();
            return;
        }

        if (listener != null) {
            listener.onSignalStatusChanged("Signalling: reconnecting…");
        }
        scheduleSignallingReconnectNow();
    }

    public void onLocalChange() {
        localDirty = true;
        Log.d(TAG_SYNC, "Local change -> dirty=true");

        if (!dcOpenPeers.isEmpty()) {
            broadcastPokeIfPossible();
            localDirty = false;
            Log.d(TAG_SYNC, "Broadcast poke sent -> dirty=false");
        } else {
            Log.d(TAG_SYNC, "No open DC -> sync later");
        }
    }

    public int getConnectedPeerCount() {
        return dcOpenPeers.size();
    }

    public int getTotalPeerCount() {
        return peers.size();
    }

    // =========================================================
    // Signalling
    // =========================================================

    private void startSignalling() {
        signalling = new SignallingClient(wsUrl, myUserId, new SignallingClient.Listener() {
            @Override
            public void onJoined(JSONArray peersArray) {
                if (listener != null) {
                    listener.onSignalStatusChanged(
                            "Signalling: joined ✅ peers=" + (peersArray == null ? 0 : peersArray.length())
                    );
                }
                handleJoinedPeers(peersArray);
            }

            @Override
            public void onPeerJoined(String userId) {
                String normalized = normalizeUserId(userId);
                if (normalized == null) return;

                knownPeers.add(normalized);

                if (listener != null) {
                    listener.onSignalStatusChanged("Signalling: peer joined ✅ " + normalized);
                }

                connectToPeerIfNeeded(normalized);
            }

            @Override
            public void onPeerLeft(String userId) {
                String normalized = normalizeUserId(userId);
                if (normalized == null) return;

                knownPeers.remove(normalized);

                if (listener != null) {
                    listener.onSignalStatusChanged("Signalling: peer left ⚠ " + normalized);
                }

                removePeer(normalized, true);
            }

            @Override
            public void onOffer(String from, String sdp) {
                String normalized = normalizeUserId(from);
                if (normalized == null) return;

                if (isDuplicateSdp(offerSeenAt, normalized)) {
                    Log.w(TAG_RTC, "Duplicate offer ignored from=" + normalized);
                    return;
                }

                knownPeers.add(normalized);

                if (listener != null) {
                    listener.onSignalStatusChanged("Signalling: offer from " + normalized);
                }

                ensurePeer(normalized);

                WebRtcPeer peer = peers.get(normalized);
                if (peer != null) {
                    peer.onRemoteOffer(sdp);
                    flushPendingIce(normalized);
                }
            }

            @Override
            public void onAnswer(String from, String sdp) {
                String normalized = normalizeUserId(from);
                if (normalized == null) return;

                if (isDuplicateSdp(answerSeenAt, normalized)) {
                    Log.w(TAG_RTC, "Duplicate answer ignored from=" + normalized);
                    return;
                }

                WebRtcPeer peer = peers.get(normalized);
                if (peer == null) {
                    Log.w(TAG_RTC, "Answer for missing peer ignored from=" + normalized);
                    return;
                }

                if (listener != null) {
                    listener.onSignalStatusChanged("Signalling: answer from " + normalized);
                }

                peer.onRemoteAnswer(sdp);
                flushPendingIce(normalized);
            }

            @Override
            public void onIce(String from, JSONObject cand) {
                String normalized = normalizeUserId(from);
                if (normalized == null || cand == null) return;

                WebRtcPeer peer = peers.get(normalized);
                if (peer == null) {
                    queueIce(normalized, cand);
                    return;
                }

                peer.onRemoteIce(
                        cand.optString("sdpMid", "0"),
                        cand.optInt("sdpMLineIndex", 0),
                        cand.optString("candidate", "")
                );
            }

            @Override
            public void onError(String msg) {
                Log.e(TAG_RTC, "signalling error=" + msg);

                if (listener != null) {
                    listener.onSignalStatusChanged("Signalling: error ❌ " + msg);
                }

                scheduleSignallingReconnectWithBackoff();
            }
        });

        signalling.connect();
    }

    private void handleJoinedPeers(JSONArray peersArray) {
        Set<String> current = new HashSet<>();

        if (peersArray != null) {
            for (int i = 0; i < peersArray.length(); i++) {
                String uid = normalizeUserId(peersArray.optString(i, null));
                if (uid == null) continue;
                current.add(uid);
            }
        }

        for (String uid : current) {
            knownPeers.add(uid);
            connectToPeerIfNeeded(uid);
        }

        Set<String> stale = new HashSet<>(knownPeers);
        stale.removeAll(current);

        for (String uid : stale) {
            knownPeers.remove(uid);
            removePeer(uid, false);
        }

        updatePeerStatusUi();
    }

    private String normalizeUserId(String userId) {
        if (userId == null) return null;
        userId = userId.trim();
        if (userId.isEmpty()) return null;
        if (userId.equalsIgnoreCase(myUserId)) return null;
        return userId;
    }

    private boolean isDuplicateSdp(Map<String, Long> store, String peerId) {
        long now = System.currentTimeMillis();
        Long prev = store.get(peerId);
        store.put(peerId, now);
        return prev != null && (now - prev) < SDP_DUP_WINDOW_MS;
    }

    // =========================================================
    // Peer connect / ensure
    // =========================================================

    private void connectToPeerIfNeeded(String otherUserId) {
        String normalized = normalizeUserId(otherUserId);
        if (normalized == null) return;

        WebRtcPeer existing = peers.get(normalized);
        if (existing != null) {
            boolean usable = dcOpenPeers.contains(normalized) && existing.isDataChannelOpen();
            if (usable) {
                return;
            }

            if (reconnectingPeers.contains(normalized)) {
                Log.d(TAG_RTC, "connectToPeerIfNeeded ignored: reconnect already in progress for " + normalized);
                return;
            }

            if (everDcOpenPeers.contains(normalized)) {
                Log.w(TAG_RTC, "Stale peer -> reconnect " + normalized);
                schedulePeerReconnect(normalized, "stale-peer");
                return;
            }
        }

        ensurePeer(normalized);

        boolean iAmCaller = myUserId.compareToIgnoreCase(normalized) < 0;
        Log.d(TAG_RTC, "tieBreak my=" + myUserId + " other=" + normalized + " caller=" + iAmCaller);

        if (iAmCaller) {
            WebRtcPeer peer = peers.get(normalized);
            if (peer != null) {
                peer.startAsCaller();
            }
        }

        updatePeerStatusUi();
    }

    private void ensurePeer(String otherUserId) {
        String normalized = normalizeUserId(otherUserId);
        if (normalized == null) return;
        if (peers.containsKey(normalized)) return;

        final String peerId = normalized;
        final int generation = nextGeneration(peerId);

        final WebRtcPeer peer = new WebRtcPeer(context, peerId, signalling, new WebRtcPeer.Listener() {
            @Override
            public void onDataChannelMessage(String text) {
                if (!isCurrentGeneration(peerId, generation)) {
                    Log.w(TAG_RTC, "Ignoring stale DC message callback from=" + peerId);
                    return;
                }

                SyncProtocol protocol = protocols.get(peerId);
                if (protocol != null) {
                    protocol.onMessage(text);
                }
            }

            @Override
            public void onIceConnected() {
                if (!isCurrentGeneration(peerId, generation)) {
                    Log.w(TAG_RTC, "Ignoring stale ICE connected callback from=" + peerId);
                    return;
                }

                iceConnectedPeers.add(peerId);
                Log.d(TAG_RTC, "ICE connected -> " + peerId);
                updatePeerStatusUi();
            }

            @Override
            public void onIceDisconnected() {
                if (!isCurrentGeneration(peerId, generation)) {
                    Log.w(TAG_RTC, "Ignoring stale ICE disconnected callback from=" + peerId);
                    return;
                }

                if (!everDcOpenPeers.contains(peerId)) {
                    Log.w(TAG_RTC, "ICE disconnected during setup -> ignoring " + peerId);
                    return;
                }

                schedulePeerReconnect(peerId, "ice-disconnected");
            }

            @Override
            public void onIceFailed() {
                if (!isCurrentGeneration(peerId, generation)) {
                    Log.w(TAG_RTC, "Ignoring stale ICE failed callback from=" + peerId);
                    return;
                }

                schedulePeerReconnect(peerId, "ice-failed");
            }

            @Override
            public void onDataChannelOpen() {
                if (!isCurrentGeneration(peerId, generation)) {
                    Log.w(TAG_RTC, "Ignoring stale DC open callback from=" + peerId);
                    return;
                }

                dcOpenPeers.add(peerId);
                everDcOpenPeers.add(peerId);
                reconnectingPeers.remove(peerId);

                SyncProtocol protocol = protocols.get(peerId);
                if (protocol == null) {
                    WebRtcPeer livePeer = peers.get(peerId);
                    if (livePeer == null) {
                        Log.w(TAG_RTC, "DC opened but live peer missing for " + peerId);
                        return;
                    }

                    protocol = new SyncProtocol(context, peerId, livePeer);
                    protocols.put(peerId, protocol);
                }

                protocol.onDataChannelOpen();

                if (localDirty) {
                    protocol.sendPoke();
                    localDirty = false;
                    if (listener != null) {
                        listener.onSyncStatusChanged("Sync: flushed local changes ✅");
                    }
                } else {
                    if (listener != null) {
                        listener.onSyncStatusChanged("Sync: ready ✅");
                    }
                }

                updatePeerStatusUi();
            }

            @Override
            public void onDataChannelClosed() {
                if (!isCurrentGeneration(peerId, generation)) {
                    Log.w(TAG_RTC, "Ignoring stale DC closed callback from=" + peerId);
                    return;
                }

                if (!everDcOpenPeers.contains(peerId)) {
                    Log.w(TAG_RTC, "DC closed during setup -> ignoring " + peerId);
                    return;
                }

                schedulePeerReconnect(peerId, "dc-closed");
            }

            @Override
            public void onError(String err) {
                if (!isCurrentGeneration(peerId, generation)) {
                    Log.w(TAG_RTC, "Ignoring stale peer error callback from=" + peerId + " err=" + err);
                    return;
                }

                Log.e(TAG_RTC, "peer err [" + peerId + "] = " + err);
            }
        });

        peers.put(peerId, peer);
        peer.createPeerConnection();
        flushPendingIce(peerId);
    }

    private int nextGeneration(String peerId) {
        int gen = peerGenerations.containsKey(peerId) ? peerGenerations.get(peerId) + 1 : 1;
        peerGenerations.put(peerId, gen);
        return gen;
    }

    private boolean isCurrentGeneration(String peerId, int generation) {
        Integer current = peerGenerations.get(peerId);
        return current != null && current == generation;
    }

    // =========================================================
    // Peer lifecycle
    // =========================================================

    private void removePeer(String userId, boolean updateUi) {
        if (userId == null) return;

        SyncProtocol protocol = protocols.remove(userId);
        if (protocol != null) {
            try {
                protocol.close();
            } catch (Exception ignored) {
            }
        }

        WebRtcPeer peer = peers.remove(userId);
        if (peer != null) {
            try {
                peer.close();
            } catch (Exception ignored) {
            }
        }

        pendingIce.remove(userId);
        iceConnectedPeers.remove(userId);
        dcOpenPeers.remove(userId);
        reconnectingPeers.remove(userId);
        offerSeenAt.remove(userId);
        answerSeenAt.remove(userId);

        if (updateUi) {
            updatePeerStatusUi();
        }
    }

    private void safeCloseAllPeers() {
        List<String> peerIds = new ArrayList<>(peers.keySet());
        for (String peerId : peerIds) {
            removePeer(peerId, false);
        }
        updatePeerStatusUi();
    }

    private void schedulePeerReconnect(String peerUserId, String reason) {
        String normalized = normalizeUserId(peerUserId);
        if (normalized == null) return;

        if (reconnectingPeers.contains(normalized)) {
            Log.d(TAG_RTC, "Reconnect already scheduled for " + normalized + " reason=" + reason);
            return;
        }

        reconnectingPeers.add(normalized);
        Log.w(TAG_RTC, "schedulePeerReconnect reason=" + reason + " peer=" + normalized);

        removePeer(normalized, false);

        final String targetPeer = normalized;
        mainHandler.postDelayed(() -> {
            reconnectingPeers.remove(targetPeer);

            if (signalling != null && signalling.isConnected() && knownPeers.contains(targetPeer)) {
                connectToPeerIfNeeded(targetPeer);
            } else {
                Log.d(TAG_RTC, "Reconnect skipped: signalling down or peer not known " + targetPeer);
            }
        }, PEER_RECONNECT_DELAY_MS);

        updatePeerStatusUi();
    }

    // =========================================================
    // ICE queue
    // =========================================================

    private void queueIce(String from, JSONObject cand) {
        List<JSONObject> q = pendingIce.get(from);
        if (q == null) q = new ArrayList<>();
        q.add(cand);
        pendingIce.put(from, q);
    }

    private void flushPendingIce(String from) {
        WebRtcPeer peer = peers.get(from);
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

    // =========================================================
    // Sync helpers
    // =========================================================

    private void broadcastPokeIfPossible() {
        if (protocols.isEmpty()) return;

        int sent = 0;

        for (Map.Entry<String, SyncProtocol> entry : protocols.entrySet()) {
            String peerId = entry.getKey();
            SyncProtocol protocol = entry.getValue();

            if (dcOpenPeers.contains(peerId) && protocol != null) {
                protocol.sendPoke();
                sent++;
            }
        }

        if (sent > 0 && listener != null) {
            listener.onSyncStatusChanged("Sync: broadcast to " + sent + " peer(s)");
        }
    }

    // =========================================================
    // Signalling reconnect
    // =========================================================

    private void scheduleSignallingReconnectNow() {
        signallingReconnectAttempt = 0;
        scheduleSignallingReconnectWithBackoff();
    }

    private void scheduleSignallingReconnectWithBackoff() {
        if (signallingReconnectScheduled) return;
        signallingReconnectScheduled = true;

        long delay = Math.min(8000, 500L * (1L << Math.min(signallingReconnectAttempt, 4)));
        signallingReconnectAttempt++;

        mainHandler.postDelayed(() -> {
            signallingReconnectScheduled = false;
            tryReconnectSignalling();
        }, delay);
    }

    private void tryReconnectSignalling() {
        if (signalling != null && signalling.isConnected()) return;

        if (listener != null) {
            listener.onSignalStatusChanged("Signalling: reconnecting… (" + signallingReconnectAttempt + ")");
            listener.onSyncStatusChanged("Sync: idle");
        }

        safeCloseAllPeers();

        if (signalling == null) {
            startSignalling();
            return;
        }

        signalling.reconnect();
    }

    // =========================================================
    // UI summary
    // =========================================================

    private void updatePeerStatusUi() {
        int totalPeers = peers.size();
        int iceCount = iceConnectedPeers.size();
        int dcCount = dcOpenPeers.size();

        if (listener != null) {
            listener.onWebRtcStatusChanged("WebRTC: " + iceCount + "/" + totalPeers + " ICE connected");
            listener.onDataChannelStatusChanged("DataChannel: " + dcCount + "/" + totalPeers + " open");

            if (dcCount > 0) {
                listener.onSyncStatusChanged("Sync: mesh ready with " + dcCount + " peer(s)");
            } else if (totalPeers > 0) {
                listener.onSyncStatusChanged("Sync: peers discovered, waiting for channels…");
            } else {
                listener.onSyncStatusChanged("Sync: idle");
            }
        }
    }
}