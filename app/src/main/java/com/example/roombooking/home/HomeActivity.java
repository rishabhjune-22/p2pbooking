package com.example.roombooking.home;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.roombooking.R;
import com.example.roombooking.booking.BookingAdapter;
import com.example.roombooking.booking.BookingDetailActivity;
import com.example.roombooking.booking.BookingRepository;
import com.example.roombooking.booking.CreateBookingActivity;
import com.example.roombooking.booking.LandingActivity;
import com.example.roombooking.common.LocalUserManager;
import com.example.roombooking.common.RequiredUserNamePrompt;
import com.example.roombooking.model.booking.BookingStatus;
import com.example.roombooking.model.booking.BookingItem;
import com.example.roombooking.model.room.RoomPrefix;
import com.example.roombooking.utils.EdgeToEdgeUtils;
import com.example.roombooking.utils.InternetErrorBanner;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class HomeActivity extends AppCompatActivity {

    private static final String EXTRA_UPDATED_BOOKING_ID = "updated_booking_id";
    private static final String EXTRA_UPDATED_STATUS = "updated_status";
    private static final String EXTRA_ARRIVAL_AT = "arrival_at";
    private static final String EXTRA_DEPARTURE_AT = "departure_at";
    private static final String EXTRA_BOOKING_CREATED = "booking_created";
    private static final String EXTRA_BOOKING_DELETED = "booking_deleted";
    private static final String STATE_COMPACT_VIEW = "compact_view";

    private static final String QUICK_RANGE_CUSTOM = "custom";
    private static final String QUICK_RANGE_3_MONTHS = "3_months";
    private static final String QUICK_RANGE_6_MONTHS = "6_months";

    private static final int PAGINATION_THRESHOLD = 2;
    private static final long PAGINATION_DEBOUNCE_MS = 500L;
    private static final long SYNC_STATUS_REFRESH_INTERVAL_MS = 30L * 1000L;

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvMessage;
    private TextView tvTitle;
    private TextView tvStatusToggleLabel;
    private TextView tvCompactToggleLabel;
    private TextView tvSyncStatus;
    private SwipeRefreshLayout swipeRefreshLayout;

    private ImageButton btnCreateBooking;
    private ImageButton btnFilter;
    private ImageButton btnToggleStatus;
    private ImageButton btnClearFilter;
    private ImageButton btnToggleCompact;

    private MaterialToolbar materialToolbar;

    private BookingAdapter bookingAdapter;
    private LinearLayoutManager layoutManager;
    private RecyclerView.OnScrollListener paginationScrollListener;
    private HomeViewModel viewModel;
    private LocalUserManager localUserManager;
    private boolean userNamePromptShowing = false;

    private String selectedPrefix = null;
    private String selectedQuickRange = QUICK_RANGE_CUSTOM;
    private String selectedArrivalFrom = null;
    private String selectedDepartureTo = null;
    private String selectedStatus = BookingStatus.ACTIVE;

    private TextView activeDateRangeTextView;

    private final SimpleDateFormat apiDateFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private final SimpleDateFormat displayDateFormat =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    private MaterialDatePicker<Pair<Long, Long>> dateRangePicker;
    private long lastPaginationTriggerAtMillis = 0L;
    private boolean hasHandledInitialResume = false;
    private final Handler syncStatusHandler = new Handler(Looper.getMainLooper());
    private final Runnable syncStatusRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            viewModel.refreshVisibleSyncStatusAge();
            syncStatusHandler.postDelayed(this, SYNC_STATUS_REFRESH_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        EdgeToEdgeUtils.applySystemBarInsets(this, findViewById(R.id.rootView));

        setupDateFormats();
        localUserManager = new LocalUserManager(getApplicationContext());

        initViews();
        initViewModel();
        initDateRangePicker();
        setupRecyclerView();
        restoreCompactView(savedInstanceState);
        setupListeners();
        updateStatusToggleUi();
        updateFilterTitle();
        observeViewModel();
        ensureLocalUserName();

        viewModel.loadInitialBookings();
    }

    private void setupDateFormats() {
        apiDateFormat.setTimeZone(TimeZone.getDefault());
        displayDateFormat.setTimeZone(TimeZone.getDefault());
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewBookings);
        progressBar = findViewById(R.id.progressBar);
        tvMessage = findViewById(R.id.tvMessage);
        tvTitle = findViewById(R.id.tvTitle);
        tvStatusToggleLabel = findViewById(R.id.tvStatusToggleLabel);
        tvCompactToggleLabel = findViewById(R.id.tvCompactToggleLabel);
        tvSyncStatus = findViewById(R.id.tvSyncStatus);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        btnCreateBooking = findViewById(R.id.btnCreateBooking);
        btnFilter = findViewById(R.id.btnFilter);
        btnToggleStatus = findViewById(R.id.btnToggleStatus);
        btnClearFilter = findViewById(R.id.btnClearFilter);
        btnToggleCompact = findViewById(R.id.btnToggleCompact);

        materialToolbar = findViewById(R.id.toolbar);
    }

    private void initViewModel() {
        BookingRepository bookingRepository = new BookingRepository(getApplicationContext());

        HomeViewModelFactory factory = new HomeViewModelFactory(bookingRepository);

        viewModel = new ViewModelProvider(this, factory).get(HomeViewModel.class);
    }

    private void initDateRangePicker() {
        dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select booking date range")
                .build();

        dateRangePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null || selection.first == null || selection.second == null) {
                return;
            }

            applySelectedDateRange(selection.first, selection.second);
        });
    }

    private void configureDatePickerWindow() {
        if (dateRangePicker.getDialog() == null) {
            return;
        }

        Window window = dateRangePicker.getDialog().getWindow();
        if (window == null) {
            return;
        }

        WindowCompat.setDecorFitsSystemWindows(window, true);
        window.setStatusBarColor(Color.WHITE);
        window.setNavigationBarColor(Color.WHITE);
        WindowCompat.getInsetsController(window, window.getDecorView())
                .setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(window, window.getDecorView())
                .setAppearanceLightNavigationBars(true);
    }

    private void applySelectedDateRange(long startMillis, long endMillis) {
        Date startDate = new Date(startMillis);
        Date endDate = new Date(endMillis);

        selectedQuickRange = QUICK_RANGE_CUSTOM;

        selectedArrivalFrom = apiDateFormat.format(startDate);
        selectedDepartureTo = apiDateFormat.format(endDate);

        updateFilterTitle();

        if (activeDateRangeTextView != null) {
            activeDateRangeTextView.setText(
                    displayDateFormat.format(startDate)
                            + " → "
                            + displayDateFormat.format(endDate)
            );
        }
    }

    private void setupRecyclerView() {
        bookingAdapter = new BookingAdapter(this, new BookingAdapter.OnBookingClickListener() {
            @Override
            public void onBookingClick(BookingItem bookingItem) {
                openBookingDetailScreen(bookingItem);
            }

            @Override
            public void onBookingLongClick(BookingItem bookingItem, int position) {
                showDeleteBookingDialog(bookingItem);
            }
        });

        layoutManager = new LinearLayoutManager(this);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(bookingAdapter);
        attachPaginationScrollListenerOnce();
    }

    private void attachPaginationScrollListenerOnce() {
        if (paginationScrollListener != null) {
            return;
        }

        paginationScrollListener = createPaginationScrollListener();
        recyclerView.addOnScrollListener(paginationScrollListener);
    }

    private RecyclerView.OnScrollListener createPaginationScrollListener() {
        return new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(
                    @NonNull RecyclerView recyclerView,
                    int dx,
                    int dy
            ) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy <= 0) {
                    return;
                }

                if (viewModel.isLoading() || viewModel.isLastPage()) {
                    return;
                }

                if (shouldLoadNextPage() && canTriggerPaginationNow()) {
                    viewModel.loadNextPage();
                }
            }
        };
    }

    private boolean canTriggerPaginationNow() {
        long now = System.currentTimeMillis();
        if (now - lastPaginationTriggerAtMillis < PAGINATION_DEBOUNCE_MS) {
            return false;
        }

        lastPaginationTriggerAtMillis = now;
        return true;
    }

    private boolean shouldLoadNextPage() {
        int visibleItemCount = layoutManager.getChildCount();
        int totalItemCount = layoutManager.getItemCount();
        int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

        return (visibleItemCount + firstVisibleItemPosition)
                >= totalItemCount - PAGINATION_THRESHOLD
                && firstVisibleItemPosition >= 0;
    }

    private void setupListeners() {
        setupToolbarMenu();
        setupSwipeRefresh();
        setupActionButtons();
    }

    private void setupToolbarMenu() {
        materialToolbar.post(this::tintToolbarBreadcrumbIcon);

        materialToolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.actionBreadcrumb) {
                View menuView = materialToolbar.findViewById(R.id.actionBreadcrumb);
                showLandingPopupMenu(menuView != null ? menuView : materialToolbar);
                return true;
            }

            return false;
        });
    }

    private void tintToolbarBreadcrumbIcon() {
        MenuItem breadcrumbItem = materialToolbar.getMenu().findItem(R.id.actionBreadcrumb);

        if (breadcrumbItem != null && breadcrumbItem.getIcon() != null) {
            breadcrumbItem.getIcon().setTint(getColor(R.color.white));
        }
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> viewModel.refreshBookings());
    }

    private void setupActionButtons() {
        btnCreateBooking.setOnClickListener(v -> openCreateBookingScreen());

        btnFilter.setOnClickListener(v -> showFilterBottomSheet());

        btnClearFilter.setOnClickListener(v -> clearAllFilters());

        btnToggleStatus.setOnClickListener(v -> {
            cycleBookingStatus();
            updateStatusToggleUi();
            updateFilterTitle();
            applyCurrentFilter();
        });

        btnToggleCompact.setOnClickListener(v -> toggleBookingView());
    }

    private void restoreCompactView(Bundle savedInstanceState) {
        boolean compactView = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_COMPACT_VIEW, false);
        bookingAdapter.setCompactView(compactView);
        updateCompactToggleUi();
    }

    private void toggleBookingView() {
        int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
        View firstVisibleView = layoutManager.findViewByPosition(firstVisiblePosition);
        int topOffset = firstVisibleView != null
                ? firstVisibleView.getTop() - recyclerView.getPaddingTop()
                : 0;

        bookingAdapter.setCompactView(!bookingAdapter.isCompactView());
        updateCompactToggleUi();

        if (firstVisiblePosition != RecyclerView.NO_POSITION) {
            layoutManager.scrollToPositionWithOffset(firstVisiblePosition, topOffset);
        }

        requestCompactViewportFill();
    }

    private void updateCompactToggleUi() {
        boolean compactView = bookingAdapter.isCompactView();
        btnToggleCompact.setImageResource(
                compactView ? R.drawable.ic_detailed_view : R.drawable.ic_compact_view
        );
        btnToggleCompact.setContentDescription(
                compactView ? "Switch to detailed booking view" : "Switch to compact booking view"
        );
        tvCompactToggleLabel.setText(
                compactView ? "Detailed\nView" : "Compact\nView"
        );
    }

    private void requestCompactViewportFill() {
        if (!bookingAdapter.isCompactView()) {
            return;
        }

        recyclerView.post(() -> {
            if (!bookingAdapter.isCompactView()
                    || bookingAdapter.getItemCount() == 0
                    || recyclerView.canScrollVertically(1)
                    || viewModel.isLoading()
                    || viewModel.isLastPage()) {
                return;
            }

            viewModel.loadNextPage();
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(STATE_COMPACT_VIEW, bookingAdapter.isCompactView());
        super.onSaveInstanceState(outState);
    }

    private void showLandingPopupMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor, android.view.Gravity.END);

        popupMenu.getMenuInflater().inflate(
                R.menu.menu_landing_popup,
                popupMenu.getMenu()
        );

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.menuBookings) {
                return true;
            }

            if (itemId == R.id.menuAvailability) {
                openAvailabilityScreen();
                return true;
            }

            if (itemId == R.id.menuAboutUs) {
                showAboutDialog();
                return true;
            }

            return false;
        });

        popupMenu.show();
    }

    private void openBookingDetailScreen(BookingItem bookingItem) {
        Intent intent = new Intent(HomeActivity.this, BookingDetailActivity.class);
        intent.putExtra(BookingDetailActivity.EXTRA_BOOKING_DATA, bookingItem);
        bookingDetailLauncher.launch(intent);
    }

    private void openCreateBookingScreen() {
        Intent intent = new Intent(HomeActivity.this, CreateBookingActivity.class);
        createBookingLauncher.launch(intent);
    }

    private void openAvailabilityScreen() {
        Intent intent = new Intent(HomeActivity.this, LandingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.about_title)
                .setMessage(R.string.about_message)
                .setPositiveButton(R.string.action_close, null)
                .show();
    }

    private void cycleBookingStatus() {
        if (BookingStatus.isActive(selectedStatus)) {
            selectedStatus = BookingStatus.EXPIRED;
        } else {
            selectedStatus = BookingStatus.ACTIVE;
        }
    }

    private void updateStatusToggleUi() {
        if (BookingStatus.isActive(selectedStatus)) {
            btnToggleStatus.setImageResource(R.drawable.ic_booking_status_active);
            btnToggleStatus.setContentDescription("Showing active bookings");
            tvStatusToggleLabel.setText("Active\nBookings");
            return;
        }

        btnToggleStatus.setImageResource(R.drawable.ic_booking_status_expired);
        btnToggleStatus.setContentDescription("Showing expired bookings");
        tvStatusToggleLabel.setText("Expired\nBookings");
    }

    private void updateFilterTitle() {
        String statusText = getReadableStatusText();
        String title = statusText + " Bookings";

        if (selectedPrefix != null && !selectedPrefix.trim().isEmpty()) {
            title += " | " + selectedPrefix;
        }

        if (hasSelectedDateRange()) {
            title += " | " + selectedArrivalFrom + " → " + selectedDepartureTo;
        }

        tvTitle.setText(title);
    }

    private String getReadableStatusText() {
        return BookingStatus.displayName(selectedStatus);
    }

    private boolean hasSelectedDateRange() {
        return selectedArrivalFrom != null
                && !selectedArrivalFrom.trim().isEmpty()
                && selectedDepartureTo != null
                && !selectedDepartureTo.trim().isEmpty();
    }

    private void clearAllFilters() {
        selectedPrefix = null;
        selectedArrivalFrom = null;
        selectedDepartureTo = null;
        selectedStatus = BookingStatus.ACTIVE;
        selectedQuickRange = QUICK_RANGE_CUSTOM;

        updateStatusToggleUi();
        updateFilterTitle();

        viewModel.applyFilter(
                null,
                null,
                null,
                BookingStatus.ACTIVE
        );

        showToast("Filters cleared");
    }

    private void applyCurrentFilter() {
        viewModel.applyFilter(
                selectedPrefix,
                selectedArrivalFrom,
                selectedDepartureTo,
                selectedStatus
        );
    }

    private void showFilterBottomSheet() {
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_filter, null);

        TextView tvDateRange = view.findViewById(R.id.tvSelectedDateRange);
        AutoCompleteTextView actBuilding = view.findViewById(R.id.actBuilding);
        Button btnCustomRange = view.findViewById(R.id.btnCustomRange);
        Button btnThreeMonths = view.findViewById(R.id.btnThreeMonths);
        Button btnSixMonths = view.findViewById(R.id.btnSixMonths);
        Button btnApply = view.findViewById(R.id.btnApplyFilter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        bindBuildingDropdown(actBuilding);
        bindCurrentFilterValues(tvDateRange, actBuilding);

        setupQuickRangeButtons(
                tvDateRange,
                btnCustomRange,
                btnThreeMonths,
                btnSixMonths
        );

        setupFilterDialogListeners(
                dialog,
                tvDateRange,
                actBuilding,
                btnApply
        );

        dialog.show();
    }

    private void bindBuildingDropdown(AutoCompleteTextView actBuilding) {
        List<String> buildingNames = new ArrayList<>();
        buildingNames.addAll(RoomPrefix.filterOptions());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                HomeActivity.this,
                android.R.layout.simple_dropdown_item_1line,
                buildingNames
        );

        actBuilding.setAdapter(adapter);
        actBuilding.setOnClickListener(v -> actBuilding.showDropDown());
        actBuilding.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                actBuilding.showDropDown();
            }
        });

        if (selectedPrefix == null || selectedPrefix.trim().isEmpty()) {
            actBuilding.setText(RoomPrefix.ALL_BUILDINGS, false);
        } else {
            actBuilding.setText(selectedPrefix, false);
        }
    }

    private void bindCurrentFilterValues(
            TextView tvDateRange,
            AutoCompleteTextView actBuilding
    ) {
        if (selectedPrefix == null || selectedPrefix.trim().isEmpty()) {
            actBuilding.setText(RoomPrefix.ALL_BUILDINGS, false);
        } else {
            actBuilding.setText(selectedPrefix, false);
        }

        if (selectedArrivalFrom != null && selectedDepartureTo != null) {
            tvDateRange.setText(selectedArrivalFrom + " → " + selectedDepartureTo);
        } else {
            tvDateRange.setText("Select date range");
        }
    }

    private void setupQuickRangeButtons(
            TextView tvDateRange,
            Button btnCustomRange,
            Button btnThreeMonths,
            Button btnSixMonths
    ) {
        btnCustomRange.setOnClickListener(v -> {
            selectedQuickRange = QUICK_RANGE_CUSTOM;
            activeDateRangeTextView = tvDateRange;
            showDateRangePickerIfNeeded();
        });

        btnThreeMonths.setOnClickListener(v -> {
            selectedQuickRange = QUICK_RANGE_3_MONTHS;
            applyQuickMonthRange(3, tvDateRange);
        });

        btnSixMonths.setOnClickListener(v -> {
            selectedQuickRange = QUICK_RANGE_6_MONTHS;
            applyQuickMonthRange(6, tvDateRange);
        });
    }

    private void applyQuickMonthRange(int months, TextView tvDateRange) {
        Calendar calendar = Calendar.getInstance();

        Date endDate = calendar.getTime();

        calendar.add(Calendar.MONTH, -months);
        Date startDate = calendar.getTime();

        selectedArrivalFrom = apiDateFormat.format(startDate);
        selectedDepartureTo = apiDateFormat.format(endDate);

        tvDateRange.setText(
                displayDateFormat.format(startDate)
                        + " → "
                        + displayDateFormat.format(endDate)
        );

        updateFilterTitle();
    }

    private void setupFilterDialogListeners(
            AlertDialog dialog,
            TextView tvDateRange,
            AutoCompleteTextView actBuilding,
            Button btnApply
    ) {
        actBuilding.setOnItemClickListener((parent, itemView, position, id) -> {
            String selectedBuilding = parent.getItemAtPosition(position).toString();

            if (RoomPrefix.isAllBuildings(selectedBuilding)) {
                selectedPrefix = null;
            } else {
                selectedPrefix = selectedBuilding;
            }
        });

        tvDateRange.setOnClickListener(v -> {
            selectedQuickRange = QUICK_RANGE_CUSTOM;
            activeDateRangeTextView = tvDateRange;
            showDateRangePickerIfNeeded();
        });

        btnApply.setOnClickListener(v -> {
            String buildingText = actBuilding.getText().toString().trim();

            if (buildingText.isEmpty() || RoomPrefix.isAllBuildings(buildingText)) {
                selectedPrefix = null;
            } else {
                selectedPrefix = buildingText;
            }

            applyCurrentFilter();
            updateFilterTitle();
            dialog.dismiss();
        });
    }

    private void showDateRangePickerIfNeeded() {
        if (!dateRangePicker.isAdded()) {
            dateRangePicker.show(
                    getSupportFragmentManager(),
                    "BOOKING_DATE_RANGE_PICKER"
            );
            getSupportFragmentManager().executePendingTransactions();
            configureDatePickerWindow();
        }
    }

    private void observeViewModel() {
        viewModel.getBookingsLiveData().observe(this, bookingItems -> {
            bookingAdapter.setItems(bookingItems);
            requestCompactViewportFill();
        });

        viewModel.getFullScreenLoadingLiveData().observe(this, isLoading ->
                progressBar.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE)
        );

        viewModel.getPaginationLoadingLiveData().observe(this, isLoading -> {
            if (Boolean.TRUE.equals(isLoading)) {
                bookingAdapter.showPaginationLoader();
            } else {
                bookingAdapter.hidePaginationLoader();
            }
        });

        viewModel.getSwipeRefreshingLiveData().observe(this, isRefreshing ->
                swipeRefreshLayout.setRefreshing(Boolean.TRUE.equals(isRefreshing))
        );

        viewModel.getMessageLiveData().observe(this, message -> {
            updateInternetErrorBanner(message);
            if (message == null
                    || message.trim().isEmpty()
                    || InternetErrorBanner.isNetworkErrorMessage(message)) {
                tvMessage.setVisibility(View.GONE);
            } else {
                tvMessage.setVisibility(View.VISIBLE);
                tvMessage.setText(message);
            }
        });

        viewModel.getToastLiveData().observe(this, message -> {
            if (message != null && !message.trim().isEmpty()) {
                updateInternetErrorBanner(message);
                showToast(message);
            }
        });

        viewModel.getSyncStatusLiveData().observe(this, this::updateSyncStatus);
    }

    private void updateSyncStatus(String message) {
        if (tvSyncStatus == null) {
            return;
        }

        if (message == null || message.trim().isEmpty()) {
            tvSyncStatus.setVisibility(View.GONE);
            tvSyncStatus.setText("");
            return;
        }

        tvSyncStatus.setText(message.trim());
        tvSyncStatus.setVisibility(View.VISIBLE);
    }

    private void updateInternetErrorBanner(String message) {
        if (InternetErrorBanner.isNetworkErrorMessage(message)) {
            InternetErrorBanner.show(this);
        } else {
            InternetErrorBanner.hide(this);
        }
    }

    private void showDeleteBookingDialog(BookingItem bookingItem) {
        if (bookingItem == null) {
            return;
        }

        if (!canDeleteBooking(bookingItem)) {
            return;
        }

        String displayName = getBookingDisplayName(bookingItem);

        new AlertDialog.Builder(this)
                .setTitle("Delete Booking")
                .setMessage("Delete booking for " + displayName + " permanently?")
                .setPositiveButton("Delete Booking", (dialog, which) ->
                        viewModel.deleteBooking(bookingItem)
                )
                .setNegativeButton("Close", null)
                .show();
    }

    private boolean canDeleteBooking(BookingItem bookingItem) {
        if (BookingStatus.isExpired(bookingItem.getStatus())) {
            showToast("Expired booking cannot be deleted");
            return false;
        }

        return true;
    }

    private String getBookingDisplayName(BookingItem bookingItem) {
        String displayName = bookingItem.getVisitorName();

        if (displayName == null || displayName.trim().isEmpty()) {
            return "this booking";
        }

        return displayName;
    }

    private void handleBookingDetailResult(Intent data) {
        int bookingId = data.getIntExtra(EXTRA_UPDATED_BOOKING_ID, -1);
        String updatedStatus = data.getStringExtra(EXTRA_UPDATED_STATUS);
        String arrivalAt = data.getStringExtra(EXTRA_ARRIVAL_AT);
        String departureAt = data.getStringExtra(EXTRA_DEPARTURE_AT);
        boolean bookingDeleted = data.getBooleanExtra(EXTRA_BOOKING_DELETED, false);

        if (bookingId == -1) {
            return;
        }

        if (bookingDeleted) {
            viewModel.removeBookingById(bookingId);
            return;
        }

        viewModel.updateBookingById(
                bookingId,
                updatedStatus,
                arrivalAt,
                departureAt
        );
    }

    private void handleCreateBookingResult(Intent data) {
        boolean bookingCreated = data.getBooleanExtra(EXTRA_BOOKING_CREATED, false);

        if (bookingCreated) {
            viewModel.invalidateBookingPageOneCacheForMutation();
            viewModel.refreshBookings();
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void ensureLocalUserName() {
        if (localUserManager == null || localUserManager.hasValidUserName()) {
            return;
        }

        showRequiredUserNamePrompt();
    }

    private void showRequiredUserNamePrompt() {
        if (isFinishing() || isDestroyed() || userNamePromptShowing) {
            return;
        }

        userNamePromptShowing = true;
        RequiredUserNamePrompt.show(this, localUserManager, null)
                .setOnDismissListener(d -> userNamePromptShowing = false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ensureLocalUserName();
        startSyncStatusTimer();
        if (!hasHandledInitialResume) {
            hasHandledInitialResume = true;
            return;
        }

        viewModel.refreshBookingsIfStaleOnForeground();
    }

    @Override
    protected void onPause() {
        stopSyncStatusTimer();
        super.onPause();
    }

    private void startSyncStatusTimer() {
        syncStatusHandler.removeCallbacks(syncStatusRefreshRunnable);
        syncStatusHandler.post(syncStatusRefreshRunnable);
    }

    private void stopSyncStatusTimer() {
        syncStatusHandler.removeCallbacks(syncStatusRefreshRunnable);
    }

    private final ActivityResultLauncher<Intent> bookingDetailLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            handleBookingDetailResult(result.getData());
                        }
                    }
            );

    private final ActivityResultLauncher<Intent> createBookingLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            handleCreateBookingResult(result.getData());
                        }
                    }
            );
}
