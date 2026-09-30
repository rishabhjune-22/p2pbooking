package com.example.roombooking.notification;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class NotificationBadgeDrawable extends Drawable {
    private final Drawable icon;
    private final Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int count;

    NotificationBadgeDrawable(Drawable icon, int count, int badgeColor, int textColor) {
        this.icon = icon.mutate();
        this.count = count;
        badgePaint.setColor(badgeColor);
        textPaint.setColor(textColor);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
    }

    @Override protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        icon.setBounds(bounds);
    }

    @Override public void draw(@NonNull Canvas canvas) {
        icon.draw(canvas);
        if (count <= 0) return;
        Rect bounds = getBounds();
        float radius = Math.max(7f, bounds.width() * 0.27f);
        float centerX = bounds.right - radius * 0.55f;
        float centerY = bounds.top + radius * 0.65f;
        canvas.drawCircle(centerX, centerY, radius, badgePaint);
        String label = count > 99 ? "99+" : String.valueOf(count);
        textPaint.setTextSize(label.length() > 2 ? radius * 0.85f : radius * 1.15f);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        canvas.drawText(label, centerX, centerY - (metrics.ascent + metrics.descent) / 2f, textPaint);
    }

    @Override public void setAlpha(int alpha) {
        icon.setAlpha(alpha);
        badgePaint.setAlpha(alpha);
        textPaint.setAlpha(alpha);
    }

    @Override public void setColorFilter(@Nullable ColorFilter colorFilter) {
        icon.setColorFilter(colorFilter);
    }

    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    @Override public int getIntrinsicWidth() { return icon.getIntrinsicWidth(); }
    @Override public int getIntrinsicHeight() { return icon.getIntrinsicHeight(); }
}
