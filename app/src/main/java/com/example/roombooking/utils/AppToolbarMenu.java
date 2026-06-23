package com.example.roombooking.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.example.roombooking.R;
import com.example.roombooking.admin.AdminBookingRequestsActivity;
import com.example.roombooking.admin.AdminRequesterAccountsActivity;
import com.example.roombooking.auth.AuthLogoutManager;
import com.example.roombooking.auth.AuthSessionManager;
import com.example.roombooking.booking.LandingActivity;
import com.example.roombooking.home.HomeActivity;
import com.example.roombooking.requester.RequesterLandingActivity;
import com.example.roombooking.requester.RequesterRequestsActivity;
import com.google.android.material.appbar.MaterialToolbar;

public final class AppToolbarMenu {

    private AppToolbarMenu() {
    }

    public static void setup(Activity activity, MaterialToolbar toolbar) {
        setupAdmin(activity, toolbar);
    }

    public static void setupAdmin(Activity activity, MaterialToolbar toolbar) {
        setup(activity, toolbar, R.menu.menu_landing_popup, currentAdminMenuItem(activity));
    }

    public static void setupRequester(Activity activity, MaterialToolbar toolbar) {
        setup(
                activity,
                toolbar,
                R.menu.menu_requester_secondary_popup,
                currentRequesterMenuItem(activity)
        );
    }

    public static void setupAdminSecondary(Activity activity, MaterialToolbar toolbar) {
        setupAdmin(activity, toolbar);
    }

    public static void setupRequesterSecondary(Activity activity, MaterialToolbar toolbar) {
        setupRequester(activity, toolbar);
    }

    public static void setupForCurrentRoleSecondary(Activity activity, MaterialToolbar toolbar) {
        AuthSessionManager sessionManager = new AuthSessionManager(activity);
        if (sessionManager.isRequester()) {
            setupRequester(activity, toolbar);
        } else {
            setupAdmin(activity, toolbar);
        }
    }

    private static void setup(
            Activity activity,
            MaterialToolbar toolbar,
            int popupMenuRes,
            int currentMenuItemId
    ) {
        ensureToolbarMenu(toolbar);
        toolbar.post(() -> tintMenuIcon(activity, toolbar));
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() != R.id.actionBreadcrumb) {
                return false;
            }

            View menuView = toolbar.findViewById(R.id.actionBreadcrumb);
            showPopup(
                    activity,
                    menuView != null ? menuView : toolbar,
                    popupMenuRes,
                    currentMenuItemId
            );
            return true;
        });
    }

    private static void ensureToolbarMenu(MaterialToolbar toolbar) {
        if (toolbar.getMenu().findItem(R.id.actionBreadcrumb) == null) {
            toolbar.inflateMenu(R.menu.menu_landing_toolbar);
        }
    }

    private static void tintMenuIcon(Activity activity, MaterialToolbar toolbar) {
        MenuItem item = toolbar.getMenu().findItem(R.id.actionBreadcrumb);

        if (item != null && item.getIcon() != null) {
            item.getIcon().setTint(activity.getColor(R.color.white));
        }
    }

    private static void showPopup(
            Activity activity,
            View anchor,
            int popupMenuRes,
            int currentMenuItemId
    ) {
        PopupMenu popupMenu = new PopupMenu(activity, anchor, Gravity.END);
        popupMenu.getMenuInflater().inflate(popupMenuRes, popupMenu.getMenu());
        if (currentMenuItemId != 0) {
            popupMenu.getMenu().removeItem(currentMenuItemId);
        }
        popupMenu.setOnMenuItemClickListener(item -> handleItem(activity, item.getItemId()));
        popupMenu.show();
    }

    private static int currentAdminMenuItem(Activity activity) {
        if (activity instanceof LandingActivity) {
            return R.id.menuAvailability;
        }

        if (activity instanceof HomeActivity) {
            return R.id.menuBookings;
        }

        if (activity instanceof AdminBookingRequestsActivity) {
            return R.id.menuBookingRequests;
        }

        if (activity instanceof AdminRequesterAccountsActivity) {
            return R.id.menuRequesterAccounts;
        }

        return 0;
    }

    private static int currentRequesterMenuItem(Activity activity) {
        if (activity instanceof RequesterLandingActivity) {
            return R.id.menuRequesterHome;
        }

        if (activity instanceof RequesterRequestsActivity) {
            return R.id.menuMyRequests;
        }

        return 0;
    }

    private static boolean handleItem(Activity activity, int itemId) {
        if (itemId == R.id.menuRequesterHome) {
            open(activity, RequesterLandingActivity.class);
            return true;
        }

        if (itemId == R.id.menuMyRequests) {
            open(activity, RequesterRequestsActivity.class);
            return true;
        }

        if (itemId == R.id.menuAvailability) {
            open(activity, LandingActivity.class);
            return true;
        }

        if (itemId == R.id.menuBookings) {
            open(activity, HomeActivity.class);
            return true;
        }

        if (itemId == R.id.menuBookingRequests) {
            open(activity, AdminBookingRequestsActivity.class);
            return true;
        }

        if (itemId == R.id.menuRequesterAccounts) {
            open(activity, AdminRequesterAccountsActivity.class);
            return true;
        }

        if (itemId == R.id.menuLogout) {
            AuthLogoutManager.confirmAndLogout(activity);
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
