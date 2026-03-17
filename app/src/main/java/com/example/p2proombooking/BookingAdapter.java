package com.example.p2proombooking;

import android.app.AlertDialog;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class BookingAdapter extends ListAdapter<BookingEntity, BookingAdapter.VH> {

    public interface OnCancelRequested {
        void onCancel(BookingEntity booking);
    }

    public interface OnEditRequested {
        void onEdit(BookingEntity booking);
    }

    private final OnCancelRequested cancelCallback;
    private final OnEditRequested editCallback;

    private final SimpleDateFormat fmt =
            new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    public BookingAdapter(OnCancelRequested cancelCallback, OnEditRequested editCallback) {
        super(DIFF_CALLBACK);
        this.cancelCallback = cancelCallback;
        this.editCallback = editCallback;
        setHasStableIds(true);
    }

    public void submit(List<BookingEntity> list) {
        if (list == null) {
            submitList(null);
            return;
        }

        List<BookingEntity> filtered = new ArrayList<>();
        for (BookingEntity b : list) {
            if (b == null) continue;
            if (b.deletedFlag == 1) continue; // never show tombstones
            filtered.add(b);
        }

        submitList(new ArrayList<>(filtered));
    }

    @Override
    public long getItemId(int position) {
        BookingEntity b = getItem(position);
        if (b == null || b.bookingId == null) return RecyclerView.NO_ID;
        return b.bookingId.hashCode();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        BookingEntity b = getItem(position);
        if (b == null) return;

        h.tvRoom.setText(buildRoomLine(b));
        h.tvTime.setText(buildTimeLine(b));

        String meta = buildMeta(b);
        SpannableString spannable = new SpannableString(meta);

        int dotStart = meta.lastIndexOf('●');
        if (dotStart >= 0) {
            spannable.setSpan(
                    new ForegroundColorSpan(getIndicatorColor(b)),
                    dotStart,
                    meta.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        h.tvMeta.setText(spannable);

        h.itemView.setOnClickListener(v -> {
            if (b.deletedFlag == 1) {
                Toast.makeText(v.getContext(),
                        "Deleted entries cannot be edited.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            String status = safe(b.status);

            if (BookingConstants.STATUS_CANCELED.equalsIgnoreCase(status)) {
                Toast.makeText(v.getContext(),
                        "Canceled bookings cannot be edited.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            if (editCallback != null) {
                editCallback.onEdit(b);
            }
        });

        h.itemView.setOnLongClickListener(v -> {
            if (b.deletedFlag == 1) return true;

            String status = safe(b.status);
            if (!BookingConstants.STATUS_ACTIVE.equalsIgnoreCase(status)) {
                return true;
            }

            new AlertDialog.Builder(v.getContext())
                    .setTitle("Cancel booking?")
                    .setMessage(
                            "Room: " + safe(b.roomId) + "\n" +
                                    fmt.format(new Date(b.startUtc)) +
                                    " → " +
                                    fmt.format(new Date(b.endUtc))
                    )
                    .setPositiveButton("Cancel booking", (d, which) -> {
                        if (cancelCallback != null) cancelCallback.onCancel(b);
                    })
                    .setNegativeButton("Keep", null)
                    .show();

            return true;
        });
    }

    private String buildRoomLine(BookingEntity b) {
        return "Room: " + safe(b.roomId) + " | Status: " + getDisplayStatus(b);
    }

    private String buildTimeLine(BookingEntity b) {
        return "Start: " + fmt.format(new Date(b.startUtc)) +
                "   End: " + fmt.format(new Date(b.endUtc));
    }

    private String buildMeta(BookingEntity b) {
        StringBuilder sb = new StringBuilder();

        sb.append("By: ").append(safe(b.createdByUserId))
                .append("\nBookingId: ").append(safe(b.bookingId));

        if (b.createdAt > 0) {
            sb.append("\nCreated at: ").append(fmt.format(new Date(b.createdAt)));
        }

        if (b.updatedAt > 0 && b.updatedAt != b.createdAt) {
            sb.append("\nUpdated at: ").append(fmt.format(new Date(b.updatedAt)));
        }

        if (BookingConstants.STATUS_CANCELED.equalsIgnoreCase(safe(b.status))) {
            String who = (b.canceledByUserId == null || b.canceledByUserId.trim().isEmpty())
                    ? "Unknown"
                    : b.canceledByUserId;
            sb.append("\nCanceled by: ").append(who);

            if (b.canceledAt > 0) {
                sb.append("\nCanceled at: ").append(fmt.format(new Date(b.canceledAt)));
            }
        }

        sb.append("\nDevice: ").append(safe(b.createdByDeviceId));
        sb.append("\n").append(getIndicatorText(b));

        return sb.toString();
    }

    private String getDisplayStatus(BookingEntity b) {
        return safe(b.status);
    }

    private String getIndicatorText(BookingEntity b) {
        if (BookingConstants.SYNC_PENDING.equalsIgnoreCase(safe(b.syncFlag))) {
            return "● Pending Sync";
        }
        if (BookingConstants.STATUS_CANCELED.equalsIgnoreCase(safe(b.status))) {
            return "● Canceled";
        }
        return "● Synced";
    }

    private int getIndicatorColor(BookingEntity b) {
        if (BookingConstants.SYNC_PENDING.equalsIgnoreCase(safe(b.syncFlag))) {
            return Color.RED;
        }
        if (BookingConstants.STATUS_CANCELED.equalsIgnoreCase(safe(b.status))) {
            return Color.parseColor("#616161");
        }
        return Color.parseColor("#2E7D32");
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvRoom, tvTime, tvMeta;

        VH(@NonNull View itemView) {
            super(itemView);
            tvRoom = itemView.findViewById(R.id.tvRoom);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvMeta = itemView.findViewById(R.id.tvMeta);
        }
    }

    private static final DiffUtil.ItemCallback<BookingEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<BookingEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull BookingEntity oldItem, @NonNull BookingEntity newItem) {
                    return Objects.equals(oldItem.bookingId, newItem.bookingId);
                }

                @Override
                public boolean areContentsTheSame(@NonNull BookingEntity oldItem, @NonNull BookingEntity newItem) {
                    return Objects.equals(oldItem.roomId, newItem.roomId)
                            && oldItem.startUtc == newItem.startUtc
                            && oldItem.endUtc == newItem.endUtc
                            && Objects.equals(oldItem.status, newItem.status)
                            && oldItem.version == newItem.version
                            && oldItem.updatedAt == newItem.updatedAt
                            && oldItem.createdAt == newItem.createdAt
                            && Objects.equals(oldItem.createdByUserId, newItem.createdByUserId)
                            && Objects.equals(oldItem.createdByDeviceId, newItem.createdByDeviceId)
                            && Objects.equals(oldItem.canceledByUserId, newItem.canceledByUserId)
                            && oldItem.canceledAt == newItem.canceledAt
                            && oldItem.deletedFlag == newItem.deletedFlag
                            && Objects.equals(oldItem.syncFlag, newItem.syncFlag);
                }
            };
}