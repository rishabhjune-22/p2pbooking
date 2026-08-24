package com.example.roombooking.booking;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roombooking.R;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class CalendarAvailabilityAdapter
        extends RecyclerView.Adapter<CalendarAvailabilityAdapter.CalendarDayViewHolder> {

    public interface OnDayClickListener {
        void onDayClick(CalendarDayItem item);
    }

    private final List<CalendarDayItem> items = new ArrayList<>();
    private final OnDayClickListener listener;

    private String selectedArrivalDate = null;
    private String selectedDepartureDate = null;

    public CalendarAvailabilityAdapter(OnDayClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<CalendarDayItem> newItems) {
        List<CalendarDayItem> oldItems = new ArrayList<>(items);
        List<CalendarDayItem> updatedItems = newItems != null
                ? new ArrayList<>(newItems)
                : new ArrayList<>();

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new CalendarDayDiffCallback(oldItems, updatedItems)
        );

        items.clear();
        items.addAll(updatedItems);

        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public CalendarDayViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);

        return new CalendarDayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull CalendarDayViewHolder holder,
            int position
    ) {
        if (position < 0 || position >= items.size()) {
            return;
        }

        CalendarDayItem item = items.get(position);
        boolean isInSelectedRange = isDateInSelectedRange(item);

        holder.bind(item, listener, isInSelectedRange);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public CalendarDayItem getItemAt(int position) {
        if (position == RecyclerView.NO_POSITION || position < 0 || position >= items.size()) {
            return null;
        }

        return items.get(position);
    }

    public void setSelectedRange(String arrivalDate, String departureDate) {
        String oldStartDate = selectedArrivalDate;
        String oldEndDate = selectedDepartureDate;

        selectedArrivalDate = arrivalDate;
        selectedDepartureDate = departureDate;

        notifyRangeSelectionChanged(
                oldStartDate,
                oldEndDate,
                selectedArrivalDate,
                selectedDepartureDate
        );
    }

    public void clearSelectedRange() {
        String oldStartDate = selectedArrivalDate;
        String oldEndDate = selectedDepartureDate;

        selectedArrivalDate = null;
        selectedDepartureDate = null;

        notifyRangeSelectionChanged(
                oldStartDate,
                oldEndDate,
                null,
                null
        );
    }

    private void notifyRangeSelectionChanged(
            String oldStartDate,
            String oldEndDate,
            String newStartDate,
            String newEndDate
    ) {
        for (int i = 0; i < items.size(); i++) {
            CalendarDayItem item = items.get(i);

            boolean wasSelected = isDateInRange(item, oldStartDate, oldEndDate);
            boolean isSelected = isDateInRange(item, newStartDate, newEndDate);

            if (wasSelected != isSelected) {
                notifyItemChanged(i);
            }
        }
    }

    private boolean isDateInSelectedRange(CalendarDayItem item) {
        return isDateInRange(item, selectedArrivalDate, selectedDepartureDate);
    }

    private boolean isDateInRange(
            CalendarDayItem item,
            String startDate,
            String endDate
    ) {
        if (item == null || item.isEmpty()) {
            return false;
        }

        if (isBlank(startDate) || isBlank(endDate)) {
            return false;
        }

        String normalizedStartDate = startDate.trim();
        String normalizedEndDate = endDate.trim();

        if (normalizedStartDate.compareTo(normalizedEndDate) > 0) {
            String temp = normalizedStartDate;
            normalizedStartDate = normalizedEndDate;
            normalizedEndDate = temp;
        }

        String currentDate = item.getDate();

        if (isBlank(currentDate)) {
            return false;
        }

        return currentDate.compareTo(normalizedStartDate) >= 0
                && currentDate.compareTo(normalizedEndDate) <= 0;
    }

    private static boolean hasSameContent(
            CalendarDayItem oldItem,
            CalendarDayItem newItem
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

    static class CalendarDayViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardDay;
        private final TextView tvDayNumber;
        private final TextView tvAvailableRooms;

        CalendarDayViewHolder(@NonNull View itemView) {
            super(itemView);

            cardDay = itemView.findViewById(R.id.cardDay);
            tvDayNumber = itemView.findViewById(R.id.tvDayNumber);
            tvAvailableRooms = itemView.findViewById(R.id.tvAvailableRooms);
        }

        void bind(
                CalendarDayItem item,
                OnDayClickListener listener,
                boolean isInSelectedRange
        ) {
            if (item == null || item.isEmpty()) {
                bindEmptyDay();
                return;
            }

            bindVisibleDay(item, listener, isInSelectedRange);
        }

        private void bindEmptyDay() {
            tvDayNumber.setText("");
            tvAvailableRooms.setText("");

            cardDay.setVisibility(View.INVISIBLE);
            cardDay.setOnClickListener(null);
        }

        private void bindVisibleDay(
                CalendarDayItem item,
                OnDayClickListener listener,
                boolean isInSelectedRange
        ) {
            cardDay.setVisibility(View.VISIBLE);
            cardDay.setEnabled(true);
            cardDay.setClickable(true);
            cardDay.setAlpha(1.0f);

            tvDayNumber.setText(String.valueOf(item.getDayNumber()));
            tvAvailableRooms.setText(String.valueOf(item.getAvailableRooms()));

            if (isInSelectedRange) {
                applySelectedRangeStyle();
                cardDay.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDayClick(item);
                    }
                });
            } else {
                applyAvailabilityStyle(item);
                cardDay.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDayClick(item);
                    }
                });
            }
        }

        private void applySelectedRangeStyle() {
            cardDay.setCardBackgroundColor(
                    itemView.getContext().getColor(R.color.availability_selected_range)
            );

            tvDayNumber.setTextColor(
                    itemView.getContext().getColor(android.R.color.white)
            );

            tvAvailableRooms.setTextColor(
                    itemView.getContext().getColor(android.R.color.white)
            );
        }

        private void applyAvailabilityStyle(CalendarDayItem item) {
            switch (item.getAvailabilityType()) {
                case CalendarDayItem.TYPE_NOT_AVAILABLE:
                    applyNotAvailableStyle();
                    break;

                case CalendarDayItem.TYPE_HALF_AVAILABLE:
                    applyHalfAvailableStyle();
                    break;

                case CalendarDayItem.TYPE_LESS_THAN_HALF_AVAILABLE:
                    applyLessThanHalfAvailableStyle();
                    break;

                case CalendarDayItem.TYPE_AVAILABLE:
                default:
                    applyAvailableStyle();
                    break;
            }
        }

        private void applyAvailableStyle() {
            cardDay.setCardBackgroundColor(
                    itemView.getContext().getColor(android.R.color.white)
            );

            tvDayNumber.setTextColor(
                    itemView.getContext().getColor(R.color.detail_text_primary)
            );

            tvAvailableRooms.setTextColor(
                    itemView.getContext().getColor(R.color.info_blue)
            );
        }

        private void applyNotAvailableStyle() {
            cardDay.setCardBackgroundColor(
                    itemView.getContext().getColor(R.color.availability_not_available)
            );

            tvDayNumber.setTextColor(
                    itemView.getContext().getColor(android.R.color.white)
            );

            tvAvailableRooms.setTextColor(
                    itemView.getContext().getColor(android.R.color.white)
            );
        }

        private void applyHalfAvailableStyle() {
            cardDay.setCardBackgroundColor(
                    itemView.getContext().getColor(R.color.availability_half_available)
            );

            tvDayNumber.setTextColor(
                    itemView.getContext().getColor(R.color.detail_text_primary)
            );

            tvAvailableRooms.setTextColor(
                    itemView.getContext().getColor(R.color.info_blue)
            );
        }

        private void applyLessThanHalfAvailableStyle() {
            cardDay.setCardBackgroundColor(
                    itemView.getContext().getColor(
                            R.color.availability_less_than_half_available
                    )
            );

            tvDayNumber.setTextColor(
                    itemView.getContext().getColor(android.R.color.white)
            );

            tvAvailableRooms.setTextColor(
                    itemView.getContext().getColor(android.R.color.white)
            );
        }
    }

    private static class CalendarDayDiffCallback extends DiffUtil.Callback {

        private final List<CalendarDayItem> oldList;
        private final List<CalendarDayItem> newList;

        CalendarDayDiffCallback(
                List<CalendarDayItem> oldList,
                List<CalendarDayItem> newList
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
            CalendarDayItem oldItem = oldList.get(oldItemPosition);
            CalendarDayItem newItem = newList.get(newItemPosition);

            if (oldItem == null && newItem == null) {
                return true;
            }

            if (oldItem == null || newItem == null) {
                return false;
            }

            if (oldItem.isEmpty() && newItem.isEmpty()) {
                return oldItemPosition == newItemPosition;
            }

            return safe(oldItem.getDate()).equals(safe(newItem.getDate()));
        }

        @Override
        public boolean areContentsTheSame(
                int oldItemPosition,
                int newItemPosition
        ) {
            CalendarDayItem oldItem = oldList.get(oldItemPosition);
            CalendarDayItem newItem = newList.get(newItemPosition);

            return hasSameContent(oldItem, newItem);
        }
    }
}
