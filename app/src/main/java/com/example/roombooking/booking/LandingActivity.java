package com.example.roombooking.booking;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.util.TypedValue;

import com.example.roombooking.R;
import com.example.roombooking.auth.AuthSessionGuard;
import com.example.roombooking.model.booking.RoomAvailabilityBookingItem;
import com.example.roombooking.model.room.RoomInventory;
import com.example.roombooking.model.room.RoomPrefix;
import com.example.roombooking.utils.DateTimeUtils;
import com.example.roombooking.utils.NullSafeCollections;
import com.example.roombooking.utils.EdgeToEdgeUtils;
import com.example.roombooking.utils.InternetErrorBanner;
import com.example.roombooking.utils.AppDiagnostics;
import com.example.roombooking.utils.AppToolbarMenu;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.core.widget.NestedScrollView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class LandingActivity extends AppCompatActivity {

    private static final int CALENDAR_SPAN_COUNT = 7;
    private static final String DEFAULT_ROOM_PREFIX = RoomPrefix.DELTA;
    private static final double LESS_THAN_HALF_PERCENTAGE = 50.0;
    private static final String STATE_SELECTED_MONTH = "selected_month";
    private static final String STATE_SELECTED_PREFIX = "selected_prefix";
    private static final String STATE_WAITING_FOR_DEPARTURE = "waiting_for_departure";
    private static final String STATE_HAS_ACTIVE_RANGE = "has_active_range";
    private static final String STATE_ARRIVAL_DATE = "arrival_date";
    private static final String STATE_DEPARTURE_DATE = "departure_date";
    private static final long SYNC_STATUS_REFRESH_INTERVAL_MS = 30L * 1000L;
    private static final long CHECK_AVAILABILITY_CLICK_DEBOUNCE_MS = 700L;

    private MaterialButton btnPreviousMonth;
    private MaterialButton btnNextMonth;
    private ImageButton btnCheckAvailability;

    private TextView tvMonthYear;
    private TextView bannerSelectedDateRange;
    private TextView tvTotalRoomCapacity;
    private TextView tvAvailabilityStatus;

    private MaterialToolbar materialToolbar;
    private TabLayout tabLayoutRooms;
    private SwipeRefreshLayout swipeRefreshAvailability;
    private RecyclerView rvAvailabilityGroups;

    private LandingViewModel viewModel;
    private CalendarAvailabilityAdapter calendarAdapter;

    private final Calendar selectedMonth = Calendar.getInstance();

    private final SimpleDateFormat monthYearFormat =
            new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

    private final List<RoomAvailabilityGroup> allGroups = new ArrayList<>();

    private String selectedPrefix = DEFAULT_ROOM_PREFIX;

    private boolean isWaitingForDepartureDate = false;
    private boolean hasActiveRangeSelection = false;

    private CalendarDayItem arrivalDayItem = null;
    private CalendarDayItem departureDayItem = null;

    private String arrivalDateForRange = null;
    private String departureDateForRange = null;

    private String selectedAvailableArrivalDate = null;
    private String selectedAvailableDepartureDate = null;
    private BottomSheetDialog activeBottomSheetDialog = null;
    private String activeAvailabilitySheetKey = null;
    private boolean replacingAvailabilitySheet = false;
    private boolean hasHandledInitialResume = false;
    private TextView activeAvailabilitySheetStatus = null;
    private AvailableRoomsResponse activeAvailableRoomsResponse = null;
    private AvailableRoomsRangeResponse activeAvailableRoomsRangeResponse = null;
    private long lastCheckAvailabilityClickAtMillis = 0L;
    private final Handler syncStatusHandler = new Handler(Looper.getMainLooper());
    private final Runnable syncStatusRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            viewModel.refreshVisibleSyncStatusAge();
            updateActiveAvailabilitySheetStatus();
            syncStatusHandler.postDelayed(this, SYNC_STATUS_REFRESH_INTERVAL_MS);
        }
    };

    private LinearLayout pendingDeleteLayoutDetails = null;
    private View pendingDeleteCard = null;
    private RoomAvailabilityBookingItem pendingDeleteItem = null;

    private final ActivityResultLauncher<Intent> createBookingLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            handleBookingCreated();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);
        if (!AuthSessionGuard.ensureAdmin(this)) {
            return;
        }

        EdgeToEdgeUtils.applySystemBarInsets(this, findViewById(R.id.main));

        initViewModel();
        initViews();
        setupRecyclerView();
        restoreState(savedInstanceState);
        selectRestoredRoomTab();
        setupListeners();
        setupCalendarGestureSelection();
        observeViewModel();

        loadAvailability();
    }

    private void initViewModel() {
        AvailabilityRepository availabilityRepository =
                new AvailabilityRepository(getApplicationContext());
        LandingViewModelFactory factory =
                new LandingViewModelFactory(availabilityRepository);
        viewModel = new ViewModelProvider(this, factory).get(LandingViewModel.class);
    }

    private void initViews() {
        btnPreviousMonth = findViewById(R.id.btnPreviousMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        btnCheckAvailability = findViewById(R.id.btnCheckAvailability);
        bannerSelectedDateRange = findViewById(R.id.bannerSelectedDateRange);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        tvTotalRoomCapacity = findViewById(R.id.tvTotalRoomCapacity);
        tvAvailabilityStatus = findViewById(R.id.tvAvailabilityStatus);

        materialToolbar = findViewById(R.id.toolbar);
        tabLayoutRooms = findViewById(R.id.tabLayoutRooms);
        swipeRefreshAvailability = findViewById(R.id.swipeRefreshAvailability);
        rvAvailabilityGroups = findViewById(R.id.rvAvailabilityGroups);
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        selectedMonth.setTimeInMillis(savedInstanceState.getLong(
                STATE_SELECTED_MONTH,
                selectedMonth.getTimeInMillis()
        ));
        selectedPrefix = savedInstanceState.getString(
                STATE_SELECTED_PREFIX,
                DEFAULT_ROOM_PREFIX
        );
        isWaitingForDepartureDate = savedInstanceState.getBoolean(
                STATE_WAITING_FOR_DEPARTURE
        );
        hasActiveRangeSelection = savedInstanceState.getBoolean(STATE_HAS_ACTIVE_RANGE);
        arrivalDateForRange = savedInstanceState.getString(STATE_ARRIVAL_DATE);
        departureDateForRange = savedInstanceState.getString(STATE_DEPARTURE_DATE);
        updateSelectedDateRangeBanner();
    }

    private void selectRestoredRoomTab() {
        for (int index = 0; index < tabLayoutRooms.getTabCount(); index++) {
            TabLayout.Tab tab = tabLayoutRooms.getTabAt(index);

            if (tab != null
                    && tab.getText() != null
                    && selectedPrefix.equals(tab.getText().toString())) {
                tab.select();
                return;
            }
        }

        selectedPrefix = DEFAULT_ROOM_PREFIX;
    }

    private void setupRecyclerView() {
        calendarAdapter = new CalendarAvailabilityAdapter(day -> {
            // Calendar click actions are handled through GestureDetector.
        });

        rvAvailabilityGroups.setLayoutManager(
                new GridLayoutManager(this, CALENDAR_SPAN_COUNT)
        );

        rvAvailabilityGroups.setAdapter(calendarAdapter);
    }


    private void setupListeners() {
        setupToolbarMenu();
        setupCheckAvailabilityButton();
        setupMonthNavigationButtons();
        setupMainViewClickListener();
        setupRoomTabs();
        setupSwipeRefresh();
    }

    private void setupToolbarMenu() {
        AppToolbarMenu.setupAdmin(this, materialToolbar);
    }

    private void setupCheckAvailabilityButton() {
        btnCheckAvailability.setOnClickListener(v -> {
            if (isRapidCheckAvailabilityClick()) {
                return;
            }

            if (arrivalDateForRange == null || departureDateForRange == null) {
                showToast("Please select arrival and departure date first");
                return;
            }

            fetchAvailableRoomsForDateRange(arrivalDateForRange, departureDateForRange);
        });
    }

    private boolean isRapidCheckAvailabilityClick() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastCheckAvailabilityClickAtMillis < CHECK_AVAILABILITY_CLICK_DEBOUNCE_MS) {
            return true;
        }

        lastCheckAvailabilityClickAtMillis = now;
        return false;
    }

    private void setupMonthNavigationButtons() {
        btnPreviousMonth.setOnClickListener(v -> {
            selectedMonth.add(Calendar.MONTH, -1);
            loadAvailability();
        });

        btnNextMonth.setOnClickListener(v -> {
            selectedMonth.add(Calendar.MONTH, 1);
            loadAvailability();
        });
    }

    private void setupMainViewClickListener() {
        View mainView = findViewById(R.id.main);

        if (mainView == null) return;

        mainView.setOnClickListener(v -> {
            if (hasActiveRangeSelection) {
                clearRangeSelectionAndEnableRefresh();
            }
        });
    }

    private void setupRoomTabs() {
        tabLayoutRooms.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getText() == null) return;

                selectedPrefix = tab.getText().toString();

                clearRangeSelectionAndEnableRefresh();
                applyPrefixFilter();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // No action required.
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // No action required.
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshAvailability.setColorSchemeResources(
                R.color.primary,
                R.color.success_green,
                R.color.error_red
        );

        swipeRefreshAvailability.setOnRefreshListener(this::refreshAvailability);
    }

    private void observeViewModel() {
        viewModel.getAvailabilityLoadingLiveData().observe(this, isLoading ->
                swipeRefreshAvailability.setRefreshing(Boolean.TRUE.equals(isLoading))
        );

        viewModel.getAvailableRoomsLoadingLiveData().observe(
                this,
                isLoading -> setAvailableRoomsLoading(Boolean.TRUE.equals(isLoading))
        );

        viewModel.getAvailabilityGroupsLiveData().observe(this, groups -> {
            allGroups.clear();
            allGroups.addAll(NullSafeCollections.copyWithoutNulls(groups));
            applyPrefixFilter();
        });

        viewModel.getAvailabilityStatusLiveData().observe(
                this,
                this::updateAvailabilityStatus
        );

        viewModel.getToastLiveData().observe(this, event -> {
            if (event == null) return;

            String message = event.getContentIfNotHandled();
            if (message == null || message.trim().isEmpty()) return;

            resetPendingDeleteCardIfNeeded();
            showToast(message);
        });

        viewModel.getNetworkBannerLiveData().observe(this, event -> {
            if (event == null) return;

            Boolean shouldShow = event.getContentIfNotHandled();
            if (shouldShow == null) return;

            if (shouldShow) {
                InternetErrorBanner.show(this);
            } else {
                InternetErrorBanner.hide(this);
            }
        });

        viewModel.getAvailabilityDetailsLiveData().observe(this, event -> {
            if (event == null) return;

            RoomAvailabilityDetailsResponse data = event.getContentIfNotHandled();
            if (data == null || !matchesSelectedPrefix(data.getPrefix())) return;

            showAvailabilityDetailsDialog(data);
        });

        viewModel.getAvailableRoomsLiveData().observe(this, event -> {
            if (event == null) return;

            AvailableRoomsResponse data = event.getContentIfNotHandled();
            if (data == null || !matchesSelectedPrefix(data.getPrefix())) {
                AppDiagnostics.logEvent("available_rooms_ui_update_skipped");
                return;
            }

            showAvailableRoomsBottomSheet(data);
        });

        viewModel.getAvailableRoomsRangeLiveData().observe(this, event -> {
            if (event == null) return;

            AvailableRoomsRangeResponse data = event.getContentIfNotHandled();
            if (data == null || !matchesSelectedPrefix(data.getPrefix())) {
                AppDiagnostics.logEvent("available_rooms_range_ui_update_skipped");
                return;
            }

            showAvailableRoomsRangeBottomSheet(data);
        });

        viewModel.getDeleteBookingResultLiveData().observe(this, event -> {
            if (event == null) return;

            LandingViewModel.DeleteBookingResult result =
                    event.getContentIfNotHandled();
            if (result == null) return;

            handleDeleteBookingResult(result);
        });
    }

    private void setupCalendarGestureSelection() {
        GestureDetector gestureDetector = new GestureDetector(
                this,
                new GestureDetector.SimpleOnGestureListener() {

                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        CalendarDayItem item = getCalendarItemFromTouch(e);

                        if (item == null || item.isEmpty()) {
                            clearRangeSelectionIfActive();
                            return true;
                        }

                        handleCalendarDateSingleTap(item);
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        CalendarDayItem item = getCalendarItemFromTouch(e);

                        if (item == null || item.isEmpty()) {
                            return true;
                        }

                        fetchAvailableRoomsForDate(item.getDate());
                        return true;
                    }

                    @Override
                    public void onLongPress(MotionEvent e) {
                        CalendarDayItem item = getCalendarItemFromTouch(e);

                        if (item == null || item.isEmpty()) {
                            return;
                        }

                        startRangeSelection(item);
                    }
                }
        );

        rvAvailabilityGroups.addOnItemTouchListener(
                new RecyclerView.SimpleOnItemTouchListener() {

                    @Override
                    public boolean onInterceptTouchEvent(
                            @NonNull RecyclerView rv,
                            @NonNull MotionEvent e
                    ) {
                        gestureDetector.onTouchEvent(e);
                        return false;
                    }

                    @Override
                    public void onTouchEvent(
                            @NonNull RecyclerView rv,
                            @NonNull MotionEvent e
                    ) {
                        gestureDetector.onTouchEvent(e);
                    }
                }
        );
    }

    private CalendarDayItem getCalendarItemFromTouch(MotionEvent e) {
        View child = rvAvailabilityGroups.findChildViewUnder(e.getX(), e.getY());

        if (child == null) {
            return null;
        }

        int position = rvAvailabilityGroups.getChildAdapterPosition(child);

        if (position == RecyclerView.NO_POSITION || calendarAdapter == null) {
            return null;
        }

        return calendarAdapter.getItemAt(position);
    }

    private void startRangeSelection(CalendarDayItem item) {
        arrivalDayItem = item;
        departureDayItem = null;

        arrivalDateForRange = item.getDate();
        departureDateForRange = item.getDate();

        isWaitingForDepartureDate = true;
        hasActiveRangeSelection = true;

        calendarAdapter.setSelectedRange(arrivalDateForRange, departureDateForRange);
        updateSelectedDateRangeBanner();
    }

    private void handleCalendarDateSingleTap(CalendarDayItem day) {
        if (isWaitingForDepartureDate && arrivalDateForRange != null) {
            completeRangeSelection(day);
            return;
        }

        if (hasActiveRangeSelection) {
            clearRangeSelectionAndEnableRefresh();
            return;
        }

        fetchAvailabilityDetails(day.getDate());
    }

    private void completeRangeSelection(CalendarDayItem day) {
        String clickedDate = day.getDate();

        if (clickedDate.compareTo(arrivalDateForRange) < 0) {
            departureDateForRange = arrivalDateForRange;
            arrivalDateForRange = clickedDate;
        } else {
            departureDateForRange = clickedDate;
        }

        departureDayItem = day;

        calendarAdapter.setSelectedRange(arrivalDateForRange, departureDateForRange);

        isWaitingForDepartureDate = false;
        hasActiveRangeSelection = true;
        updateSelectedDateRangeBanner();
    }

    private void clearRangeSelectionIfActive() {
        if (hasActiveRangeSelection) {
            clearRangeSelectionAndEnableRefresh();
        }
    }

    private void clearRangeSelectionAndEnableRefresh() {
        if (calendarAdapter != null) {
            calendarAdapter.clearSelectedRange();
        }

        hasActiveRangeSelection = false;
        isWaitingForDepartureDate = false;

        arrivalDayItem = null;
        departureDayItem = null;

        arrivalDateForRange = null;
        departureDateForRange = null;

        selectedAvailableArrivalDate = null;
        selectedAvailableDepartureDate = null;
        updateSelectedDateRangeBanner();

        if (swipeRefreshAvailability != null) {
            swipeRefreshAvailability.setRefreshing(false);
            swipeRefreshAvailability.setEnabled(true);
        }

        if (rvAvailabilityGroups != null && rvAvailabilityGroups.getParent() != null) {
            rvAvailabilityGroups.getParent().requestDisallowInterceptTouchEvent(false);
        }
    }

    private void updateSelectedDateRangeBanner() {
        if (bannerSelectedDateRange == null) {
            return;
        }

        if (!hasActiveRangeSelection || arrivalDateForRange == null) {
            bannerSelectedDateRange.setText("Long press to select arrival date");
            bannerSelectedDateRange.setVisibility(View.VISIBLE);
            return;
        }

        if (isWaitingForDepartureDate) {
            bannerSelectedDateRange.setText(
                    "Arrival: " + arrivalDateForRange + "  |  Select departure date"
            );
        } else {
            bannerSelectedDateRange.setText(
                    "Arrival: " + arrivalDateForRange
                            + "  |  Departure: " + departureDateForRange
            );
        }

        bannerSelectedDateRange.setVisibility(View.VISIBLE);
    }

    private void loadAvailability() {
        int month = selectedMonth.get(Calendar.MONTH) + 1;
        int year = selectedMonth.get(Calendar.YEAR);

        tvMonthYear.setText(monthYearFormat.format(selectedMonth.getTime()));
        viewModel.loadAvailability(month, year);
    }

    private void refreshAvailability() {
        int month = selectedMonth.get(Calendar.MONTH) + 1;
        int year = selectedMonth.get(Calendar.YEAR);

        tvMonthYear.setText(monthYearFormat.format(selectedMonth.getTime()));
        viewModel.refreshAvailability(month, year);
    }

    private void refreshAvailabilityIfStaleOnForeground() {
        int month = selectedMonth.get(Calendar.MONTH) + 1;
        int year = selectedMonth.get(Calendar.YEAR);

        viewModel.refreshAvailabilityIfStaleOnForeground(month, year);
    }

    private void updateAvailabilityStatus(String message) {
        if (tvAvailabilityStatus == null) {
            return;
        }

        if (message == null || message.trim().isEmpty()) {
            tvAvailabilityStatus.setVisibility(View.GONE);
            tvAvailabilityStatus.setText("");
            return;
        }

        tvAvailabilityStatus.setText(message.trim());
        tvAvailabilityStatus.setVisibility(View.VISIBLE);
    }

    private void applyPrefixFilter() {
        RoomAvailabilityGroup selectedGroup = findSelectedRoomGroup();

        if (selectedGroup == null) {
            showEmptyCalendarForSelectedPrefix();
            return;
        }

        tvTotalRoomCapacity.setText(String.valueOf(
                RoomInventory.displayTotalRooms(selectedPrefix, selectedGroup.getTotalRooms())
        ));
        calendarAdapter.submitList(buildCalendarItems(selectedGroup));

        restoreSelectedRangeIfNeeded();
    }

    private RoomAvailabilityGroup findSelectedRoomGroup() {
        for (RoomAvailabilityGroup group : allGroups) {
            if (group != null && group.matchesPrefix(selectedPrefix)) {
                return group;
            }
        }

        return null;
    }

    private void showEmptyCalendarForSelectedPrefix() {
        tvTotalRoomCapacity.setText("0");
        calendarAdapter.submitList(buildEmptyCalendar());

        showToast("No " + selectedPrefix + " rooms found");
    }

    private void restoreSelectedRangeIfNeeded() {
        if (hasActiveRangeSelection
                && arrivalDateForRange != null
                && departureDateForRange != null) {

            calendarAdapter.setSelectedRange(arrivalDateForRange, departureDateForRange);
        }
    }

    private List<CalendarDayItem> buildEmptyCalendar() {
        List<CalendarDayItem> calendarItems = new ArrayList<>();

        addEmptyCellsBeforeMonth(calendarItems);

        int maxDays = getMaxDaysInSelectedMonth();

        for (int day = 1; day <= maxDays; day++) {
            calendarItems.add(new CalendarDayItem(
                    day,
                    buildDateString(day),
                    CalendarDayItem.TYPE_AVAILABLE,
                    0
            ));
        }

        return calendarItems;
    }

    private List<CalendarDayItem> buildCalendarItems(RoomAvailabilityGroup group) {
        List<CalendarDayItem> calendarItems = new ArrayList<>();

        addEmptyCellsBeforeMonth(calendarItems);

        int maxDays = getMaxDaysInSelectedMonth();

        Map<String, RoomAvailabilityDay> availabilityMap = buildAvailabilityMap(group);

        for (int day = 1; day <= maxDays; day++) {
            String date = buildDateString(day);

            RoomAvailabilityDay availabilityDay = availabilityMap.get(date);

            int availabilityType = CalendarDayItem.TYPE_AVAILABLE;
            int availableRooms = 0;

            if (availabilityDay != null) {
                availableRooms = RoomInventory.displayAvailableRooms(selectedPrefix, availabilityDay);
                availabilityType = getAvailabilityType(availabilityDay);
            }

            calendarItems.add(new CalendarDayItem(
                    day,
                    date,
                    availabilityType,
                    availableRooms
            ));
        }

        return calendarItems;
    }

    private Map<String, RoomAvailabilityDay> buildAvailabilityMap(RoomAvailabilityGroup group) {
        Map<String, RoomAvailabilityDay> availabilityMap = new HashMap<>();

        if (group == null || !group.hasCalendar()) {
            return availabilityMap;
        }

        for (RoomAvailabilityDay day : group.getCalendar()) {
            if (day != null && day.getDate() != null) {
                availabilityMap.put(day.getDate(), day);
            }
        }

        return availabilityMap;
    }

    private void addEmptyCellsBeforeMonth(List<CalendarDayItem> calendarItems) {
        Calendar temp = (Calendar) selectedMonth.clone();
        temp.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = temp.get(Calendar.DAY_OF_WEEK);
        int emptyCellsBeforeMonth = firstDayOfWeek - Calendar.SUNDAY;

        for (int i = 0; i < emptyCellsBeforeMonth; i++) {
            calendarItems.add(new CalendarDayItem(
                    0,
                    "",
                    CalendarDayItem.TYPE_EMPTY,
                    0
            ));
        }
    }

    private int getMaxDaysInSelectedMonth() {
        Calendar temp = (Calendar) selectedMonth.clone();
        temp.set(Calendar.DAY_OF_MONTH, 1);
        return temp.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    private int getAvailabilityType(RoomAvailabilityDay day) {
        int totalRooms = RoomInventory.displayTotalRooms(selectedPrefix, day.getTotalRooms());
        int availableRooms = RoomInventory.displayAvailableRooms(selectedPrefix, day);

        if (totalRooms <= 0) {
            return CalendarDayItem.TYPE_AVAILABLE;
        }

        if (day.hasPartialBooking() || day.hasBefore6pmBooking()) {
            return CalendarDayItem.TYPE_HALF_AVAILABLE;
        }

        if (availableRooms == totalRooms) {
            return CalendarDayItem.TYPE_AVAILABLE;
        }

        if (availableRooms <= 0) {
            return CalendarDayItem.TYPE_NOT_AVAILABLE;
        }

        double availablePercentage = (availableRooms * 100.0) / totalRooms;

        if (availablePercentage < LESS_THAN_HALF_PERCENTAGE) {
            return CalendarDayItem.TYPE_LESS_THAN_HALF_AVAILABLE;
        }

        return CalendarDayItem.TYPE_HALF_AVAILABLE;
    }

    private String buildDateString(int day) {
        int month = selectedMonth.get(Calendar.MONTH) + 1;
        int year = selectedMonth.get(Calendar.YEAR);

        return String.format(
                Locale.getDefault(),
                "%04d-%02d-%02d",
                year,
                month,
                day
        );
    }

    private void fetchAvailabilityDetails(String date) {
        viewModel.loadAvailabilityDetails(date, selectedPrefix);
    }

    private void fetchAvailableRoomsForDate(String date) {
        viewModel.loadAvailableRoomsForDate(date, selectedPrefix);
    }

    private void fetchAvailableRoomsForDateRange(String arrivalDate, String departureDate) {
        viewModel.loadAvailableRoomsForDateRange(
                arrivalDate,
                departureDate,
                selectedPrefix
        );
    }

    private void setAvailableRoomsLoading(boolean loading) {
        btnCheckAvailability.setEnabled(!loading);
        btnCheckAvailability.setAlpha(loading ? 0.55f : 1.0f);
    }

    private void showAvailableRoomsBottomSheet(AvailableRoomsResponse data) {
        selectedAvailableArrivalDate = data.getDate();
        selectedAvailableDepartureDate = data.getDate();
        String sheetKey = AvailabilityRepository.availableRoomsCacheKey(
                data.getPrefix(),
                data.getDate()
        );
        if (isSameAvailabilitySheetShowing(sheetKey)) {
            activeAvailableRoomsResponse = data;
            activeAvailableRoomsRangeResponse = null;
            updateActiveAvailabilitySheetStatus();
            AppDiagnostics.logEvent("availability_sheet_duplicate_ignored key=" + safe(sheetKey));
            return;
        }
        replaceActiveAvailabilitySheet(sheetKey);

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        activeBottomSheetDialog = dialog;
        activeAvailabilitySheetKey = sheetKey;
        activeAvailableRoomsResponse = data;
        activeAvailableRoomsRangeResponse = null;

        LinearLayout container = createBottomSheetContainer();
        List<AvailableRoomItem> visibleRooms = RoomInventory.visibleAvailableRooms(
                data.getPrefix(),
                data.getRooms()
        );

        TextView title = createBottomSheetTitle("Available Rooms");

        TextView subtitle = createBottomSheetSubtitle(
                "Date: " + safe(data.getDate())
                        + "\nBuilding: " + safe(data.getPrefix())
                        + "\nTotal Available: " + visibleRooms.size()
        );

        container.addView(title);
        container.addView(subtitle);
        TextView status = createBottomSheetStatus(viewModel.getAvailableRoomsSheetStatus(data));
        activeAvailabilitySheetStatus = status;
        container.addView(status);

        addAvailableRoomViews(
                container,
                visibleRooms,
                "No room available on this date."
        );

        View scrollableContent = createScrollableBottomSheetContent(container);

        dialog.setOnDismissListener(d -> {
            handleAvailabilitySheetDismissed(dialog);
        });

        showExpandedBottomSheet(dialog, scrollableContent);
    }

    private void showAvailableRoomsRangeBottomSheet(AvailableRoomsRangeResponse data) {
        selectedAvailableArrivalDate = data.getArrivalDate();
        selectedAvailableDepartureDate = data.getDepartureDate();
        String sheetKey = AvailabilityRepository.availableRoomsRangeCacheKey(
                data.getPrefix(),
                data.getArrivalDate(),
                data.getDepartureDate()
        );
        if (isSameAvailabilitySheetShowing(sheetKey)) {
            activeAvailableRoomsResponse = null;
            activeAvailableRoomsRangeResponse = data;
            updateActiveAvailabilitySheetStatus();
            AppDiagnostics.logEvent("availability_sheet_duplicate_ignored key=" + safe(sheetKey));
            return;
        }
        replaceActiveAvailabilitySheet(sheetKey);

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        activeBottomSheetDialog = dialog;
        activeAvailabilitySheetKey = sheetKey;
        activeAvailableRoomsResponse = null;
        activeAvailableRoomsRangeResponse = data;

        LinearLayout container = createBottomSheetContainer();
        List<AvailableRoomItem> visibleRooms = RoomInventory.visibleAvailableRooms(
                data.getPrefix(),
                data.getRooms()
        );

        TextView title = createBottomSheetTitle("Available Rooms");

        TextView subtitle = createBottomSheetSubtitle(
                "Arrival: " + safe(data.getArrivalDate())
                        + "\nDeparture: " + safe(data.getDepartureDate())
                        + "\nBuilding: " + safe(data.getPrefix())
                        + "\nTotal Available: " + visibleRooms.size()
        );

        container.addView(title);
        container.addView(subtitle);
        TextView status = createBottomSheetStatus(
                viewModel.getAvailableRoomsRangeSheetStatus(data)
        );
        activeAvailabilitySheetStatus = status;
        container.addView(status);

        addAvailableRoomViews(
                container,
                visibleRooms,
                "No room available for selected date range."
        );

        View scrollableContent = createScrollableBottomSheetContent(container);

        dialog.setOnDismissListener(d -> {
            handleAvailabilitySheetDismissed(dialog);
        });

        showExpandedBottomSheet(dialog, scrollableContent);
    }

    private void replaceActiveAvailabilitySheet(String nextSheetKey) {
        if (activeBottomSheetDialog == null || !activeBottomSheetDialog.isShowing()) {
            return;
        }

        replacingAvailabilitySheet = true;
        activeBottomSheetDialog.dismiss();
        replacingAvailabilitySheet = false;
        AppDiagnostics.logEvent("availability_sheet_replaced key=" + safe(nextSheetKey));
    }

    private boolean isSameAvailabilitySheetShowing(String sheetKey) {
        return sheetKey != null
                && sheetKey.equals(activeAvailabilitySheetKey)
                && activeBottomSheetDialog != null
                && activeBottomSheetDialog.isShowing();
    }

    private void handleAvailabilitySheetDismissed(BottomSheetDialog dialog) {
        if (dialog != activeBottomSheetDialog) {
            return;
        }

        activeBottomSheetDialog = null;
        activeAvailabilitySheetKey = null;
        activeAvailabilitySheetStatus = null;
        activeAvailableRoomsResponse = null;
        activeAvailableRoomsRangeResponse = null;
        if (!replacingAvailabilitySheet) {
            clearRangeSelectionAndEnableRefresh();
        }
    }

    private void updateActiveAvailabilitySheetStatus() {
        if (activeAvailabilitySheetStatus == null) {
            return;
        }

        if (activeAvailableRoomsResponse != null) {
            activeAvailabilitySheetStatus.setText(
                    viewModel.getAvailableRoomsSheetStatus(activeAvailableRoomsResponse)
            );
            return;
        }

        if (activeAvailableRoomsRangeResponse != null) {
            activeAvailabilitySheetStatus.setText(
                    viewModel.getAvailableRoomsRangeSheetStatus(activeAvailableRoomsRangeResponse)
            );
        }
    }

    private LinearLayout createBottomSheetContainer() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(
                getDimenPx(R.dimen.space_32),
                getDimenPx(R.dimen.space_28),
                getDimenPx(R.dimen.space_32),
                getDimenPx(R.dimen.space_56)
        );
        return container;
    }


    private View createScrollableBottomSheetContent(View contentView) {
        NestedScrollView scrollView = new NestedScrollView(this);

        scrollView.setFillViewport(false);
        scrollView.setClipToPadding(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);

        scrollView.setPadding(
                0,
                0,
                0,
                getDimenPx(R.dimen.space_36)
        );

        scrollView.addView(
                contentView,
                new NestedScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        return scrollView;
    }

    private void showExpandedBottomSheet(BottomSheetDialog dialog, View contentView) {
        dialog.setContentView(contentView);

        dialog.setOnShowListener(dialogInterface -> {
            FrameLayout bottomSheet = dialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet
            );

            if (bottomSheet == null) {
                return;
            }

            ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
            layoutParams.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.90f);
            bottomSheet.setLayoutParams(layoutParams);

            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setSkipCollapsed(true);
            behavior.setPeekHeight((int) (getResources().getDisplayMetrics().heightPixels * 0.90f));
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });

        dialog.show();
    }

    private TextView createBottomSheetTitle(String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResponsiveDimension(R.dimen.text_20, R.dimen.text_22));
        title.setTextColor(getColor(R.color.detail_text_primary));
        title.setTypeface(null, Typeface.BOLD);
        return title;
    }

    private TextView createBottomSheetSubtitle(String text) {
        TextView subtitle = new TextView(this);
        subtitle.setText(text);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResponsiveDimension(R.dimen.text_14, R.dimen.text_16));
        subtitle.setTextColor(getColor(R.color.detail_text_secondary));
        subtitle.setPadding(0, getDimenPx(R.dimen.space_8), 0, getDimenPx(R.dimen.space_20));
        return subtitle;
    }

    private TextView createBottomSheetStatus(String text) {
        TextView status = new TextView(this);
        status.setText(safe(text));
        status.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResponsiveDimension(R.dimen.text_12, R.dimen.text_13));
        status.setTextColor(getColor(R.color.text_secondary));
        status.setPadding(0, 0, 0, getDimenPx(R.dimen.space_14));
        return status;
    }

    private void addAvailableRoomViews(
            LinearLayout container,
            List<AvailableRoomItem> rooms,
            String emptyMessage
    ) {
        if (rooms == null || rooms.isEmpty()) {
            addEmptyMessage(container, emptyMessage);
            return;
        }

        List<AvailableRoomItem> validRooms = RoomInventory.visibleAvailableRooms(
                selectedPrefix,
                rooms
        );

        if (validRooms.isEmpty()) {
            addEmptyMessage(container, emptyMessage);
            return;
        }

        for (AvailableRoomItem room : validRooms) {
            TextView roomView = createAvailableRoomView(room);
            container.addView(roomView);
        }
    }

    private void addEmptyMessage(LinearLayout container, String message) {
        TextView empty = new TextView(this);
        empty.setText(message);
        empty.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResponsiveDimension(R.dimen.text_15, R.dimen.text_16));
        empty.setTextColor(getColor(R.color.detail_text_secondary));
        container.addView(empty);
    }

    private TextView createAvailableRoomView(AvailableRoomItem room) {
        TextView roomView = new TextView(this);

        if (room.isPartiallyAvailable()) {
            roomView.setText(
                    room.getSafeSelectionLabel()
                            + "\nAvailable from: "
                            + room.getSafeAvailableFromDate()
                            + ", "
                            + room.getSafeAvailableFromTime()
            );

            roomView.setBackgroundColor(getColor(R.color.availability_border));
        } else {
            roomView.setText(
                    room.getSafeSelectionLabel()
                            + "\nAvailable"
            );

            roomView.setBackgroundResource(R.drawable.bg_detail_card);
        }

        roomView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResponsiveDimension(R.dimen.text_16, R.dimen.text_17));
        roomView.setTextColor(getColor(R.color.detail_text_primary));
        roomView.setPadding(
                getResponsiveDimenPx(R.dimen.space_28, R.dimen.space_32),
                getResponsiveDimenPx(R.dimen.space_18, R.dimen.space_20),
                getResponsiveDimenPx(R.dimen.space_28, R.dimen.space_32),
                getResponsiveDimenPx(R.dimen.space_18, R.dimen.space_20)
        );
        roomView.setClickable(true);
        roomView.setFocusable(true);
        roomView.setLayoutParams(createRoomViewLayoutParams());

        roomView.setOnClickListener(v -> openCreateBookingScreen(room));

        return roomView;
    }

    private LinearLayout.LayoutParams createRoomViewLayoutParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        params.setMargins(0, 0, 0, getDimenPx(R.dimen.space_14));
        return params;
    }

    private void openCreateBookingScreen(AvailableRoomItem room) {
        Intent intent = new Intent(LandingActivity.this, CreateBookingActivity.class);

        intent.putExtra(CreateBookingActivity.EXTRA_ROOM_ID, room.getRoomId());
        intent.putExtra(CreateBookingActivity.EXTRA_ROOM_NAME, room.getSafeSelectionLabel());

        intent.putExtra(CreateBookingActivity.EXTRA_ARRIVAL_DATE, selectedAvailableArrivalDate);
        intent.putExtra(CreateBookingActivity.EXTRA_DEPARTURE_DATE, selectedAvailableDepartureDate);
        intent.putExtra(CreateBookingActivity.EXTRA_IS_PARTIAL_ROOM, room.isPartiallyAvailable());
        intent.putExtra(CreateBookingActivity.EXTRA_AVAILABLE_FROM_DATE, room.getSafeAvailableFromDate());
        intent.putExtra(CreateBookingActivity.EXTRA_AVAILABLE_FROM_TIME, room.getSafeAvailableFromTime());

        createBookingLauncher.launch(intent);
    }

    private void handleBookingCreated() {
        String arrivalDate = selectedAvailableArrivalDate;
        String departureDate = selectedAvailableDepartureDate;

        if (activeBottomSheetDialog != null && activeBottomSheetDialog.isShowing()) {
            activeBottomSheetDialog.dismiss();
        }

        viewModel.invalidateCalendarAvailabilityCacheForMutation();
        loadAvailability();

        if (arrivalDate == null || departureDate == null) {
            return;
        }

        if (arrivalDate.equals(departureDate)) {
            fetchAvailableRoomsForDate(arrivalDate);
        } else {
            fetchAvailableRoomsForDateRange(arrivalDate, departureDate);
        }
    }

    private void showAvailabilityDetailsDialog(RoomAvailabilityDetailsResponse data) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        activeBottomSheetDialog = dialog;
        activeAvailabilitySheetKey = null;
        activeAvailabilitySheetStatus = null;
        activeAvailableRoomsResponse = null;
        activeAvailableRoomsRangeResponse = null;

        View view = getLayoutInflater().inflate(
                R.layout.bottom_sheet_availability_details,
                null
        );

        TextView tvTitle = view.findViewById(R.id.tvSheetTitle);
        TextView tvSubtitle = view.findViewById(R.id.tvSheetSubtitle);
        LinearLayout layoutDetails = view.findViewById(R.id.layoutBookingDetails);

        tvTitle.setText("Bookings on " + safe(data.getDate()));
        tvSubtitle.setText("Hostel: " + selectedPrefix);

        addBookingDetailsViews(layoutDetails, data.getBookings());

        View scrollableContent = createScrollableBottomSheetContent(view);

        dialog.setOnDismissListener(d -> {
            activeBottomSheetDialog = null;
            activeAvailabilitySheetKey = null;
            activeAvailabilitySheetStatus = null;
            activeAvailableRoomsResponse = null;
            activeAvailableRoomsRangeResponse = null;
            clearRangeSelectionAndEnableRefresh();
        });

        showExpandedBottomSheet(dialog, scrollableContent);
    }

    private void addBookingDetailsViews(
            LinearLayout layoutDetails,
            List<RoomAvailabilityBookingItem> bookings
    ) {
        if (bookings == null || bookings.isEmpty()) {
            addEmptyMessage(layoutDetails, "No bookings found for this date.");
            return;
        }

        List<RoomAvailabilityBookingItem> validBookings =
                NullSafeCollections.copyWithoutNulls(bookings);

        if (validBookings.isEmpty()) {
            addEmptyMessage(layoutDetails, "No bookings found for this date.");
            return;
        }

        for (RoomAvailabilityBookingItem item : validBookings) {
            TextView card = createBookingDetailsCard(layoutDetails, item);
            layoutDetails.addView(card);
        }
    }

    private TextView createBookingDetailsCard(
            LinearLayout layoutDetails,
            RoomAvailabilityBookingItem item
    ) {
        TextView card = new TextView(this);

        card.setText(
                "Person Name: " + item.getSafeGuestName() + "\n"
                        + "Room: " + item.getSafeSelectionLabel() + "\n"
                        + "Requestor: " + item.getSafeRequestorName() + "\n"
                        + "Arrival: " + DateTimeUtils.formatUtcToLocal(item.getArrivalAt()) + "\n"
                        + "Departure: " + DateTimeUtils.formatUtcToLocal(item.getDepartureAt())
        );

        card.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResponsiveDimension(R.dimen.text_15, R.dimen.text_16));
        card.setTextColor(getColor(R.color.detail_text_primary));
        card.setBackgroundResource(R.drawable.bg_detail_card);
        card.setPadding(
                getResponsiveDimenPx(R.dimen.space_28, R.dimen.space_32),
                getDimenPx(R.dimen.space_22),
                getResponsiveDimenPx(R.dimen.space_28, R.dimen.space_32),
                getDimenPx(R.dimen.space_22)
        );
        card.setLayoutParams(createBookingCardLayoutParams());
        card.setOnLongClickListener(v -> {
            showDeleteBookingDialog(layoutDetails, card, item);
            return true;
        });

        return card;
    }

    private void showDeleteBookingDialog(
            LinearLayout layoutDetails,
            View card,
            RoomAvailabilityBookingItem item
    ) {
        if (item == null || item.getBookingId() <= 0) {
            showToast("Booking details are missing.");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Booking")
                .setMessage("Delete booking for " + item.getSafeGuestName() + " permanently?")
                .setPositiveButton("Delete Booking", (dialog, which) ->
                        deleteBookingFromDetails(layoutDetails, card, item)
                )
                .setNegativeButton("Close", null)
                .show();
    }

    private void deleteBookingFromDetails(
            LinearLayout layoutDetails,
            View card,
            RoomAvailabilityBookingItem item
    ) {
        if (pendingDeleteCard != null) {
            showToast("Deletion is already in progress.");
            return;
        }

        pendingDeleteLayoutDetails = layoutDetails;
        pendingDeleteCard = card;
        pendingDeleteItem = item;
        setCardDeletingState(card, true);
        viewModel.deleteBooking(item.getBookingId());
    }

    private void handleDeleteBookingResult(LandingViewModel.DeleteBookingResult result) {
        if (pendingDeleteItem == null
                || pendingDeleteItem.getBookingId() != result.getBookingId()) {
            return;
        }

        if (pendingDeleteLayoutDetails != null && pendingDeleteCard != null) {
            pendingDeleteLayoutDetails.removeView(pendingDeleteCard);
            if (pendingDeleteLayoutDetails.getChildCount() == 0) {
                addEmptyMessage(pendingDeleteLayoutDetails, "No bookings found for this date.");
            }
        }

        clearPendingDeleteState();
        loadAvailability();
        showToast(result.getMessage());
    }

    private void resetPendingDeleteCardIfNeeded() {
        if (pendingDeleteCard != null) {
            setCardDeletingState(pendingDeleteCard, false);
            clearPendingDeleteState();
        }
    }

    private void clearPendingDeleteState() {
        pendingDeleteLayoutDetails = null;
        pendingDeleteCard = null;
        pendingDeleteItem = null;
    }

    private void setCardDeletingState(View card, boolean deleting) {
        if (card == null) {
            return;
        }

        card.setEnabled(!deleting);
        card.setClickable(!deleting);
        card.setLongClickable(!deleting);
        card.setAlpha(deleting ? 0.55f : 1.0f);
    }

    private LinearLayout.LayoutParams createBookingCardLayoutParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        params.setMargins(0, 0, 0, getDimenPx(R.dimen.space_14));
        return params;
    }

    private float getResponsiveDimension(int phoneDimenResId, int tabletDimenResId) {
        int dimenResId = getResources().getConfiguration().smallestScreenWidthDp >= 600
                ? tabletDimenResId
                : phoneDimenResId;
        return getResources().getDimension(dimenResId);
    }

    private int getResponsiveDimenPx(int phoneDimenResId, int tabletDimenResId) {
        return Math.round(getResponsiveDimension(phoneDimenResId, tabletDimenResId));
    }

    private int getDimenPx(int dimenResId) {
        return Math.round(getResources().getDimension(dimenResId));
    }

    private void showToast(String message) {
        showToast(message, Toast.LENGTH_SHORT);
    }

    private void showToast(String message, int duration) {
        Toast.makeText(this, message, duration).show();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean matchesSelectedPrefix(String prefix) {
        return prefix == null
                || prefix.trim().isEmpty()
                || prefix.equalsIgnoreCase(selectedPrefix);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putLong(STATE_SELECTED_MONTH, selectedMonth.getTimeInMillis());
        outState.putString(STATE_SELECTED_PREFIX, selectedPrefix);
        outState.putBoolean(STATE_WAITING_FOR_DEPARTURE, isWaitingForDepartureDate);
        outState.putBoolean(STATE_HAS_ACTIVE_RANGE, hasActiveRangeSelection);
        outState.putString(STATE_ARRIVAL_DATE, arrivalDateForRange);
        outState.putString(STATE_DEPARTURE_DATE, departureDateForRange);
        super.onSaveInstanceState(outState);
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

        refreshAvailabilityIfStaleOnForeground();
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

    @Override
    protected void onDestroy() {
        stopSyncStatusTimer();
        super.onDestroy();
    }
}
