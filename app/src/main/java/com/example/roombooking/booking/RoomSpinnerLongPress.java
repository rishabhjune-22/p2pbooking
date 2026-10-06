package com.example.roombooking.booking;

import android.text.Layout;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

final class RoomSpinnerLongPress {

    private RoomSpinnerLongPress() {
    }

    static void attach(Spinner spinner, RoomSpinnerAdapter adapter) {
        spinner.setOnLongClickListener(view -> {
            int position = spinner.getSelectedItemPosition();
            if (position < 0 || position >= adapter.getCount()) {
                return false;
            }

            RoomSpinnerEntry entry = adapter.getItem(position);
            View selectedView = spinner.getSelectedView();
            if (entry == null
                    || !entry.isSelectable()
                    || !(selectedView instanceof TextView)
                    || !isTruncated((TextView) selectedView)) {
                return false;
            }

            Toast.makeText(spinner.getContext(), entry.toString(), Toast.LENGTH_LONG).show();
            return true;
        });
    }

    private static boolean isTruncated(TextView textView) {
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
