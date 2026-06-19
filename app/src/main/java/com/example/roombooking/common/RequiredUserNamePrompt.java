package com.example.roombooking.common;

import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.roombooking.R;

public final class RequiredUserNamePrompt {

    public interface Callback {
        void onUserNameSaved(String name);
    }

    private RequiredUserNamePrompt() {
    }

    public static AlertDialog show(
            AppCompatActivity activity,
            LocalUserManager localUserManager,
            Callback callback
    ) {
        EditText input = new EditText(activity);
        input.setHint(activity.getString(R.string.hint_enter_name));
        input.setSingleLine(true);

        int padding = getDialogInputPadding(activity);
        input.setPadding(padding, padding, padding, padding);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.dialog_title_welcome)
                .setMessage(R.string.dialog_message_enter_name)
                .setView(input)
                .setCancelable(false)
                .setPositiveButton(R.string.action_continue, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String name = input.getText() != null
                            ? input.getText().toString().trim()
                            : "";

                    if (!LocalUserManager.isValidUserName(name)) {
                        input.setError(activity.getString(R.string.error_name_required));
                        return;
                    }

                    localUserManager.saveUserName(name);
                    dialog.dismiss();

                    if (callback != null) {
                        callback.onUserNameSaved(name);
                    }
                }));

        dialog.show();
        return dialog;
    }

    private static int getDialogInputPadding(AppCompatActivity activity) {
        return (int) (20 * activity.getResources().getDisplayMetrics().density);
    }
}
