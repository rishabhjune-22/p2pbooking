package com.example.roombooking.booking;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import android.util.TypedValue;
import java.util.ArrayList;
import java.util.List;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.roombooking.model.booking.RoomAvailabilityBookingItem;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.List;
import com.example.roombooking.R;
import com.example.roombooking.api.RetrofitClient;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.utils.EdgeToEdgeUtils;
import com.example.roombooking.utils.InternetErrorBanner;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoomAvailabilityActivity extends AppCompatActivity {

    private ImageView ivBack;
    private ImageButton btnPreviousMonth;
    private ImageButton btnNextMonth;
    private TextView tvMonthYear;
    private TextView tvAvailabilityMessage;
    private ProgressBar progressBarAvailability;
    private RecyclerView rvAvailabilityGroups;
    private TextView tvToggleBeta;
    private TextView tvToggleGamma;
    private TextView tvToggleDelta;
    private SwipeRefreshLayout swipeRefreshAvailability;
    private String selectedPrefix = "Beta";
    private List<RoomAvailabilityGroup> allGroups = new ArrayList<>();
    private final Calendar selectedMonth = Calendar.getInstance();
    private AvailabilityGroupAdapter groupAdapter;
    private final SimpleDateFormat monthYearFormat =
            new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_availability);
        EdgeToEdgeUtils.applySystemBarInsets(this, findViewById(R.id.rootView));

        initViews();
        groupAdapter = new AvailabilityGroupAdapter(day -> {
            fetchAvailabilityDetails(day.getDate());
        });
        setupRecyclerView();
        setupListeners();
        updateToggleUi();
        loadAvailability();
    }

    private void fetchAvailabilityDetails(String date) {
        RetrofitClient.getApiService(this)
                .getRoomAvailabilityDetails(date, selectedPrefix)
                .enqueue(new Callback<ApiResponse<RoomAvailabilityDetailsResponse>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<ApiResponse<RoomAvailabilityDetailsResponse>> call,
                            @NonNull Response<ApiResponse<RoomAvailabilityDetailsResponse>> response
                    ) {
                        InternetErrorBanner.hide(RoomAvailabilityActivity.this);
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()
                                && response.body().getData() != null) {

                            showAvailabilityDetailsDialog(response.body().getData());
                            return;
                        }

                        Toast.makeText(
                                RoomAvailabilityActivity.this,
                                "Failed to load booking details",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<ApiResponse<RoomAvailabilityDetailsResponse>> call,
                            @NonNull Throwable t
                    ) {
                        InternetErrorBanner.show(RoomAvailabilityActivity.this);
                        Toast.makeText(
                                RoomAvailabilityActivity.this,
                                "Please check your internet connection",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    private void showAvailabilityDetailsDialog(RoomAvailabilityDetailsResponse data) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);

        View view = getLayoutInflater().inflate(
                R.layout.bottom_sheet_availability_details,
                null
        );

        TextView tvTitle = view.findViewById(R.id.tvSheetTitle);
        TextView tvSubtitle = view.findViewById(R.id.tvSheetSubtitle);
        LinearLayout layoutDetails = view.findViewById(R.id.layoutBookingDetails);

        tvTitle.setText("Bookings on " + safe(data.getDate()));
        tvSubtitle.setText("Hostel: " + selectedPrefix);

        List<RoomAvailabilityBookingItem> bookings = data.getBookings();

            if (bookings == null || bookings.isEmpty()) {
                TextView empty = new TextView(this);
                empty.setText("No bookings found for this date.");
                empty.setTextColor(getColor(R.color.detail_text_secondary));
                empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                layoutDetails.addView(empty);
            } else {
                for (RoomAvailabilityBookingItem item : bookings) {
                    TextView card = new TextView(this);

                card.setText(
                        "Guest: " + safe(item.getGuestName()) + "\n" +
                                "Room: " + safe(item.getRoomName()) + "\n" +
                                "Requestee: " + safe(item.getRequesteeName()) + "\n" +
                                "Arrival: " + safe(item.getArrivalAt()) + "\n" +
                                "Departure: " + safe(item.getDepartureAt()) + "\n" +
                                "Status: " + safe(item.getStatus())
                );

                card.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                card.setTextColor(getColor(R.color.detail_text_primary));
                card.setBackgroundResource(R.drawable.bg_detail_card);
                card.setPadding(
                        getDimenPx(R.dimen.space_28),
                        getDimenPx(R.dimen.space_22),
                        getDimenPx(R.dimen.space_28),
                        getDimenPx(R.dimen.space_22)
                );

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 0, getDimenPx(R.dimen.space_14));
                card.setLayoutParams(params);

                layoutDetails.addView(card);
            }
        }

        dialog.setContentView(view);
        dialog.show();
    }



    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int getDimenPx(int dimenResId) {
        return Math.round(getResources().getDimension(dimenResId));
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        btnPreviousMonth = findViewById(R.id.btnPreviousMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        tvAvailabilityMessage = findViewById(R.id.tvAvailabilityMessage);
        progressBarAvailability = findViewById(R.id.progressBarAvailability);
        rvAvailabilityGroups = findViewById(R.id.rvAvailabilityGroups);
        tvToggleBeta = findViewById(R.id.tvToggleBeta);
        tvToggleGamma = findViewById(R.id.tvToggleGamma);
        tvToggleDelta = findViewById(R.id.tvToggleDelta);
        swipeRefreshAvailability = findViewById(R.id.swipeRefreshAvailability);
    }

    private void setupRecyclerView() {
        rvAvailabilityGroups.setLayoutManager(new LinearLayoutManager(this));
        rvAvailabilityGroups.setAdapter(groupAdapter);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        btnPreviousMonth.setOnClickListener(v -> {
            selectedMonth.add(Calendar.MONTH, -1);
            loadAvailability();
        });

        btnNextMonth.setOnClickListener(v -> {
            selectedMonth.add(Calendar.MONTH, 1);
            loadAvailability();
        });


        tvToggleBeta.setOnClickListener(v -> {
            selectedPrefix = "Beta";
            updateToggleUi();
            applyPrefixFilter();
        });

        tvToggleGamma.setOnClickListener(v -> {
            selectedPrefix = "Gamma";
            updateToggleUi();
            applyPrefixFilter();
        });

        tvToggleDelta.setOnClickListener(v -> {
            selectedPrefix = "Delta";
            updateToggleUi();
            applyPrefixFilter();
        });
        swipeRefreshAvailability.setColorSchemeResources(
                R.color.primary,
                R.color.success_green,
                R.color.error_red
        );
        swipeRefreshAvailability.setOnRefreshListener(() -> {
            android.util.Log.d("ROOM_AVAIL", "Swipe refresh triggered");
            loadAvailability();
        });
    }

    private void loadAvailability() {
        int month = selectedMonth.get(Calendar.MONTH) + 1;
        int year = selectedMonth.get(Calendar.YEAR);

        tvMonthYear.setText(monthYearFormat.format(selectedMonth.getTime()));

        setLoading(true);
        showMessage(null);

        RetrofitClient.getApiService(this)
                .getRoomAvailability(month, year)
                .enqueue(new Callback<ApiResponse<RoomAvailabilityResponse>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<ApiResponse<RoomAvailabilityResponse>> call,
                            @NonNull Response<ApiResponse<RoomAvailabilityResponse>> response
                    ) {
                        InternetErrorBanner.hide(RoomAvailabilityActivity.this);
                        setLoading(false);
                        swipeRefreshAvailability.setRefreshing(false);
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()
                                && response.body().getData() != null) {

                            RoomAvailabilityResponse data = response.body().getData();

                            if (data.getGroups() == null || data.getGroups().isEmpty()) {
                                groupAdapter.submitList(null);
                                showMessage("No room availability found.");
                                return;
                            }

                            allGroups.clear();
                            allGroups.addAll(data.getGroups());

                            applyPrefixFilter();
                            return;
                        }

                        showMessage("Failed to load room availability.");
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<ApiResponse<RoomAvailabilityResponse>> call,
                            @NonNull Throwable t
                    ) {
                        setLoading(false);
                        swipeRefreshAvailability.setRefreshing(false);
                        InternetErrorBanner.show(RoomAvailabilityActivity.this);
                        showMessage("Please check your internet connection.");

                        Toast.makeText(
                                RoomAvailabilityActivity.this,
                                "Network error",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }
    private void applyPrefixFilter() {
        List<RoomAvailabilityGroup> filteredGroups = new ArrayList<>();

        for (RoomAvailabilityGroup group : allGroups) {
            if (group.getPrefix() != null
                    && group.getPrefix().equalsIgnoreCase(selectedPrefix)) {
                filteredGroups.add(group);
                break;
            }
        }

        if (filteredGroups.isEmpty()) {
            groupAdapter.submitList(null);
            showMessage("No " + selectedPrefix + " rooms found.");
        } else {
            showMessage(null);
            groupAdapter.submitList(filteredGroups);
        }
    }

    private void updateToggleUi() {
        setToggleSelected(tvToggleBeta, "Beta".equalsIgnoreCase(selectedPrefix));
        setToggleSelected(tvToggleGamma, "Gamma".equalsIgnoreCase(selectedPrefix));
        setToggleSelected(tvToggleDelta, "Delta".equalsIgnoreCase(selectedPrefix));
    }

    private void setToggleSelected(TextView textView, boolean selected) {
        if (selected) {
            textView.setBackgroundResource(R.drawable.bg_btn_gradient);
            textView.setTextColor(getColor(R.color.white));
        } else {
            textView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            textView.setTextColor(getColor(R.color.detail_text_secondary));
        }
    }
    private void setLoading(boolean loading) {
        swipeRefreshAvailability.setEnabled(!loading);
        progressBarAvailability.setVisibility(
                loading && !swipeRefreshAvailability.isRefreshing() ? View.VISIBLE : View.GONE
        );
        rvAvailabilityGroups.setVisibility(View.VISIBLE);
    }

    private void showMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            tvAvailabilityMessage.setVisibility(View.GONE);
            return;
        }

        tvAvailabilityMessage.setVisibility(View.VISIBLE);
        tvAvailabilityMessage.setText(message);
    }
}
