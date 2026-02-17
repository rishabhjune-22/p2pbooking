package com.example.p2proombooking;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthActivity extends AppCompatActivity {

    private EditText etName, etUser, etPass;
    private Button btnPrimary;
    private TextView tvToggle, tvMsg;

    private boolean isSignup = false;

    // TEMP (will replace with Room DB)
    private static final Map<String, String> userPass = new HashMap<>();
    private static final Map<String, String> userIdMap = new HashMap<>(); // username -> userId

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        etName = findViewById(R.id.etName);
        etUser = findViewById(R.id.etUser);
        etPass = findViewById(R.id.etPass);
        btnPrimary = findViewById(R.id.btnPrimary);
        tvToggle = findViewById(R.id.tvToggle);
        tvMsg = findViewById(R.id.tvMsg);

        btnPrimary.setOnClickListener(v -> onPrimary());
        tvToggle.setOnClickListener(v -> toggleMode());

        renderMode();
    }

    private void toggleMode() {
        isSignup = !isSignup;
        renderMode();
        tvMsg.setText("");
    }

    private void renderMode() {
        etName.setVisibility(isSignup ? View.VISIBLE : View.GONE);
        btnPrimary.setText(isSignup ? "Signup" : "Login");
        tvToggle.setText(isSignup ? "Already have an account? Login" : "No account? Signup");
    }

    private void onPrimary() {
        AppDatabase db = AppDatabase.getInstance(this);
        String username = etUser.getText().toString().trim();
        String pass = etPass.getText().toString();

        if (username.isEmpty() || pass.isEmpty()) {
            tvMsg.setText("Fill all required fields.");
            return;
        }

        SessionManager session = new SessionManager(this);

        if (isSignup) {

            if (db.userDao().getByUsername(username) != null) {
                tvMsg.setText("User already exists.");
                return;
            }

            byte[] salt = PasswordUtils.generateSalt();
            byte[] hash = PasswordUtils.hashPassword(pass.toCharArray(), salt);

            UserEntity user = new UserEntity();
            user.userId = UUID.randomUUID().toString();
            user.username = username;
            user.displayName = etName.getText().toString().trim();
            user.passwordHash = hash;
            user.salt = salt;
            user.createdAt = System.currentTimeMillis();

            db.userDao().insert(user);

            session.setLoggedInUserId(user.userId);

            goToHome();
        } else {

            UserEntity user = db.userDao().getByUsername(username);

            if (user == null) {
                tvMsg.setText("User not found.");
                return;
            }

            byte[] inputHash = PasswordUtils.hashPassword(pass.toCharArray(), user.salt);

            if (!PasswordUtils.verify(user.passwordHash, inputHash)) {
                tvMsg.setText("Wrong password.");
                return;
            }

            session.setLoggedInUserId(user.userId);
            goToHome();
        }
    }
    private void goToHome() {
        Intent i = new Intent(this, HomeActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

}