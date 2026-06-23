package com.example.roombooking.auth;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
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

public class SignupActivity extends AppCompatActivity {

    private TextView tabAdmin;
    private TextView tabRequester;
    private TextView tvRoleContext;
    private EditText etName;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private EditText etAdminCode;
    private EditText etDesignation;
    private EditText etDepartment;
    private EditText etMobile;
    private LinearLayout layoutRequesterProfile;
    private AppCompatButton btnSignup;
    private TextView tvLogin;
    private TextView tvError;
    private ProgressBar progressBar;
    private ScrollView rootView;

    private AuthSessionManager sessionManager;
    private Call<ApiResponse<AuthResponse>> signupCall;
    private String selectedRole = AuthSessionManager.ROLE_ADMIN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new AuthSessionManager(getApplicationContext());
        if (sessionManager.isLoggedIn() && sessionManager.isApproved()) {
            AuthSessionGuard.openLandingForSession(this);
            return;
        }

        setContentView(R.layout.activity_signup);
        EdgeToEdgeUtils.applySystemBarAndImeInsets(this, findViewById(R.id.rootView));

        bindViews();
        selectRole(getIntent().getStringExtra(LoginActivity.EXTRA_SELECTED_ROLE));
        setupListeners();
    }

    private void bindViews() {
        rootView = findViewById(R.id.rootView);
        tabAdmin = findViewById(R.id.tabAdmin);
        tabRequester = findViewById(R.id.tabRequester);
        tvRoleContext = findViewById(R.id.tvRoleContext);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etAdminCode = findViewById(R.id.etAdminCode);
        etDesignation = findViewById(R.id.etDesignation);
        etDepartment = findViewById(R.id.etDepartment);
        etMobile = findViewById(R.id.etMobile);
        layoutRequesterProfile = findViewById(R.id.layoutRequesterProfile);
        btnSignup = findViewById(R.id.btnSignup);
        tvLogin = findViewById(R.id.tvLogin);
        tvError = findViewById(R.id.tvError);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        btnSignup.setOnClickListener(v -> signup());
        tabAdmin.setOnClickListener(v -> selectRole(AuthSessionManager.ROLE_ADMIN));
        tabRequester.setOnClickListener(v -> selectRole(AuthSessionManager.ROLE_REQUESTER));
        tvLogin.setOnClickListener(v -> finish());
        setupKeyboardNavigation();
        setupFocusScroll();
        PasswordVisibilityToggle.attach(etPassword);
        PasswordVisibilityToggle.attach(etConfirmPassword);
        PasswordVisibilityToggle.attach(etAdminCode);
    }

    private void setupKeyboardNavigation() {
        etName.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                focusAndScroll(etEmail);
                return true;
            }
            return false;
        });
        etEmail.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                focusAndScroll(etPassword);
                return true;
            }
            return false;
        });
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                focusAndScroll(etConfirmPassword);
                return true;
            }
            return false;
        });
        etConfirmPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                focusAndScroll(AuthSessionManager.ROLE_ADMIN.equals(selectedRole)
                        ? etAdminCode
                        : etDesignation);
                return true;
            }
            return false;
        });
        etDesignation.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                focusAndScroll(etDepartment);
                return true;
            }
            return false;
        });
        etDepartment.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                focusAndScroll(etMobile);
                return true;
            }
            return false;
        });
        etAdminCode.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard();
                signup();
                return true;
            }
            return false;
        });
        etMobile.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard();
                signup();
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
        etName.setOnFocusChangeListener(listener);
        etEmail.setOnFocusChangeListener(listener);
        etPassword.setOnFocusChangeListener(listener);
        etConfirmPassword.setOnFocusChangeListener(listener);
        etAdminCode.setOnFocusChangeListener(listener);
        etDesignation.setOnFocusChangeListener(listener);
        etDepartment.setOnFocusChangeListener(listener);
        etMobile.setOnFocusChangeListener(listener);
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
                ? R.string.creating_admin_account
                : R.string.creating_requester_account);
        etAdminCode.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        layoutRequesterProfile.setVisibility(isAdmin ? View.GONE : View.VISIBLE);
        rootView.post(() -> rootView.smoothScrollTo(0, 0));
        hideError();
    }

    private void signup() {
        if (signupCall != null) {
            return;
        }

        String name = text(etName);
        String email = text(etEmail);
        String password = text(etPassword);
        String confirmPassword = text(etConfirmPassword);
        String adminCode = text(etAdminCode);
        String designation = text(etDesignation);
        String department = text(etDepartment);
        String mobile = text(etMobile);
        if (!validate(name, email, password, confirmPassword, adminCode)) {
            return;
        }

        setLoading(true);
        SignupRequest request = new SignupRequest(
                name,
                email,
                password,
                confirmPassword,
                adminCode,
                designation,
                department,
                mobile
        );
        signupCall = AuthSessionManager.ROLE_REQUESTER.equals(selectedRole)
                ? RetrofitClient.getAuthApiService(getApplicationContext()).requesterSignup(request)
                : RetrofitClient.getAuthApiService(getApplicationContext()).adminSignup(request);
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

                AuthResponse authResponse = response.body().getData();
                String message = response.body().getMessage();
                if (TextUtils.isEmpty(message)) {
                    message = AuthSessionManager.ROLE_ADMIN.equals(selectedRole)
                            ? getString(R.string.message_admin_signup_pending)
                            : getString(R.string.message_requester_signup_pending);
                }

                if (!authResponse.hasTokens()) {
                    Toast.makeText(SignupActivity.this, message, Toast.LENGTH_LONG).show();
                    AuthSessionGuard.openLogin(SignupActivity.this, message);
                    return;
                }

                sessionManager.saveSession(authResponse);
                LightBackgroundSyncScheduler.schedule(getApplicationContext());
                Toast.makeText(
                        SignupActivity.this,
                        R.string.message_signup_success,
                        Toast.LENGTH_SHORT
                ).show();
                AuthSessionGuard.openLandingForSession(SignupActivity.this);
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
            String confirmPassword,
            String adminCode
    ) {
        if (name.isEmpty()) {
            showError(getString(R.string.error_full_name_required));
            focusAndScroll(etName);
            return false;
        }

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

        if (confirmPassword.isEmpty()) {
            showError(getString(R.string.error_confirm_password_required));
            focusAndScroll(etConfirmPassword);
            return false;
        }

        if (!password.equals(confirmPassword)) {
            showError(getString(R.string.error_passwords_do_not_match));
            focusAndScroll(etConfirmPassword);
            return false;
        }

        if (AuthSessionManager.ROLE_ADMIN.equals(selectedRole) && adminCode.isEmpty()) {
            showError(getString(R.string.error_admin_code_required));
            focusAndScroll(etAdminCode);
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
        etAdminCode.setEnabled(!loading);
        etDesignation.setEnabled(!loading);
        etDepartment.setEnabled(!loading);
        etMobile.setEnabled(!loading);
        tabAdmin.setEnabled(!loading);
        tabRequester.setEnabled(!loading);
        btnSignup.setText(loading ? R.string.action_creating_account : R.string.action_create_account);
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
        if (signupCall != null && !signupCall.isCanceled()) {
            signupCall.cancel();
        }
        signupCall = null;
        super.onDestroy();
    }
}
