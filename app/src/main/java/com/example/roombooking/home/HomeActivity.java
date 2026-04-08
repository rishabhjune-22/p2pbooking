package com.example.roombooking.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roombooking.booking.BookingAdapter;
import com.example.roombooking.booking.BookingCancelRequest;
import com.example.roombooking.booking.BookingCancelResponse;
import com.example.roombooking.booking.BookingDetailActivity;
import com.example.roombooking.booking.BookingItem;
import com.example.roombooking.booking.BookingListResponse;
import com.example.roombooking.booking.CreateBookingActivity;
import com.example.roombooking.R;
import com.example.roombooking.api.RetrofitClient;
import com.example.roombooking.auth.AuthActivity;
import com.example.roombooking.auth.LogoutRequest;
import com.example.roombooking.auth.SessionManager;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvMessage;

    private BookingAdapter bookingAdapter;
    private LinearLayoutManager layoutManager;
    private Button btnCreateBooking;
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private boolean isFirstLoad = true;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Button btnLogout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        recyclerView = findViewById(R.id.recyclerViewBookings);
        progressBar = findViewById(R.id.progressBar);
        tvMessage = findViewById(R.id.tvMessage);

        bookingAdapter = new BookingAdapter(this, new BookingAdapter.OnBookingClickListener() {
            @Override
            public void onBookingClick(BookingItem bookingItem) {
                Intent intent = new Intent(HomeActivity.this, BookingDetailActivity.class);
                intent.putExtra("booking_data", bookingItem);
                bookingDetailLauncher.launch(intent);
            }

            @Override
            public void onBookingLongClick(BookingItem bookingItem, int position) {
                showCancelBookingDialog(bookingItem, position);
            }
        });
        layoutManager = new LinearLayoutManager(this);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(bookingAdapter);
        btnLogout = findViewById(R.id.btnLogout);
        swipeRefreshLayout.setOnRefreshListener(this::refreshBookings);
        btnLogout.setOnClickListener(v -> performLogout());
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy <= 0) return;
                if (isLoading || isLastPage) return;

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                boolean shouldLoadMore =
                        (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2
                                && firstVisibleItemPosition >= 0;

                if (shouldLoadMore) {
                    loadBookings(currentPage + 1);
                }
            }
        });

        loadBookings(1);



        btnCreateBooking = findViewById(R.id.btnCreateBooking);

        btnCreateBooking.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, CreateBookingActivity.class);
            createBookingLauncher.launch(intent);
        });
    }
    private void refreshBookings() {
        currentPage = 1;
        isLastPage = false;
        isLoading = false;
        isFirstLoad = false;

        tvMessage.setVisibility(View.GONE);
        bookingAdapter.clearItems();

        loadBookings(1);
    }
    private void loadBookings(int page) {
        isLoading = true;

        if (page == 1) {
            if (isFirstLoad) {
                progressBar.setVisibility(View.VISIBLE);
                tvMessage.setVisibility(View.GONE);
            }
        } else {
            bookingAdapter.showPaginationLoader();
        }

        RetrofitClient.getApiService(this).getBookings(page).enqueue(new Callback<BookingListResponse>() {
            @Override
            public void onResponse(Call<BookingListResponse> call, Response<BookingListResponse> response) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                bookingAdapter.hidePaginationLoader();
                isFirstLoad = false;

                if (response.isSuccessful() && response.body() != null) {
                    BookingListResponse body = response.body();
                    java.util.List<BookingItem> results = body.getResults();

                    if (page == 1) {
                        if (results == null || results.isEmpty()) {
                            bookingAdapter.clearItems();
                            tvMessage.setVisibility(View.VISIBLE);
                            tvMessage.setText("No bookings found.");
                            return;
                        }
                        bookingAdapter.setItems(results);
                    } else {
                        bookingAdapter.addItems(results);
                    }

                    currentPage = page;
                    isLastPage = body.getNext() == null;
                    tvMessage.setVisibility(View.GONE);

                } else if (response.code() == 401) {
                    SessionManager sessionManager = new SessionManager(HomeActivity.this);
                    sessionManager.logout();

                    tvMessage.setVisibility(View.VISIBLE);
                    tvMessage.setText("Session expired. Please login again.");

                    startActivity(new Intent(HomeActivity.this, AuthActivity.class));
                    finish();

                } else {
                    if (page == 1) {
                        tvMessage.setVisibility(View.VISIBLE);
                        tvMessage.setText("Failed to load bookings. Code: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<BookingListResponse> call, Throwable t) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                bookingAdapter.hidePaginationLoader();
                isFirstLoad = false;

                if (page == 1) {
                    tvMessage.setVisibility(View.VISIBLE);
                    tvMessage.setText("Network error: " + t.getMessage());
                }
            }
        });
    }

    private void performLogout() {

        SessionManager sessionManager = new SessionManager(this);
        String refreshToken = sessionManager.getRefreshToken();

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            sessionManager.logout();
            goToLogin();
            return;
        }

        LogoutRequest request = new LogoutRequest(refreshToken);

        RetrofitClient.getApiService(this).logout(request)
                .enqueue(new retrofit2.Callback<Void>() {
                    @Override
                    public void onResponse(retrofit2.Call<Void> call,
                                           retrofit2.Response<Void> response) {

                        // Whether success or failure → clear session anyway
                        sessionManager.logout();
                        goToLogin();
                    }

                    @Override
                    public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                        // Network fail → still logout locally
                        sessionManager.logout();
                        goToLogin();
                    }
                });
    }

    private void goToLogin() {
        Intent intent = new Intent(HomeActivity.this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showCancelBookingDialog(BookingItem bookingItem, int position) {
        if ("cancelled".equalsIgnoreCase(bookingItem.getStatus())) {
            Toast.makeText(this, "Booking is already cancelled", Toast.LENGTH_SHORT).show();
            return;
        }

        EditText input = new EditText(this);
        input.setHint("Enter cancellation reason (optional)");
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle("Cancel Booking")
                .setMessage("Do you want to cancel booking for " + bookingItem.getVisitor_name() + "?")
                .setView(input)
                .setPositiveButton("Cancel Booking", (dialog, which) -> {
                    String reason = input.getText().toString().trim();
                    cancelBooking(bookingItem, position, reason);
                })
                .setNegativeButton("Close", null)
                .show();
    }private void cancelBooking(BookingItem bookingItem, int position, String reason) {
        BookingCancelRequest request = new BookingCancelRequest(reason);

        RetrofitClient.getApiService(this)
                .cancelBooking(bookingItem.getId(), request)
                .enqueue(new Callback<BookingCancelResponse>() {
                    @Override
                    public void onResponse(Call<BookingCancelResponse> call, Response<BookingCancelResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            BookingCancelResponse body = response.body();
                            bookingAdapter.updateBookingStatus(position, body.getStatus());
                            Toast.makeText(HomeActivity.this, body.getMessage(), Toast.LENGTH_SHORT).show();
                        } else if (response.code() == 400) {
                            Toast.makeText(HomeActivity.this, "Booking is already cancelled or invalid request", Toast.LENGTH_SHORT).show();
                        } else if (response.code() == 403) {
                            Toast.makeText(HomeActivity.this, "You can cancel only your own booking", Toast.LENGTH_SHORT).show();
                        } else if (response.code() == 404) {
                            Toast.makeText(HomeActivity.this, "Booking not found", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(HomeActivity.this, "Cancel failed. Code: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<BookingCancelResponse> call, Throwable t) {
                        Toast.makeText(HomeActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private final ActivityResultLauncher<Intent> bookingDetailLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();

                    int bookingId = data.getIntExtra("updated_booking_id", -1);
                    String updatedStatus = data.getStringExtra("updated_status");
                    String visitorName = data.getStringExtra("visitor_name");
                    String visitorMobile = data.getStringExtra("visitor_mobile");
                    String purpose = data.getStringExtra("purpose_of_visit");
                    String arrivalDate = data.getStringExtra("arrival_date");
                    String arrivalTime = data.getStringExtra("arrival_time");
                    String departureDate = data.getStringExtra("departure_date");
                    String departureTime = data.getStringExtra("departure_time");

                    if (bookingId != -1) {
                        bookingAdapter.updateBookingById(
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
                    boolean bookingCreated = result.getData().getBooleanExtra("booking_created", false);
                    if (bookingCreated) {
                        refreshBookings();
                    }
                }
            });
}