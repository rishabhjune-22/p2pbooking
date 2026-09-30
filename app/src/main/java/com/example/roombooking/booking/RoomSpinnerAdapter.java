package com.example.roombooking.booking;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Layout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

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
            textView.setOnLongClickListener(pressedView -> {
                TextView pressedText = (TextView) pressedView;
                if (!isTruncated(pressedText)) {
                    return false;
                }
                Toast.makeText(getContext(), entry.toString(), Toast.LENGTH_LONG).show();
                return true;
            });
        }

        return view;
    }

    private boolean isTruncated(TextView textView) {
        Layout layout = textView.getLayout();
        if (layout != null) {
            for (int line = 0; line < layout.getLineCount(); line++) {
                if (layout.getEllipsisCount(line) > 0) {
                    return true;
                }
            }
        }
        int availableWidth = textView.getWidth()
                - textView.getPaddingLeft()
                - textView.getPaddingRight();
        return availableWidth > 0
                && textView.getPaint().measureText(textView.getText().toString()) > availableWidth;
    }
}
