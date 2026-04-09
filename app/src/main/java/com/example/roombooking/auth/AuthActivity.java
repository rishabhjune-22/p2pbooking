package com.example.roombooking.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
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
import com.google.gson.Gson;
import com.example.roombooking.auth.SignupRequest;
import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etUser;
    private EditText etPass;
    private Button btnPrimary;
    private TextView tvToggle;
    private TextView tvMsg;

    private boolean isSignup = false;

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
    }

    private void initObjects() {
        sessionManager = new SessionManager(this);
        gson = new Gson();
    }

    private void setListeners() {
        btnPrimary.setOnClickListener(v -> onPrimaryClicked());
        tvToggle.setOnClickListener(v -> toggleMode());
    }

    private void toggleMode() {
        isSignup = !isSignup;
        renderMode();
        clearInputs();
        showMessage("");
    }

    private void renderMode() {
        etEmail.setVisibility(isSignup ? View.VISIBLE : View.GONE);
        btnPrimary.setText(isSignup ? "Signup" : "Login");
        tvToggle.setText(isSignup ? "Already have an account? Login" : "No account? Signup");
    }

    private void clearInputs() {
        etEmail.setText("");
        etUser.setText("");
        etPass.setText("");
    }

    private void onPrimaryClicked() {
        String username = getTrimmedText(etUser).toLowerCase();
        String password = getTrimmedText(etPass);

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

            doSignup(username, email, password);
        } else {
            doLogin(username, password);
        }
    }

    private void doSignup(String username, String email, String password) {
        setLoading(true);
        showMessage("");

        SignupRequest request = new SignupRequest(username, email, password);

        AuthRepository repo = new AuthRepository(this);
        repo.signup(request).enqueue(new Callback<ApiResponse<SignupData>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<ApiResponse<SignupData>> call,
                            @NonNull Response<ApiResponse<SignupData>> response
                    ) {
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
                    public void onFailure(
                            @NonNull Call<ApiResponse<SignupData>> call,
                            @NonNull Throwable t
                    ) {
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
                    public void onResponse(
                            @NonNull Call<ApiResponse<LoginData>> call,
                            @NonNull Response<ApiResponse<LoginData>> response
                    ) {
                        setLoading(false);

                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<LoginData> apiResponse = response.body();

                            if (!apiResponse.isSuccess() || apiResponse.getData() == null) {
                                showMessage(apiResponse.getFirstErrorMessage());
                                return;
                            }

                            LoginData loginData = apiResponse.getData();
                            String accessToken = loginData.getAccessToken();
                            String refreshToken = loginData.getRefreshToken();
                            UserData userData = loginData.getUser();

                            if (TextUtils.isEmpty(accessToken) || TextUtils.isEmpty(refreshToken)) {
                                showMessage("Login failed: token missing.");
                                return;
                            }

                            sessionManager.saveSession(
                                    accessToken,
                                    refreshToken,
                                    userData != null ? userData.getUsername() : username,
                                    userData != null ? userData.getEmail() : null
                            );

                            showMessage(apiResponse.getMessage());
                            navigateToHome();
                            return;
                        }

                        showMessage(extractErrorMessage(response));
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<ApiResponse<LoginData>> call,
                            @NonNull Throwable t
                    ) {
                        setLoading(false);
                        showMessage(getNetworkErrorMessage(t));
                    }
                });
    }

    private void refreshAccessToken() {
        String refreshToken = sessionManager.getRefreshToken();

        if (TextUtils.isEmpty(refreshToken)) {
            showMessage("Session expired. Please login again.");
            return;
        }

        RefreshRequest request = new RefreshRequest(refreshToken);

        RetrofitClient.getApiService(this).refreshToken(request)
                .enqueue(new Callback<ApiResponse<RefreshTokenData>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<ApiResponse<RefreshTokenData>> call,
                            @NonNull Response<ApiResponse<RefreshTokenData>> response
                    ) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<RefreshTokenData> apiResponse = response.body();

                            if (apiResponse.isSuccess()
                                    && apiResponse.getData() != null
                                    && !TextUtils.isEmpty(apiResponse.getData().getAccessToken())) {

                                sessionManager.updateAccessToken(
                                        apiResponse.getData().getAccessToken()
                                );
                                showMessage("Token refreshed successfully");
                                return;
                            }
                        }

                        sessionManager.logout();
                        showMessage("Session expired. Please login again.");
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<ApiResponse<RefreshTokenData>> call,
                            @NonNull Throwable t
                    ) {
                        showMessage(getNetworkErrorMessage(t));
                    }
                });
    }

    private void navigateToHome() {
        Intent intent = new Intent(AuthActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        btnPrimary.setEnabled(!loading);
        tvToggle.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etUser.setEnabled(!loading);
        etPass.setEnabled(!loading);

        btnPrimary.setText(loading ? "Please wait..." : (isSignup ? "Signup" : "Login"));
    }

    private void showMessage(String message) {
        tvMsg.setText(message != null ? message : "");
    }

    private String getTrimmedText(EditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private String getNetworkErrorMessage(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().trim().isEmpty()) {
            return "Network error occurred.";
        }
        return "Network error: " + throwable.getMessage();
    }

    private <T> String extractErrorMessage(Response<ApiResponse<T>> response) {
        if (response == null) {
            return "Something went wrong.";
        }

        try {
            if (response.errorBody() != null) {
                String errorJson = response.errorBody().string();

                if (!TextUtils.isEmpty(errorJson)) {
                    ApiResponse<?> errorResponse = gson.fromJson(errorJson, ApiResponse.class);

                    if (errorResponse != null) {
                        String firstError = errorResponse.getFirstErrorMessage();
                        if (!TextUtils.isEmpty(firstError)) {
                            return firstError;
                        }

                        if (!TextUtils.isEmpty(errorResponse.getMessage())) {
                            return errorResponse.getMessage();
                        }
                    }
                }
            }
        } catch (IOException ignored) {
        } catch (Exception ignored) {
        }

        return "Request failed. Code: " + response.code();
    }
}