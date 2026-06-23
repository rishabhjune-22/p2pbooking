package com.example.roombooking.auth;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.roombooking.R;
import com.example.roombooking.api.RetrofitClient;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.sync.LightBackgroundSyncScheduler;
import com.example.roombooking.utils.ApiErrorUtils;
import com.example.roombooking.utils.EdgeToEdgeUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_ROLE = "selected_role";

    private TextView tabAdmin;
    private TextView tabRequester;
    private TextView tvRoleContext;
    private EditText etEmail;
    private EditText etPassword;
    private AppCompatButton btnLogin;
    private TextView tvSignup;
    private TextView tvError;
    private ProgressBar progressBar;
    private ScrollView rootView;

    private AuthSessionManager sessionManager;
    private Call<ApiResponse<AuthResponse>> loginCall;
    private String selectedRole = AuthSessionManager.ROLE_ADMIN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new AuthSessionManager(getApplicationContext());
        if (sessionManager.isLoggedIn() && sessionManager.isApproved()) {
            AuthSessionGuard.openLandingForSession(this);
            return;
        }

        setContentView(R.layout.activity_login);
        EdgeToEdgeUtils.applySystemBarAndImeInsets(this, findViewById(R.id.rootView));

        bindViews();
        selectRole(getIntent().getStringExtra(EXTRA_SELECTED_ROLE));
        setupListeners();
        showSessionMessageIfNeeded();
    }

    private void bindViews() {
        rootView = findViewById(R.id.rootView);
        tabAdmin = findViewById(R.id.tabAdmin);
        tabRequester = findViewById(R.id.tabRequester);
        tvRoleContext = findViewById(R.id.tvRoleContext);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignup = findViewById(R.id.tvSignup);
        tvError = findViewById(R.id.tvError);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> login());
        tabAdmin.setOnClickListener(v -> selectRole(AuthSessionManager.ROLE_ADMIN));
        tabRequester.setOnClickListener(v -> selectRole(AuthSessionManager.ROLE_REQUESTER));
        tvSignup.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignupActivity.class);
            intent.putExtra(EXTRA_SELECTED_ROLE, selectedRole);
            startActivity(intent);
        });
        setupKeyboardNavigation();
        setupFocusScroll();
        PasswordVisibilityToggle.attach(etPassword);
    }

    private void setupKeyboardNavigation() {
        etEmail.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                focusAndScroll(etPassword);
                return true;
            }
            return false;
        });
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard();
                login();
                return true;
            }
            return false;
        });
    }

    private void setupFocusScroll() {
        View.OnFocusChangeListener listener = (view, hasFocus) -> {
            if (hasFocus) {
                scrollToView(rootView, view);
            }
        };
        etEmail.setOnFocusChangeListener(listener);
        etPassword.setOnFocusChangeListener(listener);
    }

    private void selectRole(String role) {
        selectedRole = AuthSessionManager.ROLE_REQUESTER.equals(role)
                ? AuthSessionManager.ROLE_REQUESTER
                : AuthSessionManager.ROLE_ADMIN;

        boolean isAdmin = AuthSessionManager.ROLE_ADMIN.equals(selectedRole);
        tabAdmin.setBackgroundResource(isAdmin
                ? R.drawable.bg_create_booking_primary_button
                : R.drawable.bg_create_booking_info);
        tabRequester.setBackgroundResource(isAdmin
                ? R.drawable.bg_create_booking_info
                : R.drawable.bg_create_booking_primary_button);
        tabAdmin.setTextColor(getColor(isAdmin ? R.color.white : R.color.detail_text_secondary));
        tabRequester.setTextColor(getColor(isAdmin ? R.color.detail_text_secondary : R.color.white));
        tvRoleContext.setText(isAdmin
                ? R.string.logging_in_as_admin
                : R.string.logging_in_as_requester);
        hideError();
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
        LoginRequest request = new LoginRequest(email, password, selectedRole);
        loginCall = AuthSessionManager.ROLE_REQUESTER.equals(selectedRole)
                ? RetrofitClient.getAuthApiService(getApplicationContext()).requesterLogin(request)
                : RetrofitClient.getAuthApiService(getApplicationContext()).adminLogin(request);
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

                AuthResponse authResponse = response.body().getData();
                if (!authResponse.hasTokens()) {
                    showError(getString(R.string.error_invalid_credentials));
                    return;
                }

                sessionManager.saveSession(authResponse);
                LightBackgroundSyncScheduler.schedule(getApplicationContext());
                Toast.makeText(
                        LoginActivity.this,
                        R.string.message_login_success,
                        Toast.LENGTH_SHORT
                ).show();
                AuthSessionGuard.openLandingForSession(LoginActivity.this);
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
            focusAndScroll(etEmail);
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.error_email_invalid));
            focusAndScroll(etEmail);
            return false;
        }

        if (password.isEmpty()) {
            showError(getString(R.string.error_password_required));
            focusAndScroll(etPassword);
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
        scrollToView(rootView, tvError);
    }

    private void hideError() {
        tvError.setText("");
        tvError.setVisibility(View.GONE);
    }

    private String text(EditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void focusAndScroll(View view) {
        if (view == null) return;
        view.requestFocus();
        scrollToView(rootView, view);
    }

    private void scrollToView(final ScrollView scrollView, final View view) {
        if (scrollView == null || view == null) return;
        view.postDelayed(() -> {
            View content = scrollView.getChildAt(0);
            if (content == null) return;
            int targetTop = getRelativeTop(content, view)
                    - getResources().getDimensionPixelSize(R.dimen.space_24);
            scrollView.smoothScrollTo(0, Math.max(0, targetTop));
        }, 250);
    }

    private int getRelativeTop(View parent, View child) {
        int top = child.getTop();
        View current = child;
        while (current.getParent() instanceof View && current.getParent() != parent) {
            current = (View) current.getParent();
            top += current.getTop();
        }
        return top;
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view == null) {
            view = rootView;
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
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
