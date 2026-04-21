package com.example.roombooking.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import com.example.roombooking.booking.CreateBookingActivity;
import com.example.roombooking.model.booking.BookingItem;
import com.example.roombooking.security.CryptoManager;
import com.example.roombooking.security.EncryptedBookingPayload;
import com.example.roombooking.security.KeystoreBackedCryptoSessionManager;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

public class HomeActivity extends AppCompatActivity {

    private static final String EXTRA_UPDATED_BOOKING_ID = "updated_booking_id";
    private static final String EXTRA_UPDATED_STATUS = "updated_status";
    private static final String EXTRA_ARRIVAL_AT = "arrival_at";
    private static final String EXTRA_DEPARTURE_AT = "departure_at";
    private static final String EXTRA_BOOKING_CREATED = "booking_created";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvMessage;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ImageButton btnCreateBooking;
    private ImageButton btnFilter;
    private ImageButton btnToggleCancelled;

    private BookingAdapter bookingAdapter;
    private LinearLayoutManager layoutManager;

    private HomeViewModel viewModel;
    private final Gson gson = new Gson();

    private List<BookingItem> allBookings = new ArrayList<>();
    private boolean showCancelledOnly = false;

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
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        recyclerView = findViewById(R.id.recyclerViewBookings);
        progressBar = findViewById(R.id.progressBar);
        tvMessage = findViewById(R.id.tvMessage);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        btnCreateBooking = findViewById(R.id.btnCreateBooking);
        btnFilter = findViewById(R.id.btnFilter);
        btnToggleCancelled = findViewById(R.id.btnToggleCancelled);
    }

    private void initViewModel() {
        BookingRepository bookingRepository = new BookingRepository(getApplicationContext());
        AuthRepository authRepository = new AuthRepository(getApplicationContext());
        SessionManager sessionManager = new SessionManager(getApplicationContext());

        HomeViewModelFactory factory = new HomeViewModelFactory(
                getApplicationContext(),
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
                intent.putExtra(BookingDetailActivity.EXTRA_BOOKING_DATA, bookingItem);
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

                if (dy <= 0) {
                    return;
                }
                if (viewModel.isLoading() || viewModel.isLastPage()) {
                    return;
                }

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

        btnCreateBooking.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, CreateBookingActivity.class);
            createBookingLauncher.launch(intent);
        });

        btnFilter.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, FilterActivity.class);
            startActivity(intent);
        });

        btnToggleCancelled.setOnClickListener(v -> {
            showCancelledOnly = !showCancelledOnly;
            updateToggleIcon();
            applyFilter(allBookings);
        });
    }

    private void updateToggleIcon() {
        if (showCancelledOnly) {
            btnToggleCancelled.setImageResource(R.drawable.switch_on_svgrepo_com);
        } else {
            btnToggleCancelled.setImageResource(R.drawable.switch_off_svgrepo_com);
        }
    }

    private void observeViewModel() {
        viewModel.getBookingsLiveData().observe(this, bookingItems -> {
            allBookings = bookingItems;
            applyFilter(allBookings);
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

    private void applyFilter(List<BookingItem> list) {
        if (list == null) return;
        
        List<BookingItem> filteredList = new ArrayList<>();
        
        for (BookingItem item : list) {
            boolean isCancelled = "cancelled".equalsIgnoreCase(item.getStatus());
            if (showCancelledOnly) {
                // Show only cancelled
                if (isCancelled) {
                    filteredList.add(item);
                }
            } else {
                // Show only active
                if (!isCancelled) {
                    filteredList.add(item);
                }
            }
        }
        
        bookingAdapter.setItems(filteredList);
        
        if (filteredList.isEmpty()) {
            tvMessage.setVisibility(View.VISIBLE);
            tvMessage.setText(showCancelledOnly ? "No cancelled bookings found." : "No active bookings.");
        } else {
            tvMessage.setVisibility(View.GONE);
        }
    }

    private void showCancelBookingDialog(BookingItem bookingItem) {
        if ("cancelled".equalsIgnoreCase(bookingItem.getStatus())) {
            android.widget.Toast.makeText(this, "Booking is already cancelled", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        String displayName = getVisitorNameForDisplay(bookingItem);

        EditText input = new EditText(this);
        input.setHint("Enter cancellation reason (optional)");
        int margin = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(margin, margin, margin, margin);

        new AlertDialog.Builder(this)
                .setTitle("Cancel Booking")
                .setMessage("Do you want to cancel booking for " + displayName + "?")
                .setView(input)
                .setPositiveButton("Cancel Booking", (dialog, which) -> {
                    String reason = input.getText().toString().trim();
                    viewModel.cancelBooking(bookingItem, reason);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private String getVisitorNameForDisplay(BookingItem bookingItem) {
        if (bookingItem == null) {
            return "this booking";
        }

        if (!bookingItem.canDecrypt() || !bookingItem.hasEncryptedPayload()) {
            return "this booking";
        }

        try {
            KeystoreBackedCryptoSessionManager sessionManager =
                    KeystoreBackedCryptoSessionManager.getInstance(getApplicationContext());

            SecretKey dek = sessionManager.getDek();
            if (dek == null) {
                return "this booking";
            }

            CryptoManager cryptoManager = new CryptoManager();
            String decryptedJson = cryptoManager.decryptPayload(
                    bookingItem.getEncryptedPayload(),
                    bookingItem.getPayloadNonce(),
                    dek
            );

            EncryptedBookingPayload payload =
                    EncryptedBookingPayload.fromJson(decryptedJson, gson);

            if (payload != null) {
                String name = payload.getVisitorName();
                if (name != null && !name.trim().isEmpty()) {
                    return name;
                }
            }
        } catch (Exception ignored) {
        }

        return "this booking";
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
                    String arrivalAt = data.getStringExtra(EXTRA_ARRIVAL_AT);
                    String departureAt = data.getStringExtra(EXTRA_DEPARTURE_AT);

                    if (bookingId != -1) {
                        viewModel.updateBookingById(
                                bookingId,
                                updatedStatus,
                                null,
                                null,
                                null,
                                arrivalAt,
                                departureAt
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