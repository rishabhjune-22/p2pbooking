package com.example.p2proombooking;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;

import java.util.Calendar;

public class CreateBookingUi {

    public static void pickDateTime(Context ctx, Calendar target, Runnable onDone) {
        int y = target.get(Calendar.YEAR);
        int m = target.get(Calendar.MONTH);
        int d = target.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dp = new DatePickerDialog(ctx, (view, year, month, dayOfMonth) -> {
            target.set(Calendar.YEAR, year);
            target.set(Calendar.MONTH, month);
            target.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            int hh = target.get(Calendar.HOUR_OF_DAY);
            int mm = target.get(Calendar.MINUTE);

            TimePickerDialog tp = new TimePickerDialog(ctx, (tview, hourOfDay, minute) -> {
                target.set(Calendar.HOUR_OF_DAY, hourOfDay);
                target.set(Calendar.MINUTE, minute);
                target.set(Calendar.SECOND, 0);
                target.set(Calendar.MILLISECOND, 0);
                onDone.run();
            }, hh, mm, false);

            tp.show();
        }, y, m, d);

        dp.show();
    }
}