package com.example.roombooking.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.roombooking.home.HomeActivity;
import com.example.roombooking.R;
import com.example.roombooking.api.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthActivity extends AppCompatActivity {

    private EditText etEmail, etUser, etPass;
    private Button btnPrimary;
    private TextView tvToggle, tvMsg;

    private boolean isSignup = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        etEmail = findViewById(R.id.etEmail);
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
        clearInputs();
    }

    private void renderMode() {
        etEmail.setVisibility(isSignup ? android.view.View.VISIBLE : android.view.View.GONE);
        btnPrimary.setText(isSignup ? "Signup" : "Login");
        tvToggle.setText(isSignup ? "Already have an account? Login" : "No account? Signup");
    }

    private void clearInputs() {
        etEmail.setText("");
        etUser.setText("");
        etPass.setText("");
    }

    private void onPrimary() {
        String username = etUser.getText().toString().trim().toLowerCase();
        String password = etPass.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            tvMsg.setText("Fill all required fields.");
            return;
        }

        if (isSignup) {
            String email = etEmail.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                tvMsg.setText("Enter email.");
                return;
            }

            doSignup(username, email, password);
        } else {
            doLogin(username, password);
        }
    }

    private void doSignup(String username, String email, String password) {
        setLoading(true);
        tvMsg.setText("");

        SignupRequest request = new SignupRequest(username, email, password);

        RetrofitClient.getApiService(this).signup(request).enqueue(new retrofit2.Callback<SignupResponse>() {
            @Override
            public void onResponse(retrofit2.Call<SignupResponse> call,
                                   retrofit2.Response<SignupResponse> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    SignupResponse signupResponse = response.body();

                    if (signupResponse.getUser() != null) {
                        tvMsg.setText(signupResponse.getMessage());

                        tvMsg.setText("Signup successful. Logging in...");
                        doLogin(username, password);
                    } else {
                        tvMsg.setText("Signup succeeded but user data missing.");
                    }

                } else if (response.code() == 400) {
                    try {
                        String errorJson = response.errorBody() != null ? response.errorBody().string() : null;

                        if (errorJson != null && !errorJson.isEmpty()) {
                            com.google.gson.Gson gson = new com.google.gson.Gson();
                            SignupErrorResponse errorResponse = gson.fromJson(errorJson, SignupErrorResponse.class);

                            String parsedMessage = parseSignupErrors(errorResponse);
                            tvMsg.setText(parsedMessage);
                        } else {
                            tvMsg.setText("Signup failed.");
                        }
                    } catch (Exception e) {
                        tvMsg.setText("Signup failed.");
                    }

                } else {
                    tvMsg.setText("Signup failed. Code: " + response.code());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<SignupResponse> call, Throwable t) {
                setLoading(false);
                tvMsg.setText("Network error: " + t.getMessage());
            }
        });
    }

    private void doLogin(String username, String password) {
        setLoading(true);
        tvMsg.setText("");

        LoginRequest request = new LoginRequest(username, password);

        RetrofitClient.getApiService(this).login(request).enqueue(new Callback<TokenResponse>() {
            @Override
            public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    TokenResponse tokenResponse = response.body();

                    String access = tokenResponse.getAccess();
                    String refresh = tokenResponse.getRefresh();

                    if (access != null && !access.isEmpty()) {
                        SessionManager sessionManager = new SessionManager(AuthActivity.this);
                        sessionManager.saveTokens(access, refresh);
                        sessionManager.saveUserInfo(username, null);

                        tvMsg.setText("Login successful.");

                        startActivity(new Intent(AuthActivity.this, HomeActivity.class));
                        finish();
                    } else {
                        tvMsg.setText("Login failed: token missing.");
                    }
                } else if (response.code() == 401) {
                    tvMsg.setText("Invalid username or password.");
                } else {
                    tvMsg.setText("Login failed. Code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<TokenResponse> call, Throwable t) {
                setLoading(false);
                tvMsg.setText("Network error: " + t.getMessage());
            }
        });
    }

    private void setLoading(boolean loading) {
        btnPrimary.setEnabled(!loading);
        tvToggle.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etUser.setEnabled(!loading);
        etPass.setEnabled(!loading);

        btnPrimary.setText(loading ? "Please wait..." : (isSignup ? "Signup" : "Login"));
    }



    private String parseSignupErrors(SignupErrorResponse errorResponse) {
        if (errorResponse == null) {
            return "Signup failed.";
        }

        if (errorResponse.getErrors() == null || errorResponse.getErrors().isEmpty()) {
            return errorResponse.getMessage() != null ? errorResponse.getMessage() : "Signup failed.";
        }

        StringBuilder sb = new StringBuilder();

        for (java.util.Map.Entry<String, java.util.List<String>> entry : errorResponse.getErrors().entrySet()) {
            java.util.List<String> messages = entry.getValue();
            if (messages != null && !messages.isEmpty()) {
                sb.append(messages.get(0)).append("\n");
            }
        }

        String result = sb.toString().trim();
        return result.isEmpty() ? "Signup failed." : result;
    }



    private void refreshAccessToken() {
        SessionManager sessionManager = new SessionManager(this);
        String refreshToken = sessionManager.getRefreshToken();

        if (refreshToken == null) {
            tvMsg.setText("Session expired. Please login again.");
            return;
        }

        RefreshRequest request = new RefreshRequest(refreshToken);

        RetrofitClient.getApiService(this).refreshToken(request)
                .enqueue(new retrofit2.Callback<RefreshResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<RefreshResponse> call,
                                           retrofit2.Response<RefreshResponse> response) {

                        if (response.isSuccessful() && response.body() != null) {
                            String newAccess = response.body().getAccess();

                            sessionManager.updateAccessToken(newAccess);

                            tvMsg.setText("Token refreshed successfully");

                        } else {
                            sessionManager.logout();
                            tvMsg.setText("Session expired. Please login again.");
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<RefreshResponse> call, Throwable t) {
                        tvMsg.setText("Refresh failed: " + t.getMessage());
                    }
                });
    }
}