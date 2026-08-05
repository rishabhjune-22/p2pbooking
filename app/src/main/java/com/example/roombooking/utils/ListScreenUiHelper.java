package com.example.roombooking.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.roombooking.R;
import com.google.android.material.card.MaterialCardView;

import java.util.Locale;

public final class ListScreenUiHelper {

    public interface FilterSelectionListener {
        void onFilterSelected(String filter);
    }

    private ListScreenUiHelper() {
        // Utility class.
    }

    public static HorizontalScrollView createFilterBar(
            Context context,
            String[] filters,
            String selectedFilter,
            FilterSelectionListener listener
    ) {
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        scrollView.setHorizontalScrollBarEnabled(false);
        scrollView.setFillViewport(false);
        scrollView.setBackgroundColor(context.getColor(R.color.booking_list_bg));

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 6));

        for (String filter : filters) {
            TextView chip = new TextView(context);
            chip.setTag(filter);
            chip.setText(filter);
            chip.setTextSize(13);
            chip.setTypeface(null, Typeface.BOLD);
            chip.setSingleLine(true);
            chip.setEllipsize(TextUtils.TruncateAt.END);
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setPadding(dp(context, 16), dp(context, 8), dp(context, 16), dp(context, 8));
            chip.setClickable(true);
            chip.setOnClickListener(v -> {
                updateFilterBar(context, container, filter);
                if (listener != null) {
                    listener.onFilterSelected(filter);
                }
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, dp(context, 8), 0);
            container.addView(chip, params);
        }

        scrollView.addView(container);
        updateFilterBar(context, container, selectedFilter);
        return scrollView;
    }

    public static void updateFilterBar(Context context, LinearLayout container, String selectedFilter) {
        if (container == null) {
            return;
        }
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (!(child instanceof TextView)) {
                continue;
            }
            TextView chip = (TextView) child;
            boolean selected = safe(selectedFilter).equalsIgnoreCase(safe(String.valueOf(chip.getTag())));
            chip.setTextColor(context.getColor(selected ? R.color.white : R.color.info_blue));
            chip.setBackground(rounded(
                    context,
                    selected ? context.getColor(R.color.info_blue) : context.getColor(R.color.palette_f0f9ff),
                    context.getColor(R.color.info_blue),
                    1,
                    18
            ));
        }
    }

    public static TextView detailRow(Context context, String label, String value) {
        TextView view = new TextView(context);
        String safeLabel = safe(label);
        String safeValue = isBlank(value) ? "-" : value.trim();
        SpannableString text = new SpannableString(safeLabel + ": " + safeValue);
        text.setSpan(
                new StyleSpan(Typeface.BOLD),
                0,
                safeLabel.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        text.setSpan(
                new ForegroundColorSpan(context.getColor(R.color.detail_text_secondary)),
                0,
                safeLabel.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        view.setText(text);
        view.setTextColor(context.getColor(R.color.detail_text_primary));
        view.setTextSize(14);
        view.setLineSpacing(dp(context, 2), 1f);
        view.setPadding(0, dp(context, 5), 0, dp(context, 5));
        return view;
    }

    public static TextView sectionHeader(Context context, String title) {
        TextView view = new TextView(context);
        view.setText(title);
        view.setTextColor(context.getColor(R.color.info_blue));
        view.setTextSize(13);
        view.setTypeface(null, Typeface.BOLD);
        view.setAllCaps(true);
        view.setLetterSpacing(0f);
        view.setPadding(0, dp(context, 14), 0, dp(context, 5));
        return view;
    }

    public static TextView cardTitle(Context context, String value) {
        TextView view = new TextView(context);
        view.setText(isBlank(value) ? "-" : value.trim());
        view.setTextColor(context.getColor(R.color.detail_text_primary));
        view.setTextSize(15);
        view.setTypeface(null, Typeface.BOLD);
        view.setSingleLine(true);
        view.setEllipsize(TextUtils.TruncateAt.END);
        return view;
    }

    public static TextView cardMeta(Context context, String label, String value) {
        TextView view = new TextView(context);
        String safeValue = isBlank(value) ? "-" : value.trim();
        view.setText(isBlank(label) ? safeValue : label + ": " + safeValue);
        view.setTextColor(context.getColor(R.color.detail_text_secondary));
        view.setTextSize(13);
        view.setSingleLine(true);
        view.setEllipsize(TextUtils.TruncateAt.END);
        view.setPadding(0, dp(context, 3), 0, 0);
        return view;
    }

    public static TextView cardNote(Context context, String value) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextColor(context.getColor(R.color.info_blue));
        view.setTextSize(12);
        view.setMaxLines(2);
        view.setEllipsize(TextUtils.TruncateAt.END);
        view.setPadding(dp(context, 10), dp(context, 8), dp(context, 10), dp(context, 8));
        view.setBackground(rounded(
                context,
                context.getColor(R.color.palette_f0f9ff),
                context.getColor(R.color.availability_border),
                1,
                10
        ));
        return view;
    }

    public static LinearLayout dialogContent(Context context) {
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(context, 20), dp(context, 6), dp(context, 20), dp(context, 10));
        return content;
    }

    public static void styleCard(Context context, MaterialCardView card) {
        card.setCardBackgroundColor(context.getColor(R.color.white));
        card.setStrokeColor(context.getColor(R.color.availability_border));
        card.setStrokeWidth(dp(context, 1));
        card.setRadius(dp(context, 12));
        card.setCardElevation(dp(context, 3));
        card.setUseCompatPadding(true);
    }

    public static TextView statusChip(Context context, String status) {
        String normalized = safe(status).toLowerCase(Locale.ROOT);
        int textColor;
        int backgroundColor;
        if ("approved".equals(normalized) || "accepted".equals(normalized) || "read".equals(normalized)) {
            textColor = context.getColor(R.color.success_green);
            backgroundColor = context.getColor(R.color.status_active_bg);
        } else if ("rejected".equals(normalized)) {
            textColor = context.getColor(R.color.error_red);
            backgroundColor = context.getColor(R.color.palette_fef2f2);
        } else if ("unread".equals(normalized) || "correction_required".equals(normalized)) {
            textColor = context.getColor(R.color.info_blue);
            backgroundColor = context.getColor(R.color.palette_f0f9ff);
        } else {
            textColor = context.getColor(R.color.warning_orange);
            backgroundColor = context.getColor(R.color.status_pending_bg);
        }

        TextView chip = new TextView(context);
        chip.setText(displayStatus(status));
        chip.setTextSize(12);
        chip.setTypeface(null, Typeface.BOLD);
        chip.setSingleLine(true);
        chip.setTextColor(textColor);
        chip.setPadding(dp(context, 10), dp(context, 4), dp(context, 10), dp(context, 4));
        chip.setBackground(rounded(context, backgroundColor, textColor, 1, 14));
        return chip;
    }

    public static String displayStatus(String status) {
        String safeStatus = safe(status);
        if (safeStatus.isEmpty()) {
            return "-";
        }
        if ("approved".equalsIgnoreCase(safeStatus)) {
            return "Approved";
        }
        if ("correction_required".equalsIgnoreCase(safeStatus)) {
            return "Correction Required";
        }
        return safeStatus.substring(0, 1).toUpperCase(Locale.ROOT)
                + safeStatus.substring(1).toLowerCase(Locale.ROOT);
    }

    public static boolean matchesStatusFilter(String selectedFilter, String actualStatus) {
        String filter = safe(selectedFilter).toLowerCase(Locale.ROOT);
        if (filter.isEmpty() || "all".equals(filter)) {
            return true;
        }
        String status = safe(actualStatus).toLowerCase(Locale.ROOT);
        if ("accepted".equals(filter)) {
            filter = "approved";
        }
        if ("correction required".equals(filter)) {
            filter = "correction_required";
        }
        return filter.equals(status);
    }

    public static String snippet(String value, int maxChars) {
        String safeValue = safe(value).replace('\n', ' ').trim();
        if (maxChars <= 0 || safeValue.length() <= maxChars) {
            return safeValue;
        }
        return safeValue.substring(0, Math.max(0, maxChars - 1)).trim() + "...";
    }

    public static GradientDrawable rounded(
            Context context,
            int fillColor,
            int strokeColor,
            int strokeDp,
            int radiusDp
    ) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(context, radiusDp));
        if (strokeDp > 0) {
            drawable.setStroke(dp(context, strokeDp), strokeColor);
        }
        return drawable;
    }

    public static boolean isBlank(String value) {
        return safe(value).isEmpty();
    }

    public static String safe(String value) {
        return value != null ? value.trim() : "";
    }

    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
