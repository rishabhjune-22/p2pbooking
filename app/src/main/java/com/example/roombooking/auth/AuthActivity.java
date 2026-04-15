package com.example.roombooking.auth;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.roombooking.R;
import com.example.roombooking.api.RetrofitClient;
import com.example.roombooking.model.auth.LoginData;
import com.example.roombooking.model.auth.RefreshTokenData;
import com.example.roombooking.model.auth.SignupData;
import com.example.roombooking.model.auth.UserData;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.home.HomeActivity;
import com.example.roombooking.room.RoomCache;
import com.example.roombooking.security.CryptoManager;
import com.example.roombooking.security.KeystoreBackedCryptoSessionManager;
import com.example.roombooking.security.UnlockCryptoActivity;
import com.google.gson.Gson;
import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etUser;
    private EditText etPass;
    private EditText etPassphrase;
    private Button btnPrimary;
    private TextView tvToggle;
    private TextView tvMsg;
    private TextView tvCardTitle;

    private boolean isSignup = false;
    private boolean isPassVisible = false;
    private boolean isPassphraseVisible = false;

    private SessionManager sessionManager;
    private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        initViews();
        initObjects();
        setListeners();
        renderMode();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etUser = findViewById(R.id.etUser);
        etPass = findViewById(R.id.etPass);
        btnPrimary = findViewById(R.id.btnPrimary);
        tvToggle = findViewById(R.id.tvToggle);
        tvMsg = findViewById(R.id.tvMsg);
        etPassphrase = findViewById(R.id.etPassphrase);
        tvCardTitle = findViewById(R.id.tvCardTitle);
    }

    private void initObjects() {
        sessionManager = new SessionManager(this);
        gson = new Gson();
    }

    private void setListeners() {
        btnPrimary.setOnClickListener(v -> {
            try {
                onPrimaryClicked();
            } catch (Exception e) {
                showMessage("Failed to initialize encryption.");
            }
        });
        tvToggle.setOnClickListener(v -> toggleMode());

        // Password visibility toggle logic
        etPass.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (etPass.getCompoundDrawables()[DRAWABLE_RIGHT] != null) {
                    if (event.getRawX() >= (etPass.getRight() - etPass.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - etPass.getPaddingEnd())) {
                        togglePasswordVisibility();
                        v.performClick();
                        return true;
                    }
                }
            }
            return false;
        });

        // Passphrase visibility toggle logic
        etPassphrase.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (etPassphrase.getCompoundDrawables()[DRAWABLE_RIGHT] != null) {
                    if (event.getRawX() >= (etPassphrase.getRight() - etPassphrase.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - etPassphrase.getPaddingEnd())) {
                        togglePassphraseVisibility();
                        v.performClick();
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private void togglePasswordVisibility() {
        isPassVisible = !isPassVisible;
        int inputType = InputType.TYPE_CLASS_TEXT | (isPassVisible ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etPass.setInputType(inputType);
        int endIcon = isPassVisible ? R.drawable.eye_password_show_svgrepo_com : R.drawable.eye_password_hide_svgrepo_com;
        etPass.setCompoundDrawablesWithIntrinsicBounds(R.drawable.lock_01, 0, endIcon, 0);
        etPass.setSelection(etPass.getText().length());
    }

    private void togglePassphraseVisibility() {
        isPassphraseVisible = !isPassphraseVisible;
        int inputType = InputType.TYPE_CLASS_TEXT | (isPassphraseVisible ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etPassphrase.setInputType(inputType);
        int endIcon = isPassphraseVisible ? R.drawable.eye_password_show_svgrepo_com : R.drawable.eye_password_hide_svgrepo_com;
        etPassphrase.setCompoundDrawablesWithIntrinsicBounds(R.drawable.password_svgrepo_com, 0, endIcon, 0);
        etPassphrase.setSelection(etPassphrase.getText().length());
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void toggleMode() {
        isSignup = !isSignup;
        renderMode();
        clearInputs();
        showMessage("");
    }

    private void renderMode() {
        etEmail.setVisibility(isSignup ? View.VISIBLE : View.GONE);
        etPassphrase.setVisibility(isSignup ? View.VISIBLE : View.GONE);
        tvCardTitle.setText(isSignup ? "Signup" : "Login");
        btnPrimary.setText(isSignup ? "Signup" : "Login");
        tvToggle.setText(isSignup ? "Already have an account? Login" : "No account? Signup");
    }

    private void clearInputs() {
        etEmail.setText("");
        etUser.setText("");
        etPass.setText("");
        etPassphrase.setText("");
        isPassVisible = false;
        isPassphraseVisible = false;
        etPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etPass.setCompoundDrawablesWithIntrinsicBounds(R.drawable.lock_01, 0, R.drawable.eye_password_hide_svgrepo_com, 0);
        etPassphrase.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etPassphrase.setCompoundDrawablesWithIntrinsicBounds(R.drawable.password_svgrepo_com, 0, R.drawable.eye_password_hide_svgrepo_com, 0);
    }

    private void onPrimaryClicked() throws Exception {
        String username = getTrimmedText(etUser).toLowerCase();
        String password = getTrimmedText(etPass);
        String passphrase = getTrimmedText(etPassphrase);

        if (TextUtils.isEmpty(username)) {
            showMessage("Enter username.");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            showMessage("Enter password.");
            return;
        }

        if (isSignup) {
            String email = getTrimmedText(etEmail);
            if (TextUtils.isEmpty(email)) {
                showMessage("Enter email.");
                return;
            }
            if (passphrase.length() < 8) {
                showMessage("Passphrase must be at least 8 characters.");
                return;
            }
            doSignup(username, email, password, passphrase);
        } else {
            doLogin(username, password);
        }
    }

    private void doSignup(String username, String email, String password, String passphrase) throws Exception {
        setLoading(true);
        showMessage("");

        CryptoManager cryptoManager = new CryptoManager();
        char[] passphraseChars = passphrase.toCharArray();

        CryptoManager.WrappedDekResult wrappedDekResult = cryptoManager.createAndWrapDek(passphraseChars);
        SignupRequest request = new SignupRequest(
                username,
                email,
                password,
                wrappedDekResult.getEncryptedDekBase64(),
                wrappedDekResult.getDekWrapNonceBase64(),
                wrappedDekResult.getKdfMetadata()
        );

        AuthRepository repo = new AuthRepository(this);
        repo.signup(request).enqueue(new Callback<ApiResponse<SignupData>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<SignupData>> call, @NonNull Response<ApiResponse<SignupData>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<SignupData> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        showMessage("Signup successful. Logging in...");
                        doLogin(username, password);
                    } else {
                        showMessage(apiResponse.getFirstErrorMessage());
                    }
                    return;
                }
                showMessage(extractErrorMessage(response));
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<SignupData>> call, @NonNull Throwable t) {
                setLoading(false);
                showMessage(getNetworkErrorMessage(t));
            }
        });
    }

    private void doLogin(String username, String password) {
        setLoading(true);
        showMessage("");

        LoginRequest request = new LoginRequest(username, password);
        AuthRepository repo = new AuthRepository(this);

        repo.login(request).enqueue(new Callback<ApiResponse<LoginData>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<LoginData>> call, @NonNull Response<ApiResponse<LoginData>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<LoginData> apiResponse = response.body();
                    if (!apiResponse.isSuccess() || apiResponse.getData() == null) {
                        showMessage(apiResponse.getFirstErrorMessage());
                        return;
                    }

                    LoginData loginData = apiResponse.getData();
                    sessionManager.saveSession(
                            loginData.getAccessToken(),
                            loginData.getRefreshToken(),
                            loginData.getUser() != null ? loginData.getUser().getUsername() : username,
                            loginData.getUser() != null ? loginData.getUser().getEmail() : null
                    );

                    showMessage(apiResponse.getMessage());
                    goToNextScreenAfterLogin();
                    return;
                }
                showMessage(extractErrorMessage(response));
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<LoginData>> call, @NonNull Throwable t) {
                setLoading(false);
                showMessage(getNetworkErrorMessage(t));
            }
        });
    }

    private void goToNextScreenAfterLogin() {
        boolean restored = KeystoreBackedCryptoSessionManager.getInstance(getApplicationContext()).restoreDekFromLocalStore();
        Intent intent = new Intent(AuthActivity.this, restored ? HomeActivity.class : UnlockCryptoActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        btnPrimary.setEnabled(!loading);
        tvToggle.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etUser.setEnabled(!loading);
        etPass.setEnabled(!loading);
        etPassphrase.setEnabled(!loading);
        btnPrimary.setText(loading ? "Please wait..." : (isSignup ? "Signup" : "Login"));
    }

    private void showMessage(String message) {
        tvMsg.setText(message != null ? message : "");
    }

    private String getTrimmedText(EditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private String getNetworkErrorMessage(Throwable throwable) {
        return (throwable == null || throwable.getMessage() == null) ? "Network error occurred." : "Network error: " + throwable.getMessage();
    }

    private <T> String extractErrorMessage(Response<ApiResponse<T>> response) {
        if (response == null) return "Something went wrong.";
        try {
            if (response.errorBody() != null) {
                String errorJson = response.errorBody().string();
                if (!TextUtils.isEmpty(errorJson)) {
                    ApiResponse<?> errorResponse = gson.fromJson(errorJson, ApiResponse.class);
                    if (errorResponse != null) {
                        String firstError = errorResponse.getFirstErrorMessage();
                        if (!TextUtils.isEmpty(firstError)) return firstError;
                        if (!TextUtils.isEmpty(errorResponse.getMessage())) return errorResponse.getMessage();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "Request failed. Code: " + response.code();
    }
}
