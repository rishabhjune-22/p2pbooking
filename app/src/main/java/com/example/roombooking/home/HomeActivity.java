package com.example.roombooking.home;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.roombooking.R;
import com.example.roombooking.auth.AuthSessionGuard;
import com.example.roombooking.booking.BookingAdapter;
import com.example.roombooking.booking.BookingDetailActivity;
import com.example.roombooking.booking.BookingMailTemplateDialog;
import com.example.roombooking.booking.BookingRepository;
import com.example.roombooking.booking.LandingActivity;
import com.example.roombooking.model.booking.BookingMailTemplate;
import com.example.roombooking.model.booking.BookingStatus;
import com.example.roombooking.model.booking.BookingItem;
import com.example.roombooking.model.room.RoomPrefix;
import com.example.roombooking.utils.AppToolbarMenu;
import com.example.roombooking.utils.DateTimeUtils;
import com.example.roombooking.utils.EdgeToEdgeUtils;
import com.example.roombooking.utils.InternetErrorBanner;
import com.example.roombooking.utils.UiEvent;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private static final String EXTRA_UPDATED_BOOKING_ID = "updated_booking_id";
    private static final String EXTRA_UPDATED_STATUS = "updated_status";
    private static final String EXTRA_ARRIVAL_AT = "arrival_at";
    private static final String EXTRA_DEPARTURE_AT = "departure_at";
    private static final String EXTRA_BOOKING_DELETED = "booking_deleted";
    private static final String STATE_COMPACT_VIEW = "compact_view";

    private static final String QUICK_RANGE_CUSTOM = "custom";
    private static final String QUICK_RANGE_3_MONTHS = "3_months";
    private static final String QUICK_RANGE_6_MONTHS = "6_months";

    private static final int PAGINATION_THRESHOLD = 2;
    private static final long PAGINATION_DEBOUNCE_MS = 500L;
    private static final long SEARCH_REQUEST_DELAY_MS = 350L;
    private static final long SYNC_STATUS_REFRESH_INTERVAL_MS = 30L * 1000L;

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvMessage;
    private TextView tvTitle;
    private TextView tvStatusToggleLabel;
    private TextView tvCompactToggleLabel;
    private TextView tvSyncStatus;
    private TextInputEditText etBookingSearch;
    private SwipeRefreshLayout swipeRefreshLayout;

    private ImageButton btnCreateBooking;
    private ImageButton btnFilter;
    private ImageButton btnToggleStatus;
    private ImageButton btnClearFilter;
    private ImageButton btnToggleCompact;
    private View layoutBulkBookingActions;
    private Button btnGenerateSelectedMailTemplate;
    private Button btnDeleteSelectedBookings;

    private MaterialToolbar materialToolbar;

    private BookingAdapter bookingAdapter;
    private LinearLayoutManager layoutManager;
    private RecyclerView.OnScrollListener paginationScrollListener;
    private HomeViewModel viewModel;

    private String selectedPrefix = null;
    private String selectedQuickRange = QUICK_RANGE_CUSTOM;
    private String selectedArrivalFrom = null;
    private String selectedDepartureTo = null;
    private String selectedStatus = BookingStatus.ACTIVE;
    private String bookingSearchQuery = "";

    private TextView activeDateRangeTextView;

    private final SimpleDateFormat apiDateFormat =
            DateTimeUtils.newApiDateFormat();

    private final SimpleDateFormat displayDateFormat =
            DateTimeUtils.newDisplayDateFormat();

    private long lastPaginationTriggerAtMillis = 0L;
    private boolean hasHandledInitialResume = false;
    private final Handler syncStatusHandler = new Handler(Looper.getMainLooper());
    private final Handler searchRequestHandler = new Handler(Looper.getMainLooper());
    private final Runnable searchRequestRunnable = () ->
            viewModel.applySearch(bookingSearchQuery);
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
        if (!AuthSessionGuard.ensureAdmin(this)) {
            return;
        }

        EdgeToEdgeUtils.applySystemBarInsets(this, findViewById(R.id.rootView));

        initViews();
        initViewModel();
        setupRecyclerView();
        restoreCompactView(savedInstanceState);
        setupListeners();
        updateStatusToggleUi();
        updateFilterTitle();
        observeViewModel();

        viewModel.loadInitialBookings();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewBookings);
        progressBar = findViewById(R.id.progressBar);
        tvMessage = findViewById(R.id.tvMessage);
        tvTitle = findViewById(R.id.tvTitle);
        tvStatusToggleLabel = findViewById(R.id.tvStatusToggleLabel);
        tvCompactToggleLabel = findViewById(R.id.tvCompactToggleLabel);
        tvSyncStatus = findViewById(R.id.tvSyncStatus);
        etBookingSearch = findViewById(R.id.etBookingSearch);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        btnCreateBooking = findViewById(R.id.btnCreateBooking);
        btnFilter = findViewById(R.id.btnFilter);
        btnToggleStatus = findViewById(R.id.btnToggleStatus);
        btnClearFilter = findViewById(R.id.btnClearFilter);
        btnToggleCompact = findViewById(R.id.btnToggleCompact);
        layoutBulkBookingActions = findViewById(R.id.layoutBulkBookingActions);
        btnGenerateSelectedMailTemplate = findViewById(R.id.btnGenerateSelectedMailTemplate);
        btnDeleteSelectedBookings = findViewById(R.id.btnDeleteSelectedBookings);

        materialToolbar = findViewById(R.id.toolbar);
    }

    private void initViewModel() {
        BookingRepository bookingRepository = new BookingRepository(getApplicationContext());

        HomeViewModelFactory factory = new HomeViewModelFactory(bookingRepository);

        viewModel = new ViewModelProvider(this, factory).get(HomeViewModel.class);
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

            @Override
            public void onBookingSelectionChanged(int selectedCount) {
                updateBulkActionUi();
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
        setupBookingSearch();
    }

    private void setupBookingSearch() {
        etBookingSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
                // No-op.
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                bookingSearchQuery = text != null ? text.toString().trim() : "";
                searchRequestHandler.removeCallbacks(searchRequestRunnable);
                searchRequestHandler.postDelayed(
                        searchRequestRunnable,
                        SEARCH_REQUEST_DELAY_MS
                );
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // No-op.
            }
        });
    }

    private void setupToolbarMenu() {
        AppToolbarMenu.setupAdmin(this, materialToolbar);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            clearBookingSelection();
            viewModel.refreshBookings();
        });
    }

    private void setupActionButtons() {
        btnCreateBooking.setOnClickListener(v -> openAvailabilityCalendar());

        btnFilter.setOnClickListener(v -> showFilterBottomSheet());

        btnClearFilter.setOnClickListener(v -> clearAllFilters());

        btnToggleStatus.setOnClickListener(v -> {
            cycleBookingStatus();
            updateStatusToggleUi();
            updateFilterTitle();
            applyCurrentFilter();
        });

        btnToggleCompact.setOnClickListener(v -> toggleBookingView());

        if (btnGenerateSelectedMailTemplate != null) {
            btnGenerateSelectedMailTemplate.setOnClickListener(v ->
                    generateSelectedMailTemplate()
            );
        }

        if (btnDeleteSelectedBookings != null) {
            btnDeleteSelectedBookings.setOnClickListener(v ->
                    showDeleteSelectedBookingsDialog()
            );
        }

        updateBulkActionUi();
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

    private void openBookingDetailScreen(BookingItem bookingItem) {
        Intent intent = new Intent(HomeActivity.this, BookingDetailActivity.class);
        intent.putExtra(BookingDetailActivity.EXTRA_BOOKING_DATA, bookingItem);
        bookingDetailLauncher.launch(intent);
    }

    private void openAvailabilityCalendar() {
        Intent intent = new Intent(HomeActivity.this, LandingActivity.class);
        startActivity(intent);
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

        applyCurrentFilter();

        showToast("Filters cleared");
    }

    private void applyCurrentFilter() {
        clearBookingSelection();
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
        Button btnReset = view.findViewById(R.id.btnResetFilters);
        ImageButton btnClose = view.findViewById(R.id.btnCloseFilter);

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
        updateQuickRangeButtonStates(btnCustomRange, btnThreeMonths, btnSixMonths);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnReset.setOnClickListener(v -> {
            selectedPrefix = null;
            selectedArrivalFrom = null;
            selectedDepartureTo = null;
            selectedQuickRange = QUICK_RANGE_CUSTOM;
            actBuilding.setText(RoomPrefix.ALL_BUILDINGS, false);
            tvDateRange.setText("Select date range");
            updateQuickRangeButtonStates(btnCustomRange, btnThreeMonths, btnSixMonths);
        });

        setupFilterDialogListeners(
                dialog,
                tvDateRange,
                actBuilding,
                btnApply
        );

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int horizontalMargin = (int) (getResources().getDisplayMetrics().density * 24);
            int dialogWidth = getResources().getDisplayMetrics().widthPixels - horizontalMargin;
            window.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
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
            updateQuickRangeButtonStates(btnCustomRange, btnThreeMonths, btnSixMonths);
            activeDateRangeTextView = tvDateRange;
            showDateRangePickerIfNeeded();
        });

        btnThreeMonths.setOnClickListener(v -> {
            selectedQuickRange = QUICK_RANGE_3_MONTHS;
            updateQuickRangeButtonStates(btnCustomRange, btnThreeMonths, btnSixMonths);
            applyQuickMonthRange(3, tvDateRange);
        });

        btnSixMonths.setOnClickListener(v -> {
            selectedQuickRange = QUICK_RANGE_6_MONTHS;
            updateQuickRangeButtonStates(btnCustomRange, btnThreeMonths, btnSixMonths);
            applyQuickMonthRange(6, tvDateRange);
        });
    }

    private void updateQuickRangeButtonStates(
            Button btnCustomRange,
            Button btnThreeMonths,
            Button btnSixMonths
    ) {
        btnCustomRange.setSelected(QUICK_RANGE_CUSTOM.equals(selectedQuickRange));
        btnThreeMonths.setSelected(QUICK_RANGE_3_MONTHS.equals(selectedQuickRange));
        btnSixMonths.setSelected(QUICK_RANGE_6_MONTHS.equals(selectedQuickRange));
    }

    private void applyQuickMonthRange(int months, TextView tvDateRange) {
        Calendar calendar = DateTimeUtils.newBookingCalendar();

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
        View pickerView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_compact_date_range, null, false);
        TextView tvSelection = pickerView.findViewById(R.id.tvRangeSelection);
        TextView tvMonth = pickerView.findViewById(R.id.tvCalendarMonth);
        ImageButton btnPreviousMonth = pickerView.findViewById(R.id.btnPreviousCalendarMonth);
        ImageButton btnNextMonth = pickerView.findViewById(R.id.btnNextCalendarMonth);
        RecyclerView rvCalendar = pickerView.findViewById(R.id.rvCompactCalendar);

        Calendar[] selectedDates = {
                calendarFromFilterDate(selectedArrivalFrom),
                calendarFromFilterDate(selectedDepartureTo)
        };
        Calendar displayedMonth = selectedDates[0] != null
                ? (Calendar) selectedDates[0].clone()
                : DateTimeUtils.newBookingCalendar();
        displayedMonth.set(Calendar.DAY_OF_MONTH, 1);

        Button[] applyButton = new Button[1];
        CompactDateRangeAdapter[] adapterHolder = new CompactDateRangeAdapter[1];
        adapterHolder[0] = new CompactDateRangeAdapter(date -> {
            if (selectedDates[0] == null || selectedDates[1] != null) {
                selectedDates[0] = date;
                selectedDates[1] = null;
            } else if (date.before(selectedDates[0])) {
                selectedDates[1] = selectedDates[0];
                selectedDates[0] = date;
            } else {
                selectedDates[1] = date;
            }

            adapterSelectionChanged(
                    adapterHolder[0],
                    tvSelection,
                    applyButton[0],
                    selectedDates
            );
        });
        CompactDateRangeAdapter adapter = adapterHolder[0];
        rvCalendar.setLayoutManager(new GridLayoutManager(this, 7));
        rvCalendar.setAdapter(adapter);

        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        Runnable renderMonth = () -> {
            tvMonth.setText(monthFormat.format(displayedMonth.getTime()));
            adapter.showMonth(displayedMonth);
        };
        btnPreviousMonth.setOnClickListener(v -> {
            displayedMonth.add(Calendar.MONTH, -1);
            renderMonth.run();
        });
        btnNextMonth.setOnClickListener(v -> {
            displayedMonth.add(Calendar.MONTH, 1);
            renderMonth.run();
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Select booking date range")
                .setView(pickerView)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Apply", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            applyButton[0] = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            applyButton[0].setOnClickListener(v -> {
                if (selectedDates[0] == null || selectedDates[1] == null) {
                    return;
                }
                applySelectedDateRange(
                        selectedDates[0].getTimeInMillis(),
                        selectedDates[1].getTimeInMillis()
                );
                dialog.dismiss();
            });
            adapterSelectionChanged(adapter, tvSelection, applyButton[0], selectedDates);
        });

        renderMonth.run();
        dialog.show();
    }

    private Calendar calendarFromFilterDate(String filterDate) {
        if (filterDate == null || filterDate.trim().isEmpty()) {
            return null;
        }

        try {
            Date parsedDate = apiDateFormat.parse(filterDate);
            if (parsedDate != null) {
                Calendar calendar = DateTimeUtils.newBookingCalendar();
                calendar.setTime(parsedDate);
                return calendar;
            }
        } catch (java.text.ParseException ignored) {
            // Treat a malformed saved filter value as no selection.
        }
        return null;
    }

    private void adapterSelectionChanged(
            CompactDateRangeAdapter adapter,
            TextView selectionView,
            Button applyButton,
            Calendar[] selectedDates
    ) {
        adapter.setSelectedRange(selectedDates[0], selectedDates[1]);

        if (selectedDates[0] == null) {
            selectionView.setText("Tap an arrival date");
        } else if (selectedDates[1] == null) {
            selectionView.setText(
                    displayDateFormat.format(selectedDates[0].getTime())
                            + " → Tap departure"
            );
        } else {
            selectionView.setText(
                    displayDateFormat.format(selectedDates[0].getTime())
                            + " → "
                            + displayDateFormat.format(selectedDates[1].getTime())
            );
        }

        if (applyButton != null) {
            applyButton.setEnabled(selectedDates[0] != null && selectedDates[1] != null);
        }
    }

    private void observeViewModel() {
        viewModel.getBookingsLiveData().observe(this, bookingItems -> {
            bookingAdapter.setItems(bookingItems);
            updateBulkActionUi();
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

        viewModel.getMailTemplateLiveData().observe(this, this::showMailTemplateIfNeeded);
    }

    private void showMailTemplateIfNeeded(UiEvent<BookingMailTemplate> event) {
        if (event == null) {
            return;
        }

        BookingMailTemplate template = event.getContentIfNotHandled();
        if (template != null) {
            BookingMailTemplateDialog.show(this, template);
        }
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

    private void generateSelectedMailTemplate() {
        List<Integer> selectedBookingIds = bookingAdapter.getSelectedBookingIds();

        if (selectedBookingIds.isEmpty()) {
            showToast("Select at least one booking.");
            return;
        }

        viewModel.generateBulkMailTemplate(selectedBookingIds);
    }

    private void showDeleteSelectedBookingsDialog() {
        List<Integer> selectedBookingIds = bookingAdapter.getSelectedBookingIds();

        if (selectedBookingIds.isEmpty()) {
            showToast("Select at least one booking.");
            return;
        }

        String message = selectedBookingIds.size() == 1
                ? "Delete selected booking permanently?"
                : "Delete " + selectedBookingIds.size() + " selected bookings permanently?";

        new AlertDialog.Builder(this)
                .setTitle("Delete Selected Bookings")
                .setMessage(message)
                .setPositiveButton("Delete", (dialog, which) -> {
                    bookingAdapter.clearSelection();
                    updateBulkActionUi();
                    viewModel.deleteBookings(selectedBookingIds);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void clearBookingSelection() {
        if (bookingAdapter == null) {
            return;
        }

        bookingAdapter.clearSelection();
        updateBulkActionUi();
    }

    private void updateBulkActionUi() {
        if (bookingAdapter == null) {
            return;
        }

        int selectedCount = bookingAdapter.getSelectedCount();
        boolean hasSelection = selectedCount > 0;

        if (layoutBulkBookingActions != null) {
            layoutBulkBookingActions.setVisibility(View.VISIBLE);
        }

        if (btnGenerateSelectedMailTemplate != null) {
            // Keep this action clickable so an empty selection can explain what is required.
            btnGenerateSelectedMailTemplate.setEnabled(true);
            btnGenerateSelectedMailTemplate.setText(
                    hasSelection
                            ? "Generate Email\nTemplate (" + selectedCount + ")"
                            : "Generate Email\nTemplate"
            );
        }

        if (btnDeleteSelectedBookings != null) {
            // Keep this action clickable so an empty selection can explain what is required.
            btnDeleteSelectedBookings.setEnabled(true);
            btnDeleteSelectedBookings.setText(
                    hasSelection
                            ? "Delete Selected (" + selectedCount + ")"
                            : "Delete Selected"
            );
        }
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!AuthSessionGuard.ensureAdmin(this)) {
            return;
        }
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

    @Override
    protected void onDestroy() {
        searchRequestHandler.removeCallbacks(searchRequestRunnable);
        super.onDestroy();
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

}
