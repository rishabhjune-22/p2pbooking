package com.example.roombooking.booking;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

final class RoomSpinnerAdapter extends ArrayAdapter<RoomSpinnerEntry> {

    RoomSpinnerAdapter(Context context, List<RoomSpinnerEntry> entries) {
        super(context, android.R.layout.simple_spinner_item, entries);
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    @Override
    public boolean isEnabled(int position) {
        RoomSpinnerEntry entry = getItem(position);
        return entry != null && entry.isSelectable();
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        View view = super.getDropDownView(position, convertView, parent);
        RoomSpinnerEntry entry = getItem(position);

        if (view instanceof TextView && entry != null) {
            TextView textView = (TextView) view;
            textView.setTypeface(null, entry.isHeader() ? Typeface.BOLD : Typeface.NORMAL);
            textView.setAlpha(entry.isHeader() ? 0.72f : 1.0f);
        }

        return view;
    }
}
