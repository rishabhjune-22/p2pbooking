package com.example.roombooking.booking;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roombooking.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

class AvailabilityDayAdapter extends RecyclerView.Adapter<AvailabilityDayAdapter.DayViewHolder> {

    interface OnDayClickListener {
        void onDayClick(RoomAvailabilityDay day);
    }

    private static final float ALPHA_UNAVAILABLE = 0.45f;
    private static final float ALPHA_AVAILABLE = 1.0f;

    private final List<RoomAvailabilityDay> days = new ArrayList<>();
    private final OnDayClickListener listener;

    private final SimpleDateFormat apiDateFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private final SimpleDateFormat dayFormat =
            new SimpleDateFormat("d", Locale.getDefault());

    AvailabilityDayAdapter(OnDayClickListener listener) {
        this.listener = listener;
    }

    void submitList(List<RoomAvailabilityDay> newDays) {
        List<RoomAvailabilityDay> oldDays = new ArrayList<>(days);
        List<RoomAvailabilityDay> updatedDays = newDays != null
                ? new ArrayList<>(newDays)
                : new ArrayList<>();

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new AvailabilityDayDiffCallback(oldDays, updatedDays)
        );

        days.clear();
        days.addAll(updatedDays);

        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_availability_day, parent, false);

        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull DayViewHolder holder,
            int position
    ) {
        if (position < 0 || position >= days.size()) {
            return;
        }

        RoomAvailabilityDay item = days.get(position);

        if (item == null) {
            holder.bindEmpty();
            return;
        }

        holder.bind(
                item,
                listener,
                getDayNumber(item.getDate())
        );
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    private String getDayNumber(String dateValue) {
        if (isBlank(dateValue)) {
            return "";
        }

        try {
            Date date = apiDateFormat.parse(dateValue.trim());

            if (date != null) {
                return dayFormat.format(date);
            }
        } catch (ParseException ignored) {
            // Return empty string for invalid date.
        }

        return "";
    }

    private static boolean hasSameContent(
            RoomAvailabilityDay oldItem,
            RoomAvailabilityDay newItem
    ) {
        if (oldItem == null && newItem == null) return true;
        if (oldItem == null || newItem == null) return false;

        return oldItem.hasSameContent(newItem);
    }

    private static String safe(String value) {
        return value != null ? value.trim() : "";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {

        private final LinearLayout dayContainer;
        private final TextView tvDayNumber;
        private final TextView tvAvailabilityCount;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);

            dayContainer = itemView.findViewById(R.id.dayContainer);
            tvDayNumber = itemView.findViewById(R.id.tvDayNumber);
            tvAvailabilityCount = itemView.findViewById(R.id.tvAvailabilityCount);
        }

        private void bind(
                RoomAvailabilityDay item,
                OnDayClickListener listener,
                String dayNumber
        ) {
            itemView.setVisibility(View.VISIBLE);

            bindDayData(item, dayNumber);
            bindAvailabilityStyle(item);
            bindDayBackground(item);
            bindClickListener(item, listener);
        }

        private void bindDayData(RoomAvailabilityDay item, String dayNumber) {
            tvDayNumber.setText(dayNumber);
            tvAvailabilityCount.setText(String.valueOf(item.getAvailableRooms()));
        }

        private void bindAvailabilityStyle(RoomAvailabilityDay item) {
            if (item.getAvailableRooms() == 0) {
                tvAvailabilityCount.setTextColor(
                        itemView.getContext().getColor(R.color.detail_cancel_red)
                );
                dayContainer.setAlpha(ALPHA_UNAVAILABLE);
                return;
            }

            tvAvailabilityCount.setTextColor(
                    itemView.getContext().getColor(R.color.success_green)
            );
            dayContainer.setAlpha(ALPHA_AVAILABLE);
        }

        private void bindDayBackground(RoomAvailabilityDay item) {
            if (item.hasBefore6pmBooking()) {
                dayContainer.setBackgroundResource(R.drawable.bg_day_orange);
                return;
            }

            dayContainer.setBackgroundResource(R.drawable.bg_detail_card);
        }

        private void bindClickListener(
                RoomAvailabilityDay item,
                OnDayClickListener listener
        ) {
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDayClick(item);
                }
            });
        }

        private void bindEmpty() {
            itemView.setVisibility(View.INVISIBLE);
            itemView.setOnClickListener(null);
        }
    }

    private static class AvailabilityDayDiffCallback extends DiffUtil.Callback {

        private final List<RoomAvailabilityDay> oldList;
        private final List<RoomAvailabilityDay> newList;

        AvailabilityDayDiffCallback(
                List<RoomAvailabilityDay> oldList,
                List<RoomAvailabilityDay> newList
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
        public boolean areItemsTheSame(
                int oldItemPosition,
                int newItemPosition
        ) {
            RoomAvailabilityDay oldItem = oldList.get(oldItemPosition);
            RoomAvailabilityDay newItem = newList.get(newItemPosition);

            if (oldItem == null && newItem == null) {
                return true;
            }

            if (oldItem == null || newItem == null) {
                return false;
            }

            return safe(oldItem.getDate()).equals(safe(newItem.getDate()));
        }

        @Override
        public boolean areContentsTheSame(
                int oldItemPosition,
                int newItemPosition
        ) {
            RoomAvailabilityDay oldItem = oldList.get(oldItemPosition);
            RoomAvailabilityDay newItem = newList.get(newItemPosition);

            return hasSameContent(oldItem, newItem);
        }
    }
}