package com.example.roombooking.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.roombooking.R;
import com.example.roombooking.api.RetrofitClient;
import com.example.roombooking.booking.LandingActivity;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.sync.LightBackgroundSyncScheduler;
import com.example.roombooking.utils.ApiErrorUtils;
import com.example.roombooking.utils.EdgeToEdgeUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private AppCompatButton btnLogin;
    private TextView tvSignup;
    private TextView tvError;
    private ProgressBar progressBar;

    private AuthSessionManager sessionManager;
    private Call<ApiResponse<AuthResponse>> loginCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new AuthSessionManager(getApplicationContext());
        if (sessionManager.isLoggedIn()) {
            openLanding();
            return;
        }

        setContentView(R.layout.activity_login);
        EdgeToEdgeUtils.applySystemBarInsets(this, findViewById(R.id.rootView));

        bindViews();
        setupListeners();
        showSessionMessageIfNeeded();
    }

    private void bindViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignup = findViewById(R.id.tvSignup);
        tvError = findViewById(R.id.tvError);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> login());
        tvSignup.setOnClickListener(v -> {
            startActivity(new Intent(this, SignupActivity.class));
        });
    }

    private void showSessionMessageIfNeeded() {
        String message = getIntent().getStringExtra(AuthSessionGuard.EXTRA_SESSION_MESSAGE);
        if (!TextUtils.isEmpty(message)) {
            showError(message);
        }
        sessionManager.clearSessionExpired();
    }

    private void login() {
        if (loginCall != null) {
            return;
        }

        String email = text(etEmail);
        String password = text(etPassword);
        if (!validate(email, password)) {
            return;
        }

        setLoading(true);
        loginCall = RetrofitClient.getAuthApiService(getApplicationContext())
                .login(new LoginRequest(email, password));
        loginCall.enqueue(new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<AuthResponse>> call,
                    @NonNull Response<ApiResponse<AuthResponse>> response
            ) {
                if (call != loginCall) return;
                loginCall = null;
                setLoading(false);

                if (!response.isSuccessful()
                        || response.body() == null
                        || !response.body().isSuccess()
                        || response.body().getData() == null) {
                    showError(ApiErrorUtils.messageFromResponse(
                            response,
                            getString(R.string.error_invalid_credentials)
                    ));
                    return;
                }

                sessionManager.saveSession(response.body().getData());
                LightBackgroundSyncScheduler.schedule(getApplicationContext());
                Toast.makeText(
                        LoginActivity.this,
                        R.string.message_login_success,
                        Toast.LENGTH_SHORT
                ).show();
                openLanding();
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<AuthResponse>> call,
                    @NonNull Throwable t
            ) {
                if (call != loginCall) return;
                loginCall = null;
                setLoading(false);
                if (!call.isCanceled()) {
                    showError(ApiErrorUtils.messageFromThrowable(t));
                }
            }
        });
    }

    private boolean validate(String email, String password) {
        if (email.isEmpty()) {
            showError(getString(R.string.error_email_required));
            etEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.error_email_invalid));
            etEmail.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            showError(getString(R.string.error_password_required));
            etPassword.requestFocus();
            return false;
        }

        hideError();
        return true;
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        tvSignup.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etPassword.setEnabled(!loading);
        btnLogin.setText(loading ? R.string.action_logging_in : R.string.action_login);
    }

    private void showError(String message) {
        tvError.setText(TextUtils.isEmpty(message)
                ? getString(R.string.error_generic)
                : message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvError.setText("");
        tvError.setVisibility(View.GONE);
    }

    private String text(EditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void openLanding() {
        Intent intent = new Intent(this, LandingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (loginCall != null && !loginCall.isCanceled()) {
            loginCall.cancel();
        }
        loginCall = null;
        super.onDestroy();
    }
}
