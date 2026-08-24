package com.example.roombooking.requester;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.roombooking.R;
import com.example.roombooking.auth.AuthSessionManager;
import com.example.roombooking.auth.AuthSessionGuard;
import com.example.roombooking.booking.AvailableRoomItem;
import com.example.roombooking.booking.AvailableRoomsRangeResponse;
import com.example.roombooking.booking.CalendarAvailabilityAdapter;
import com.example.roombooking.booking.CalendarDayItem;
import com.example.roombooking.booking.RoomAvailabilityDay;
import com.example.roombooking.booking.RoomAvailabilityGroup;
import com.example.roombooking.model.room.RoomInventory;
import com.example.roombooking.model.room.RoomPrefix;
import com.example.roombooking.utils.AppToolbarMenu;
import com.example.roombooking.utils.DateTimeUtils;
import com.example.roombooking.utils.EdgeToEdgeUtils;
import com.example.roombooking.utils.InternetErrorBanner;
import com.example.roombooking.utils.NullSafeCollections;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RequesterLandingActivity extends AppCompatActivity {

    private static final int CALENDAR_SPAN_COUNT = 7;
    private static final double LESS_THAN_HALF_PERCENTAGE = 50.0;
    private static final long SYNC_STATUS_REFRESH_INTERVAL_MS = 30L * 1000L;
    private static final long REQUEST_BOOKING_CLICK_DEBOUNCE_MS = 700L;
    private static final String DEFAULT_ROOM_PREFIX = RoomPrefix.DELTA;
    private static final String STATE_SELECTED_MONTH = "requester_selected_month";
    private static final String STATE_SELECTED_PREFIX = "requester_selected_prefix";
    private static final String STATE_WAITING_FOR_DEPARTURE = "requester_waiting_for_departure";
    private static final String STATE_HAS_ACTIVE_RANGE = "requester_has_active_range";
    private static final String STATE_ARRIVAL_DATE = "requester_arrival_date";
    private static final String STATE_DEPARTURE_DATE = "requester_departure_date";

    private final Calendar selectedMonth = Calendar.getInstance();
    private final List<RoomAvailabilityGroup> allGroups = new ArrayList<>();
    private final SimpleDateFormat monthYearFormat =
            new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

    private MaterialToolbar toolbar;
    private MaterialButton btnPreviousMonth;
    private MaterialButton btnNextMonth;
    private ImageButton btnRequestBooking;
    private TextView tvMonthYear;
    private TextView tvCapacity;
    private TextView tvStatus;
    private TextView tvRange;
    private TabLayout tabLayoutRooms;
    private SwipeRefreshLayout swipeRefreshAvailability;
    private RecyclerView rvAvailabilityGroups;

    private CalendarAvailabilityAdapter calendarAdapter;
    private String selectedPrefix = DEFAULT_ROOM_PREFIX;
    private String arrivalDateForRange = null;
    private String departureDateForRange = null;
    private boolean isWaitingForDepartureDate = false;
    private boolean hasActiveRangeSelection = false;
    private boolean hasHandledInitialResume = false;
    private boolean suppressRoomTabLoad = false;
    private BottomSheetDialog activeAvailableRoomsDialog = null;
    private String activeAvailableRoomsSheetKey = null;
    private long lastRequestBookingClickAtMillis = 0L;
    private AuthSessionManager sessionManager;
    private RequesterLandingViewModel viewModel;
    private final Handler syncStatusHandler = new Handler(Looper.getMainLooper());
    private final Runnable syncStatusRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewModel != null) {
                viewModel.refreshVisibleSyncStatusAge();
            }
            syncStatusHandler.postDelayed(this, SYNC_STATUS_REFRESH_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requester_landing);
        if (!AuthSessionGuard.ensureRequester(this)) {
            return;
        }

        sessionManager = new AuthSessionManager(getApplicationContext());
        selectedMonth.set(Calendar.DAY_OF_MONTH, 1);
        restoreState(savedInstanceState);
        EdgeToEdgeUtils.applySystemBarInsets(this, findViewById(R.id.main));

        initViewModel();
        bindViews();
        setupToolbarMenu();
        setupRecyclerView();
        setupCalendarGestureSelection();
        setupListeners();
        setupRoomTabs();
        observeViewModel();

        updateMonthTitle();
        updateSelectedDateRangeBanner();
        loadAvailability();
    }

    private void initViewModel() {
        RequesterAvailabilityRepository availabilityRepository =
                new RequesterAvailabilityRepository(getApplicationContext());
        RequesterLandingViewModelFactory factory =
                new RequesterLandingViewModelFactory(availabilityRepository);
        viewModel = new ViewModelProvider(this, factory).get(RequesterLandingViewModel.class);
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar);
        btnPreviousMonth = findViewById(R.id.btnPreviousMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        btnRequestBooking = findViewById(R.id.btnRequestBooking);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        tvCapacity = findViewById(R.id.tvTotalRoomCapacity);
        tvStatus = findViewById(R.id.tvAvailabilityStatus);
        tvRange = findViewById(R.id.bannerSelectedDateRange);
        tabLayoutRooms = findViewById(R.id.tabLayoutRooms);
        swipeRefreshAvailability = findViewById(R.id.swipeRefreshAvailability);
        rvAvailabilityGroups = findViewById(R.id.rvAvailabilityGroups);
    }

    private void setupToolbarMenu() {
        AppToolbarMenu.setupRequester(this, toolbar);
    }

    private void setupRecyclerView() {
        calendarAdapter = new CalendarAvailabilityAdapter(day -> {
            // GestureDetector handles requester date-range selection.
        });
        rvAvailabilityGroups.setLayoutManager(new GridLayoutManager(this, CALENDAR_SPAN_COUNT));
        rvAvailabilityGroups.setAdapter(calendarAdapter);
    }

    private void setupListeners() {
        btnRequestBooking.setOnClickListener(v -> {
            if (isRapidRequestBookingClick()) {
                return;
            }

            openRequestForm();
        });

        btnPreviousMonth.setOnClickListener(v -> {
            selectedMonth.add(Calendar.MONTH, -1);
            updateMonthTitle();
            clearRangeSelection();
            loadAvailability();
        });

        btnNextMonth.setOnClickListener(v -> {
            selectedMonth.add(Calendar.MONTH, 1);
            updateMonthTitle();
            clearRangeSelection();
            loadAvailability();
        });

        swipeRefreshAvailability.setColorSchemeResources(
                R.color.primary,
                R.color.success_green,
                R.color.error_red
        );
        swipeRefreshAvailability.setOnRefreshListener(this::refreshAvailability);
    }

    private boolean isRapidRequestBookingClick() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastRequestBookingClickAtMillis < REQUEST_BOOKING_CLICK_DEBOUNCE_MS) {
            return true;
        }

        lastRequestBookingClickAtMillis = now;
        return false;
    }

    private void setupRoomTabs() {
        tabLayoutRooms.clearOnTabSelectedListeners();
        tabLayoutRooms.removeAllTabs();

        suppressRoomTabLoad = true;
        int selectedIndex = 0;
        List<String> prefixes = RoomPrefix.displayOrder();
        for (int i = 0; i < prefixes.size(); i++) {
            String prefix = prefixes.get(i);
            tabLayoutRooms.addTab(tabLayoutRooms.newTab().setText(prefix));
            if (prefix.equalsIgnoreCase(selectedPrefix)) {
                selectedIndex = i;
            }
        }

        tabLayoutRooms.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab == null || tab.getText() == null) {
                    return;
                }

                selectedPrefix = safe(String.valueOf(tab.getText()));
                clearRangeSelection();
                if (!suppressRoomTabLoad) {
                    loadAvailability();
                }
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

        TabLayout.Tab tab = tabLayoutRooms.getTabAt(selectedIndex);
        if (tab != null) {
            tab.select();
            selectedPrefix = safe(String.valueOf(tab.getText()));
        } else {
            selectedPrefix = DEFAULT_ROOM_PREFIX;
        }
        suppressRoomTabLoad = false;
    }

    private void observeViewModel() {
        viewModel.getAvailabilityLoadingLiveData().observe(this, isLoading ->
                swipeRefreshAvailability.setRefreshing(Boolean.TRUE.equals(isLoading))
        );

        viewModel.getAvailableRoomsLoadingLiveData().observe(this, this::setRequestLoading);

        viewModel.getAvailabilityGroupsLiveData().observe(this, groups -> {
            allGroups.clear();
            allGroups.addAll(NullSafeCollections.copyWithoutNulls(groups));
            applySelectedGroup();
        });

        viewModel.getAvailabilityStatusLiveData().observe(this, this::setStatus);

        viewModel.getToastLiveData().observe(this, event -> {
            if (event == null) {
                return;
            }

            String message = event.getContentIfNotHandled();
            if (!isBlank(message)) {
                showToast(message);
            }
        });

        viewModel.getAvailableRoomsRangeLiveData().observe(this, event -> {
            if (event == null) {
                return;
            }

            AvailableRoomsRangeResponse data = event.getContentIfNotHandled();
            if (data != null) {
                showAvailableRoomsBottomSheet(data);
            }
        });

        viewModel.getNetworkBannerLiveData().observe(this, event -> {
            if (event == null) {
                return;
            }

            Boolean shouldShow = event.getContentIfNotHandled();
            if (shouldShow == null) {
                return;
            }

            if (shouldShow) {
                InternetErrorBanner.show(this);
            } else {
                InternetErrorBanner.hide(this);
            }
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
                    public void onLongPress(MotionEvent e) {
                        CalendarDayItem item = getCalendarItemFromTouch(e);
                        if (item == null || item.isEmpty()) {
                            return;
                        }
                        startRangeSelection(item);
                    }
                }
        );

        rvAvailabilityGroups.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
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
        });
    }

    private CalendarDayItem getCalendarItemFromTouch(MotionEvent event) {
        View child = rvAvailabilityGroups.findChildViewUnder(event.getX(), event.getY());
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
        arrivalDateForRange = item.getSafeDate();
        departureDateForRange = item.getSafeDate();
        isWaitingForDepartureDate = true;
        hasActiveRangeSelection = true;

        calendarAdapter.setSelectedRange(arrivalDateForRange, departureDateForRange);
        updateSelectedDateRangeBanner();
    }

    private void handleCalendarDateSingleTap(CalendarDayItem day) {
        if (isWaitingForDepartureDate && !isBlank(arrivalDateForRange)) {
            completeRangeSelection(day);
            return;
        }

        if (hasActiveRangeSelection) {
            showToast("Long press another date to start a new range.");
            return;
        }

        showToast("Long press a date to select arrival.");
    }

    private void completeRangeSelection(CalendarDayItem day) {
        String clickedDate = day.getSafeDate();
        if (clickedDate.compareTo(arrivalDateForRange) < 0) {
            departureDateForRange = arrivalDateForRange;
            arrivalDateForRange = clickedDate;
        } else {
            departureDateForRange = clickedDate;
        }

        isWaitingForDepartureDate = false;
        hasActiveRangeSelection = true;
        calendarAdapter.setSelectedRange(arrivalDateForRange, departureDateForRange);
        updateSelectedDateRangeBanner();
    }

    private void clearRangeSelectionIfActive() {
        if (!hasActiveRangeSelection) {
            return;
        }

        clearRangeSelection();
    }

    private void clearRangeSelection() {
        if (calendarAdapter != null) {
            calendarAdapter.clearSelectedRange();
        }
        hasActiveRangeSelection = false;
        isWaitingForDepartureDate = false;
        arrivalDateForRange = null;
        departureDateForRange = null;
        updateSelectedDateRangeBanner();
    }

    private void loadAvailability() {
        viewModel.loadAvailability(
                sessionManager.getUserId(),
                selectedPrefix,
                selectedMonth.get(Calendar.MONTH) + 1,
                selectedMonth.get(Calendar.YEAR)
        );
    }

    private void refreshAvailability() {
        viewModel.refreshAvailability(
                sessionManager.getUserId(),
                selectedPrefix,
                selectedMonth.get(Calendar.MONTH) + 1,
                selectedMonth.get(Calendar.YEAR)
        );
    }

    private void refreshAvailabilityIfStaleOnForeground() {
        viewModel.refreshAvailabilityIfStaleOnForeground(
                sessionManager.getUserId(),
                selectedPrefix,
                selectedMonth.get(Calendar.MONTH) + 1,
                selectedMonth.get(Calendar.YEAR)
        );
    }

    private void applySelectedGroup() {
        RoomAvailabilityGroup group = findSelectedGroup();
        if (group == null) {
            tvCapacity.setText("0");
            calendarAdapter.submitList(buildEmptyCalendar());
            return;
        }

        tvCapacity.setText(String.valueOf(
                RoomInventory.displayTotalRooms(selectedPrefix, group.getTotalRooms())
        ));
        calendarAdapter.submitList(buildCalendarItems(group));
        restoreSelectedRangeIfNeeded();
    }

    private RoomAvailabilityGroup findSelectedGroup() {
        return findGroupByPrefix(selectedPrefix);
    }

    private RoomAvailabilityGroup findGroupByPrefix(String prefix) {
        for (RoomAvailabilityGroup group : allGroups) {
            if (group != null && group.matchesPrefix(prefix)) {
                return group;
            }
        }
        return null;
    }

    private void restoreSelectedRangeIfNeeded() {
        if (hasActiveRangeSelection
                && !isBlank(arrivalDateForRange)
                && !isBlank(departureDateForRange)) {
            calendarAdapter.setSelectedRange(arrivalDateForRange, departureDateForRange);
        }
    }

    private void openRequestForm() {
        if (!hasActiveRangeSelection || isBlank(arrivalDateForRange)) {
            showToast("Long press to select arrival date first.");
            return;
        }

        if (isBlank(departureDateForRange)) {
            departureDateForRange = arrivalDateForRange;
        }

        if (departureDateForRange.compareTo(arrivalDateForRange) < 0) {
            showToast("Departure date cannot be before arrival date.");
            return;
        }

        isWaitingForDepartureDate = false;
        calendarAdapter.setSelectedRange(arrivalDateForRange, departureDateForRange);
        updateSelectedDateRangeBanner();

        viewModel.loadAvailableRoomsForDateRange(
                arrivalDateForRange,
                departureDateForRange,
                selectedPrefix
        );
    }

    private void openRequestForm(AvailableRoomItem room) {
        Intent intent = new Intent(this, RequesterRequestBookingActivity.class);
        intent.putExtra(RequesterRequestBookingActivity.EXTRA_PREFERRED_PREFIX, selectedPrefix);
        intent.putExtra(
                RequesterRequestBookingActivity.EXTRA_ARRIVAL_AT,
                toIsoDateTime(arrivalDateForRange, "10:00:00")
        );
        intent.putExtra(
                RequesterRequestBookingActivity.EXTRA_DEPARTURE_AT,
                toIsoDateTime(
                        departureDateForRange,
                        arrivalDateForRange.equals(departureDateForRange)
                                ? "18:00:00"
                                : "10:00:00"
                )
        );
        if (room != null && room.getRoomId() > 0) {
            intent.putExtra(
                    RequesterRequestBookingActivity.EXTRA_PREFERRED_ROOM_ID,
                    room.getRoomId()
            );
            intent.putExtra(
                    RequesterRequestBookingActivity.EXTRA_PREFERRED_ROOM_NAME,
                    RoomInventory.displayAvailableRoomLabel(selectedPrefix, room)
            );
        }
        startActivity(intent);
    }

    private void setRequestLoading(Boolean isLoading) {
        boolean loading = Boolean.TRUE.equals(isLoading);
        btnRequestBooking.setEnabled(!loading);
        btnRequestBooking.setAlpha(loading ? 0.55f : 1.0f);
    }

    private void showAvailableRoomsBottomSheet(AvailableRoomsRangeResponse data) {
        String sheetKey = availableRoomsSheetKey(data);
        if (isSameAvailableRoomsSheetShowing(sheetKey)) {
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        activeAvailableRoomsDialog = dialog;
        activeAvailableRoomsSheetKey = sheetKey;
        LinearLayout container = createBottomSheetContainer();

        container.addView(createBottomSheetTitle("Available Rooms"));
        container.addView(createBottomSheetSubtitle(
                "Select a preferred room for your booking request."
                        + "\nArrival: " + safe(data.getArrivalDate())
                        + "\nDeparture: " + safe(data.getDepartureDate())
                        + "\nBuilding: " + safe(data.getPrefix())
        ));

        List<AvailableRoomItem> rooms = RoomInventory.visibleAvailableRooms(
                data.getPrefix(),
                data.getRooms()
        );

        if (rooms.isEmpty()) {
            container.addView(createEmptyMessage(
                    "No rooms are available for the selected range."
            ));
        } else {
            for (AvailableRoomItem room : rooms) {
                TextView roomView = createAvailableRoomView(room);
                roomView.setOnClickListener(v -> {
                    openRequestForm(room);
                    dialog.dismiss();
                });
                container.addView(roomView);
            }
        }

        dialog.setOnDismissListener(d -> handleAvailableRoomsSheetDismissed(dialog));
        showExpandedBottomSheet(dialog, createScrollableBottomSheetContent(container));
    }

    private void handleAvailableRoomsSheetDismissed(BottomSheetDialog dialog) {
        if (dialog != activeAvailableRoomsDialog) {
            return;
        }

        activeAvailableRoomsDialog = null;
        activeAvailableRoomsSheetKey = null;
        clearRangeSelectionIfActive();
    }

    private boolean isSameAvailableRoomsSheetShowing(String sheetKey) {
        return !isBlank(sheetKey)
                && sheetKey.equals(activeAvailableRoomsSheetKey)
                && activeAvailableRoomsDialog != null
                && activeAvailableRoomsDialog.isShowing();
    }

    private String availableRoomsSheetKey(AvailableRoomsRangeResponse data) {
        if (data == null) {
            return "";
        }

        return safe(data.getPrefix())
                + "|"
                + safe(data.getArrivalDate())
                + "|"
                + safe(data.getDepartureDate());
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
        scrollView.setPadding(0, 0, 0, getDimenPx(R.dimen.space_36));
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
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, getDimen(R.dimen.text_20));
        title.setTextColor(getColor(R.color.detail_text_primary));
        title.setTypeface(null, Typeface.BOLD);
        return title;
    }

    private TextView createBottomSheetSubtitle(String text) {
        TextView subtitle = new TextView(this);
        subtitle.setText(text);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, getDimen(R.dimen.text_14));
        subtitle.setTextColor(getColor(R.color.detail_text_secondary));
        subtitle.setPadding(0, getDimenPx(R.dimen.space_8), 0, getDimenPx(R.dimen.space_20));
        return subtitle;
    }

    private TextView createEmptyMessage(String message) {
        TextView empty = new TextView(this);
        empty.setText(message);
        empty.setTextSize(TypedValue.COMPLEX_UNIT_PX, getDimen(R.dimen.text_15));
        empty.setTextColor(getColor(R.color.detail_text_secondary));
        empty.setPadding(0, 0, 0, getDimenPx(R.dimen.space_16));
        return empty;
    }

    private TextView createAvailableRoomView(AvailableRoomItem room) {
        TextView roomView = new TextView(this);
        roomView.setClickable(true);
        roomView.setFocusable(true);
        roomView.setLayoutParams(createRoomViewLayoutParams());
        roomView.setPadding(
                getDimenPx(R.dimen.space_28),
                getDimenPx(R.dimen.space_18),
                getDimenPx(R.dimen.space_28),
                getDimenPx(R.dimen.space_18)
        );
        roomView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getDimen(R.dimen.text_16));
        TypedValue selectableItemBackground = new TypedValue();
        if (getTheme().resolveAttribute(
                android.R.attr.selectableItemBackground,
                selectableItemBackground,
                true
        )) {
            roomView.setForeground(getDrawable(selectableItemBackground.resourceId));
        }
        styleAvailableRoomView(roomView, room);
        return roomView;
    }

    private void styleAvailableRoomView(
            TextView roomView,
            AvailableRoomItem room
    ) {
        StringBuilder text = new StringBuilder(
                RoomInventory.displayAvailableRoomLabel(selectedPrefix, room)
        );
        if (room.isPartiallyAvailable()) {
            text.append("\nAvailable from: ").append(formatAvailableFrom(room));
        } else {
            text.append("\nAvailable");
        }

        roomView.setText(text.toString());
        roomView.setTextColor(getColor(R.color.detail_text_primary));
        roomView.setTypeface(null, Typeface.NORMAL);
        if (room.isPartiallyAvailable()) {
            roomView.setBackgroundColor(getColor(R.color.availability_border));
        } else {
            roomView.setBackgroundResource(R.drawable.bg_detail_card);
        }
    }

    private String formatAvailableFrom(AvailableRoomItem room) {
        String availableFrom = room.getSafeAvailableFrom();
        if (!isBlank(availableFrom)) {
            return DateTimeUtils.formatUtcToLocal(availableFrom);
        }

        String date = room.getSafeAvailableFromDate();
        String time = room.getSafeAvailableFromTime();
        if (isBlank(date) && isBlank(time)) {
            return "";
        }
        if (isBlank(date)) {
            return time;
        }
        if (isBlank(time)) {
            return date;
        }
        return date + ", " + time;
    }

    private LinearLayout.LayoutParams createRoomViewLayoutParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, getDimenPx(R.dimen.space_14));
        return params;
    }

    private int getDimenPx(int dimenRes) {
        return Math.round(getResources().getDimension(dimenRes));
    }

    private float getDimen(int dimenRes) {
        return getResources().getDimension(dimenRes);
    }

    private void updateMonthTitle() {
        tvMonthYear.setText(monthYearFormat.format(selectedMonth.getTime()));
    }

    private void updateSelectedDateRangeBanner() {
        if (tvRange == null) {
            return;
        }

        if (!hasActiveRangeSelection || isBlank(arrivalDateForRange)) {
            tvRange.setText("Long press to select arrival date");
            return;
        }

        if (isWaitingForDepartureDate) {
            tvRange.setText("Arrival: " + arrivalDateForRange + "  |  Select departure date");
        } else {
            tvRange.setText(
                    "Arrival: " + arrivalDateForRange
                            + "  |  Departure: " + departureDateForRange
            );
        }
    }

    private void setStatus(String message) {
        String cleanMessage = safe(message);
        tvStatus.setText(cleanMessage);
        tvStatus.setVisibility(isBlank(cleanMessage) ? View.GONE : View.VISIBLE);
    }

    private List<CalendarDayItem> buildEmptyCalendar() {
        List<CalendarDayItem> calendarItems = new ArrayList<>();
        addEmptyCellsBeforeMonth(calendarItems);

        int maxDays = selectedMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
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

        Map<String, RoomAvailabilityDay> availabilityMap = new HashMap<>();
        if (group != null && group.hasCalendar()) {
            for (RoomAvailabilityDay day : group.getCalendar()) {
                if (day != null && !isBlank(day.getDate())) {
                    availabilityMap.put(day.getDate(), day);
                }
            }
        }

        int maxDays = selectedMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int day = 1; day <= maxDays; day++) {
            String date = buildDateString(day);
            RoomAvailabilityDay availabilityDay = availabilityMap.get(date);
            calendarItems.add(new CalendarDayItem(
                    day,
                    date,
                    availabilityDay != null
                            ? getAvailabilityType(availabilityDay)
                            : CalendarDayItem.TYPE_AVAILABLE,
                    availabilityDay != null
                            ? RoomInventory.displayAvailableRooms(selectedPrefix, availabilityDay)
                            : 0
            ));
        }
        return calendarItems;
    }

    private void addEmptyCellsBeforeMonth(List<CalendarDayItem> calendarItems) {
        Calendar temp = (Calendar) selectedMonth.clone();
        temp.set(Calendar.DAY_OF_MONTH, 1);
        int emptyCellsBeforeMonth = temp.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
        for (int i = 0; i < emptyCellsBeforeMonth; i++) {
            calendarItems.add(new CalendarDayItem(
                    0,
                    "",
                    CalendarDayItem.TYPE_EMPTY,
                    0
            ));
        }
    }

    private int getAvailabilityType(RoomAvailabilityDay day) {
        int totalRooms = RoomInventory.displayTotalRooms(selectedPrefix, day.getTotalRooms());
        int availableRooms = RoomInventory.displayAvailableRooms(selectedPrefix, day);
        if (totalRooms <= 0) {
            return CalendarDayItem.TYPE_AVAILABLE;
        }
        if (availableRooms <= 0) {
            return CalendarDayItem.TYPE_NOT_AVAILABLE;
        }
        if (availableRooms == totalRooms) {
            return CalendarDayItem.TYPE_AVAILABLE;
        }
        double availablePercentage = (availableRooms * 100.0) / totalRooms;
        if (availablePercentage < LESS_THAN_HALF_PERCENTAGE) {
            return CalendarDayItem.TYPE_LESS_THAN_HALF_AVAILABLE;
        }
        return CalendarDayItem.TYPE_HALF_AVAILABLE;
    }

    private String buildDateString(int day) {
        return String.format(
                Locale.getDefault(),
                "%04d-%02d-%02d",
                selectedMonth.get(Calendar.YEAR),
                selectedMonth.get(Calendar.MONTH) + 1,
                day
        );
    }

    private String toIsoDateTime(String date, String time) {
        return date + "T" + time + "+05:30";
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
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_SELECTED_MONTH, selectedMonth.getTimeInMillis());
        outState.putString(STATE_SELECTED_PREFIX, selectedPrefix);
        outState.putBoolean(STATE_WAITING_FOR_DEPARTURE, isWaitingForDepartureDate);
        outState.putBoolean(STATE_HAS_ACTIVE_RANGE, hasActiveRangeSelection);
        outState.putString(STATE_ARRIVAL_DATE, arrivalDateForRange);
        outState.putString(STATE_DEPARTURE_DATE, departureDateForRange);
    }

    private static String safe(String value) {
        return value != null ? value.trim() : "";
    }

    private static boolean isBlank(String value) {
        return TextUtils.isEmpty(safe(value));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!AuthSessionGuard.ensureRequester(this)) {
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
