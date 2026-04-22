package com.example.roombooking.home;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.roombooking.R;

public class FilterActivity extends AppCompatActivity {
    private ScrollView filterScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        setupWindow();
        initViews();
        setupListeners();
        setupFieldScrolling();
    }

    private void setupWindow() {
        Window window = getWindow();
        window.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
        );
        window.setGravity(Gravity.BOTTOM);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    private void initViews() {
        filterScrollView = findViewById(R.id.filterScrollView);
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

    private void setupFieldScrolling() {
        int[] editTextIds = {
                R.id.etVisitorName,
                R.id.etOrganisation,
                R.id.etMobile,
                R.id.etEmail,
                R.id.etRequesteeName,
                R.id.etDepartment,
                R.id.etRequesteeMobile
        };

        for (int id : editTextIds) {
            EditText editText = findViewById(id);
            editText.setOnFocusChangeListener((view, hasFocus) -> {
                if (hasFocus && filterScrollView != null) {
                    filterScrollView.post(() ->
                            filterScrollView.smoothScrollTo(0, Math.max(view.getTop() - 40, 0))
                    );
                }
            });
        }
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

    private float touchDownX, touchDownY;
    private static final int CLICK_ACTION_THRESHOLD = 50;

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (ev.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            touchDownX = ev.getX();
            touchDownY = ev.getY();
        } else if (ev.getAction() == android.view.MotionEvent.ACTION_UP) {
            float upX = ev.getX();
            float upY = ev.getY();
            
            boolean isTap = Math.abs(upX - touchDownX) < CLICK_ACTION_THRESHOLD 
                         && Math.abs(upY - touchDownY) < CLICK_ACTION_THRESHOLD;
                         
            if (isTap) {
                View view = getCurrentFocus();
                if (view != null && view instanceof EditText) {
                    android.graphics.Rect r = new android.graphics.Rect();
                    view.getGlobalVisibleRect(r);
                    if (!r.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                        view.clearFocus();
                        android.view.inputmethod.InputMethodManager imm = 
                                (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        }
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }
}
