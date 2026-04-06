package com.example.p2proombooking;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
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


    }

    private void doLogin(String username, String password) {
        setLoading(true);
        tvMsg.setText("");


    }

    private void setLoading(boolean loading) {
        btnPrimary.setEnabled(!loading);
        tvToggle.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etUser.setEnabled(!loading);
        etPass.setEnabled(!loading);

        btnPrimary.setText(loading ? "Please wait..." : (isSignup ? "Signup" : "Login"));
    }


}