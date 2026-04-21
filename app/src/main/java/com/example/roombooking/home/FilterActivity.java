package com.example.roombooking.home;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.roombooking.R;

public class FilterActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Filter Bookings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}