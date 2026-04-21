package com.example.roombooking.booking;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roombooking.R;
import com.example.roombooking.model.booking.BookingItem;
import com.example.roombooking.security.CryptoManager;
import com.example.roombooking.security.EncryptedBookingPayload;
import com.example.roombooking.security.KeystoreBackedCryptoSessionManager;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

public class BookingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnBookingClickListener {
        void onBookingClick(BookingItem bookingItem);
        void onBookingLongClick(BookingItem bookingItem, int position);
    }

    private static final int VIEW_TYPE_ITEM = 1;
    private static final int VIEW_TYPE_LOADING = 2;
    private static final String MASK = "****";
    private static final int[] ACCENT_COLORS = {
            0xFF71DB93,
            0xFF7C75F2,
            0xFFFFA33D,
            0xFF4DB6FF,
            0xFFFF6FAE,
            0xFF22C7A9
    };

    private final Context context;
    private final List<BookingItem> items = new ArrayList<>();
    private final Gson gson = new Gson();
    private final OnBookingClickListener listener;

    private boolean showLoading = false;

    public BookingAdapter(Context context, OnBookingClickListener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    public void setItems(List<BookingItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void addItems(List<BookingItem> newItems) {
        if (newItems == null || newItems.isEmpty()) {
            return;
        }
        int start = items.size();
        items.addAll(newItems);
        notifyItemRangeInserted(start, newItems.size());
    }

    public void clearItems() {
        items.clear();
        showLoading = false;
        notifyDataSetChanged();
    }

    public void showPaginationLoader() {
        if (!showLoading) {
            showLoading = true;
            notifyItemInserted(items.size());
        }
    }

    public void hidePaginationLoader() {
        if (showLoading) {
            int loaderPosition = items.size();
            showLoading = false;
            notifyItemRemoved(loaderPosition);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (showLoading && position == items.size()) {
            return VIEW_TYPE_LOADING;
        }
        return VIEW_TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return items.size() + (showLoading ? 1 : 0);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_LOADING) {
            View view = inflater.inflate(R.layout.item_pagination_loader, parent, false);
            return new LoadingViewHolder(view);
        }

        View view = inflater.inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (!(holder instanceof BookingViewHolder)) {
            return;
        }

        BookingItem item = items.get(position);
        BookingViewHolder bookingHolder = (BookingViewHolder) holder;

        DecryptedDisplayData displayData = getDisplayData(item);

        bookingHolder.tvVisitorName.setText(displayData.visitorName);
        bookingHolder.tvRoomName.setText(safeOrMask(item.getRoomName()));
        bookingHolder.tvOrganisation.setText(displayData.visitorOrganisation);
        bookingHolder.tvArrival.setText(safeOrMask(item.getArrivalAt()));
        bookingHolder.tvDeparture.setText(safeOrMask(item.getDepartureAt()));
        bookingHolder.tvPurpose.setText(displayData.purposeOfVisit);

        String status = safe(item.getStatus());
        int accentColor = getAccentColor(item, position);
        applyAccentColor(bookingHolder, accentColor);

        if (item.canDecrypt()) {
            bookingHolder.tvCreatedBy.setText("Created By: You");
        } else {
            bookingHolder.tvCreatedBy.setText("Created By: " + safe(item.getCreatedByUsername()));
        }

        bookingHolder.tvVisitorName.setAlpha(item.canDecrypt() ? 1f : 0.6f);
        bookingHolder.tvOrganisation.setAlpha(item.canDecrypt() ? 1f : 0.6f);
        bookingHolder.tvPurpose.setAlpha(item.canDecrypt() ? 1f : 0.6f);

        bookingHolder.itemView.setAlpha(1f);
        if ("cancelled".equalsIgnoreCase(status)) {
            bookingHolder.tvStatus.setText("Cancelled");
            bookingHolder.tvStatus.setTextColor(0xFFC65B5B);
            bookingHolder.tvStatus.setBackgroundResource(R.drawable.bg_status_cancelled_soft);
        } else {
            bookingHolder.tvStatus.setText("Active");
            bookingHolder.tvStatus.setTextColor(0xFF35A645);
            bookingHolder.tvStatus.setBackgroundResource(R.drawable.bg_status_active_soft);
        }

        bookingHolder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBookingClick(item);
            }
        });

        bookingHolder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onBookingLongClick(item, holder.getBindingAdapterPosition());
            }
            return true;
        });
    }

    private DecryptedDisplayData getDisplayData(BookingItem item) {
        String visitorName = MASK;
        String visitorOrganisation = MASK;
        String purposeOfVisit = MASK;

        if (item != null && item.canDecrypt() && item.hasEncryptedPayload()) {
            try {
                KeystoreBackedCryptoSessionManager cryptoSessionManager =
                        KeystoreBackedCryptoSessionManager.getInstance(context);

                SecretKey dek = cryptoSessionManager.getDek();
                if (dek != null) {
                    CryptoManager cryptoManager = new CryptoManager();
                    String decryptedJson = cryptoManager.decryptPayload(
                            item.getEncryptedPayload(),
                            item.getPayloadNonce(),
                            dek
                    );

                    EncryptedBookingPayload payload =
                            EncryptedBookingPayload.fromJson(decryptedJson, gson);

                    if (payload != null) {
                        visitorName = safeOrMask(payload.getVisitorName());
                        visitorOrganisation = safeOrMask(payload.getVisitorOrganisation());
                        purposeOfVisit = safeOrMask(payload.getPurposeOfVisit());
                    }
                }
            } catch (Exception ignored) {
                visitorName = MASK;
                visitorOrganisation = MASK;
                purposeOfVisit = MASK;
            }
        }

        return new DecryptedDisplayData(visitorName, visitorOrganisation, purposeOfVisit);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safeOrMask(String value) {
        if (value == null || value.trim().isEmpty()) {
            return MASK;
        }
        return value;
    }

    private int getAccentColor(BookingItem item, int position) {
        int seed = position + 1;
        if (item != null && item.getId() != 0) {
            seed = Math.abs(item.getId());
        }
        return ACCENT_COLORS[seed % ACCENT_COLORS.length];
    }

    private void applyAccentColor(BookingViewHolder holder, int color) {
        tintDrawable(holder.viewAccent, color);
        tintDrawable(holder.ivAvatar, color);
    }

    private void tintDrawable(View view, int color) {
        if (!(view.getBackground() instanceof GradientDrawable)) {
            return;
        }
        GradientDrawable drawable = (GradientDrawable) view.getBackground().mutate();
        drawable.setColor(color);
    }

    private static class DecryptedDisplayData {
        final String visitorName;
        final String visitorOrganisation;
        final String purposeOfVisit;

        DecryptedDisplayData(String visitorName, String visitorOrganisation, String purposeOfVisit) {
            this.visitorName = visitorName;
            this.visitorOrganisation = visitorOrganisation;
            this.purposeOfVisit = purposeOfVisit;
        }
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        View viewAccent;
        ImageView ivAvatar;
        TextView tvVisitorName;
        TextView tvCreatedBy;
        TextView tvRoomName;
        TextView tvOrganisation;
        TextView tvArrival;
        TextView tvDeparture;
        TextView tvPurpose;
        TextView tvStatus;

        BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            viewAccent = itemView.findViewById(R.id.viewAccent);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvVisitorName = itemView.findViewById(R.id.tvVisitorName);
            tvRoomName = itemView.findViewById(R.id.tvRoomName);
            tvOrganisation = itemView.findViewById(R.id.tvOrganisation);
            tvArrival = itemView.findViewById(R.id.tvArrival);
            tvDeparture = itemView.findViewById(R.id.tvDeparture);
            tvPurpose = itemView.findViewById(R.id.tvPurpose);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvCreatedBy = itemView.findViewById(R.id.tvCreatedBy);
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
