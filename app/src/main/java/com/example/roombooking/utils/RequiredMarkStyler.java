package com.example.roombooking.utils;

import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.roombooking.R;

public final class RequiredMarkStyler {

    private RequiredMarkStyler() {
    }

    public static void applyTo(View root) {
        if (root == null) {
            return;
        }
        int color = ContextCompat.getColor(root.getContext(), R.color.error_red);
        applyTo(root, color);
    }

    public static CharSequence style(Context context, CharSequence source) {
        if (context == null) {
            return source;
        }
        return style(source, ContextCompat.getColor(context, R.color.error_red));
    }

    private static void applyTo(View view, int color) {
        if (view instanceof EditText) {
            EditText editText = (EditText) view;
            editText.setHint(style(editText.getHint(), color));
        } else if (view instanceof TextView) {
            TextView textView = (TextView) view;
            textView.setText(style(textView.getText(), color), TextView.BufferType.SPANNABLE);
        }

        if (!(view instanceof ViewGroup)) {
            return;
        }

        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            applyTo(group.getChildAt(i), color);
        }
    }

    private static CharSequence style(CharSequence source, int color) {
        if (source == null) {
            return null;
        }
        String text = source.toString();
        if (!text.contains("*")) {
            return source;
        }

        SpannableString styled = new SpannableString(text);
        int index = text.indexOf('*');
        while (index >= 0) {
            styled.setSpan(
                    new ForegroundColorSpan(color),
                    index,
                    index + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            index = text.indexOf('*', index + 1);
        }
        return styled;
    }
}
