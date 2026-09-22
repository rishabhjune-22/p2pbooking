package com.example.roombooking.booking;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.InputType;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.roombooking.model.booking.BookingMailTemplate;

public final class BookingMailTemplateDialog {

    private BookingMailTemplateDialog() {
    }

    public static void show(Context context, BookingMailTemplate template) {
        show(context, template, null);
    }

    public static void show(Context context, BookingMailTemplate template, Runnable onDismiss) {
        if (context == null || template == null) {
            return;
        }

        String subject = template.getSafeSubject();
        String body = template.getSafeBody();
        String html = template.getSafeHtml();
        if (html.trim().isEmpty()) {
            html = "<pre style=\"font-family:Arial,sans-serif;white-space:pre-wrap;\">"
                    + escapeHtml(body)
                    + "</pre>";
        }

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(context, 18);
        container.setPadding(padding, padding, padding, 0);

        TextView helper = new TextView(context);
        helper.setText("Copy this subject and email content into your mail client.");
        helper.setTextSize(13);
        container.addView(helper, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        EditText subjectField = new EditText(context);
        subjectField.setText(subject);
        subjectField.setSingleLine(true);
        subjectField.setSelectAllOnFocus(true);
        subjectField.setInputType(InputType.TYPE_NULL);
        subjectField.setFocusable(true);
        subjectField.setFocusableInTouchMode(true);
        subjectField.setHint("Subject");
        LinearLayout.LayoutParams subjectParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subjectParams.topMargin = dp(context, 12);
        container.addView(subjectField, subjectParams);

        WebView preview = new WebView(context);
        preview.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(context, 420)
        );
        previewParams.topMargin = dp(context, 12);
        container.addView(preview, previewParams);

        final String finalHtml = html;
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Booking Mail Template")
                .setView(container)
                .setNegativeButton("Close", null)
                .setNeutralButton("Copy Subject", null)
                .setPositiveButton("Copy Email Content", null)
                .create();

        dialog.setOnShowListener(shownDialog -> {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                copyPlainText(context, "Booking mail subject", subject);
                Toast.makeText(context, "Mail subject copied.", Toast.LENGTH_SHORT).show();
            });
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                copyHtml(context, body, finalHtml);
                Toast.makeText(context, "Mail content copied.", Toast.LENGTH_SHORT).show();
            });
        });

        if (onDismiss != null) {
            dialog.setOnDismissListener(ignored -> onDismiss.run());
        }

        dialog.show();
    }

    private static void copyPlainText(Context context, String label, String text) {
        ClipboardManager clipboard =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            return;
        }
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text));
    }

    private static void copyHtml(Context context, String plainText, String html) {
        ClipboardManager clipboard =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            return;
        }
        clipboard.setPrimaryClip(ClipData.newHtmlText(
                "Booking mail content",
                plainText == null ? "" : plainText,
                html == null ? "" : html
        ));
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
