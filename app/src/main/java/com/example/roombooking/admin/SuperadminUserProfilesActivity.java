package com.example.roombooking.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
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
import com.example.roombooking.api.RetrofitClient;
import com.example.roombooking.auth.AuthSessionGuard;
import com.example.roombooking.auth.AuthSessionManager;
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
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SuperadminUserProfilesActivity extends AppCompatActivity {

    private static final long REFRESH_STALE_MS = 30_000L;
    private static final String ROLE_ALL = "All";
    private static final String ROLE_ADMIN = "Admin";
    private static final String ROLE_REQUESTER = "Requester";
    private static final String STATUS_ALL = "All";
    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_APPROVED = "Approved";
    private static final String STATUS_REJECTED = "Rejected";
    private static final String[] ROLE_FILTERS = {
            ROLE_ALL,
            ROLE_ADMIN,
            ROLE_REQUESTER,
    };
    private static final String[] STATUS_FILTERS = {
            STATUS_ALL,
            STATUS_PENDING,
            STATUS_APPROVED,
            STATUS_REJECTED,
    };
    private static final Type ACCOUNT_REQUEST_LIST_TYPE =
            new TypeToken<List<AccountRequestItem>>() {}.getType();

    private LinearLayout listContainer;
    private TextView tvStatus;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LocalJsonCacheStore cacheStore;
    private String cacheKey;

    private Call<ApiResponse<List<AccountRequestItem>>> requestCall;
    private Call<ApiResponse<AccountRequestItem>> decisionCall;
    private Call<ApiResponse<Map<String, Object>>> deleteCall;

    private long lastSuccessfulRefreshAt = 0L;
    private boolean hasRenderedAccounts = false;
    private boolean cacheReadPending = false;
    private boolean showingCachedData = false;
    private String selectedRoleFilter = ROLE_ALL;
    private String selectedStatusFilter = STATUS_PENDING;
    private List<AccountRequestItem> allAccounts = Collections.emptyList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AuthSessionGuard.ensureAdmin(this)) {
            return;
        }
        if (!new AuthSessionManager(this).isSuperadmin()) {
            Toast.makeText(this, "Superadmin access required.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        View rootView = buildContentView();
        setContentView(rootView);
        EdgeToEdgeUtils.applySystemBarInsets(this, rootView);
        cacheStore = new LocalJsonCacheStore(getApplicationContext());
        cacheKey = ListScreenCache.superadminUserProfilesKey(getApplicationContext());
        loadCachedAccountsThenRefresh();
    }

    private View buildContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setId(R.id.rootView);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(R.color.booking_list_bg));

        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setId(R.id.appToolbar);
        toolbar.setTitle("User Profiles");
        toolbar.setTitleTextColor(getColor(R.color.white));
        toolbar.setTitleCentered(true);
        toolbar.setBackgroundColor(getColor(R.color.info_blue));
        AppToolbarMenu.setupAdminSecondary(this, toolbar);
        root.addView(toolbar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
        ));

        root.addView(filterLabel("Role"));
        root.addView(ListScreenUiHelper.createFilterBar(
                this,
                ROLE_FILTERS,
                selectedRoleFilter,
                filter -> {
                    selectedRoleFilter = filter;
                    renderFilteredAccounts();
                    showCurrentStatus();
                }
        ));
        root.addView(filterLabel("Status"));
        root.addView(ListScreenUiHelper.createFilterBar(
                this,
                STATUS_FILTERS,
                selectedStatusFilter,
                filter -> {
                    selectedStatusFilter = filter;
                    renderFilteredAccounts();
                    showCurrentStatus();
                }
        ));

        tvStatus = new TextView(this);
        tvStatus.setTextColor(getColor(R.color.detail_text_secondary));
        tvStatus.setTextSize(14);
        tvStatus.setPadding(dp(16), dp(10), dp(16), dp(8));
        root.addView(tvStatus);

        swipeRefreshLayout = new SwipeRefreshLayout(this);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.primary,
                R.color.success_green,
                R.color.error_red
        );
        swipeRefreshLayout.setOnRefreshListener(() -> loadAccounts(false));

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

    private TextView filterLabel(String label) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextColor(getColor(R.color.detail_text_secondary));
        view.setTextSize(12);
        view.setTypeface(null, android.graphics.Typeface.BOLD);
        view.setPadding(dp(16), dp(10), dp(16), 0);
        return view;
    }

    private void loadCachedAccountsThenRefresh() {
        tvStatus.setText("Loading user profiles...");
        cacheReadPending = true;
        cacheStore.<List<AccountRequestItem>>read(
                cacheKey,
                ACCOUNT_REQUEST_LIST_TYPE,
                REFRESH_STALE_MS,
                result -> {
                    cacheReadPending = false;
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    boolean renderedCache = renderCachedAccounts(result);
                    loadAccounts(!renderedCache);
                }
        );
    }

    private boolean renderCachedAccounts(CacheReadResult<List<AccountRequestItem>> result) {
        if (result == null || !result.isHit()) {
            return false;
        }

        List<AccountRequestItem> accounts = result.getValue() != null
                ? result.getValue()
                : Collections.emptyList();
        lastSuccessfulRefreshAt = result.getUpdatedAtMillis();
        showingCachedData = true;
        renderAccounts(accounts);
        showCurrentStatus();
        return true;
    }

    private void loadAccounts(boolean clearExisting) {
        if (requestCall != null) {
            stopSwipeRefresh();
            return;
        }

        if (clearExisting || !hasRenderedAccounts) {
            tvStatus.setText("Loading user profiles...");
            listContainer.removeAllViews();
        } else {
            tvStatus.setText("Refreshing user profiles...");
        }

        requestCall = RetrofitClient.getApiService(getApplicationContext())
                .getSuperadminAccountRequests(null, null);
        requestCall.enqueue(new Callback<ApiResponse<List<AccountRequestItem>>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<List<AccountRequestItem>>> call,
                    @NonNull Response<ApiResponse<List<AccountRequestItem>>> response
            ) {
                if (call != requestCall) return;
                requestCall = null;
                stopSwipeRefresh();

                if (!response.isSuccessful()
                        || response.body() == null
                        || !response.body().isSuccess()) {
                    showLoadError(ApiErrorUtils.messageFromResponse(
                            response,
                            "User profiles could not be loaded."
                    ));
                    return;
                }

                List<AccountRequestItem> accounts = response.body().getData() != null
                        ? response.body().getData()
                        : Collections.emptyList();
                lastSuccessfulRefreshAt = System.currentTimeMillis();
                showingCachedData = false;
                cacheStore.write(cacheKey, accounts);
                renderAccounts(accounts);
                showCurrentStatus();
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<List<AccountRequestItem>>> call,
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

    private void showLoadError(String message) {
        if (hasRenderedAccounts) {
            tvStatus.setText("Could not refresh user profiles. Showing saved data.");
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

    private void renderAccounts(List<AccountRequestItem> accounts) {
        allAccounts = accounts != null ? accounts : Collections.emptyList();
        renderFilteredAccounts();
    }

    private void renderFilteredAccounts() {
        listContainer.removeAllViews();
        hasRenderedAccounts = true;
        List<AccountRequestItem> visibleAccounts = filteredAccounts();
        if (visibleAccounts.isEmpty()) {
            tvStatus.setText(emptyMessageForFilter());
            return;
        }

        tvStatus.setText("");
        for (AccountRequestItem item : visibleAccounts) {
            listContainer.addView(createCard(item));
        }
    }

    private List<AccountRequestItem> filteredAccounts() {
        List<AccountRequestItem> visibleAccounts = new ArrayList<>();
        for (AccountRequestItem item : allAccounts) {
            if (!matchesRoleFilter(item)) {
                continue;
            }
            if (!ListScreenUiHelper.matchesStatusFilter(
                    selectedStatusFilter,
                    item.getApprovalStatus()
            )) {
                continue;
            }
            visibleAccounts.add(item);
        }
        return visibleAccounts;
    }

    private boolean matchesRoleFilter(AccountRequestItem item) {
        String filter = safe(selectedRoleFilter);
        if (filter.isEmpty() || ROLE_ALL.equalsIgnoreCase(filter)) {
            return true;
        }
        return filter.equalsIgnoreCase(item.getRole());
    }

    private void showCurrentStatus() {
        if (!hasRenderedAccounts) {
            return;
        }
        List<AccountRequestItem> visibleAccounts = filteredAccounts();
        if (visibleAccounts.isEmpty()) {
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
        String status = selectedStatusFilter != null ? selectedStatusFilter : STATUS_PENDING;
        String role = selectedRoleFilter != null ? selectedRoleFilter : ROLE_ALL;
        String roleText = ROLE_ALL.equalsIgnoreCase(role)
                ? "accounts"
                : role.toLowerCase() + " accounts";
        if (STATUS_ALL.equalsIgnoreCase(status)) return "No " + roleText + " found.";
        if (STATUS_PENDING.equalsIgnoreCase(status)) return "No pending " + roleText + ".";
        if (STATUS_APPROVED.equalsIgnoreCase(status)) return "No approved " + roleText + ".";
        if (STATUS_REJECTED.equalsIgnoreCase(status)) return "No rejected " + roleText + ".";
        return "No " + roleText + " found.";
    }

    private View createCard(AccountRequestItem item) {
        MaterialCardView card = new MaterialCardView(this);
        ListScreenUiHelper.styleCard(this, card);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(14), dp(16), dp(14));

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView name = ListScreenUiHelper.cardTitle(this, item.getName());
        topRow.addView(name, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));
        topRow.addView(ListScreenUiHelper.statusChip(this, item.getApprovalStatus()));
        content.addView(topRow);
        content.addView(ListScreenUiHelper.cardMeta(this, "Role", displayRole(item)));
        content.addView(ListScreenUiHelper.cardMeta(this, "Email", item.getEmail()));
        if (!isBlank(item.getDepartment())) {
            content.addView(ListScreenUiHelper.cardMeta(this, "Department", item.getDepartment()));
        }

        card.addView(content);
        card.setClickable(true);
        card.setOnClickListener(v -> showAccountDetails(item));
        card.setOnLongClickListener(v -> {
            showQuickAccountActions(item);
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

    private void showAccountDetails(AccountRequestItem item) {
        final AlertDialog[] dialogRef = new AlertDialog[1];
        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = ListScreenUiHelper.dialogContent(this);

        content.addView(ListScreenUiHelper.sectionHeader(this, "Account"));
        content.addView(ListScreenUiHelper.detailRow(this, "Name", item.getName()));
        content.addView(ListScreenUiHelper.detailRow(this, "Email", item.getEmail()));
        content.addView(ListScreenUiHelper.detailRow(this, "Role", displayRole(item)));
        content.addView(ListScreenUiHelper.detailRow(this, "Approval Status", item.getApprovalStatus()));
        content.addView(ListScreenUiHelper.detailRow(this, "Requested", DateTimeUtils.formatUtcToLocal(item.getCreatedAt())));

        content.addView(ListScreenUiHelper.sectionHeader(this, "Profile"));
        content.addView(ListScreenUiHelper.detailRow(this, "Department", item.getDepartment()));
        content.addView(ListScreenUiHelper.detailRow(this, "Designation", item.getDesignation()));
        content.addView(ListScreenUiHelper.detailRow(this, "Mobile", item.getMobile()));

        content.addView(ListScreenUiHelper.sectionHeader(this, "Review"));
        content.addView(ListScreenUiHelper.detailRow(this, "Approved By", item.getApprovedByName()));
        content.addView(ListScreenUiHelper.detailRow(this, "Approved At", DateTimeUtils.formatUtcToLocal(item.getApprovedAt())));
        content.addView(ListScreenUiHelper.detailRow(this, "Remarks", item.getRemarks()));

        addAccountActions(content, item, dialogRef);
        scrollView.addView(content);

        dialogRef[0] = new AlertDialog.Builder(this)
                .setTitle("User Profile")
                .setView(scrollView)
                .setPositiveButton(R.string.action_close, null)
                .create();
        dialogRef[0].show();
    }

    private void showQuickAccountActions(AccountRequestItem item) {
        final AlertDialog[] dialogRef = new AlertDialog[1];
        LinearLayout content = ListScreenUiHelper.dialogContent(this);
        content.addView(ListScreenUiHelper.sectionHeader(this, "Account"));
        content.addView(ListScreenUiHelper.detailRow(this, "Name", item.getName()));
        content.addView(ListScreenUiHelper.detailRow(this, "Email", item.getEmail()));
        content.addView(ListScreenUiHelper.detailRow(this, "Role", displayRole(item)));
        content.addView(ListScreenUiHelper.detailRow(this, "Status", item.getApprovalStatus()));
        addAccountActions(content, item, dialogRef);

        dialogRef[0] = new AlertDialog.Builder(this)
                .setTitle("Quick Actions")
                .setView(content)
                .setPositiveButton(R.string.action_close, null)
                .create();
        dialogRef[0].show();
    }

    private void addAccountActions(
            LinearLayout content,
            AccountRequestItem item,
            AlertDialog[] dialogRef
    ) {
        String status = item.getApprovalStatus();
        if ("pending".equalsIgnoreCase(status)) {
            LinearLayout actions = detailActionRow();
            AppCompatButton approve = makePrimaryButton("Approve");
            approve.setOnClickListener(v -> {
                dismissDialog(dialogRef);
                approve(item);
            });
            actions.addView(approve, new LinearLayout.LayoutParams(0, dp(48), 1f));

            AppCompatButton reject = makeSecondaryButton("Reject");
            reject.setOnClickListener(v -> {
                dismissDialog(dialogRef);
                showRejectDialog(item);
            });
            LinearLayout.LayoutParams rejectParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
            rejectParams.setMargins(dp(8), 0, 0, 0);
            actions.addView(reject, rejectParams);
            content.addView(actions);
            content.addView(deleteButton(item, dialogRef, dp(10)));
            return;
        }

        if ("approved".equalsIgnoreCase(status)) {
            LinearLayout actions = detailActionRow();
            AppCompatButton reject = makeSecondaryButton("Reject");
            reject.setOnClickListener(v -> {
                dismissDialog(dialogRef);
                showRejectDialog(item);
            });
            actions.addView(reject, new LinearLayout.LayoutParams(0, dp(48), 1f));
            AppCompatButton delete = makeDangerButton("Delete");
            delete.setOnClickListener(v -> {
                dismissDialog(dialogRef);
                confirmDelete(item);
            });
            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
            deleteParams.setMargins(dp(8), 0, 0, 0);
            actions.addView(delete, deleteParams);
            content.addView(actions);
            return;
        }

        if ("rejected".equalsIgnoreCase(status)) {
            LinearLayout actions = detailActionRow();
            AppCompatButton approve = makePrimaryButton("Approve Again");
            approve.setOnClickListener(v -> {
                dismissDialog(dialogRef);
                approve(item);
            });
            actions.addView(approve, new LinearLayout.LayoutParams(0, dp(48), 1f));
            AppCompatButton delete = makeDangerButton("Delete");
            delete.setOnClickListener(v -> {
                dismissDialog(dialogRef);
                confirmDelete(item);
            });
            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
            deleteParams.setMargins(dp(8), 0, 0, 0);
            actions.addView(delete, deleteParams);
            content.addView(actions);
            return;
        }

        content.addView(deleteButton(item, dialogRef, dp(14)));
    }

    private AppCompatButton deleteButton(
            AccountRequestItem item,
            AlertDialog[] dialogRef,
            int topMargin
    ) {
        AppCompatButton delete = makeDangerButton("Delete");
        delete.setOnClickListener(v -> {
            dismissDialog(dialogRef);
            confirmDelete(item);
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
        );
        params.setMargins(0, topMargin, 0, 0);
        delete.setLayoutParams(params);
        return delete;
    }

    private void dismissDialog(AlertDialog[] dialogRef) {
        if (dialogRef != null && dialogRef.length > 0 && dialogRef[0] != null) {
            dialogRef[0].dismiss();
        }
    }

    private void showRejectDialog(AccountRequestItem item) {
        EditText remarks = new EditText(this);
        remarks.setHint("Remarks (optional)");
        remarks.setSingleLine(false);
        remarks.setMinLines(2);
        LinearLayout content = ListScreenUiHelper.dialogContent(this);
        content.addView(remarks);
        new AlertDialog.Builder(this)
                .setTitle("Reject " + item.getName())
                .setView(content)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton("Reject", (dialog, which) -> reject(item, text(remarks)))
                .show();
    }

    private void confirmDelete(AccountRequestItem item) {
        LinearLayout content = ListScreenUiHelper.dialogContent(this);
        TextView message = new TextView(this);
        message.setText("Are you sure you want to delete this account? This action cannot be undone.");
        message.setTextColor(getColor(R.color.detail_text_primary));
        message.setTextSize(14);
        content.addView(message);
        content.addView(ListScreenUiHelper.sectionHeader(this, "Selected Account"));
        content.addView(ListScreenUiHelper.detailRow(this, "Email", item.getEmail()));
        content.addView(ListScreenUiHelper.detailRow(this, "Role", displayRole(item)));
        content.addView(ListScreenUiHelper.detailRow(this, "Status", item.getApprovalStatus()));

        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setView(content)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount(item))
                .show();
    }

    private void approve(AccountRequestItem item) {
        decisionCall = RetrofitClient.getApiService(getApplicationContext())
                .approveSuperadminAccountRequest(
                        item.getId(),
                        new AccountApprovalDecisionRequest("")
                );
        enqueueDecision("Account approved.", "Account could not be approved.");
    }

    private void reject(AccountRequestItem item, String remarks) {
        decisionCall = RetrofitClient.getApiService(getApplicationContext())
                .rejectSuperadminAccountRequest(
                        item.getId(),
                        new AccountApprovalDecisionRequest(remarks)
                );
        enqueueDecision("Account rejected.", "Account could not be rejected.");
    }

    private void enqueueDecision(String successMessage, String fallbackError) {
        if (decisionCall == null) {
            return;
        }
        tvStatus.setText("Saving decision...");
        decisionCall.enqueue(new Callback<ApiResponse<AccountRequestItem>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<AccountRequestItem>> call,
                    @NonNull Response<ApiResponse<AccountRequestItem>> response
            ) {
                if (call != decisionCall) return;
                decisionCall = null;
                tvStatus.setText("");

                if (!response.isSuccessful()
                        || response.body() == null
                        || !response.body().isSuccess()) {
                    Toast.makeText(
                            SuperadminUserProfilesActivity.this,
                            ApiErrorUtils.messageFromResponse(response, fallbackError),
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                Toast.makeText(
                        SuperadminUserProfilesActivity.this,
                        successMessage,
                        Toast.LENGTH_SHORT
                ).show();
                loadAccounts(false);
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<AccountRequestItem>> call,
                    @NonNull Throwable t
            ) {
                if (call != decisionCall) return;
                decisionCall = null;
                tvStatus.setText("");
                if (!call.isCanceled()) {
                    Toast.makeText(
                            SuperadminUserProfilesActivity.this,
                            ApiErrorUtils.messageFromThrowable(t),
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        });
    }

    private void deleteAccount(AccountRequestItem item) {
        if (deleteCall != null) {
            return;
        }
        tvStatus.setText("Deleting account...");
        deleteCall = RetrofitClient.getApiService(getApplicationContext())
                .deleteSuperadminAccountRequest(item.getId());
        deleteCall.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<Map<String, Object>>> call,
                    @NonNull Response<ApiResponse<Map<String, Object>>> response
            ) {
                if (call != deleteCall) return;
                deleteCall = null;
                tvStatus.setText("");

                if (!response.isSuccessful()
                        || response.body() == null
                        || !response.body().isSuccess()) {
                    Toast.makeText(
                            SuperadminUserProfilesActivity.this,
                            ApiErrorUtils.messageFromResponse(response, "Account could not be deleted."),
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                Toast.makeText(
                        SuperadminUserProfilesActivity.this,
                        "Account deleted.",
                        Toast.LENGTH_SHORT
                ).show();
                loadAccounts(false);
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<Map<String, Object>>> call,
                    @NonNull Throwable t
            ) {
                if (call != deleteCall) return;
                deleteCall = null;
                tvStatus.setText("");
                if (!call.isCanceled()) {
                    Toast.makeText(
                            SuperadminUserProfilesActivity.this,
                            ApiErrorUtils.messageFromThrowable(t),
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        });
    }

    private LinearLayout detailActionRow() {
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(14), 0, 0);
        return actions;
    }

    private AppCompatButton makePrimaryButton(String text) {
        AppCompatButton button = new AppCompatButton(this);
        button.setText(text);
        button.setTextColor(getColor(R.color.white));
        button.setTextSize(14);
        button.setAllCaps(false);
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

    private void refreshIfStaleOnResume() {
        if (cacheReadPending || requestCall != null) {
            return;
        }

        if (ListScreenCache.isStale(lastSuccessfulRefreshAt, REFRESH_STALE_MS)) {
            loadAccounts(false);
        }
    }

    private String displayRole(AccountRequestItem item) {
        String role = safe(item.getRole());
        if ("admin".equalsIgnoreCase(role)) {
            return "Admin";
        }
        if ("requester".equalsIgnoreCase(role)) {
            return "Requester";
        }
        return role;
    }

    private String text(EditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private String safe(String value) {
        return value != null ? value.trim() : "";
    }

    private boolean isBlank(String value) {
        return TextUtils.isEmpty(value) || value.trim().isEmpty();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!AuthSessionGuard.ensureAdmin(this)) {
            return;
        }
        if (!new AuthSessionManager(this).isSuperadmin()) {
            finish();
            return;
        }
        refreshIfStaleOnResume();
    }

    @Override
    protected void onDestroy() {
        if (requestCall != null && !requestCall.isCanceled()) {
            requestCall.cancel();
        }
        if (decisionCall != null && !decisionCall.isCanceled()) {
            decisionCall.cancel();
        }
        if (deleteCall != null && !deleteCall.isCanceled()) {
            deleteCall.cancel();
        }
        requestCall = null;
        decisionCall = null;
        deleteCall = null;
        super.onDestroy();
    }
}
