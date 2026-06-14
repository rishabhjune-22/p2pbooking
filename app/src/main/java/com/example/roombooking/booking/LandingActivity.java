package com.example.roombooking.booking;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.util.TypedValue;

import com.example.roombooking.R;
import com.example.roombooking.api.RetrofitClient;
import com.example.roombooking.home.HomeActivity;
import com.example.roombooking.model.booking.RoomAvailabilityBookingItem;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.utils.DateTimeUtils;
import com.example.roombooking.utils.EdgeToEdgeUtils;
import com.example.roombooking.utils.InternetErrorBanner;
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
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LandingActivity extends AppCompatActivity {

    private static final int CALENDAR_SPAN_COUNT = 7;
    private static final String DEFAULT_ROOM_PREFIX = "Beta";
    private static final double LESS_THAN_HALF_PERCENTAGE = 50.0;
    private static final String STATE_SELECTED_MONTH = "selected_month";
    private static final String STATE_SELECTED_PREFIX = "selected_prefix";
    private static final String STATE_WAITING_FOR_DEPARTURE = "waiting_for_departure";
    private static final String STATE_HAS_ACTIVE_RANGE = "has_active_range";
    private static final String STATE_ARRIVAL_DATE = "arrival_date";
    private static final String STATE_DEPARTURE_DATE = "departure_date";

    private MaterialButton btnPreviousMonth;
    private MaterialButton btnNextMonth;
    private ImageButton btnCheckAvailability;

    private TextView tvMonthYear;
    private TextView bannerSelectedDateRange;
    private TextView tvTotalRoomCapacity;

    private MaterialToolbar materialToolbar;
    private TabLayout tabLayoutRooms;
    private SwipeRefreshLayout swipeRefreshAvailability;
    private RecyclerView rvAvailabilityGroups;

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

    private Call<ApiResponse<RoomAvailabilityResponse>> availabilityCall;
    private Call<ApiResponse<RoomAvailabilityDetailsResponse>> availabilityDetailsCall;
    private Call<ApiResponse<AvailableRoomsResponse>> availableRoomsCall;
    private Call<ApiResponse<AvailableRoomsRangeResponse>> availableRoomsRangeCall;

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

        EdgeToEdgeUtils.applySystemBarInsets(this, findViewById(R.id.main));

        initViews();
        setupRecyclerView();
        restoreState(savedInstanceState);
        selectRestoredRoomTab();
        setupListeners();
        setupCalendarGestureSelection();

        loadAvailability();
    }

    private void initViews() {
        btnPreviousMonth = findViewById(R.id.btnPreviousMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        btnCheckAvailability = findViewById(R.id.btnCheckAvailability);
        bannerSelectedDateRange = findViewById(R.id.bannerSelectedDateRange);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        tvTotalRoomCapacity = findViewById(R.id.tvTotalRoomCapacity);

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

    private void setupCheckAvailabilityButton() {
        btnCheckAvailability.setOnClickListener(v -> {
            if (arrivalDateForRange == null || departureDateForRange == null) {
                showToast("Please select arrival and departure date first");
                return;
            }

            fetchAvailableRoomsForDateRange(arrivalDateForRange, departureDateForRange);
        });
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

        swipeRefreshAvailability.setOnRefreshListener(this::loadAvailability);
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
                openBookingsScreen();
                return true;
            }

            if (itemId == R.id.menuAvailability) {
                showToast("Availability clicked");
                return true;
            }

            if (itemId == R.id.menuAboutUs) {
                showToast("AboutUs clicked");
                return true;
            }

            return false;
        });

        popupMenu.show();
    }

    private void openBookingsScreen() {
        Intent intent = new Intent(LandingActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
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
        swipeRefreshAvailability.setRefreshing(true);

        cancelCall(availabilityCall);
        Call<ApiResponse<RoomAvailabilityResponse>> request = RetrofitClient.getApiService(this)
                .getRoomAvailability(month, year);
        availabilityCall = request;
        request.enqueue(new Callback<ApiResponse<RoomAvailabilityResponse>>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<ApiResponse<RoomAvailabilityResponse>> call,
                            @NonNull Response<ApiResponse<RoomAvailabilityResponse>> response
                    ) {
                        if (!isCurrentAvailabilityRequest(call, month, year)) {
                            return;
                        }

                        availabilityCall = null;
                        swipeRefreshAvailability.setRefreshing(false);

                        if (isValidAvailabilityResponse(response)) {
                            InternetErrorBanner.hide(LandingActivity.this);

                            RoomAvailabilityResponse data = response.body().getData();

                            allGroups.clear();

                            if (data.hasGroups()) {
                                allGroups.addAll(data.getGroups());
                            }

                            applyPrefixFilter();
                            return;
                        }

                        showToast("Failed to load room availability");
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<ApiResponse<RoomAvailabilityResponse>> call,
                            @NonNull Throwable t
                    ) {
                        if (!isCurrentAvailabilityRequest(call, month, year)) {
                            return;
                        }

                        availabilityCall = null;
                        swipeRefreshAvailability.setRefreshing(false);
                        if (!call.isCanceled()) {
                            InternetErrorBanner.show(LandingActivity.this);
                        }
                    }
                });
    }

    private boolean isCurrentAvailabilityRequest(
            Call<ApiResponse<RoomAvailabilityResponse>> call,
            int requestedMonth,
            int requestedYear
    ) {
        return canUpdateUi()
                && call == availabilityCall
                && requestedMonth == selectedMonth.get(Calendar.MONTH) + 1
                && requestedYear == selectedMonth.get(Calendar.YEAR);
    }

    private boolean isCurrentCall(
            Call<?> callbackCall,
            Call<?> trackedCall,
            String requestedPrefix
    ) {
        return canUpdateUi()
                && callbackCall == trackedCall
                && requestedPrefix.equals(selectedPrefix);
    }

    private boolean canUpdateUi() {
        return !isFinishing() && !isDestroyed();
    }

    private void cancelCall(Call<?> call) {
        if (call != null && !call.isCanceled()) {
            call.cancel();
        }
    }

    private boolean isValidAvailabilityResponse(
            Response<ApiResponse<RoomAvailabilityResponse>> response
    ) {
        return response.isSuccessful()
                && response.body() != null
                && response.body().isSuccess()
                && response.body().getData() != null;
    }

    private void applyPrefixFilter() {
        RoomAvailabilityGroup selectedGroup = findSelectedRoomGroup();

        if (selectedGroup == null) {
            showEmptyCalendarForSelectedPrefix();
            return;
        }

        tvTotalRoomCapacity.setText(String.valueOf(selectedGroup.getTotalRooms()));
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
                availableRooms = availabilityDay.getAvailableRooms();
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
        int totalRooms = day.getTotalRooms();
        int availableRooms = day.getAvailableRooms();

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
        String requestedPrefix = selectedPrefix;
        cancelCall(availabilityDetailsCall);
        Call<ApiResponse<RoomAvailabilityDetailsResponse>> request =
                RetrofitClient.getApiService(this)
                        .getRoomAvailabilityDetails(date, requestedPrefix);
        availabilityDetailsCall = request;
        request.enqueue(new Callback<ApiResponse<RoomAvailabilityDetailsResponse>>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<ApiResponse<RoomAvailabilityDetailsResponse>> call,
                            @NonNull Response<ApiResponse<RoomAvailabilityDetailsResponse>> response
                    ) {
                        if (!isCurrentCall(call, availabilityDetailsCall, requestedPrefix)) {
                            return;
                        }

                        availabilityDetailsCall = null;
                        InternetErrorBanner.hide(LandingActivity.this);
                        if (isValidAvailabilityDetailsResponse(response)) {
                            showAvailabilityDetailsDialog(response.body().getData());
                            return;
                        }

                        showToast("Failed to load booking details");
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<ApiResponse<RoomAvailabilityDetailsResponse>> call,
                            @NonNull Throwable t
                    ) {
                        if (!isCurrentCall(call, availabilityDetailsCall, requestedPrefix)) {
                            return;
                        }

                        availabilityDetailsCall = null;
                        if (!call.isCanceled()) {
                            InternetErrorBanner.show(LandingActivity.this);
                        }
                    }
                });
    }

    private boolean isValidAvailabilityDetailsResponse(
            Response<ApiResponse<RoomAvailabilityDetailsResponse>> response
    ) {
        return response.isSuccessful()
                && response.body() != null
                && response.body().isSuccess()
                && response.body().getData() != null;
    }

    private void fetchAvailableRoomsForDate(String date) {
        String requestedPrefix = selectedPrefix;
        cancelCall(availableRoomsCall);
        Call<ApiResponse<AvailableRoomsResponse>> request = RetrofitClient.getApiService(this)
                .getAvailableRoomsByDate(date, requestedPrefix);
        availableRoomsCall = request;
        request.enqueue(new Callback<ApiResponse<AvailableRoomsResponse>>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<ApiResponse<AvailableRoomsResponse>> call,
                            @NonNull Response<ApiResponse<AvailableRoomsResponse>> response
                    ) {
                        if (!isCurrentCall(call, availableRoomsCall, requestedPrefix)) {
                            return;
                        }

                        availableRoomsCall = null;
                        InternetErrorBanner.hide(LandingActivity.this);
                        if (isValidAvailableRoomsResponse(response)) {
                            showAvailableRoomsBottomSheet(response.body().getData());
                            return;
                        }

                        showToast("Failed to load available rooms");
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<ApiResponse<AvailableRoomsResponse>> call,
                            @NonNull Throwable t
                    ) {
                        if (!isCurrentCall(call, availableRoomsCall, requestedPrefix)) {
                            return;
                        }

                        availableRoomsCall = null;
                        if (!call.isCanceled()) {
                            InternetErrorBanner.show(LandingActivity.this);
                        }
                    }
                });
    }

    private boolean isValidAvailableRoomsResponse(
            Response<ApiResponse<AvailableRoomsResponse>> response
    ) {
        return response.isSuccessful()
                && response.body() != null
                && response.body().isSuccess()
                && response.body().getData() != null;
    }

    private void fetchAvailableRoomsForDateRange(String arrivalDate, String departureDate) {
        String requestedPrefix = selectedPrefix;
        cancelCall(availableRoomsRangeCall);
        Call<ApiResponse<AvailableRoomsRangeResponse>> request =
                RetrofitClient.getApiService(this)
                        .getAvailableRoomsByDateRange(
                                arrivalDate,
                                departureDate,
                                requestedPrefix
                        );
        availableRoomsRangeCall = request;
        request.enqueue(new Callback<ApiResponse<AvailableRoomsRangeResponse>>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<ApiResponse<AvailableRoomsRangeResponse>> call,
                            @NonNull Response<ApiResponse<AvailableRoomsRangeResponse>> response
                    ) {
                        if (!isCurrentCall(call, availableRoomsRangeCall, requestedPrefix)) {
                            return;
                        }

                        availableRoomsRangeCall = null;
                        InternetErrorBanner.hide(LandingActivity.this);
                        if (isValidAvailableRoomsRangeResponse(response)) {
                            showAvailableRoomsRangeBottomSheet(response.body().getData());
                            return;
                        }

                        showToast("Failed to load available rooms");
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<ApiResponse<AvailableRoomsRangeResponse>> call,
                            @NonNull Throwable t
                    ) {
                        if (!isCurrentCall(call, availableRoomsRangeCall, requestedPrefix)) {
                            return;
                        }

                        availableRoomsRangeCall = null;
                        if (!call.isCanceled()) {
                            InternetErrorBanner.show(LandingActivity.this);
                        }
                    }
                });
    }

    private boolean isValidAvailableRoomsRangeResponse(
            Response<ApiResponse<AvailableRoomsRangeResponse>> response
    ) {
        return response.isSuccessful()
                && response.body() != null
                && response.body().isSuccess()
                && response.body().getData() != null;
    }

    private void showAvailableRoomsBottomSheet(AvailableRoomsResponse data) {
        selectedAvailableArrivalDate = data.getDate();
        selectedAvailableDepartureDate = data.getDate();

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        activeBottomSheetDialog = dialog;

        LinearLayout container = createBottomSheetContainer();

        TextView title = createBottomSheetTitle("Available Rooms");

        TextView subtitle = createBottomSheetSubtitle(
                "Date: " + safe(data.getDate())
                        + "\nBuilding: " + safe(data.getPrefix())
                        + "\nTotal Available: " + data.getTotalAvailableRooms()
        );

        container.addView(title);
        container.addView(subtitle);

        addAvailableRoomViews(
                container,
                data.getRooms(),
                "No room available on this date."
        );

        View scrollableContent = createScrollableBottomSheetContent(container);

        dialog.setOnDismissListener(d -> {
            activeBottomSheetDialog = null;
            clearRangeSelectionAndEnableRefresh();
        });

        showExpandedBottomSheet(dialog, scrollableContent);
    }

    private void showAvailableRoomsRangeBottomSheet(AvailableRoomsRangeResponse data) {
        selectedAvailableArrivalDate = data.getArrivalDate();
        selectedAvailableDepartureDate = data.getDepartureDate();

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        activeBottomSheetDialog = dialog;

        LinearLayout container = createBottomSheetContainer();

        TextView title = createBottomSheetTitle("Available Rooms");

        TextView subtitle = createBottomSheetSubtitle(
                "Arrival: " + safe(data.getArrivalDate())
                        + "\nDeparture: " + safe(data.getDepartureDate())
                        + "\nBuilding: " + safe(data.getPrefix())
                        + "\nTotal Available: " + data.getTotalAvailableRooms()
        );

        container.addView(title);
        container.addView(subtitle);

        addAvailableRoomViews(
                container,
                data.getRooms(),
                "No room available for selected date range."
        );

        View scrollableContent = createScrollableBottomSheetContent(container);

        dialog.setOnDismissListener(d -> {
            activeBottomSheetDialog = null;
            clearRangeSelectionAndEnableRefresh();
        });

        showExpandedBottomSheet(dialog, scrollableContent);
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

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private ScrollView createScrollableBottomSheet(LinearLayout container) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.addView(container, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));
        return scrollView;
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

    private void addAvailableRoomViews(
            LinearLayout container,
            List<AvailableRoomItem> rooms,
            String emptyMessage
    ) {
        if (rooms == null || rooms.isEmpty()) {
            addEmptyMessage(container, emptyMessage);
            return;
        }

        for (AvailableRoomItem room : rooms) {
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
                    room.getSafeRoomName()
                            + "\nAvailable from: "
                            + room.getSafeAvailableFromDate()
                            + ", "
                            + room.getSafeAvailableFromTime()
            );

            roomView.setBackgroundColor(getColor(R.color.availability_border));
        } else {
            roomView.setText(
                    room.getSafeRoomName()
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
        intent.putExtra(CreateBookingActivity.EXTRA_ROOM_NAME, room.getSafeRoomName());

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

        for (RoomAvailabilityBookingItem item : bookings) {
            TextView card = createBookingDetailsCard(item);
            layoutDetails.addView(card);
        }
    }

    private TextView createBookingDetailsCard(RoomAvailabilityBookingItem item) {
        TextView card = new TextView(this);

        card.setText(
                "Person Name: " + item.getSafeGuestName() + "\n"
                        + "Room: " + item.getSafeRoomName() + "\n"
                        + "Requestee: " + item.getSafeRequesteeName() + "\n"
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

        return card;
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
    protected void onDestroy() {
        cancelCall(availabilityCall);
        cancelCall(availabilityDetailsCall);
        cancelCall(availableRoomsCall);
        cancelCall(availableRoomsRangeCall);
        super.onDestroy();
    }
}
