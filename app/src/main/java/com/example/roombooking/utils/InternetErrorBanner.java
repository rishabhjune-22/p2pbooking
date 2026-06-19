package com.example.roombooking.utils;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.example.roombooking.R;
import com.google.android.material.appbar.MaterialToolbar;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;

public final class InternetErrorBanner {

    public static final String MESSAGE = "Please check your internet connection";

    private InternetErrorBanner() {
    }

    public static void show(Activity activity) {
        if (!canUpdateUi(activity)) {
            return;
        }

        MaterialToolbar toolbar = findToolbar(activity);
        if (toolbar == null) {
            return;
        }

        ToolbarState state = getOrCreateState(toolbar);
        toolbar.setBackgroundColor(activity.getColor(R.color.error_red));
        toolbar.setTitle(createSmallTitle(activity, MESSAGE));
        toolbar.setTitleTextColor(activity.getColor(R.color.white));
        toolbar.setVisibility(View.VISIBLE);
        toolbar.bringToFront();
        toolbar.setTag(state);
    }

    public static void hide(Activity activity) {
        MaterialToolbar toolbar = findToolbar(activity);
        if (toolbar == null) {
            return;
        }

        Object tag = toolbar.getTag();
        if (tag instanceof ToolbarState) {
            ToolbarState state = (ToolbarState) tag;
            toolbar.setBackground(state.background);
            toolbar.setTitle(state.title);
            toolbar.setTitleTextColor(
                    state.titleColor != null
                            ? state.titleColor
                            : toolbar.getResources().getColor(R.color.white)
            );
            toolbar.setTag(null);
        }
    }

    public static boolean isNetworkErrorMessage(String message) {
        if (message == null) {
            return false;
        }

        String normalizedMessage = message.trim();
        return normalizedMessage.equalsIgnoreCase(MESSAGE)
                || normalizedMessage.equalsIgnoreCase(MESSAGE + ".")
                || normalizedMessage.equalsIgnoreCase(ApiErrorUtils.NETWORK_ERROR_MESSAGE)
                || normalizedMessage.equalsIgnoreCase(ApiErrorUtils.NO_INTERNET_ERROR_MESSAGE)
                || normalizedMessage.equalsIgnoreCase(ApiErrorUtils.SERVER_UNAVAILABLE_ERROR_MESSAGE)
                || normalizedMessage.equalsIgnoreCase(ApiErrorUtils.TIMEOUT_ERROR_MESSAGE);
    }

    private static MaterialToolbar findToolbar(Activity activity) {
        if (activity == null) {
            return null;
        }

        View view = activity.findViewById(R.id.toolbar);
        if (view instanceof MaterialToolbar) {
            return (MaterialToolbar) view;
        }

        view = activity.findViewById(R.id.appToolbar);
        if (view instanceof MaterialToolbar) {
            return (MaterialToolbar) view;
        }

        return null;
    }

    private static boolean canUpdateUi(Activity activity) {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }

    private static ToolbarState getOrCreateState(MaterialToolbar toolbar) {
        Object tag = toolbar.getTag();
        if (tag instanceof ToolbarState) {
            return (ToolbarState) tag;
        }

        ToolbarState state = new ToolbarState();
        state.title = toolbar.getTitle();
        state.titleColor = toolbar.getResources().getColor(R.color.white);
        state.background = toolbar.getBackground();
        return state;
    }

    private static CharSequence createSmallTitle(Activity activity, String text) {
        SpannableString title = new SpannableString(text);
        int sizeSp = 12;
        title.setSpan(
                new AbsoluteSizeSpan(sizeSp, true),
                0,
                title.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        return title;
    }

    private static final class ToolbarState {
        private CharSequence title;
        private Integer titleColor;
        private Drawable background;
    }
}
