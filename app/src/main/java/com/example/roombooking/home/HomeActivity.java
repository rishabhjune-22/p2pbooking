package com.example.roombooking.home;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.roombooking.R;
import com.example.roombooking.auth.AuthActivity;
import com.example.roombooking.auth.AuthRepository;
import com.example.roombooking.auth.SessionManager;
import com.example.roombooking.booking.BookingAdapter;
import com.example.roombooking.booking.BookingDetailActivity;
import com.example.roombooking.booking.BookingRepository;
import com.example.roombooking.model.booking.BookingItem;

public class HomeActivity extends AppCompatActivity {

    private static final String EXTRA_BOOKING_DATA = "booking_data";
    private static final String EXTRA_UPDATED_BOOKING_ID = "updated_booking_id";
    private static final String EXTRA_UPDATED_STATUS = "updated_status";
    private static final String EXTRA_VISITOR_NAME = "visitor_name";
    private static final String EXTRA_VISITOR_MOBILE = "visitor_mobile";
    private static final String EXTRA_PURPOSE_OF_VISIT = "purpose_of_visit";
    private static final String EXTRA_ARRIVAL_DATE = "arrival_date";
    private static final String EXTRA_ARRIVAL_TIME = "arrival_time";
    private static final String EXTRA_DEPARTURE_DATE = "departure_date";
    private static final String EXTRA_DEPARTURE_TIME = "departure_time";
    private static final String EXTRA_BOOKING_CREATED = "booking_created";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvMessage;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Button btnCreateBooking;
    private Button btnLogout;

    private BookingAdapter bookingAdapter;
    private LinearLayoutManager layoutManager;

    private HomeViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initViews();
        initViewModel();
        setupRecyclerView();
        setupListeners();
        observeViewModel();

        viewModel.loadInitialBookings();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewBookings);
        progressBar = findViewById(R.id.progressBar);
        tvMessage = findViewById(R.id.tvMessage);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        btnCreateBooking = findViewById(R.id.btnCreateBooking);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void initViewModel() {
        BookingRepository bookingRepository = new BookingRepository(getApplicationContext());
        AuthRepository authRepository = new AuthRepository(getApplicationContext());
        SessionManager sessionManager = new SessionManager(getApplicationContext());

        HomeViewModelFactory factory = new HomeViewModelFactory(
                bookingRepository,
                authRepository,
                sessionManager
        );

        viewModel = new ViewModelProvider(this, factory).get(HomeViewModel.class);
    }

    private void setupRecyclerView() {
        bookingAdapter = new BookingAdapter(this, new BookingAdapter.OnBookingClickListener() {
            @Override
            public void onBookingClick(BookingItem bookingItem) {
                Intent intent = new Intent(HomeActivity.this, BookingDetailActivity.class);
                intent.putExtra(EXTRA_BOOKING_DATA, (Parcelable) bookingItem);
                bookingDetailLauncher.launch(intent);
            }

            @Override
            public void onBookingLongClick(BookingItem bookingItem, int position) {
                showCancelBookingDialog(bookingItem);
            }
        });

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(bookingAdapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy <= 0) return;
                if (viewModel.isLoading() || viewModel.isLastPage()) return;

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                boolean shouldLoadMore =
                        (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2
                                && firstVisibleItemPosition >= 0;

                if (shouldLoadMore) {
                    viewModel.loadNextPage();
                }
            }
        });
    }

    private void setupListeners() {
        swipeRefreshLayout.setOnRefreshListener(() -> viewModel.refreshBookings());

        btnLogout.setOnClickListener(v -> viewModel.performLogout());


    }

    private void observeViewModel() {
        viewModel.getBookingsLiveData().observe(this, bookingItems -> {
            bookingAdapter.setItems(bookingItems);
        });

        viewModel.getFullScreenLoadingLiveData().observe(this, isLoading -> {
            progressBar.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE);
        });

        viewModel.getPaginationLoadingLiveData().observe(this, isLoading -> {
            if (Boolean.TRUE.equals(isLoading)) {
                bookingAdapter.showPaginationLoader();
            } else {
                bookingAdapter.hidePaginationLoader();
            }
        });

        viewModel.getSwipeRefreshingLiveData().observe(this, isRefreshing -> {
            swipeRefreshLayout.setRefreshing(Boolean.TRUE.equals(isRefreshing));
        });

        viewModel.getMessageLiveData().observe(this, message -> {
            if (message == null || message.trim().isEmpty()) {
                tvMessage.setVisibility(View.GONE);
            } else {
                tvMessage.setVisibility(View.VISIBLE);
                tvMessage.setText(message);
            }
        });

        viewModel.getToastLiveData().observe(this, message -> {
            if (message != null && !message.trim().isEmpty()) {
                android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getLogoutEventLiveData().observe(this, shouldLogout -> {
            if (Boolean.TRUE.equals(shouldLogout)) {
                goToLogin();
            }
        });
    }

    private void showCancelBookingDialog(BookingItem bookingItem) {
        if ("cancelled".equalsIgnoreCase(bookingItem.getStatus())) {
            android.widget.Toast.makeText(this, "Booking is already cancelled", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        EditText input = new EditText(this);
        input.setHint("Enter cancellation reason (optional)");
        int margin = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(margin, margin, margin, margin);

        new AlertDialog.Builder(this)
                .setTitle("Cancel Booking")
                .setMessage("Do you want to cancel booking for " + bookingItem.getVisitorName() + "?")
                .setView(input)
                .setPositiveButton("Cancel Booking", (dialog, which) -> {
                    String reason = input.getText().toString().trim();
                    viewModel.cancelBooking(bookingItem, reason);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void goToLogin() {
        Intent intent = new Intent(HomeActivity.this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private final ActivityResultLauncher<Intent> bookingDetailLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();

                    int bookingId = data.getIntExtra(EXTRA_UPDATED_BOOKING_ID, -1);
                    String updatedStatus = data.getStringExtra(EXTRA_UPDATED_STATUS);
                    String visitorName = data.getStringExtra(EXTRA_VISITOR_NAME);
                    String visitorMobile = data.getStringExtra(EXTRA_VISITOR_MOBILE);
                    String purpose = data.getStringExtra(EXTRA_PURPOSE_OF_VISIT);
                    String arrivalDate = data.getStringExtra(EXTRA_ARRIVAL_DATE);
                    String arrivalTime = data.getStringExtra(EXTRA_ARRIVAL_TIME);
                    String departureDate = data.getStringExtra(EXTRA_DEPARTURE_DATE);
                    String departureTime = data.getStringExtra(EXTRA_DEPARTURE_TIME);

                    if (bookingId != -1) {
                        viewModel.updateBookingById(
                                bookingId,
                                updatedStatus,
                                visitorName,
                                visitorMobile,
                                purpose,
                                arrivalDate,
                                arrivalTime,
                                departureDate,
                                departureTime
                        );
                    }
                }
            });

    private final ActivityResultLauncher<Intent> createBookingLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    boolean bookingCreated = result.getData().getBooleanExtra(EXTRA_BOOKING_CREATED, false);
                    if (bookingCreated) {
                        viewModel.refreshBookings();
                    }
                }
            });
}