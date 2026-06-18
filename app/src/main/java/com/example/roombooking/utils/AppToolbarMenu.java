package com.example.roombooking.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import androidx.appcompat.app.AlertDialog;

import com.example.roombooking.R;
import com.example.roombooking.booking.LandingActivity;
import com.example.roombooking.home.HomeActivity;
import com.google.android.material.appbar.MaterialToolbar;

public final class AppToolbarMenu {

    private AppToolbarMenu() {
    }

    public static void setup(Activity activity, MaterialToolbar toolbar) {
        toolbar.post(() -> tintMenuIcon(activity, toolbar));
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() != R.id.actionBreadcrumb) {
                return false;
            }

            View menuView = toolbar.findViewById(R.id.actionBreadcrumb);
            showPopup(activity, menuView != null ? menuView : toolbar);
            return true;
        });
    }

    private static void tintMenuIcon(Activity activity, MaterialToolbar toolbar) {
        MenuItem item = toolbar.getMenu().findItem(R.id.actionBreadcrumb);

        if (item != null && item.getIcon() != null) {
            item.getIcon().setTint(activity.getColor(R.color.white));
        }
    }

    private static void showPopup(Activity activity, View anchor) {
        PopupMenu popupMenu = new PopupMenu(activity, anchor, Gravity.END);
        popupMenu.getMenuInflater().inflate(R.menu.menu_landing_popup, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> handleItem(activity, item.getItemId()));
        popupMenu.show();
    }

    private static boolean handleItem(Activity activity, int itemId) {
        if (itemId == R.id.menuBookings) {
            open(activity, HomeActivity.class);
            return true;
        }

        if (itemId == R.id.menuAvailability) {
            open(activity, LandingActivity.class);
            return true;
        }

        if (itemId == R.id.menuAboutUs) {
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.about_title)
                    .setMessage(R.string.about_message)
                    .setPositiveButton(R.string.action_close, null)
                    .show();
            return true;
        }

        return false;
    }

    private static void open(Activity activity, Class<? extends Activity> destination) {
        Intent intent = new Intent(activity, destination);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }
}
