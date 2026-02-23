package com.example.p2proombooking;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.webrtc.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

public class WebRtcPeer {

    private static final String TAG = "RTC";

    public interface Listener {
        void onDataChannelMessage(String text);

        void onIceConnected();
        void onIceDisconnected();   // treat as "stale"
        void onIceFailed();

        void onDataChannelOpen();
        void onDataChannelClosed();

        void onError(String err);
    }

    private final PeerConnectionFactory factory;
    private PeerConnection pc;
    private DataChannel dc;

    private final String remoteUserId;
    private final SignallingClient signalling;
    private final Listener listener;

    // IMPORTANT: close guard to stop recursion / duplicate closes
    private final AtomicBoolean closing = new AtomicBoolean(false);

    // Post all teardown to main thread to avoid JNI re-entrancy
    private final Handler main = new Handler(Looper.getMainLooper());

    private volatile boolean iceConnectedNotified = false;
    private volatile boolean dcOpenNotified = false;
    private volatile boolean dcClosedNotified = false;

    public WebRtcPeer(Context ctx,
                      String remoteUserId,
                      SignallingClient signalling,
                      Listener listener) {

        this.remoteUserId = remoteUserId;
        this.signalling = signalling;
        this.listener = listener;

        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions
                        .builder(ctx.getApplicationContext())
                        .createInitializationOptions()
        );

