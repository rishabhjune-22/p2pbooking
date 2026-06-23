package com.example.roombooking.auth;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import com.example.roombooking.R;

final class PasswordVisibilityToggle {

    private PasswordVisibilityToggle() {
    }

    static void attach(EditText field) {
        if (field == null) {
            return;
        }

        ToggleState state = new ToggleState(field);
        state.apply(false);
        field.setOnTouchListener((view, event) -> {
            if (event.getAction() != MotionEvent.ACTION_UP || !isEndIconTouched(field, event)) {
                return false;
            }

            state.apply(!state.visible);
            view.performClick();
            return true;
        });
    }

    private static boolean isEndIconTouched(EditText field, MotionEvent event) {
        Drawable endIcon = field.getCompoundDrawablesRelative()[2];
        if (endIcon == null) {
            return false;
        }

        int touchSlop = dp(field.getContext(), 16);
        int threshold = field.getWidth()
                - field.getPaddingEnd()
                - endIcon.getBounds().width()
                - touchSlop;
        return event.getX() >= threshold;
    }

    private static Drawable icon(Context context, int resId) {
        Drawable drawable = AppCompatResources.getDrawable(context, resId);
        if (drawable == null) {
            return null;
        }

        Drawable wrapped = DrawableCompat.wrap(drawable.mutate());
        DrawableCompat.setTint(wrapped, context.getColor(R.color.icon_tint));
        return wrapped;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static final class ToggleState {
        private final EditText field;
        private final Drawable start;
        private final Drawable top;
        private final Drawable bottom;
        private boolean visible;

        private ToggleState(EditText field) {
            this.field = field;
            Drawable[] drawables = field.getCompoundDrawablesRelative();
            this.start = drawables[0];
            this.top = drawables[1];
            this.bottom = drawables[3];
        }

        private void apply(boolean visible) {
            this.visible = visible;
            int selection = Math.max(0, field.getSelectionStart());
            field.setTransformationMethod(visible ? null : PasswordTransformationMethod.getInstance());
            field.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    start,
                    top,
                    icon(field.getContext(), visible ? R.drawable.ic_visibility_off : R.drawable.ic_visibility),
                    bottom
            );
            field.setSelection(Math.min(selection, field.length()));
        }
    }
}
