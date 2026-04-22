package com.example.roombooking.home;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.roombooking.R;

public class FilterActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        setupWindow();
        setupListeners();
    }

    private void setupWindow() {
        Window window = getWindow();
        window.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
        );
        window.setGravity(Gravity.BOTTOM);
    }

    private void setupListeners() {
        ImageView ivClose = findViewById(R.id.ivClose);
        TextView btnReset = findViewById(R.id.btnReset);
        TextView btnApply = findViewById(R.id.btnApply);
        View scrim = findViewById(R.id.filterScrim);

        ivClose.setOnClickListener(v -> finish());
        btnApply.setOnClickListener(v -> finish());
        scrim.setOnClickListener(v -> finish());
        btnReset.setOnClickListener(v -> resetFields());
    }

    private void resetFields() {
        ((EditText) findViewById(R.id.etVisitorName)).setText("");
        ((EditText) findViewById(R.id.etOrganisation)).setText("");
        ((TextView) findViewById(R.id.tvGender)).setText(R.string.filter_select_gender);
        ((EditText) findViewById(R.id.etMobile)).setText("");
        ((EditText) findViewById(R.id.etEmail)).setText("");
        ((TextView) findViewById(R.id.tvArrivalFrom)).setText(R.string.filter_arrival_from);
        ((TextView) findViewById(R.id.tvArrivalTo)).setText(R.string.filter_arrival_to);
        ((TextView) findViewById(R.id.tvDepartureFrom)).setText(R.string.filter_departure_from);
        ((TextView) findViewById(R.id.tvDepartureTo)).setText(R.string.filter_departure_to);
        ((TextView) findViewById(R.id.tvPurpose)).setText(R.string.filter_select_purpose);
        ((EditText) findViewById(R.id.etRequesteeName)).setText("");
        ((EditText) findViewById(R.id.etDepartment)).setText("");
        ((EditText) findViewById(R.id.etRequesteeMobile)).setText("");
        ((TextView) findViewById(R.id.tvRoom)).setText(R.string.filter_select_room);
        ((TextView) findViewById(R.id.tvStatus)).setText(R.string.filter_select_status);
    }
}
