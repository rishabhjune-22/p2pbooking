package com.example.p2proombooking;

import android.content.Context;

import java.util.List;

public class SyncEngine {

    private final BookingDao bookingDao;
    private final BookingConflictDao bookingConflictDao;

    public SyncEngine(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.bookingDao = db.bookingDao();
        this.bookingConflictDao = db.bookingConflictDao();
    }

    // -----------------------------
    // APPLY REMOTE BOOKING
    // -----------------------------
    public void applyRemoteBooking(BookingEntity remote) {

        BookingEntity local = bookingDao.getById(remote.bookingId);
        if (local != null && remote.updatedAt == local.updatedAt) {
            return;
        }
        // 1) Not exists locally → insert remote
        if (local == null) {

            BookingEntity overlap = bookingDao.findFirstOverlap(
                    remote.roomId,
                    remote.startUtc,
                    remote.endUtc
            );

            if (overlap != null) {

                // store conflict
                BookingConflictEntity c = new BookingConflictEntity();

                c.bookingId = overlap.bookingId;

                c.localRoomId = overlap.roomId;
                c.localStartUtc = overlap.startUtc;
                c.localEndUtc = overlap.endUtc;
                c.localStatus = overlap.status;
                c.localVersion = overlap.version;
                c.localUpdatedAt = overlap.updatedAt;

                c.remoteRoomId = remote.roomId;
                c.remoteStartUtc = remote.startUtc;
                c.remoteEndUtc = remote.endUtc;
                c.remoteStatus = remote.status;
                c.remoteVersion = remote.version;
                c.remoteUpdatedAt = remote.updatedAt;

                c.detectedAt = System.currentTimeMillis();

                bookingConflictDao.upsert(c);

                overlap.status = BookingConstants.STATUS_CONFLICTED;
                overlap.syncFlag = BookingConstants.SYNC_PENDING;
                overlap.updatedAt = System.currentTimeMillis();
                overlap.version++;

                bookingDao.update(overlap);

                return;
            }

            remote.syncFlag = BookingConstants.SYNCED;
            remote.lastSyncedAt = System.currentTimeMillis();
            bookingDao.insert(remote);
            return;
        }

        // 2) Remote version higher → accept
        if (remote.version > local.version) {

            remote.syncFlag = BookingConstants.SYNCED;
            remote.lastSyncedAt = System.currentTimeMillis();

            bookingDao.update(remote);

            // 🔴 IMPORTANT: remove conflict entry if exists
            bookingConflictDao.deleteById(remote.bookingId);

            return;
        }

        // 3) Local version higher → ignore
        if (remote.version < local.version) {
            return;
        }

        // 4) Same version
        if (isSameContent(local, remote)) {
            return; // nothing to do
        }

        // 4a) Same version but different content → use updatedAt as tiebreaker
        if (remote.updatedAt > local.updatedAt) {

            remote.syncFlag = BookingConstants.SYNCED;
            remote.lastSyncedAt = System.currentTimeMillis();
            bookingDao.update(remote);
            bookingConflictDao.deleteById(remote.bookingId);

            return;
        }

        if (remote.updatedAt < local.updatedAt) {
            // keep local (no conflict)
            return;
        }

        // 4b) Same version AND same updatedAt but different content → real conflict
        BookingConflictEntity c = new BookingConflictEntity();
        c.bookingId = local.bookingId;

        c.localRoomId = local.roomId;
        c.localStartUtc = local.startUtc;
        c.localEndUtc = local.endUtc;
        c.localStatus = local.status;
        c.localVersion = local.version;
        c.localUpdatedAt = local.updatedAt;

        c.remoteRoomId = remote.roomId;
        c.remoteStartUtc = remote.startUtc;
        c.remoteEndUtc = remote.endUtc;
        c.remoteStatus = remote.status;
        c.remoteVersion = remote.version;
        c.remoteUpdatedAt = remote.updatedAt;

        c.detectedAt = System.currentTimeMillis();
        bookingConflictDao.upsert(c);

        local.status = BookingConstants.STATUS_CONFLICTED;
        local.syncFlag = BookingConstants.SYNC_PENDING;
        local.updatedAt = System.currentTimeMillis();
        local.version = local.version + 1;

        bookingDao.update(local);
    }

    // -----------------------------
    // CONTENT COMPARISON
    // -----------------------------
    private boolean isSameContent(BookingEntity a, BookingEntity b) {
        return safeEquals(a.roomId, b.roomId)
                && a.startUtc == b.startUtc
                && a.endUtc == b.endUtc
                && safeEquals(a.status, b.status);
    }

    private boolean safeEquals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    // -----------------------------
    // SYNC OUTGOING
    // -----------------------------
    public List<BookingEntity> getPendingChanges() {
        return bookingDao.getPendingSync();
    }

    public void markAsSynced(String bookingId) {
        bookingDao.markSynced(bookingId, System.currentTimeMillis());
    }

    // -----------------------------
    // TEST SIMULATION
    // -----------------------------
    public void simulateRemoteConflict() {

        List<BookingEntity> list = bookingDao.getAllActive();
        if (list == null || list.isEmpty()) return;

        BookingEntity local = list.get(0);

        BookingEntity remote = new BookingEntity();
        remote.bookingId = local.bookingId;
        remote.roomId = local.roomId;
        remote.startUtc = local.startUtc + 30 * 60 * 1000L;
        remote.endUtc = local.endUtc + 30 * 60 * 1000L;
        remote.status = BookingConstants.STATUS_ACTIVE;
        remote.version = local.version; // same version
        remote.updatedAt = System.currentTimeMillis();
        remote.syncFlag = BookingConstants.SYNCED;

        // copy safe audit
        remote.createdAt = local.createdAt;
        remote.createdByUserId = local.createdByUserId;
        remote.createdByDeviceId = local.createdByDeviceId;

        applyRemoteBooking(remote);
    }
}