        // You do not need EGL for datachannel-only, but leaving it ok.
        EglBase egl = EglBase.create();
        factory = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(egl.getEglBaseContext(), true, true))
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(egl.getEglBaseContext()))
                .createPeerConnectionFactory();
    }

    public void createPeerConnection() {
        if (pc != null || closing.get()) return;

        PeerConnection.IceServer stun = PeerConnection.IceServer
                .builder("stun:stun.l.google.com:19302")
                .createIceServer();

        PeerConnection.RTCConfiguration cfg =
                new PeerConnection.RTCConfiguration(Collections.singletonList(stun));
        cfg.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        pc = factory.createPeerConnection(cfg, new PeerConnection.Observer() {

            @Override
            public void onIceCandidate(IceCandidate c) {
                if (closing.get() || c == null) return;
                signalling.sendIce(remoteUserId, c.sdpMid, c.sdpMLineIndex, c.sdp);
                Log.d(TAG, "TX ice to=" + remoteUserId);
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                if (closing.get() || dataChannel == null) return;
                Log.d(TAG, "onDataChannel label=" + dataChannel.label());
                setDataChannel(dataChannel);
            }

            @Override
            public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
                if (closing.get()) return;
                Log.d(TAG, "PC state=" + newState);

                if (newState == PeerConnection.PeerConnectionState.FAILED) {
                    safeError("PC state=FAILED");
                    notifyIceFailedOnce();
                }
                if (newState == PeerConnection.PeerConnectionState.CLOSED) {
                    // don't close() here — already closing or closed
                    notifyIceDisconnectedOnce();
                }
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState state) {
                // CRITICAL: ignore all callbacks once closing begins
                if (closing.get()) return;

                Log.d(TAG, "ICE state=" + state);

                switch (state) {
                    case CONNECTED:
                    case COMPLETED:
                        notifyIceConnectedOnce();
                        break;

                    case DISCONNECTED:
                        // Don't call close() here. Just notify; HomeActivity will schedule teardown safely.
                        notifyIceDisconnectedOnce();
                        break;

                    case FAILED:
                        safeError("ICE state=FAILED");
                        notifyIceFailedOnce();
                        break;

                    case CLOSED:
                        // CLOSED happens a lot during teardown; don’t recurse
                        notifyIceDisconnectedOnce();
                        break;

                    default:
                        break;
                }
            }

            @Override public void onIceGatheringChange(PeerConnection.IceGatheringState state) {
                if (closing.get()) return;
                Log.d(TAG, "ICE gathering=" + state);
            }

            // unused
            @Override public void onSignalingChange(PeerConnection.SignalingState newState) {}
            @Override public void onIceConnectionReceivingChange(boolean b) {}
            @Override public void onIceCandidatesRemoved(IceCandidate[] candidates) {}
            @Override public void onAddStream(MediaStream stream) {}
            @Override public void onRemoveStream(MediaStream stream) {}
            @Override public void onRenegotiationNeeded() {}
            @Override public void onAddTrack(RtpReceiver receiver, MediaStream[] mediaStreams) {}
            @Override public void onSelectedCandidatePairChanged(CandidatePairChangeEvent event) {}
            @Override public void onTrack(RtpTransceiver transceiver) {}
        });
    }

    // Caller
    public void startAsCaller() {
        if (closing.get()) return;
        if (pc == null) createPeerConnection();
        if (pc == null) return;

        // caller creates DC
        DataChannel.Init init = new DataChannel.Init();
        setDataChannel(pc.createDataChannel("sync", init));

        pc.createOffer(new SdpObserverAdapter() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                if (closing.get() || pc == null) return;
                pc.setLocalDescription(new SdpObserverAdapter(), sdp);
                signalling.sendOffer(remoteUserId, sdp.description);
                Log.d(TAG, "TX offer to=" + remoteUserId);
            }

            @Override
            public void onCreateFailure(String s) {
                safeError("createOffer failed: " + s);
            }
        }, new MediaConstraints());
    }

    // Callee
    public void onRemoteOffer(String sdpText) {
        if (closing.get()) return;
        if (pc == null) createPeerConnection();
        if (pc == null) return;

        SessionDescription offer = new SessionDescription(SessionDescription.Type.OFFER, sdpText);
        pc.setRemoteDescription(new SdpObserverAdapter() {
            @Override public void onSetFailure(String s) {
                safeError("setRemote(offer) failed: " + s);
            }
        }, offer);

        pc.createAnswer(new SdpObserverAdapter() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                if (closing.get() || pc == null) return;
                pc.setLocalDescription(new SdpObserverAdapter(), sdp);
                signalling.sendAnswer(remoteUserId, sdp.description);
                Log.d(TAG, "TX answer to=" + remoteUserId);
            }

            @Override
            public void onCreateFailure(String s) {
                safeError("createAnswer failed: " + s);
            }
        }, new MediaConstraints());
    }

    public void onRemoteAnswer(String sdpText) {
        if (closing.get() || pc == null) return;

        SessionDescription ans = new SessionDescription(SessionDescription.Type.ANSWER, sdpText);
        pc.setRemoteDescription(new SdpObserverAdapter() {
            @Override public void onSetFailure(String s) {
                safeError("setRemote(answer) failed: " + s);
            }
        }, ans);
    }

    public void onRemoteIce(String sdpMid, int sdpMLineIndex, String candidate) {
        if (closing.get() || pc == null) return;
        pc.addIceCandidate(new IceCandidate(sdpMid, sdpMLineIndex, candidate));
    }

    public boolean isDataChannelOpen() {
        return !closing.get() && dc != null && dc.state() == DataChannel.State.OPEN;
    }

    public void sendText(String text) {
        if (!isDataChannelOpen()) return;
        ByteBuffer buf = ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
        dc.send(new DataChannel.Buffer(buf, false));
    }

    /**
     * SAFE CLOSE:
     * - idempotent (runs once)
     * - posted to main thread (prevents JNI callback recursion)
     */
    public void close() {
        if (!closing.compareAndSet(false, true)) return;

        main.post(() -> {
            try {
                if (dc != null) {
                    try { dc.unregisterObserver(); } catch (Exception ignored) {}
                    try { dc.close(); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}

            try {
                if (pc != null) {
                    try { pc.close(); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}

            dc = null;
            pc = null;

            // reset notifiers for next new instance (if you recreate WebRtcPeer object)
            iceConnectedNotified = false;
            dcOpenNotified = false;
            dcClosedNotified = false;
        });
    }

    private void setDataChannel(DataChannel channel) {
        dc = channel;
        hookDataChannel();
    }

    private void hookDataChannel() {
        if (dc == null) return;

        dc.registerObserver(new DataChannel.Observer() {
            @Override public void onBufferedAmountChange(long previousAmount) {}

            @Override
            public void onStateChange() {
                if (closing.get() || dc == null) return;

                DataChannel.State state = dc.state();
                Log.d(TAG, "DC state=" + state);

                if (!dcOpenNotified && state == DataChannel.State.OPEN) {
                    dcOpenNotified = true;
                    if (listener != null) listener.onDataChannelOpen();
                    return;
                }

                if (state == DataChannel.State.CLOSED) {
                    notifyDcClosedOnce();
                }
            }

            @Override
            public void onMessage(DataChannel.Buffer buffer) {
                if (closing.get()) return;
                try {
                    byte[] bytes = new byte[buffer.data.remaining()];
                    buffer.data.get(bytes);

                    if (!buffer.binary) {
                        String text = new String(bytes, StandardCharsets.UTF_8);
                        if (listener != null) listener.onDataChannelMessage(text);
                    }
                } catch (Exception e) {
                    safeError("DC message error: " + e.getMessage());
                }
            }
        });
    }

    private void notifyIceConnectedOnce() {
        if (iceConnectedNotified) return;
        iceConnectedNotified = true;
        if (listener != null) listener.onIceConnected();
    }

    private void notifyIceDisconnectedOnce() {
        // allow re-notify on reconnect scenario
        iceConnectedNotified = false;
        if (listener != null) listener.onIceDisconnected();
    }

    private void notifyIceFailedOnce() {
        iceConnectedNotified = false;
        if (listener != null) listener.onIceFailed();
    }

    private void notifyDcClosedOnce() {
        if (dcClosedNotified) return;
        dcClosedNotified = true;
        if (listener != null) listener.onDataChannelClosed();
    }

    private void safeError(String msg) {
        Log.e(TAG, msg);
        if (listener != null) listener.onError(msg);
    }

    static class SdpObserverAdapter implements SdpObserver {
        @Override public void onCreateSuccess(SessionDescription sessionDescription) {}
        @Override public void onSetSuccess() {}
        @Override public void onCreateFailure(String s) {}
        @Override public void onSetFailure(String s) {}
    }
}