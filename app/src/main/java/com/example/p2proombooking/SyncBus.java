package com.example.p2proombooking;

import java.util.concurrent.CopyOnWriteArrayList;

public final class SyncBus {

    public interface Listener {
        void onLocalDbChanged();  // DB changed (UI observers / debug)
        void onLocalChange();     // local write -> should poke peer
        void onRemoteChange();    // remote applied -> optional UI hint
    }

    private static final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    public static void addListener(Listener l) {
        if (l != null) listeners.addIfAbsent(l);
    }

    public static void removeListener(Listener l) {
        if (l != null) listeners.remove(l);
    }

    // call this after local insert/update/cancel
    public static void notifyLocalChange() {
        for (Listener l : listeners) {
            try { l.onLocalChange(); } catch (Exception ignored) {}
            try { l.onLocalDbChanged(); } catch (Exception ignored) {}
        }
    }

    // call this after applying remote batch
    public static void notifyRemoteChange() {
        for (Listener l : listeners) {
            try { l.onRemoteChange(); } catch (Exception ignored) {}
            try { l.onLocalDbChanged(); } catch (Exception ignored) {}
        }
    }

    private SyncBus() {}
}