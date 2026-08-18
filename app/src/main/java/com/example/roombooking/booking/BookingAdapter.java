package com.example.roombooking.booking;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roombooking.R;
import com.example.roombooking.model.booking.BookingItem;
import com.example.roombooking.model.booking.BookingStatus;
import com.example.roombooking.utils.NullSafeCollections;
import com.example.roombooking.utils.DateTimeUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BookingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnBookingClickListener {
        void onBookingClick(BookingItem bookingItem);

        void onBookingLongClick(BookingItem bookingItem, int position);
    }

    private static final int VIEW_TYPE_DETAILED = 1;
    private static final int VIEW_TYPE_LOADING = 2;
    private static final int VIEW_TYPE_COMPACT = 3;

    private static final String GENDER_MALE = "male";
    private static final String GENDER_FEMALE = "female";

    private final Context context;
    private final List<BookingItem> items = new ArrayList<>();
    private final OnBookingClickListener listener;

    private boolean showLoading = false;
    private boolean compactView = false;

    public BookingAdapter(Context context, OnBookingClickListener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    public void setItems(List<BookingItem> newItems) {
        List<BookingItem> oldItems = new ArrayList<>(items);
        List<BookingItem> updatedItems = NullSafeCollections.copyWithoutNulls(newItems);

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new BookingDiffCallback(oldItems, updatedItems)
        );

        items.clear();
        items.addAll(updatedItems);

        diffResult.dispatchUpdatesTo(this);
    }

    public void setCompactView(boolean compactView) {
        if (this.compactView == compactView) {
            return;
        }

        this.compactView = compactView;
        notifyDataSetChanged();
    }

    public boolean isCompactView() {
        return compactView;
    }

    public void showPaginationLoader() {
        if (showLoading) return;

        showLoading = true;
        notifyItemInserted(items.size());
    }

    public void hidePaginationLoader() {
        if (!showLoading) return;

        int loaderPosition = items.size();
        showLoading = false;
        notifyItemRemoved(loaderPosition);
    }

    @Override
    public int getItemViewType(int position) {
        if (showLoading && position == items.size()) {
            return VIEW_TYPE_LOADING;
        }

        return compactView ? VIEW_TYPE_COMPACT : VIEW_TYPE_DETAILED;
    }

    @Override
    public int getItemCount() {
        return items.size() + (showLoading ? 1 : 0);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_LOADING) {
            View view = inflater.inflate(R.layout.item_pagination_loader, parent, false);
            return new LoadingViewHolder(view);
        }

        int layoutResId = viewType == VIEW_TYPE_COMPACT
                ? R.layout.item_booking_compact
                : R.layout.item_booking;
        View view = inflater.inflate(layoutResId, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position
    ) {
        if (!(holder instanceof BookingViewHolder)) {
            return;
        }

        if (position < 0 || position >= items.size()) {
            return;
        }

        BookingItem item = items.get(position);
        BookingViewHolder bookingHolder = (BookingViewHolder) holder;

        bookingHolder.bind(item);
    }

    private void bindBookingData(BookingViewHolder holder, BookingItem item) {
        holder.tvVisitorName.setText(safe(item.getVisitorName()));
        if (holder.tvReferenceNumber != null) {
            String referenceNumber = safe(item.getBookingReferenceNumber());
            holder.tvReferenceNumber.setText(referenceNumber.isEmpty() ? "" : "Booking ID: " + referenceNumber);
            holder.tvReferenceNumber.setVisibility(referenceNumber.isEmpty() ? View.GONE : View.VISIBLE);
        }
        holder.tvRoomName.setText(safe(item.getRoomName()));
        holder.tvOrganisation.setText(safe(item.getVisitorOrganisation()));
        holder.tvArrival.setText(DateTimeUtils.formatUtcToLocal(item.getArrivalAt()));
        holder.tvDeparture.setText(DateTimeUtils.formatUtcToLocal(item.getDepartureAt()));
        if (holder.tvDateRange != null) {
            holder.tvDateRange.setText(
                    DateTimeUtils.formatUtcToCompactLocal(item.getArrivalAt())
                            + " → "
                            + DateTimeUtils.formatUtcToCompactLocal(item.getDepartureAt())
            );
        }
        holder.tvPurpose.setText(safe(item.getPurposeOfVisit()));
        String requestorName = safe(item.getRequestorName());
        holder.tvRequestedBy.setText(
                holder.tvDateRange != null
                        ? (requestorName.isEmpty() ? "" : "By: " + requestorName)
                        : "Requestor: " + requestorName
        );
        holder.tvCreatedBy.setText("Created By: " + safe(item.getCreatedByName()));
    }

    private void bindAvatar(BookingViewHolder holder, BookingItem item) {
        holder.ivAvatar.setImageResource(getAvatarRes(item.getVisitorGender()));
        holder.ivAvatar.setColorFilter(getGenderColor(item.getVisitorGender()));
    }

    private void bindStatus(BookingViewHolder holder, BookingItem item) {
        String status = normalizeStatus(item.getStatus());

        switch (status) {
            case BookingStatus.EXPIRED:
                applyExpiredStatus(holder);
                break;

            case BookingStatus.ACTIVE:
            default:
                applyActiveStatus(holder);
                break;
        }
    }

    private void applyActiveStatus(BookingViewHolder holder) {
        holder.tvStatus.setText("Active");
        holder.tvStatus.setTextColor(
                ContextCompat.getColor(context, R.color.status_active)
        );
        holder.tvStatus.setBackground(
                ContextCompat.getDrawable(context, R.drawable.bg_status_active_soft)
        );
        applyCardAppearance(
                holder,
                R.color.booking_card_bg,
                R.color.booking_card_active_stroke
        );
    }

    private void applyExpiredStatus(BookingViewHolder holder) {
        holder.tvStatus.setText("Expired");
        holder.tvStatus.setTextColor(
                ContextCompat.getColor(context, R.color.status_expired)
        );
        holder.tvStatus.setBackground(
                ContextCompat.getDrawable(context, R.drawable.bg_status_expired)
        );
        applyCardAppearance(
                holder,
                R.color.booking_card_expired_bg,
                R.color.booking_card_expired_stroke
        );
    }

    private void applyCardAppearance(
            BookingViewHolder holder,
            int backgroundColorRes,
            int strokeColorRes
    ) {
        holder.itemView.setAlpha(1.0f);
        holder.cardView.setCardBackgroundColor(
                ContextCompat.getColor(context, backgroundColorRes)
        );
        holder.cardView.setStrokeColor(
                ContextCompat.getColor(context, strokeColorRes)
        );
    }

    private void bindClickListeners(BookingViewHolder holder, BookingItem item) {
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBookingClick(item);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            int position = holder.getBindingAdapterPosition();

            if (position == RecyclerView.NO_POSITION) {
                return true;
            }

            if (listener != null) {
                listener.onBookingLongClick(item, position);
            }

            return true;
        });
    }

    private String normalizeStatus(String status) {
        return BookingStatus.normalizeForList(status);
    }

    private int getAvatarRes(String gender) {
        String normalizedGender = normalizeGender(gender);

        switch (normalizedGender) {
            case GENDER_MALE:
                return R.drawable.ic_person_male;

            case GENDER_FEMALE:
                return R.drawable.ic_person_female;

            default:
                return R.drawable.ic_person;
        }
    }

    private int getGenderColor(String gender) {
        String normalizedGender = normalizeGender(gender);

        switch (normalizedGender) {
            case GENDER_MALE:
                return ContextCompat.getColor(context, R.color.male_color);

            case GENDER_FEMALE:
                return ContextCompat.getColor(context, R.color.female_color);

            default:
                return ContextCompat.getColor(context, R.color.neutral_color);
        }
    }

    private String normalizeGender(String gender) {
        if (gender == null) {
            return "";
        }

        return gender.trim().toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value != null ? value.trim() : "";
    }

    private class BookingViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardView;
        private final TextView tvVisitorName;
        private final TextView tvReferenceNumber;
        private final TextView tvRequestedBy;
        private final TextView tvCreatedBy;
        private final TextView tvRoomName;
        private final TextView tvOrganisation;
        private final TextView tvArrival;
        private final TextView tvDeparture;
        private final TextView tvPurpose;
        private final TextView tvStatus;
        private final TextView tvDateRange;
        private final ImageView ivAvatar;

        BookingViewHolder(@NonNull View itemView) {
            super(itemView);

            cardView = (MaterialCardView) itemView;
            tvVisitorName = itemView.findViewById(R.id.tvVisitorName);
            tvReferenceNumber = itemView.findViewById(R.id.tvReferenceNumber);
            tvRoomName = itemView.findViewById(R.id.tvRoomName);
            tvOrganisation = itemView.findViewById(R.id.tvOrganisation);
            tvArrival = itemView.findViewById(R.id.tvArrival);
            tvDeparture = itemView.findViewById(R.id.tvDeparture);
            tvPurpose = itemView.findViewById(R.id.tvPurpose);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDateRange = itemView.findViewById(R.id.tvDateRange);
            tvRequestedBy = itemView.findViewById(R.id.tvRequestedBy);
            tvCreatedBy = itemView.findViewById(R.id.tvCreatedBy);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
        }

        private void bind(BookingItem item) {
            bindBookingData(this, item);
            bindAvatar(this, item);
            bindStatus(this, item);
            bindClickListeners(this, item);
        }
    }

    private static class LoadingViewHolder extends RecyclerView.ViewHolder {

        LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    private static class BookingDiffCallback extends DiffUtil.Callback {

        private final List<BookingItem> oldList;
        private final List<BookingItem> newList;

        BookingDiffCallback(
                List<BookingItem> oldList,
                List<BookingItem> newList
        ) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            BookingItem oldItem = oldList.get(oldItemPosition);
            BookingItem newItem = newList.get(newItemPosition);

            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            BookingItem oldItem = oldList.get(oldItemPosition);
            BookingItem newItem = newList.get(newItemPosition);

            return oldItem.hasSameContent(newItem);
        }
    }
}
