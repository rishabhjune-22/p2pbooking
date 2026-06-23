package com.example.roombooking.requester;

import android.content.Intent;
import android.os.Bundle;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.roombooking.R;
import com.example.roombooking.admin.BookingRequestDecisionRequest;
import com.example.roombooking.api.RetrofitClient;
import com.example.roombooking.auth.AuthSessionGuard;
import com.example.roombooking.cache.CacheReadResult;
import com.example.roombooking.cache.LocalJsonCacheStore;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.utils.ApiErrorUtils;
import com.example.roombooking.utils.AppToolbarMenu;
import com.example.roombooking.utils.DateTimeUtils;
import com.example.roombooking.utils.EdgeToEdgeUtils;
import com.example.roombooking.utils.ListScreenCache;
import com.example.roombooking.utils.ListScreenUiHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RequesterRequestsActivity extends AppCompatActivity {

    private static final long REFRESH_STALE_MS = 30_000L;
    private static final String EMPTY_MESSAGE = "No booking requests yet.";
    private static final String FILTER_ALL = "All";
    private static final String FILTER_PENDING = "Pending";
    private static final String FILTER_CORRECTION_REQUIRED = "Correction Required";
    private static final String FILTER_APPROVED = "Approved";
    private static final String FILTER_REJECTED = "Rejected";
    private static final String[] FILTERS = {
            FILTER_ALL,
            FILTER_PENDING,
            FILTER_CORRECTION_REQUIRED,
            FILTER_APPROVED,
            FILTER_REJECTED,
    };
    private static final Type BOOKING_REQUEST_LIST_TYPE =
            new TypeToken<List<BookingRequestItem>>() {}.getType();

    private LinearLayout listContainer;
    private TextView tvStatus;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LocalJsonCacheStore cacheStore;
    private String cacheKey;
    private Call<ApiResponse<List<BookingRequestItem>>> requestCall;
    private Call<ApiResponse<BookingRequestItem>> deleteCall;
    private long lastSuccessfulRefreshAt = 0L;
    private boolean hasRenderedRequests = false;
    private boolean cacheReadPending = false;
    private boolean showingCachedData = false;
    private boolean refreshOnNextResume = false;
    private String selectedFilter = FILTER_ALL;
    private List<BookingRequestItem> allRequests = Collections.emptyList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AuthSessionGuard.ensureRequester(this)) {
            return;
        }

        View rootView = buildContentView();
        setContentView(rootView);
        EdgeToEdgeUtils.applySystemBarInsets(this, rootView);
        cacheStore = new LocalJsonCacheStore(getApplicationContext());
        cacheKey = ListScreenCache.requesterMyRequestsKey(getApplicationContext());
        loadCachedRequestsThenRefresh();
    }

    private View buildContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setId(R.id.rootView);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(R.color.white));

        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setId(R.id.appToolbar);
        toolbar.setTitle("My Requests");
        toolbar.setTitleTextColor(getColor(R.color.white));
        toolbar.setTitleCentered(true);
        toolbar.setBackgroundColor(getColor(R.color.info_blue));
        AppToolbarMenu.setupRequesterSecondary(this, toolbar);
        root.addView(toolbar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
        ));

        root.addView(ListScreenUiHelper.createFilterBar(
                this,
                FILTERS,
                selectedFilter,
                filter -> {
                    selectedFilter = filter;
                    renderFilteredRequests();
                    showCurrentStatus();
                }
        ));

        tvStatus = new TextView(this);
        tvStatus.setTextColor(getColor(R.color.detail_text_secondary));
        tvStatus.setTextSize(14);
        tvStatus.setPadding(dp(16), dp(12), dp(16), dp(8));
        root.addView(tvStatus);

        swipeRefreshLayout = new SwipeRefreshLayout(this);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.primary,
                R.color.success_green,
                R.color.error_red
        );
        swipeRefreshLayout.setOnRefreshListener(() -> loadRequests(false));

        ScrollView scrollView = new ScrollView(this);
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(dp(16), dp(8), dp(16), dp(20));
        scrollView.addView(listContainer);
        swipeRefreshLayout.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));
        root.addView(swipeRefreshLayout, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));
        return root;
    }

    private void loadRequests(boolean clearExisting) {
        if (requestCall != null) {
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
            return;
        }

        if (clearExisting || !hasRenderedRequests) {
            tvStatus.setText("Loading requests...");
            listContainer.removeAllViews();
        } else {
            tvStatus.setText("Refreshing requests...");
        }

        requestCall = RetrofitClient.getApiService(getApplicationContext())
                .getRequesterBookingRequests(null);
        requestCall.enqueue(new Callback<ApiResponse<List<BookingRequestItem>>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<List<BookingRequestItem>>> call,
                    @NonNull Response<ApiResponse<List<BookingRequestItem>>> response
            ) {
                if (call != requestCall) return;
                requestCall = null;
                stopSwipeRefresh();

                if (!response.isSuccessful()
                        || response.body() == null
                        || !response.body().isSuccess()) {
                    showLoadError(ApiErrorUtils.messageFromResponse(
                            response,
                            "Booking requests could not be loaded."
                    ));
                    return;
                }

                List<BookingRequestItem> requests = response.body().getData() != null
                        ? response.body().getData()
                        : Collections.emptyList();
                lastSuccessfulRefreshAt = System.currentTimeMillis();
                showingCachedData = false;
                cacheStore.write(cacheKey, requests);
                renderRequests(requests);
                showCurrentStatus();
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<List<BookingRequestItem>>> call,
                    @NonNull Throwable t
            ) {
                if (call != requestCall) return;
                requestCall = null;
                stopSwipeRefresh();
                if (!call.isCanceled()) {
                    showLoadError(ApiErrorUtils.messageFromThrowable(t));
                }
            }
        });
    }

    private void loadCachedRequestsThenRefresh() {
        tvStatus.setText("Loading requests...");
        cacheReadPending = true;
        cacheStore.<List<BookingRequestItem>>read(
                cacheKey,
                BOOKING_REQUEST_LIST_TYPE,
                REFRESH_STALE_MS,
                result -> {
                    cacheReadPending = false;
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    boolean renderedCache = renderCachedRequests(result);
                    loadRequests(!renderedCache);
                }
        );
    }

    private boolean renderCachedRequests(CacheReadResult<List<BookingRequestItem>> result) {
        if (result == null || !result.isHit()) {
            return false;
        }

        List<BookingRequestItem> requests = result.getValue() != null
                ? result.getValue()
                : Collections.emptyList();
        lastSuccessfulRefreshAt = result.getUpdatedAtMillis();
        showingCachedData = true;
        renderRequests(requests);
        showCurrentStatus();
        return true;
    }

    private void refreshIfStaleOnResume() {
        if (cacheReadPending || requestCall != null) {
            return;
        }

        if (ListScreenCache.isStale(lastSuccessfulRefreshAt, REFRESH_STALE_MS)) {
            loadRequests(false);
        }
    }

    private void showLoadError(String message) {
        if (hasRenderedRequests) {
            tvStatus.setText("Could not refresh requests. Showing saved data.");
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        } else {
            tvStatus.setText(message);
        }
    }

    private void stopSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void renderRequests(List<BookingRequestItem> requests) {
        if (requests == null) {
            allRequests = Collections.emptyList();
        } else {
            List<BookingRequestItem> visibleRequests = new ArrayList<>();
            for (BookingRequestItem item : requests) {
                if (item != null && !item.isDeleted()) {
                    visibleRequests.add(item);
                }
            }
            allRequests = visibleRequests;
        }
        renderFilteredRequests();
    }

    private void renderFilteredRequests() {
        listContainer.removeAllViews();
        hasRenderedRequests = true;
        List<BookingRequestItem> visibleRequests = filteredRequests();
        if (visibleRequests.isEmpty()) {
            tvStatus.setText(emptyMessageForFilter());
            return;
        }

        tvStatus.setText("");
        for (BookingRequestItem item : visibleRequests) {
            listContainer.addView(createCard(item));
        }
    }

    private List<BookingRequestItem> filteredRequests() {
        List<BookingRequestItem> visibleRequests = new ArrayList<>();
        for (BookingRequestItem item : allRequests) {
            if (ListScreenUiHelper.matchesStatusFilter(selectedFilter, item.getStatus())) {
                visibleRequests.add(item);
            }
        }
        return visibleRequests;
    }

    private void showCurrentStatus() {
        if (!hasRenderedRequests) {
            return;
        }
        List<BookingRequestItem> visibleRequests = filteredRequests();
        if (visibleRequests.isEmpty()) {
            if (lastSuccessfulRefreshAt > 0L) {
                tvStatus.setText(ListScreenCache.emptyStatus(
                        emptyMessageForFilter(),
                        lastSuccessfulRefreshAt
                ));
            } else {
                tvStatus.setText(emptyMessageForFilter());
            }
            return;
        }

        if (lastSuccessfulRefreshAt <= 0L) {
            tvStatus.setText("");
        } else if (showingCachedData) {
            tvStatus.setText(ListScreenCache.savedDataStatus(lastSuccessfulRefreshAt));
        } else {
            tvStatus.setText(ListScreenCache.lastUpdatedStatus(lastSuccessfulRefreshAt));
        }
    }

    private String emptyMessageForFilter() {
        String filter = selectedFilter != null ? selectedFilter : FILTER_ALL;
        if (FILTER_PENDING.equalsIgnoreCase(filter)) return "No pending booking requests.";
        if (FILTER_CORRECTION_REQUIRED.equalsIgnoreCase(filter)) return "No booking requests requiring correction.";
        if (FILTER_APPROVED.equalsIgnoreCase(filter)) return "No approved booking requests.";
        if (FILTER_REJECTED.equalsIgnoreCase(filter)) return "No rejected booking requests.";
        return EMPTY_MESSAGE;
    }

    private View createCard(BookingRequestItem item) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(getColor(R.color.white));
        card.setStrokeColor(getColor(R.color.availability_border));
        card.setStrokeWidth(dp(1));
        card.setRadius(dp(8));
        card.setCardElevation(dp(1));
        card.setClickable(true);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView title = label("Visitor", item.getVisitorName());
        title.setTypeface(null, Typeface.BOLD);
        topRow.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        topRow.addView(ListScreenUiHelper.statusChip(this, item.getStatus()));
        content.addView(topRow);
        content.addView(label("Arrival", DateTimeUtils.formatUtcToLocal(item.getArrivalAt())));
        content.addView(label("Departure", DateTimeUtils.formatUtcToLocal(item.getDepartureAt())));
        if (!isBlank(item.getAssignedRoomName())) {
            content.addView(label("Assigned Room", item.getAssignedRoomName()));
        }
        if (!isBlank(item.getAdminRemarks())) {
            content.addView(label("Admin Remarks", item.getAdminRemarks()));
        }
        card.addView(content);
        card.setOnClickListener(v -> showDetail(item));
        card.setOnLongClickListener(v -> {
            showDetail(item);
            return true;
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(params);
        return card;
    }

    private TextView label(String label, String value) {
        TextView view = new TextView(this);
        view.setText(label + ": " + (isBlank(value) ? "-" : value));
        view.setTextColor(getColor(R.color.detail_text_primary));
        view.setTextSize(14);
        view.setPadding(0, dp(2), 0, dp(2));
        return view;
    }

    private void showDetail(BookingRequestItem item) {
        final AlertDialog[] dialogRef = new AlertDialog[1];
        String detail = "Status: " + ListScreenUiHelper.displayStatus(item.getStatus())
                + "\nRequested: " + DateTimeUtils.formatUtcToLocal(item.getRequestedAt())
                + "\nArrival: " + DateTimeUtils.formatUtcToLocal(item.getArrivalAt())
                + "\nDeparture: " + DateTimeUtils.formatUtcToLocal(item.getDepartureAt())
                + "\nVisitor: " + safe(item.getVisitorName())
                + "\nPurpose: " + safe(item.getPurposeOfVisit())
                + "\nPreference: " + safe(item.getPreferredPrefix())
                + "\nAssigned Room: " + safe(item.getAssignedRoomName())
                + "\nReviewed By: " + safe(item.getReviewedByName())
                + "\nAdmin Remarks: " + safe(item.getAdminRemarks());
        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(10), dp(20), dp(12));
        for (String line : detail.split("\\n")) {
            content.addView(textLine(line));
        }
        boolean correctionRequired = "correction_required".equalsIgnoreCase(item.getStatus());
        boolean pending = "pending".equalsIgnoreCase(item.getStatus());
        boolean canEdit = correctionRequired || pending;
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(canEdit ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        actions.setPadding(0, dp(12), 0, 0);

        if (canEdit) {
            AppCompatButton edit = makePrimaryButton(correctionRequired ? "Edit & Resubmit" : "Edit");
            edit.setOnClickListener(v -> {
                dismissDialog(dialogRef);
                openEditRequest(item, correctionRequired);
            });
            actions.addView(edit, new LinearLayout.LayoutParams(0, dp(46), 1f));
        }

        AppCompatButton delete = makeDangerButton("Delete Request");
        delete.setOnClickListener(v -> confirmDelete(item, dialogRef));
        if (canEdit) {
            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0, dp(46), 1f);
            deleteParams.setMargins(dp(8), 0, 0, 0);
            actions.addView(delete, deleteParams);
        } else {
            actions.addView(delete, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(46)
            ));
        }
        content.addView(actions);
        scrollView.addView(content);

        dialogRef[0] = new AlertDialog.Builder(this)
                .setTitle("Booking Request #" + item.getId())
                .setView(scrollView)
                .setPositiveButton(R.string.action_close, null)
                .create();
        dialogRef[0].show();
    }

    private void openEditRequest(BookingRequestItem item, boolean resubmitMode) {
        Intent intent = new Intent(this, RequesterRequestBookingActivity.class);
        intent.putExtra(RequesterRequestBookingActivity.EXTRA_EDIT_MODE, true);
        intent.putExtra(RequesterRequestBookingActivity.EXTRA_RESUBMIT_MODE, resubmitMode);
        intent.putExtra(RequesterRequestBookingActivity.EXTRA_BOOKING_REQUEST_ID, item.getId());
        intent.putExtra(RequesterRequestBookingActivity.EXTRA_ARRIVAL_AT, item.getArrivalAt());
        intent.putExtra(RequesterRequestBookingActivity.EXTRA_DEPARTURE_AT, item.getDepartureAt());
        intent.putExtra(RequesterRequestBookingActivity.EXTRA_PREFERRED_PREFIX, item.getPreferredPrefix());
        if (item.getPreferredRoom() != null) {
            intent.putExtra(RequesterRequestBookingActivity.EXTRA_PREFERRED_ROOM_ID, item.getPreferredRoom());
        }
        intent.putExtra(RequesterRequestBookingActivity.EXTRA_PREFERRED_ROOM_NAME, item.getPreferredRoomName());
        intent.putExtra(RequesterRequestBookingActivity.EXTRA_VISITOR_NAME, item.getVisitorName());
        intent.putExtra(RequesterRequestBookingActivity.EXTRA_VISITOR_DESIGNATION, item.getVisitorDesignation());
        intent.putExtra(RequesterRequestBookingActivity.EXTRA_VISITOR_ORGANISATION, item.getVisitorOrganisation());
        intent.putExtra(RequesterRequestBookingActivity.EXTRA_VISITOR_GENDER, item.getVisitorGender());
        intent.putExtra(RequesterRequestBookingActivity.EXTRA_VISITOR_ADDRESS, item.getVisitorAddress());
        intent.putExtra(RequesterRequestBookingActivity.EXTRA_VISITOR_MOBILE, item.getVisitorMobile());
        intent.putExtra(RequesterRequestBookingActivity.EXTRA_VISITOR_EMAIL, item.getVisitorEmail());
        intent.putExtra(RequesterRequestBookingActivity.EXTRA_VISITOR_CATEGORY, item.getVisitorCategory());
        intent.putExtra(RequesterRequestBookingActivity.EXTRA_PURPOSE_OF_VISIT, item.getPurposeOfVisit());
        intent.putExtra(RequesterRequestBookingActivity.EXTRA_ATTENDER_REQUIRED, item.isAttenderRequired());
        intent.putExtra(RequesterRequestBookingActivity.EXTRA_ATTENDER_COUNT_PER_DAY, item.getAttenderCountPerDay());
        intent.putExtra(RequesterRequestBookingActivity.EXTRA_ATTENDER_GENERAL_SHIFT, item.isAttenderGeneralShift());
        intent.putExtra(RequesterRequestBookingActivity.EXTRA_ATTENDER_MORNING_SHIFT, item.isAttenderMorningShift());
        intent.putExtra(RequesterRequestBookingActivity.EXTRA_ATTENDER_DAY_SHIFT, item.isAttenderDayShift());
        intent.putExtra(RequesterRequestBookingActivity.EXTRA_REQUESTOR_NAME, item.getRequestorName());
        intent.putExtra(RequesterRequestBookingActivity.EXTRA_REQUESTOR_DESIGNATION, item.getRequestorDesignation());
        intent.putExtra(RequesterRequestBookingActivity.EXTRA_REQUESTOR_DEPARTMENT, item.getRequestorDepartment());
        intent.putExtra(RequesterRequestBookingActivity.EXTRA_REQUESTOR_MOBILE, item.getRequestorMobile());
        intent.putExtra(RequesterRequestBookingActivity.EXTRA_REQUESTOR_EMAIL, item.getRequestorEmail());
        refreshOnNextResume = true;
        startActivity(intent);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private TextView textLine(String value) {
        TextView view = new TextView(this);
        view.setText(isBlank(value) ? "-" : value);
        view.setTextColor(getColor(R.color.detail_text_primary));
        view.setTextSize(14);
        view.setPadding(0, dp(3), 0, dp(3));
        return view;
    }

    private AppCompatButton makePrimaryButton(String text) {
        AppCompatButton button = new AppCompatButton(this);
        button.setText(text);
        button.setTextColor(getColor(R.color.white));
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setTypeface(null, Typeface.BOLD);
        button.setBackgroundResource(R.drawable.bg_create_booking_primary_button);
        return button;
    }

    private AppCompatButton makeDangerButton(String text) {
        AppCompatButton button = new AppCompatButton(this);
        button.setText(text);
        button.setTextColor(getColor(R.color.white));
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setTypeface(null, Typeface.BOLD);
        button.setBackgroundColor(getColor(R.color.error_red));
        return button;
    }

    private void confirmDelete(BookingRequestItem item, AlertDialog[] detailDialogRef) {
        TextView message = new TextView(this);
        message.setText("Are you sure you want to delete this request?");
        message.setTextColor(getColor(R.color.detail_text_primary));
        message.setTextSize(14);
        message.setPadding(0, 0, 0, dp(10));

        android.widget.EditText remarks = new android.widget.EditText(this);
        remarks.setHint("Remarks");
        remarks.setSingleLine(false);
        remarks.setMinLines(2);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(6), dp(20), 0);
        content.addView(message);
        content.addView(remarks);

        new AlertDialog.Builder(this)
                .setTitle("Delete Request")
                .setView(content)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton("Delete", (dialog, which) -> deleteRequest(item, detailDialogRef, safe(remarks.getText().toString())))
                .show();
    }

    private void deleteRequest(BookingRequestItem item, AlertDialog[] detailDialogRef, String remarks) {
        if (deleteCall != null) {
            return;
        }

        tvStatus.setText("Deleting request...");
        deleteCall = RetrofitClient.getApiService(getApplicationContext())
                .deleteRequesterBookingRequest(
                        item.getId(),
                        new BookingRequestDecisionRequest(null, remarks)
                );
        deleteCall.enqueue(new Callback<ApiResponse<BookingRequestItem>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<BookingRequestItem>> call,
                    @NonNull Response<ApiResponse<BookingRequestItem>> response
            ) {
                if (call != deleteCall) return;
                deleteCall = null;
                tvStatus.setText("");

                if (!response.isSuccessful()
                        || response.body() == null
                        || !response.body().isSuccess()) {
                    Toast.makeText(
                            RequesterRequestsActivity.this,
                            ApiErrorUtils.messageFromResponse(
                                    response,
                                    "Booking request could not be deleted."
                            ),
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                Toast.makeText(
                        RequesterRequestsActivity.this,
                        "Booking request deleted successfully.",
                        Toast.LENGTH_SHORT
                ).show();
                dismissDialog(detailDialogRef);
                removeRequestFromCacheAndRender(item.getId());
                loadRequests(false);
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<BookingRequestItem>> call,
                    @NonNull Throwable t
            ) {
                if (call != deleteCall) return;
                deleteCall = null;
                tvStatus.setText("");
                if (!call.isCanceled()) {
                    Toast.makeText(
                            RequesterRequestsActivity.this,
                            ApiErrorUtils.messageFromThrowable(t),
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        });
    }

    private void removeRequestFromCacheAndRender(int requestId) {
        List<BookingRequestItem> updated = new ArrayList<>();
        for (BookingRequestItem item : allRequests) {
            if (item != null && item.getId() != requestId) {
                updated.add(item);
            }
        }
        allRequests = updated;
        cacheStore.write(cacheKey, updated);
        renderFilteredRequests();
        showCurrentStatus();
    }

    private void dismissDialog(AlertDialog[] dialogRef) {
        if (dialogRef != null && dialogRef.length > 0 && dialogRef[0] != null) {
            dialogRef[0].dismiss();
        }
    }

    private static String safe(String value) {
        return value != null ? value.trim() : "";
    }

    private static boolean isBlank(String value) {
        return TextUtils.isEmpty(safe(value));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!AuthSessionGuard.ensureRequester(this)) {
            return;
        }
        if (refreshOnNextResume) {
            refreshOnNextResume = false;
            loadRequests(false);
            return;
        }
        refreshIfStaleOnResume();
    }

    @Override
    protected void onDestroy() {
        if (requestCall != null && !requestCall.isCanceled()) {
            requestCall.cancel();
        }
        if (deleteCall != null && !deleteCall.isCanceled()) {
            deleteCall.cancel();
        }
        requestCall = null;
        deleteCall = null;
        super.onDestroy();
    }
}
