package com.example.roombooking.home;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roombooking.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

class CompactDateRangeAdapter
        extends RecyclerView.Adapter<CompactDateRangeAdapter.DayViewHolder> {

    interface OnDateClickListener {
        void onDateClick(Calendar date);
    }

    private final List<Calendar> days = new ArrayList<>();
    private final OnDateClickListener listener;
    private Long arrivalMillis;
    private Long departureMillis;

    CompactDateRangeAdapter(OnDateClickListener listener) {
        this.listener = listener;
    }

    void showMonth(Calendar month) {
        days.clear();

        Calendar firstDay = normalizedCopy(month);
        firstDay.set(Calendar.DAY_OF_MONTH, 1);
        int leadingEmptyDays = firstDay.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
        for (int index = 0; index < leadingEmptyDays; index++) {
            days.add(null);
        }

        int dayCount = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int day = 1; day <= dayCount; day++) {
            Calendar date = normalizedCopy(firstDay);
            date.set(Calendar.DAY_OF_MONTH, day);
            days.add(date);
        }

        while (days.size() < 42) {
            days.add(null);
        }
        notifyDataSetChanged();
    }

    void setSelectedRange(Calendar arrival, Calendar departure) {
        arrivalMillis = arrival != null ? normalizedCopy(arrival).getTimeInMillis() : null;
        departureMillis = departure != null ? normalizedCopy(departure).getTimeInMillis() : null;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_compact_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        Calendar date = days.get(position);
        holder.bind(date);
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    private Calendar normalizedCopy(Calendar source) {
        Calendar copy = (Calendar) source.clone();
        copy.set(Calendar.HOUR_OF_DAY, 0);
        copy.set(Calendar.MINUTE, 0);
        copy.set(Calendar.SECOND, 0);
        copy.set(Calendar.MILLISECOND, 0);
        return copy;
    }

    class DayViewHolder extends RecyclerView.ViewHolder {
        private final TextView dayView;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            dayView = itemView.findViewById(R.id.tvCompactCalendarDay);
        }

        void bind(Calendar date) {
            if (date == null) {
                dayView.setText("");
                dayView.setBackground(null);
                dayView.setOnClickListener(null);
                dayView.setClickable(false);
                return;
            }

            long dateMillis = date.getTimeInMillis();
            boolean isEndpoint = (arrivalMillis != null && dateMillis == arrivalMillis)
                    || (departureMillis != null && dateMillis == departureMillis);
            boolean isInRange = arrivalMillis != null
                    && departureMillis != null
                    && dateMillis > arrivalMillis
                    && dateMillis < departureMillis;

            dayView.setText(String.valueOf(date.get(Calendar.DAY_OF_MONTH)));
            dayView.setTextColor(ContextCompat.getColor(
                    dayView.getContext(),
                    isEndpoint ? R.color.white : R.color.text_primary
            ));
            dayView.setBackground(createDayBackground(isEndpoint, isInRange));
            dayView.setClickable(true);
            dayView.setOnClickListener(v -> listener.onDateClick(normalizedCopy(date)));
        }

        private GradientDrawable createDayBackground(boolean isEndpoint, boolean isInRange) {
            GradientDrawable background = new GradientDrawable();
            background.setShape(GradientDrawable.RECTANGLE);
            background.setCornerRadius(dayView.getResources().getDimension(R.dimen.space_20));

            int color = android.graphics.Color.TRANSPARENT;
            if (isEndpoint) {
                color = ContextCompat.getColor(dayView.getContext(), R.color.primary);
            } else if (isInRange) {
                color = ContextCompat.getColor(dayView.getContext(), R.color.primary_light);
            }
            background.setColor(color);
            return background;
        }
    }
}
