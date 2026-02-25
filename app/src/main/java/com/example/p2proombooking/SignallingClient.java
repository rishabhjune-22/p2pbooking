package com.example.p2proombooking;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class SignallingClient {

    public interface Listener {
        void onJoined(JSONArray peers);
        void onPeerJoined(String userId);
        void onPeerLeft(String userId);

        void onOffer(String from, String sdp);
        void onAnswer(String from, String sdp);
        void onIce(String from, JSONObject cand);

        void onError(String msg);
    }

    private static final String TAG = "SIG";

    private final OkHttpClient client = new OkHttpClient();
    private final String wsUrl;
    private final String myUserId;
    private final Listener listener;

    private WebSocket ws;

    // track peers so we can fire joined/left events
    private final Set<String> lastPeers = new HashSet<>();
    private volatile boolean manuallyClosed = false;
    private volatile boolean connected = false;
    public SignallingClient(String wsUrl, String myUserId, Listener listener) {
        this.wsUrl = wsUrl;
        this.myUserId = myUserId;
        this.listener = listener;
    }

    public boolean isConnected() { return connected; }

    public void disconnect() {
        manuallyClosed = true;
        connected = false;
        try { if (ws != null) ws.close(1000, "bye"); } catch (Exception ignored) {}
        ws = null;
    }

    public void reconnect() {
        manuallyClosed = false;
        connect();
    }


    public void connect() {
        Request req = new Request.Builder().url(wsUrl).build();
        ws = client.newWebSocket(req, new WebSocketListener() {

            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                connected = true;
                Log.d(TAG, "WS open: " + wsUrl);

                // IMPORTANT: register with your server
                try {
                    JSONObject hello = new JSONObject();
                    hello.put("type", "hello");
                    hello.put("userId", myUserId);
                    webSocket.send(hello.toString());
                    Log.d(TAG, "Sent hello for userId=" + myUserId);
                } catch (Exception e) {
                    Log.e(TAG, "hello build fail", e);
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d(TAG, "RX: " + text);

                try {
                    JSONObject msg = new JSONObject(text);
                    String type = msg.optString("type", "");

                    switch (type) {

                        case "hello_ack": {
                            // server confirmed registration
                            Log.d(TAG, "hello_ack userId=" + msg.optString("userId"));
                            break;
                        }

                        case "peers": {
                            JSONArray peers = msg.optJSONArray("peers");
                            if (peers == null) peers = new JSONArray();

                            // fire initial joined
                            if (listener != null) listener.onJoined(peers);

                            // diff to detect join/leave
                            Set<String> now = new HashSet<>();
                            for (int i = 0; i < peers.length(); i++) {
                                String uid = peers.optString(i, null);
                                if (uid != null && !uid.trim().isEmpty()) now.add(uid);
                            }

                            for (String uid : now) {
                                if (!lastPeers.contains(uid)) {
                                    if (listener != null) listener.onPeerJoined(uid);
                                }
                            }

                            for (String uid : lastPeers) {
                                if (!now.contains(uid)) {
                                    if (listener != null) listener.onPeerLeft(uid);
                                }
                            }

                            lastPeers.clear();
                            lastPeers.addAll(now);
                            break;
                        }

                        case "offer": {
                            String from = msg.optString("from", "");
                            String sdp = msg.optString("sdp", "");
                            if (listener != null) listener.onOffer(from, sdp);
                            break;
                        }

                        case "answer": {
                            String from = msg.optString("from", "");
                            String sdp = msg.optString("sdp", "");
                            if (listener != null) listener.onAnswer(from, sdp);
                            break;
                        }

                        case "ice": {
                            String from = msg.optString("from", "");
                            JSONObject cand = msg.optJSONObject("candidate");
                            if (listener != null) listener.onIce(from, cand);
                            break;
                        }
//tobesolvederror
                        case "error": {
                            String code = msg.optString("code", "UNKNOWN");
                            if (listener != null) listener.onError("server error: " + code);
                            break;
                        }

                        case "kicked": {
                            if (listener != null) listener.onError("kicked: " + msg.optString("reason"));
                            break;
                        }

                        default:
                            Log.d(TAG, "Unknown type=" + type);
                            break;
                    }

                } catch (Exception e) {
                    Log.e(TAG, "parse fail", e);
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                connected = false;

                Log.e(TAG, "WS failure", t);
                if (listener != null) listener.onError("ws failure: " + t.getMessage());
                if (!manuallyClosed) {
                    try { webSocket.cancel(); } catch (Exception ignored) {}
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                connected = false;
                Log.d(TAG, "WS closed: " + code + " " + reason);
                if (listener != null) listener.onError("ws closed: " + reason);
            }
            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                connected = false;
            }

        });
    }

    // -------------------------
    // Send signalling messages
    // -------------------------

    public void sendOffer(String to, String sdp) { sendSdp("offer", to, sdp); }
    public void sendAnswer(String to, String sdp) { sendSdp("answer", to, sdp); }

    private void sendSdp(String type, String to, String sdp) {
        if (ws == null) return;
        try {
            JSONObject obj = new JSONObject();
            obj.put("type", type);
            obj.put("to", to);
            obj.put("from", myUserId);
            obj.put("sdp", sdp);
            ws.send(obj.toString());
            Log.d(TAG, "TX " + type + " to=" + to);
        } catch (Exception ignored) {}
    }

    public void sendIce(String to, String sdpMid, int sdpMLineIndex, String candidate) {
        if (ws == null) return;
        try {
            JSONObject cand = new JSONObject();
            cand.put("sdpMid", sdpMid);
            cand.put("sdpMLineIndex", sdpMLineIndex);
            cand.put("candidate", candidate);

            JSONObject obj = new JSONObject();
            obj.put("type", "ice");
            obj.put("to", to);
            obj.put("from", myUserId);
            obj.put("candidate", cand);

            ws.send(obj.toString());
            Log.d(TAG, "TX ice to=" + to);
        } catch (Exception ignored) {}
    }
}
