package com.example.p2proombooking;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.p2proombooking.BookingEntity;

import java.util.ArrayList;
import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.VH> {

    private final List<BookingEntity> items = new ArrayList<>();

    public void submit(List<BookingEntity> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        BookingEntity b = items.get(position);
        h.tvRoom.setText("Room: " + b.roomId + " | Status: " + b.status);
        h.tvTime.setText("Start: " + b.startUtc + "   End: " + b.endUtc);
        h.tvMeta.setText("By: " + b.createdByUserId + "\nBookingId: " + b.bookingId);
    }

    @Override
    public int getItemCount() {
        return items.size();
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
}