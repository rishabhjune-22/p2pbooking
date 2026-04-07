package com.example.roombooking;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnBookingClickListener {
        void onBookingClick(BookingItem bookingItem);
        void onBookingLongClick(BookingItem bookingItem, int position);
    }

    private static final int VIEW_TYPE_ITEM = 1;
    private static final int VIEW_TYPE_LOADING = 2;
    private final SessionManager sessionManager;

    private final List<BookingItem> items = new ArrayList<>();
    private boolean showLoading = false;
    private final OnBookingClickListener listener;

    public BookingAdapter(Context context, OnBookingClickListener listener) {
        this.sessionManager = new SessionManager(context);
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
        if (newItems == null || newItems.isEmpty()) return;
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

    public void updateBookingStatus(int position, String newStatus) {
        if (position >= 0 && position < items.size()) {
            items.get(position).setStatus(newStatus);
            notifyItemChanged(position);
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
        } else {
            View view = inflater.inflate(R.layout.item_booking, parent, false);
            return new BookingViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof BookingViewHolder) {
            BookingItem item = items.get(position);
            BookingViewHolder bookingHolder = (BookingViewHolder) holder;

            bookingHolder.tvVisitorName.setText(item.getVisitor_name());
            bookingHolder.tvRoomName.setText("Room: " + safe(item.getRoom_name()));
            bookingHolder.tvOrganisation.setText("Organisation: " + safe(item.getVisitor_organisation()));
            bookingHolder.tvArrival.setText("Arrival: " + safe(item.getArrival_date()) + " " + safe(item.getArrival_time()));
            bookingHolder.tvDeparture.setText("Departure: " + safe(item.getDeparture_date()) + " " + safe(item.getDeparture_time()));
            bookingHolder.tvPurpose.setText("Purpose: " + safe(item.getPurpose_of_visit()));
            bookingHolder.tvStatus.setText("Status: " + safe(item.getStatus()));
            bookingHolder.tvCreatedBy.setText("Created By: " + safe(item.getCreated_by_username()));
            String currentUser = sessionManager.getUsername();

            if (currentUser != null && currentUser.equalsIgnoreCase(item.getCreated_by_username())) {
                bookingHolder.tvCreatedBy.setText("Created By: You");
            }

            if (currentUser != null && currentUser.equalsIgnoreCase(item.getCreated_by_username())) {
                bookingHolder.itemView.setAlpha(1f);
            } else {
                bookingHolder.itemView.setAlpha(0.9f);
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

            if ("cancelled".equalsIgnoreCase(item.getStatus())) {
                bookingHolder.tvStatus.setText("Status: Cancelled");
                bookingHolder.itemView.setAlpha(0.6f);
            } else {
                bookingHolder.itemView.setAlpha(1f);
            }
        }



    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvVisitorName,tvCreatedBy, tvRoomName, tvOrganisation, tvArrival, tvDeparture, tvPurpose, tvStatus;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
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
        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public void updateBookingStatusById(int bookingId, String newStatus) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId() == bookingId) {
                items.get(i).setStatus(newStatus);
                notifyItemChanged(i);
                break;
            }
        }
    }
}