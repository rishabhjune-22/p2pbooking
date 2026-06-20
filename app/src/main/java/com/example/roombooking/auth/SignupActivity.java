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

public class SignupActivity extends AppCompatActivity {

    private EditText etName;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private AppCompatButton btnSignup;
    private TextView tvLogin;
    private TextView tvError;
    private ProgressBar progressBar;

    private AuthSessionManager sessionManager;
    private Call<ApiResponse<AuthResponse>> signupCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new AuthSessionManager(getApplicationContext());
        if (sessionManager.isLoggedIn()) {
            openLanding();
            return;
        }

        setContentView(R.layout.activity_signup);
        EdgeToEdgeUtils.applySystemBarInsets(this, findViewById(R.id.rootView));

        bindViews();
        setupListeners();
    }

    private void bindViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSignup = findViewById(R.id.btnSignup);
        tvLogin = findViewById(R.id.tvLogin);
        tvError = findViewById(R.id.tvError);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        btnSignup.setOnClickListener(v -> signup());
        tvLogin.setOnClickListener(v -> finish());
    }

    private void signup() {
        if (signupCall != null) {
            return;
        }

        String name = text(etName);
        String email = text(etEmail);
        String password = text(etPassword);
        String confirmPassword = text(etConfirmPassword);
        if (!validate(name, email, password, confirmPassword)) {
            return;
        }

        setLoading(true);
        signupCall = RetrofitClient.getAuthApiService(getApplicationContext())
                .signup(new SignupRequest(name, email, password, confirmPassword));
        signupCall.enqueue(new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<AuthResponse>> call,
                    @NonNull Response<ApiResponse<AuthResponse>> response
            ) {
                if (call != signupCall) return;
                signupCall = null;
                setLoading(false);

                if (!response.isSuccessful()
                        || response.body() == null
                        || !response.body().isSuccess()
                        || response.body().getData() == null) {
                    showError(ApiErrorUtils.messageFromResponse(
                            response,
                            getString(R.string.error_signup_failed)
                    ));
                    return;
                }

                sessionManager.saveSession(response.body().getData());
                LightBackgroundSyncScheduler.schedule(getApplicationContext());
                Toast.makeText(
                        SignupActivity.this,
                        R.string.message_signup_success,
                        Toast.LENGTH_SHORT
                ).show();
                openLanding();
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<AuthResponse>> call,
                    @NonNull Throwable t
            ) {
                if (call != signupCall) return;
                signupCall = null;
                setLoading(false);
                if (!call.isCanceled()) {
                    showError(ApiErrorUtils.messageFromThrowable(t));
                }
            }
        });
    }

    private boolean validate(
            String name,
            String email,
            String password,
            String confirmPassword
    ) {
        if (name.isEmpty()) {
            showError(getString(R.string.error_full_name_required));
            etName.requestFocus();
            return false;
        }

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

        if (confirmPassword.isEmpty()) {
            showError(getString(R.string.error_confirm_password_required));
            etConfirmPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            showError(getString(R.string.error_passwords_do_not_match));
            etConfirmPassword.requestFocus();
            return false;
        }

        hideError();
        return true;
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSignup.setEnabled(!loading);
        tvLogin.setEnabled(!loading);
        etName.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etPassword.setEnabled(!loading);
        etConfirmPassword.setEnabled(!loading);
        btnSignup.setText(loading ? R.string.action_creating_account : R.string.action_create_account);
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
        if (signupCall != null && !signupCall.isCanceled()) {
            signupCall.cancel();
        }
        signupCall = null;
        super.onDestroy();
    }
}
