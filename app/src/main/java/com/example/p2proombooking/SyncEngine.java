package com.example.p2proombooking;

import android.content.Context;

import java.util.List;
import java.util.UUID;

public class SyncEngine {

    private final BookingDao bookingDao;

    public SyncEngine(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.bookingDao = db.bookingDao();
    }

    public void applyRemoteBooking(BookingEntity remote) {

        BookingEntity exactLocal = bookingDao.getById(remote.bookingId);

        // -------------------------------------------------
        // 0) Tombstone handling first
        // -------------------------------------------------
        if (remote.deletedFlag == 1) {
            applyTombstone(remote, exactLocal);
            return;
        }

        // Same exact record already present
        if (exactLocal != null
                && exactLocal.version == remote.version
                && exactLocal.updatedAt == remote.updatedAt
                && isSameContent(exactLocal, remote)) {
            return;
        }

        // -------------------------------------------------
        // 1) Find all overlapping active contenders locally
        // -------------------------------------------------
        List<BookingEntity> overlaps = bookingDao.findOverlaps(
                remote.roomId,
                remote.startUtc,
                remote.endUtc
        );

        // Remove self from overlap list if present
        if (overlaps != null) {
            for (int i = overlaps.size() - 1; i >= 0; i--) {
                BookingEntity b = overlaps.get(i);
                if (b != null && remote.bookingId.equals(b.bookingId)) {
                    overlaps.remove(i);
                }
            }
        }

        // -------------------------------------------------
        // 2) No overlap -> normal exact-id reconciliation
        // -------------------------------------------------
        if (overlaps == null || overlaps.isEmpty()) {
            applyNonOverlappingRemote(remote, exactLocal);
            return;
        }

        // -------------------------------------------------
        // 3) Automatic resolution: latest booking wins
        // -------------------------------------------------
        BookingEntity winner = remote;
        for (BookingEntity localOverlap : overlaps) {
            if (localOverlap == null) continue;
            if (compareBookingPriority(localOverlap, winner) > 0) {
                winner = localOverlap;
            }
        }

        // -------------------------------------------------
        // 4) Apply winner and tombstone all losers
        // -------------------------------------------------
        long now = System.currentTimeMillis();

        // If remote wins, store/update remote as active
        if (winner.bookingId.equals(remote.bookingId)) {
            remote.deletedFlag = 0;
            remote.syncFlag = BookingConstants.SYNCED;
            remote.lastSyncedAt = now;

            if (exactLocal == null) {
                bookingDao.insert(remote);
            } else {
                bookingDao.update(remote);
            }

            // Tombstone all overlapping local losers
            for (BookingEntity loser : overlaps) {
                if (loser == null) continue;
                tombstoneLoser(loser, remote, now);
            }
        } else {
            // Some local overlap already wins, so remote loses
            BookingEntity remoteLoser = buildRemoteTombstone(remote, winner, now);

            if (exactLocal == null) {
                bookingDao.insert(remoteLoser);
            } else {
                bookingDao.update(remoteLoser);
            }
        }
    }

    private void applyNonOverlappingRemote(BookingEntity remote, BookingEntity exactLocal) {
        long now = System.currentTimeMillis();

        if (exactLocal == null) {
            remote.deletedFlag = 0;
            remote.syncFlag = BookingConstants.SYNCED;
            remote.lastSyncedAt = now;
            bookingDao.insert(remote);
            return;
        }

        int cmp = compareBookingPriority(remote, exactLocal);

        if (cmp > 0) {
            remote.deletedFlag = 0;
            remote.syncFlag = BookingConstants.SYNCED;
            remote.lastSyncedAt = now;
            bookingDao.update(remote);
        }
    }

    private void applyTombstone(BookingEntity remoteTombstone, BookingEntity exactLocal) {
        long now = System.currentTimeMillis();

        if (exactLocal == null) {
            remoteTombstone.syncFlag = BookingConstants.SYNCED;
            remoteTombstone.lastSyncedAt = now;
            bookingDao.insert(remoteTombstone);
            return;
        }

        int cmp = compareBookingPriority(remoteTombstone, exactLocal);
        if (cmp >= 0) {
            remoteTombstone.syncFlag = BookingConstants.SYNCED;
            remoteTombstone.lastSyncedAt = now;
            bookingDao.update(remoteTombstone);
        }
    }

    private void tombstoneLoser(BookingEntity loser, BookingEntity winner, long now) {
        loser.deletedFlag = 1;
        loser.status = BookingConstants.STATUS_ACTIVE; // keep non-conflicted
        loser.updatedAt = Math.max(now, winner.updatedAt);
        loser.version = Math.max(loser.version, winner.version);
        loser.syncFlag = BookingConstants.SYNC_PENDING;
        bookingDao.update(loser);
    }

    private BookingEntity buildRemoteTombstone(BookingEntity remoteLoser, BookingEntity winner, long now) {
        BookingEntity tomb = new BookingEntity();

        tomb.bookingId = remoteLoser.bookingId;
        tomb.roomId = remoteLoser.roomId;
        tomb.startUtc = remoteLoser.startUtc;
        tomb.endUtc = remoteLoser.endUtc;

        tomb.createdAt = remoteLoser.createdAt;
        tomb.createdByUserId = remoteLoser.createdByUserId;
        tomb.createdByDeviceId = remoteLoser.createdByDeviceId;

        tomb.canceledByUserId = remoteLoser.canceledByUserId;
        tomb.canceledAt = remoteLoser.canceledAt;

        tomb.status = BookingConstants.STATUS_ACTIVE;
        tomb.deletedFlag = 1;
        tomb.updatedAt = Math.max(now, winner.updatedAt);
        tomb.version = Math.max(remoteLoser.version, winner.version);
        tomb.syncFlag = BookingConstants.SYNC_PENDING;
        tomb.lastSyncedAt = 0L;

        return tomb;
    }

    /**
     * > 0 means a wins
     * < 0 means b wins
     * = 0 means same priority
     */
    private int compareBookingPriority(BookingEntity a, BookingEntity b) {
        if (a.updatedAt != b.updatedAt) {
            return Long.compare(a.updatedAt, b.updatedAt);
        }
        if (a.version != b.version) {
            return Integer.compare(a.version, b.version);
        }
        String aId = a.bookingId == null ? "" : a.bookingId;
        String bId = b.bookingId == null ? "" : b.bookingId;
        return aId.compareTo(bId);
    }

    private boolean isSameContent(BookingEntity a, BookingEntity b) {
        return safeEquals(a.roomId, b.roomId)
                && a.startUtc == b.startUtc
                && a.endUtc == b.endUtc
                && safeEquals(a.status, b.status)
                && a.deletedFlag == b.deletedFlag;
    }

    private boolean safeEquals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    public List<BookingEntity> getPendingChanges() {
        return bookingDao.getPendingSync();
    }

    public void markAsSynced(String bookingId) {
        bookingDao.markSynced(bookingId, System.currentTimeMillis());
    }


}