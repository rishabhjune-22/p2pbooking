package com.example.roombooking.notification;

import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.example.roombooking.R;
import com.example.roombooking.api.RetrofitClient;
import com.example.roombooking.model.common.ApiResponse;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class WorkflowNotificationPoller implements DefaultLifecycleObserver {
    private static final long REFRESH_INTERVAL_MS = 30_000L;
    private final AppCompatActivity activity;
    private final MaterialToolbar toolbar;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override public void run() {
            refresh();
            handler.postDelayed(this, REFRESH_INTERVAL_MS);
        }
    };
    private Call<ApiResponse<WorkflowNotificationCounts>> activeCall;
    private WorkflowNotificationCounts latestCounts;

    public WorkflowNotificationPoller(AppCompatActivity activity, MaterialToolbar toolbar) {
        this.activity = activity;
        this.toolbar = toolbar;
        activity.getLifecycle().addObserver(this);
    }

    @Override public void onResume(@NonNull LifecycleOwner owner) {
        handler.removeCallbacks(refreshRunnable);
        handler.post(refreshRunnable);
    }

    @Override public void onPause(@NonNull LifecycleOwner owner) {
        handler.removeCallbacks(refreshRunnable);
        cancelActiveCall();
    }

    @Override public void onDestroy(@NonNull LifecycleOwner owner) {
        handler.removeCallbacks(refreshRunnable);
        cancelActiveCall();
        owner.getLifecycle().removeObserver(this);
    }

    public void refresh() {
        cancelActiveCall();
        activeCall = RetrofitClient.getApiService(activity.getApplicationContext())
                .getWorkflowNotificationCounts();
        activeCall.enqueue(new Callback<ApiResponse<WorkflowNotificationCounts>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<WorkflowNotificationCounts>> call,
                    @NonNull Response<ApiResponse<WorkflowNotificationCounts>> response) {
                if (call != activeCall) return;
                activeCall = null;
                ApiResponse<WorkflowNotificationCounts> body = response.body();
                if (response.isSuccessful() && body != null && body.isSuccess() && body.getData() != null) {
                    renderCounts(body.getData());
                }
            }

            @Override public void onFailure(@NonNull Call<ApiResponse<WorkflowNotificationCounts>> call,
                    @NonNull Throwable throwable) {
                if (call == activeCall) activeCall = null;
            }
        });
    }

    public void markCategoryRead(String category) {
        Map<String, List<String>> request = Collections.singletonMap(
                "categories", Collections.singletonList(category));
        RetrofitClient.getApiService(activity.getApplicationContext())
                .markWorkflowNotificationsRead(request)
                .enqueue(new Callback<ApiResponse<WorkflowNotificationCounts>>() {
                    @Override public void onResponse(@NonNull Call<ApiResponse<WorkflowNotificationCounts>> call,
                            @NonNull Response<ApiResponse<WorkflowNotificationCounts>> response) {
                        ApiResponse<WorkflowNotificationCounts> body = response.body();
                        if (response.isSuccessful() && body != null && body.isSuccess() && body.getData() != null) {
                            renderCounts(body.getData());
                        } else {
                            refresh();
                        }
                    }

                    @Override public void onFailure(@NonNull Call<ApiResponse<WorkflowNotificationCounts>> call,
                            @NonNull Throwable throwable) {
                        refresh();
                    }
                });
    }

    public int getBookingRequestsCount() {
        return latestCounts != null ? latestCounts.getBookingRequests() : 0;
    }

    private void renderCounts(WorkflowNotificationCounts counts) {
        latestCounts = counts;
        int count = counts.getTotal();
        MenuItem item = toolbar.getMenu().findItem(R.id.actionBreadcrumb);
        if (item == null) return;
        android.graphics.drawable.Drawable icon = ContextCompat.getDrawable(activity, R.drawable.ic_menu);
        if (icon == null) return;
        item.setIcon(new NotificationBadgeDrawable(icon, count,
                ContextCompat.getColor(activity, R.color.error_red),
                ContextCompat.getColor(activity, R.color.white)));
        item.setTitle(count > 0 ? "Menu, " + count + " unread notifications" : "Menu");
    }

    private void cancelActiveCall() {
        if (activeCall != null && !activeCall.isCanceled()) activeCall.cancel();
        activeCall = null;
    }
}
