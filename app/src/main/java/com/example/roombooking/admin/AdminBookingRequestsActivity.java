package com.example.roombooking.admin;

import android.os.Bundle;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.roombooking.R;
import com.example.roombooking.api.RetrofitClient;
import com.example.roombooking.auth.AuthSessionGuard;
import com.example.roombooking.booking.CreateBookingActivity;
import com.example.roombooking.cache.CacheReadResult;
import com.example.roombooking.cache.LocalJsonCacheStore;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.model.common.PaginatedData;
import com.example.roombooking.model.room.RoomInventory;
import com.example.roombooking.model.room.RoomItem;
import com.example.roombooking.requester.BookingRequestItem;
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

public class AdminBookingRequestsActivity extends AppCompatActivity {

    private static final long REFRESH_STALE_MS = 30_000L;
    private static final String EMPTY_MESSAGE = "No booking requests found.";
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
    private Call<ApiResponse<PaginatedData<RoomItem>>> roomsCall;
    private Call<ApiResponse<BookingRequestItem>> decisionCall;
    private Call<ApiResponse<BookingRequestItem>> deleteCall;
    private long lastSuccessfulRefreshAt = 0L;
    private boolean hasRenderedRequests = false;
    private boolean cacheReadPending = false;
    private boolean showingCachedData = false;
    private boolean refreshOnNextResume = false;
    private String selectedFilter = FILTER_PENDING;
    private List<BookingRequestItem> allRequests = Collections.emptyList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AuthSessionGuard.ensureAdmin(this)) {
            return;
        }

        View rootView = buildContentView();
        setContentView(rootView);
        EdgeToEdgeUtils.applySystemBarInsets(this, rootView);
        cacheStore = new LocalJsonCacheStore(getApplicationContext());
        cacheKey = ListScreenCache.adminBookingRequestsKey(getApplicationContext());
        loadCachedRequestsThenRefresh();
    }

    private View buildContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setId(R.id.rootView);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(R.color.booking_list_bg));

        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setId(R.id.appToolbar);
        toolbar.setTitle("Booking Requests");
        toolbar.setTitleTextColor(getColor(R.color.white));
        toolbar.setTitleCentered(true);
        toolbar.setBackgroundColor(getColor(R.color.info_blue));
        AppToolbarMenu.setupAdminSecondary(this, toolbar);
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
            tvStatus.setText("Loading pending requests...");
            listContainer.removeAllViews();
        } else {
            tvStatus.setText("Refreshing pending requests...");
        }

        requestCall = RetrofitClient.getApiService(getApplicationContext())
                .getAdminBookingRequests(null);
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
        tvStatus.setText("Loading pending requests...");
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
            tvStatus.setText("Could not refresh booking requests. Showing saved data.");
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
        ListScreenUiHelper.styleCard(this, card);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView title = ListScreenUiHelper.cardTitle(this, primaryBookingRequestLabel(item));
        topRow.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        topRow.addView(ListScreenUiHelper.statusChip(this, item.getStatus()));
        content.addView(topRow);
        content.addView(ListScreenUiHelper.cardMeta(
                this,
                secondaryBookingRequestLabelName(item),
                secondaryBookingRequestLabelValue(item)
        ));
        content.addView(ListScreenUiHelper.cardMeta(
                this,
                "Schedule",
                DateTimeUtils.formatUtcToLocal(item.getArrivalAt())
                        + " - "
                        + DateTimeUtils.formatUtcToLocal(item.getDepartureAt())
        ));
        if ("correction_required".equalsIgnoreCase(item.getStatus()) && !isBlank(item.getAdminRemarks())) {
            TextView note = ListScreenUiHelper.cardNote(
                    this,
                    "Remarks: " + ListScreenUiHelper.snippet(item.getAdminRemarks(), 90)
            );
            LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            noteParams.setMargins(0, dp(10), 0, 0);
            content.addView(note, noteParams);
        }

        card.addView(content);
        card.setClickable(true);
        card.setOnClickListener(v -> openCreateBookingForRequest(item));
        card.setOnLongClickListener(v -> {
            showBookingRequestDetails(item);
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

    private void openCreateBookingForRequest(BookingRequestItem item) {
        Intent intent = new Intent(this, CreateBookingActivity.class);
        intent.putExtra(CreateBookingActivity.EXTRA_BOOKING_REQUEST_ID, item.getId());
        if (item.getPreferredRoom() != null) {
            intent.putExtra(CreateBookingActivity.EXTRA_ROOM_ID, item.getPreferredRoom());
        }
        intent.putExtra(
                CreateBookingActivity.EXTRA_ROOM_NAME,
                RoomInventory.displayStoredRoomLabel(item.getPreferredRoomName())
        );
        intent.putExtra(CreateBookingActivity.EXTRA_ARRIVAL_DATE, item.getArrivalAt());
        intent.putExtra(CreateBookingActivity.EXTRA_DEPARTURE_DATE, item.getDepartureAt());
        intent.putExtra(CreateBookingActivity.EXTRA_VISITOR_NAME, item.getVisitorName());
        intent.putExtra(CreateBookingActivity.EXTRA_VISITOR_DESIGNATION, item.getVisitorDesignation());
        intent.putExtra(CreateBookingActivity.EXTRA_VISITOR_ORGANISATION, item.getVisitorOrganisation());
        intent.putExtra(CreateBookingActivity.EXTRA_VISITOR_GENDER, item.getVisitorGender());
        intent.putExtra(CreateBookingActivity.EXTRA_VISITOR_MOBILE, item.getVisitorMobile());
        intent.putExtra(CreateBookingActivity.EXTRA_VISITOR_EMAIL, item.getVisitorEmail());
        intent.putExtra(CreateBookingActivity.EXTRA_VISITOR_CATEGORY, item.getVisitorCategory());
        intent.putExtra(CreateBookingActivity.EXTRA_PURPOSE_OF_VISIT, item.getPurposeOfVisit());
        intent.putExtra(CreateBookingActivity.EXTRA_BUDGET_HEAD_TYPE, item.getBudgetHeadType());
        intent.putExtra(CreateBookingActivity.EXTRA_BUDGET_HEAD_VALUE, item.getBudgetHeadValue());
        intent.putExtra(CreateBookingActivity.EXTRA_BUDGET_HEAD_NAME, item.getBudgetHeadName());
        intent.putExtra(CreateBookingActivity.EXTRA_BUDGET_HEAD_DEPARTMENT_NAME, item.getBudgetHeadDepartmentName());
        intent.putExtra(CreateBookingActivity.EXTRA_BUDGET_HEAD_PROJECT_CODE, item.getBudgetHeadProjectCode());
        intent.putExtra(CreateBookingActivity.EXTRA_REQUESTOR_NAME, item.getRequestorName());
        intent.putExtra(CreateBookingActivity.EXTRA_REQUESTOR_DESIGNATION, item.getRequestorDesignation());
        intent.putExtra(CreateBookingActivity.EXTRA_REQUESTOR_DEPARTMENT, item.getRequestorDepartment());
        intent.putExtra(CreateBookingActivity.EXTRA_REQUESTOR_MOBILE, item.getRequestorMobile());
        intent.putExtra(CreateBookingActivity.EXTRA_ATTENDER_REQUIRED, item.isAttenderRequired());
        intent.putExtra(CreateBookingActivity.EXTRA_ATTENDER_COUNT_PER_DAY, item.getAttenderCountPerDay());
        intent.putExtra(CreateBookingActivity.EXTRA_ATTENDER_GENERAL_SHIFT, item.isAttenderGeneralShift());
        intent.putExtra(CreateBookingActivity.EXTRA_ATTENDER_MORNING_SHIFT, item.isAttenderMorningShift());
        intent.putExtra(CreateBookingActivity.EXTRA_ATTENDER_DAY_SHIFT, item.isAttenderDayShift());
        refreshOnNextResume = true;
        startActivity(intent);
    }

    private void showBookingRequestDetails(BookingRequestItem item) {
        final AlertDialog[] dialogRef = new AlertDialog[1];
        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = ListScreenUiHelper.dialogContent(this);
        content.addView(ListScreenUiHelper.sectionHeader(this, "Request"));
        content.addView(ListScreenUiHelper.detailRow(this, "Status", ListScreenUiHelper.displayStatus(item.getStatus())));
        content.addView(ListScreenUiHelper.detailRow(this, "Requester", item.getRequesterName()));
        content.addView(ListScreenUiHelper.detailRow(this, "Requester Email", item.getRequesterEmail()));
        content.addView(ListScreenUiHelper.detailRow(this, "Requested At", DateTimeUtils.formatUtcToLocal(item.getRequestedAt())));

        content.addView(ListScreenUiHelper.sectionHeader(this, "Requestor"));
        content.addView(ListScreenUiHelper.detailRow(this, "Requestor Name", item.getRequestorName()));
        content.addView(ListScreenUiHelper.detailRow(this, "Requestor Department", item.getRequestorDepartment()));
        content.addView(ListScreenUiHelper.detailRow(this, "Requestor Designation", item.getRequestorDesignation()));
        content.addView(ListScreenUiHelper.detailRow(this, "Requestor Mobile", item.getRequestorMobile()));
        content.addView(ListScreenUiHelper.detailRow(this, "Requestor Email", item.getRequestorEmail()));

        content.addView(ListScreenUiHelper.sectionHeader(this, "Visitor"));
        content.addView(ListScreenUiHelper.detailRow(this, "Visitor", item.getVisitorName()));
        content.addView(ListScreenUiHelper.detailRow(this, "Visitor Designation", item.getVisitorDesignation()));
        content.addView(ListScreenUiHelper.detailRow(this, "Visitor Organisation", item.getVisitorOrganisation()));
        content.addView(ListScreenUiHelper.detailRow(this, "Visitor Gender", item.getVisitorGender()));
        content.addView(ListScreenUiHelper.detailRow(this, "Visitor Mobile", item.getVisitorMobile()));
        content.addView(ListScreenUiHelper.detailRow(this, "Visitor Email", item.getVisitorEmail()));
        content.addView(ListScreenUiHelper.detailRow(this, "Visitor Category", item.getVisitorCategory()));
        content.addView(ListScreenUiHelper.detailRow(this, "Purpose", item.getPurposeOfVisit()));

        content.addView(ListScreenUiHelper.sectionHeader(this, "Budget Head"));
        content.addView(ListScreenUiHelper.detailRow(this, "Individual", item.getBudgetHeadName()));
        content.addView(ListScreenUiHelper.detailRow(this, "Institute Head", item.getBudgetHeadDepartmentName()));
        content.addView(ListScreenUiHelper.detailRow(this, "Project code", item.getBudgetHeadProjectCode()));

        content.addView(ListScreenUiHelper.sectionHeader(this, "Schedule & Room"));
        content.addView(ListScreenUiHelper.detailRow(this, "Arrival", DateTimeUtils.formatUtcToLocal(item.getArrivalAt())));
        content.addView(ListScreenUiHelper.detailRow(this, "Departure", DateTimeUtils.formatUtcToLocal(item.getDepartureAt())));
        content.addView(ListScreenUiHelper.detailRow(this, "Room Preference", preferenceText(item)));
        content.addView(ListScreenUiHelper.detailRow(this, "Assigned Room", item.getAssignedRoomName()));
        content.addView(ListScreenUiHelper.detailRow(this, "Attender Required", item.isAttenderRequired() ? "Yes" : "No"));
        content.addView(ListScreenUiHelper.detailRow(this, "Attender Count", String.valueOf(item.getAttenderCountPerDay())));
        content.addView(ListScreenUiHelper.detailRow(this, "Attender Shift", attenderShiftText(item)));

        content.addView(ListScreenUiHelper.sectionHeader(this, "Review"));
        content.addView(ListScreenUiHelper.detailRow(this, "Reviewed By", item.getReviewedByName()));
        content.addView(ListScreenUiHelper.detailRow(this, "Reviewed At", DateTimeUtils.formatUtcToLocal(item.getReviewedAt())));
        content.addView(ListScreenUiHelper.detailRow(this, "Admin Remarks", item.getAdminRemarks()));

        LinearLayout actions = detailActionRow();
        if ("pending".equalsIgnoreCase(item.getStatus())) {
            AppCompatButton approve = makePrimaryButton("Approve");
            approve.setOnClickListener(v -> loadRoomsThenApprove(item));
            actions.addView(approve, new LinearLayout.LayoutParams(0, dp(48), 1f));

            AppCompatButton reject = makeSecondaryButton("Reject");
            reject.setOnClickListener(v -> showRejectDialog(item));
            LinearLayout.LayoutParams rejectParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
            rejectParams.setMargins(dp(8), 0, 0, 0);
            actions.addView(reject, rejectParams);
            content.addView(actions);

            AppCompatButton sendBack = makeSecondaryButton("Send Back");
            sendBack.setOnClickListener(v -> showSendBackDialog(item));
            LinearLayout.LayoutParams sendBackParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(48)
            );
            sendBackParams.setMargins(0, dp(10), 0, 0);
            content.addView(sendBack, sendBackParams);
        }

        AppCompatButton delete = makeDangerButton("Delete Request");
        delete.setOnClickListener(v -> confirmDelete(item, dialogRef));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
        );
        deleteParams.setMargins(0, dp(10), 0, 0);
        content.addView(delete, deleteParams);

        scrollView.addView(content);

        dialogRef[0] = new AlertDialog.Builder(this)
                .setTitle("Booking Request #" + item.getId())
                .setView(scrollView)
                .setPositiveButton(R.string.action_close, null)
                .create();
        dialogRef[0].show();
    }

    private void loadRoomsThenApprove(BookingRequestItem item) {
        tvStatus.setText("Loading rooms...");
        roomsCall = RetrofitClient.getApiService(getApplicationContext())
                .getRooms(1, 200);
        roomsCall.enqueue(new Callback<ApiResponse<PaginatedData<RoomItem>>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<PaginatedData<RoomItem>>> call,
                    @NonNull Response<ApiResponse<PaginatedData<RoomItem>>> response
            ) {
                if (call != roomsCall) return;
                roomsCall = null;
                tvStatus.setText("");

                if (!response.isSuccessful()
                        || response.body() == null
                        || !response.body().isSuccess()
                        || response.body().getData() == null) {
                    Toast.makeText(
                            AdminBookingRequestsActivity.this,
                            ApiErrorUtils.messageFromResponse(response, "Rooms could not be loaded."),
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                List<RoomItem> rooms = RoomInventory.visibleRooms(
                        response.body().getData().getResults() != null
                                ? response.body().getData().getResults()
                                : Collections.emptyList()
                );
                showApproveDialog(item, rooms);
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<PaginatedData<RoomItem>>> call,
                    @NonNull Throwable t
            ) {
                if (call != roomsCall) return;
                roomsCall = null;
                tvStatus.setText("");
                if (!call.isCanceled()) {
                    Toast.makeText(
                            AdminBookingRequestsActivity.this,
                            ApiErrorUtils.messageFromThrowable(t),
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        });
    }

    private void showApproveDialog(BookingRequestItem item, List<RoomItem> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            Toast.makeText(this, "No rooms available to select.", Toast.LENGTH_LONG).show();
            return;
        }

        LinearLayout layout = ListScreenUiHelper.dialogContent(this);
        layout.addView(ListScreenUiHelper.sectionHeader(this, "Room"));

        Spinner roomSpinner = new Spinner(this);
        List<String> labels = new ArrayList<>();
        for (RoomItem room : rooms) {
            labels.add(roomLabel(room));
        }
        roomSpinner.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                labels
        ));
        layout.addView(roomSpinner, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
        ));

        EditText remarks = new EditText(this);
        remarks.setHint("Remarks (optional)");
        remarks.setSingleLine(false);
        remarks.setMinLines(2);
        LinearLayout.LayoutParams remarksParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        remarksParams.setMargins(0, dp(12), 0, 0);
        layout.addView(remarks, remarksParams);

        new AlertDialog.Builder(this)
                .setTitle("Approve Request #" + item.getId())
                .setView(layout)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton("Approve", (dialog, which) -> {
                    int selected = roomSpinner.getSelectedItemPosition();
                    RoomItem room = rooms.get(Math.max(selected, 0));
                    approve(item, room.getId(), text(remarks));
                })
                .show();
    }

    private void showRejectDialog(BookingRequestItem item) {
        EditText remarks = new EditText(this);
        remarks.setHint("Remarks (optional)");
        remarks.setSingleLine(false);
        remarks.setMinLines(2);
        LinearLayout content = ListScreenUiHelper.dialogContent(this);
        content.addView(remarks);
        new AlertDialog.Builder(this)
                .setTitle("Reject Request #" + item.getId())
                .setView(content)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton("Reject", (dialog, which) -> reject(item, text(remarks)))
                .show();
    }

    private void showSendBackDialog(BookingRequestItem item) {
        EditText remarks = new EditText(this);
        remarks.setHint("Explain what needs to be corrected");
        remarks.setSingleLine(false);
        remarks.setMinLines(3);
        LinearLayout content = ListScreenUiHelper.dialogContent(this);
        content.addView(remarks);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Send Back for Correction")
                .setView(content)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton("Send Back", null)
                .create();
        dialog.setOnShowListener(shownDialog -> dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String value = text(remarks);
                    if (TextUtils.isEmpty(value)) {
                        remarks.setError("Remarks are required.");
                        return;
                    }
                    dialog.dismiss();
                    sendBack(item, value);
                }));
        dialog.show();
    }

    private void approve(BookingRequestItem item, int roomId, String remarks) {
        decisionCall = RetrofitClient.getApiService(getApplicationContext())
                .approveBookingRequest(
                        item.getId(),
                        new BookingRequestDecisionRequest(roomId, remarks)
                );
        enqueueDecision("Booking request approved.", "Booking request could not be approved.");
    }

    private void reject(BookingRequestItem item, String remarks) {
        decisionCall = RetrofitClient.getApiService(getApplicationContext())
                .rejectBookingRequest(
                        item.getId(),
                        new BookingRequestDecisionRequest(null, remarks)
                );
        enqueueDecision("Booking request rejected.", "Booking request could not be rejected.");
    }

    private void sendBack(BookingRequestItem item, String remarks) {
        decisionCall = RetrofitClient.getApiService(getApplicationContext())
                .sendBackBookingRequest(
                        item.getId(),
                        new BookingRequestDecisionRequest(null, remarks)
                );
        enqueueDecision(
                "Request sent back for correction.",
                "Booking request could not be sent back for correction."
        );
    }

    private void confirmDelete(BookingRequestItem item, AlertDialog[] detailDialogRef) {
        TextView message = new TextView(this);
        message.setText("Are you sure you want to delete this booking request?");
        message.setTextColor(getColor(R.color.detail_text_primary));
        message.setTextSize(14);
        message.setPadding(0, 0, 0, dp(10));

        EditText remarks = new EditText(this);
        remarks.setHint("Remarks");
        remarks.setSingleLine(false);
        remarks.setMinLines(2);

        LinearLayout content = ListScreenUiHelper.dialogContent(this);
        content.addView(message);
        content.addView(remarks);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Delete Request")
                .setView(content)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton("Delete", null)
                .create();
        dialog.setOnShowListener(shownDialog -> dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String value = text(remarks);
                    if (value.isEmpty()) {
                        remarks.setError("Remarks are required.");
                        return;
                    }
                    dialog.dismiss();
                    deleteRequest(item, detailDialogRef, value);
                }));
        dialog.show();
    }

    private void deleteRequest(BookingRequestItem item, AlertDialog[] detailDialogRef, String remarks) {
        if (deleteCall != null) {
            return;
        }

        tvStatus.setText("Deleting request...");
        deleteCall = RetrofitClient.getApiService(getApplicationContext())
                .deleteAdminBookingRequest(
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
                            AdminBookingRequestsActivity.this,
                            ApiErrorUtils.messageFromResponse(
                                    response,
                                    "Booking request could not be deleted."
                            ),
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                Toast.makeText(
                        AdminBookingRequestsActivity.this,
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
                            AdminBookingRequestsActivity.this,
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

    private void enqueueDecision(String successMessage, String fallbackError) {
        tvStatus.setText("Saving decision...");
        decisionCall.enqueue(new Callback<ApiResponse<BookingRequestItem>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<BookingRequestItem>> call,
                    @NonNull Response<ApiResponse<BookingRequestItem>> response
            ) {
                if (call != decisionCall) return;
                decisionCall = null;
                tvStatus.setText("");

                if (!response.isSuccessful()
                        || response.body() == null
                        || !response.body().isSuccess()) {
                    Toast.makeText(
                            AdminBookingRequestsActivity.this,
                            ApiErrorUtils.messageFromResponse(response, fallbackError),
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                Toast.makeText(
                        AdminBookingRequestsActivity.this,
                        successMessage,
                        Toast.LENGTH_SHORT
                ).show();
                loadRequests(false);
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<BookingRequestItem>> call,
                    @NonNull Throwable t
            ) {
                if (call != decisionCall) return;
                decisionCall = null;
                tvStatus.setText("");
                if (!call.isCanceled()) {
                    Toast.makeText(
                            AdminBookingRequestsActivity.this,
                            ApiErrorUtils.messageFromThrowable(t),
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        });
    }

    private TextView label(String label, String value) {
        TextView view = new TextView(this);
        view.setText(label + ": " + (isBlank(value) ? "-" : value));
        view.setTextColor(getColor(R.color.detail_text_primary));
        view.setTextSize(14);
        view.setPadding(0, dp(2), 0, dp(2));
        return view;
    }

    private String preferenceText(BookingRequestItem item) {
        StringBuilder builder = new StringBuilder();
        if (!isBlank(item.getPreferredPrefix())) {
            builder.append(item.getPreferredPrefix());
        }
        if (!isBlank(item.getPreferredRoomName())) {
            if (builder.length() > 0) builder.append(" / ");
            builder.append(item.getPreferredRoomName());
        }
        if (!isBlank(item.getRoomPreferenceNote())) {
            if (builder.length() > 0) builder.append(" - ");
            builder.append(item.getRoomPreferenceNote());
        }
        return builder.toString();
    }

    private String primaryBookingRequestLabel(BookingRequestItem item) {
        if (!isBlank(item.getVisitorName())) {
            return item.getVisitorName();
        }
        if (!isBlank(item.getRequesterName())) {
            return item.getRequesterName();
        }
        return "Booking Request #" + item.getId();
    }

    private String secondaryBookingRequestLabelName(BookingRequestItem item) {
        return isBlank(item.getRequestorDepartment()) ? "Requester" : "Department";
    }

    private String secondaryBookingRequestLabelValue(BookingRequestItem item) {
        if (!isBlank(item.getRequestorDepartment())) {
            return item.getRequestorDepartment();
        }
        if (!isBlank(item.getRequesterName())) {
            return item.getRequesterName();
        }
        return item.getRequesterEmail();
    }

    private String attenderShiftText(BookingRequestItem item) {
        List<String> shifts = new ArrayList<>();
        if (item.isAttenderGeneralShift()) shifts.add("General");
        if (item.isAttenderMorningShift()) shifts.add("Morning");
        if (item.isAttenderDayShift()) shifts.add("Day");
        if (shifts.isEmpty()) {
            return "";
        }
        return TextUtils.join(", ", shifts);
    }

    private String roomLabel(RoomItem room) {
        String label = RoomInventory.displayRoomLabel(room);
        return label + " (#" + room.getId() + ")";
    }

    private AppCompatButton makePrimaryButton(String text) {
        AppCompatButton button = new AppCompatButton(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(getColor(R.color.white));
        button.setTextSize(14);
        button.setTypeface(null, android.graphics.Typeface.BOLD);
        button.setBackgroundResource(R.drawable.bg_create_booking_primary_button);
        return button;
    }

    private AppCompatButton makeSecondaryButton(String text) {
        AppCompatButton button = makePrimaryButton(text);
        button.setTextColor(getColor(R.color.info_blue));
        button.setBackgroundResource(R.drawable.bg_create_booking_info);
        return button;
    }

    private AppCompatButton makeDangerButton(String text) {
        AppCompatButton button = makePrimaryButton(text);
        button.setTextColor(getColor(R.color.error_red));
        button.setBackgroundResource(R.drawable.bg_create_booking_danger_outline_button);
        return button;
    }

    private LinearLayout detailActionRow() {
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(14), 0, 0);
        return actions;
    }

    private String text(EditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static boolean isBlank(String value) {
        return TextUtils.isEmpty(value != null ? value.trim() : "");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!AuthSessionGuard.ensureAdmin(this)) {
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
        cancel(requestCall);
        cancel(roomsCall);
        cancel(decisionCall);
        cancel(deleteCall);
        requestCall = null;
        roomsCall = null;
        decisionCall = null;
        deleteCall = null;
        super.onDestroy();
    }

    private void cancel(Call<?> call) {
        if (call != null && !call.isCanceled()) {
            call.cancel();
        }
    }
}
