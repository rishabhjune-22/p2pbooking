package com.example.roombooking.booking;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roombooking.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

class AvailabilityGroupAdapter extends RecyclerView.Adapter<AvailabilityGroupAdapter.GroupViewHolder> {

    interface OnDayClickListener {
        void onDayClick(RoomAvailabilityDay day);
    }

    private final List<RoomAvailabilityGroup> groups = new ArrayList<>();
    private final OnDayClickListener listener;

    private final SimpleDateFormat apiDateFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    AvailabilityGroupAdapter(OnDayClickListener listener) {
        this.listener = listener;
    }

    void submitList(List<RoomAvailabilityGroup> newGroups) {
        groups.clear();

        if (newGroups != null) {
            groups.addAll(newGroups);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_availability_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        RoomAvailabilityGroup group = groups.get(position);

        String title = group.getPrefix() + " - " + group.getTotalRooms() + " Rooms";
        holder.tvGroupTitle.setText(title);

        AvailabilityDayAdapter dayAdapter = new AvailabilityDayAdapter(day -> {
            if (listener != null) {
                listener.onDayClick(day);
            }
        });

        holder.rvDays.setLayoutManager(new GridLayoutManager(holder.itemView.getContext(), 7));
        holder.rvDays.setAdapter(dayAdapter);

        List<RoomAvailabilityDay> paddedCalendar =
                buildCalendarWithLeadingEmptyDays(group.getCalendar());

        dayAdapter.submitList(paddedCalendar);
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    private List<RoomAvailabilityDay> buildCalendarWithLeadingEmptyDays(
            List<RoomAvailabilityDay> calendar
    ) {
        List<RoomAvailabilityDay> result = new ArrayList<>();

        if (calendar == null || calendar.isEmpty()) {
            return result;
        }

        int emptyCells = getLeadingEmptyCellCount(calendar.get(0).getDate());

        for (int i = 0; i < emptyCells; i++) {
            result.add(null);
        }

        result.addAll(calendar);

        return result;
    }

    private int getLeadingEmptyCellCount(String firstDate) {
        try {
            Date date = apiDateFormat.parse(firstDate);

            if (date == null) {
                return 0;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

            return dayOfWeek - Calendar.SUNDAY;

        } catch (ParseException ignored) {
            return 0;
        }
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {

        TextView tvGroupTitle;
        RecyclerView rvDays;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);

            tvGroupTitle = itemView.findViewById(R.id.tvGroupTitle);
            rvDays = itemView.findViewById(R.id.rvDays);
        }
    }
}