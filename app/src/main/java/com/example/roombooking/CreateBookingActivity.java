package com.example.roombooking;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.roombooking.api.RetrofitClient;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateBookingActivity extends AppCompatActivity {

    private EditText etVisitorName, etVisitorDesignation, etVisitorOrganisation, etVisitorGender,
            etVisitorAddress, etVisitorMobile, etVisitorEmail,
            etArrivalDT, etDepartureDT,
            etPurpose, etRequesteeName, etRequesteeDesignation, etRequesteeDepartment,
            etRequesteeMobile, etLogisticsName, etLogisticsDesignation, etLogisticsMobile, etRoomId;

    private TextView tvMessage;
    private Button btnCreateBooking;

    private final Calendar arrivalCal = Calendar.getInstance();
    private final Calendar departureCal = Calendar.getInstance();

    private final SimpleDateFormat displayFmt =
            new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    private final SimpleDateFormat apiDateFmt =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private final SimpleDateFormat apiTimeFmt =
            new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_booking);

        bindViews();

        departureCal.setTimeInMillis(arrivalCal.getTimeInMillis() + 60 * 60 * 1000L);
        refreshDateTimeFields();

        etArrivalDT.setOnClickListener(v -> pickDateTime(arrivalCal, () -> {
            if (departureCal.getTimeInMillis() <= arrivalCal.getTimeInMillis()) {
                departureCal.setTimeInMillis(arrivalCal.getTimeInMillis() + 60 * 60 * 1000L);
            }
            refreshDateTimeFields();
        }));

        etDepartureDT.setOnClickListener(v -> pickDateTime(departureCal, () -> {
            if (departureCal.getTimeInMillis() <= arrivalCal.getTimeInMillis()) {
                showError("Departure must be after arrival.");
                departureCal.setTimeInMillis(arrivalCal.getTimeInMillis() + 60 * 60 * 1000L);
            }
            refreshDateTimeFields();
        }));

        btnCreateBooking.setOnClickListener(v -> submitBooking());
    }

    private void bindViews() {
        etVisitorName = findViewById(R.id.etVisitorName);
        etVisitorDesignation = findViewById(R.id.etVisitorDesignation);
        etVisitorOrganisation = findViewById(R.id.etVisitorOrganisation);
        etVisitorGender = findViewById(R.id.etVisitorGender);
        etVisitorAddress = findViewById(R.id.etVisitorAddress);
        etVisitorMobile = findViewById(R.id.etVisitorMobile);
        etVisitorEmail = findViewById(R.id.etVisitorEmail);

        etArrivalDT = findViewById(R.id.etArrivalDT);
        etDepartureDT = findViewById(R.id.etDepartureDT);

        etPurpose = findViewById(R.id.etPurpose);
        etRequesteeName = findViewById(R.id.etRequesteeName);
        etRequesteeDesignation = findViewById(R.id.etRequesteeDesignation);
        etRequesteeDepartment = findViewById(R.id.etRequesteeDepartment);
        etRequesteeMobile = findViewById(R.id.etRequesteeMobile);
        etLogisticsName = findViewById(R.id.etLogisticsName);
        etLogisticsDesignation = findViewById(R.id.etLogisticsDesignation);
        etLogisticsMobile = findViewById(R.id.etLogisticsMobile);
        etRoomId = findViewById(R.id.etRoomId);

        tvMessage = findViewById(R.id.tvMessage);
        btnCreateBooking = findViewById(R.id.btnCreateBooking);
    }

    private void refreshDateTimeFields() {
        etArrivalDT.setText(displayFmt.format(arrivalCal.getTime()));
        etDepartureDT.setText(displayFmt.format(departureCal.getTime()));
    }

    private void pickDateTime(Calendar target, Runnable onDone) {
        int y = target.get(Calendar.YEAR);
        int m = target.get(Calendar.MONTH);
        int d = target.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dp = new DatePickerDialog(this, (DatePicker view, int year, int month, int dayOfMonth) -> {
            target.set(Calendar.YEAR, year);
            target.set(Calendar.MONTH, month);
            target.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            int hh = target.get(Calendar.HOUR_OF_DAY);
            int mm = target.get(Calendar.MINUTE);

            TimePickerDialog tp = new TimePickerDialog(this, (TimePicker tview, int hourOfDay, int minute) -> {
                target.set(Calendar.HOUR_OF_DAY, hourOfDay);
                target.set(Calendar.MINUTE, minute);
                target.set(Calendar.SECOND, 0);
                target.set(Calendar.MILLISECOND, 0);
                onDone.run();
            }, hh, mm, true);

            tp.show();
        }, y, m, d);

        dp.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dp.show();
    }

    private void submitBooking() {
        tvMessage.setVisibility(View.GONE);

        String visitorName = getText(etVisitorName);
        String visitorDesignation = getText(etVisitorDesignation);
        String visitorOrganisation = getText(etVisitorOrganisation);
        String visitorGender = getText(etVisitorGender);
        String visitorAddress = getText(etVisitorAddress);
        String visitorMobile = getText(etVisitorMobile);
        String visitorEmail = getText(etVisitorEmail);

        String arrivalDate = apiDateFmt.format(arrivalCal.getTime());
        String arrivalTime = apiTimeFmt.format(arrivalCal.getTime());

        String departureDate = apiDateFmt.format(departureCal.getTime());
        String departureTime = apiTimeFmt.format(departureCal.getTime());

        String purpose = getText(etPurpose);
        String requesteeName = getText(etRequesteeName);
        String requesteeDesignation = getText(etRequesteeDesignation);
        String requesteeDepartment = getText(etRequesteeDepartment);
        String requesteeMobile = getText(etRequesteeMobile);
        String logisticsName = getText(etLogisticsName);
        String logisticsDesignation = getText(etLogisticsDesignation);
        String logisticsMobile = getText(etLogisticsMobile);
        String roomText = getText(etRoomId);

        if (!validateInputs(
                visitorName,
                visitorDesignation,
                visitorOrganisation,
                visitorGender,
                visitorAddress,
                visitorMobile,
                visitorEmail,
                arrivalDate,
                arrivalTime,
                departureDate,
                departureTime,
                purpose,
                requesteeName,
                requesteeDesignation,
                requesteeDepartment,
                requesteeMobile,
                logisticsName,
                logisticsDesignation,
                logisticsMobile,
                roomText
        )) {
            return;
        }

        int roomId = Integer.parseInt(roomText);

        BookingCreateRequest request = new BookingCreateRequest(
                visitorName,
                visitorDesignation,
                visitorOrganisation,
                visitorGender,
                visitorAddress,
                visitorMobile,
                visitorEmail,
                arrivalDate,
                arrivalTime,
                departureDate,
                departureTime,
                purpose,
                requesteeName,
                requesteeDesignation,
                requesteeDepartment,
                requesteeMobile,
                logisticsName,
                logisticsDesignation,
                logisticsMobile,
                roomId
        );

        setLoading(true);

        RetrofitClient.getApiService(this).createBooking(request).enqueue(new Callback<BookingCreateResponse>() {
            @Override
            public void onResponse(Call<BookingCreateResponse> call, Response<BookingCreateResponse> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    BookingCreateResponse body = response.body();
                    Toast.makeText(CreateBookingActivity.this, body.getMessage(), Toast.LENGTH_SHORT).show();

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("booking_created", true);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else {
                    showError(parseErrorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<BookingCreateResponse> call, Throwable t) {
                setLoading(false);
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private boolean validateInputs(String visitorName,
                                   String visitorDesignation,
                                   String visitorOrganisation,
                                   String visitorGender,
                                   String visitorAddress,
                                   String visitorMobile,
                                   String visitorEmail,
                                   String arrivalDate,
                                   String arrivalTime,
                                   String departureDate,
                                   String departureTime,
                                   String purpose,
                                   String requesteeName,
                                   String requesteeDesignation,
                                   String requesteeDepartment,
                                   String requesteeMobile,
                                   String logisticsName,
                                   String logisticsDesignation,
                                   String logisticsMobile,
                                   String roomText) {

        if (TextUtils.isEmpty(visitorName) || TextUtils.isEmpty(visitorDesignation) ||
                TextUtils.isEmpty(visitorOrganisation) || TextUtils.isEmpty(visitorGender) ||
                TextUtils.isEmpty(visitorAddress) || TextUtils.isEmpty(visitorMobile) ||
                TextUtils.isEmpty(visitorEmail) || TextUtils.isEmpty(purpose) ||
                TextUtils.isEmpty(requesteeName) || TextUtils.isEmpty(requesteeDesignation) ||
                TextUtils.isEmpty(requesteeDepartment) || TextUtils.isEmpty(requesteeMobile) ||
                TextUtils.isEmpty(logisticsName) || TextUtils.isEmpty(logisticsDesignation) ||
                TextUtils.isEmpty(logisticsMobile) || TextUtils.isEmpty(roomText)) {
            showError("Please fill all fields.");
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(visitorEmail).matches()) {
            showError("Enter a valid visitor email.");
            return false;
        }

        if (!visitorMobile.matches("\\d{10}")) {
            showError("Visitor mobile must be 10 digits.");
            return false;
        }

        if (!requesteeMobile.matches("\\d{10}")) {
            showError("Requestee mobile must be 10 digits.");
            return false;
        }

        if (!logisticsMobile.matches("\\d{10}")) {
            showError("Logistics mobile must be 10 digits.");
            return false;
        }

        try {
            int roomId = Integer.parseInt(roomText);
            if (roomId <= 0) {
                showError("Room ID must be a positive number.");
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Enter a valid Room ID.");
            return false;
        }

        if (!isDepartureAfterArrival(arrivalDate, arrivalTime, departureDate, departureTime)) {
            showError("Departure date/time must be after arrival date/time.");
            return false;
        }

        return true;
    }

    private boolean isDepartureAfterArrival(String arrivalDate,
                                            String arrivalTime,
                                            String departureDate,
                                            String departureTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            sdf.setLenient(false);

            Date arrival = sdf.parse(arrivalDate + " " + arrivalTime);
            Date departure = sdf.parse(departureDate + " " + departureTime);

            return arrival != null && departure != null && departure.after(arrival);
        } catch (ParseException e) {
            return false;
        }
    }

    private void setLoading(boolean loading) {
        btnCreateBooking.setEnabled(!loading);
        btnCreateBooking.setText(loading ? "Please wait..." : "Create Booking");
    }

    private void showError(String msg) {
        tvMessage.setVisibility(View.VISIBLE);
        tvMessage.setText(msg);
    }

    private String getText(EditText editText) {
        return editText.getText().toString().trim();
    }

    private String parseErrorMessage(Response<?> response) {
        try {
            if (response.errorBody() == null) {
                return "Booking creation failed.";
            }

            String raw = response.errorBody().string();
            JSONObject jsonObject = new JSONObject(raw);

            StringBuilder sb = new StringBuilder();
            appendJsonErrors(jsonObject, sb);

            String result = sb.toString().trim();
            return result.isEmpty() ? "Booking creation failed." : result;

        } catch (Exception e) {
            return "Booking creation failed.";
        }
    }

    private void appendJsonErrors(JSONObject jsonObject, StringBuilder sb) {
        Iterator<String> keys = jsonObject.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            Object value = jsonObject.opt(key);

            if (value instanceof org.json.JSONArray) {
                org.json.JSONArray array = (org.json.JSONArray) value;
                for (int i = 0; i < array.length(); i++) {
                    String msg = array.optString(i);
                    appendFormattedMessage(sb, key, msg);
                }
            } else if (value instanceof JSONObject) {
                appendJsonErrors((JSONObject) value, sb);
            } else if (value != null) {
                appendFormattedMessage(sb, key, value.toString());
            }
        }
    }

    private void appendFormattedMessage(StringBuilder sb, String key, String message) {
        if (message == null || message.trim().isEmpty()) return;

        String label;

        switch (key) {
            case "non_field_errors":
                label = "";
                break;
            case "room":
                label = "Room";
                break;
            case "visitor_name":
                label = "Visitor Name";
                break;
            case "visitor_designation":
                label = "Visitor Designation";
                break;
            case "visitor_organisation":
                label = "Visitor Organisation";
                break;
            case "visitor_gender":
                label = "Visitor Gender";
                break;
            case "visitor_address":
                label = "Visitor Address";
                break;
            case "visitor_mobile":
                label = "Visitor Mobile";
                break;
            case "visitor_email":
                label = "Visitor Email";
                break;
            case "arrival_date":
                label = "Arrival Date";
                break;
            case "arrival_time":
                label = "Arrival Time";
                break;
            case "departure_date":
                label = "Departure Date";
                break;
            case "departure_time":
                label = "Departure Time";
                break;
            case "purpose_of_visit":
                label = "Purpose";
                break;
            case "requestee_name":
                label = "Requestee Name";
                break;
            case "requestee_designation":
                label = "Requestee Designation";
                break;
            case "requestee_department":
                label = "Requestee Department";
                break;
            case "requestee_mobile":
                label = "Requestee Mobile";
                break;
            case "logistics_name":
                label = "Logistics Name";
                break;
            case "logistics_designation":
                label = "Logistics Designation";
                break;
            case "logistics_mobile":
                label = "Logistics Mobile";
                break;
            default:
                label = prettifyKey(key);
                break;
        }

        if (sb.length() > 0) {
            sb.append("\n");
        }

        if (label.isEmpty()) {
            sb.append("• ").append(message);
        } else {
            sb.append("• ").append(label).append(": ").append(message);
        }
    }

    private String prettifyKey(String key) {
        if (key == null || key.trim().isEmpty()) return "";
        String cleaned = key.replace("_", " ").trim();
        String[] parts = cleaned.split("\\s+");
        StringBuilder sb = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1))
                    .append(" ");
        }

        return sb.toString().trim();
    }
}