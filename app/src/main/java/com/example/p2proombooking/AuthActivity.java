package com.example.p2proombooking;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.util.UUID;

public class AuthActivity extends AppCompatActivity {
    private static final int TAP_SLOP_DP = 8;

    private EditText etName, etUser, etPass, etConfirmPass;
    private Button btnPrimary;
    private TextView tvToggle, tvMsg, tvTitleSubtitle;
    private ImageView ivTopBackground;
    private ImageButton btnInfo;
    private CardView cvInfo;
    private ScrollView rootScrollView;
    private float downRawX;
    private float downRawY;

    private boolean isSignup = false;
    private boolean isPassVisible = false;
    private boolean isConfirmPassVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_page);

        etName = findViewById(R.id.etName);
        etUser = findViewById(R.id.etUser);
        etPass = findViewById(R.id.etPass);
        etConfirmPass = findViewById(R.id.etConfirmPass);
        btnPrimary = findViewById(R.id.btnPrimary);
        tvToggle = findViewById(R.id.tvToggle);
        tvMsg = findViewById(R.id.tvMsg);
        tvTitleSubtitle = findViewById(R.id.textView2);
        ivTopBackground = findViewById(R.id.ivTopBackground);
        btnInfo = findViewById(R.id.btnInfo);
        cvInfo = findViewById(R.id.cvInfo);
        rootScrollView = findViewById(R.id.rootScrollView);

        btnPrimary.setOnClickListener(v -> onPrimary());
        tvToggle.setOnClickListener(v -> toggleMode());
        btnInfo.setOnClickListener(v -> toggleInfo());

        setupPasswordVisibilityToggle(etPass, true);
        setupPasswordVisibilityToggle(etConfirmPass, false);
        setupKeyboardAwareScroll(etName);
        setupKeyboardAwareScroll(etUser);
        setupKeyboardAwareScroll(etPass);
        setupKeyboardAwareScroll(etConfirmPass);

        renderMode();
    }

    private void toggleInfo() {
        if (cvInfo != null) {
            int visibility = cvInfo.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
            cvInfo.setVisibility(visibility);
        }
    }

    private void setupKeyboardAwareScroll(View targetView) {
        targetView.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus || rootScrollView == null) {
                return;
            }

            rootScrollView.post(() -> rootScrollView.smoothScrollTo(0, Math.max(0, v.getBottom() - rootScrollView.getHeight() / 3)));
        });
    }

    private void setupPasswordVisibilityToggle(EditText editText, boolean isMainPass) {
        editText.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (editText.getCompoundDrawables()[DRAWABLE_RIGHT] != null &&
                    event.getRawX() >= (editText.getRight() - editText.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - editText.getPaddingEnd())) {
                    togglePasswordVisibility(editText, isMainPass);
                    return true;
                }
            }
            return false;
        });
    }

    private void togglePasswordVisibility(EditText editText, boolean isMainPass) {
        boolean isVisible = isMainPass ? isPassVisible : isConfirmPassVisible;
        isVisible = !isVisible;

        if (isMainPass) isPassVisible = isVisible;
        else isConfirmPassVisible = isVisible;

        if (isVisible) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            editText.setCompoundDrawablesWithIntrinsicBounds(
                    isMainPass ? R.drawable.lock_01 : R.drawable.password_svgrepo_com,
                    0, R.drawable.eye_password_show_svgrepo_com, 0);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            editText.setCompoundDrawablesWithIntrinsicBounds(
                    isMainPass ? R.drawable.lock_01 : R.drawable.password_svgrepo_com,
                    0, R.drawable.eye_password_hide_svgrepo_com, 0);
        }
        editText.setSelection(editText.getText().length());
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            downRawX = event.getRawX();
            downRawY = event.getRawY();
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            View v = getCurrentFocus();
            if (v instanceof EditText && isTap(event) && isTouchOutsideView(v, event)) {
                v.clearFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private boolean isTap(MotionEvent event) {
        float touchSlop = TAP_SLOP_DP * getResources().getDisplayMetrics().density;
        return Math.abs(event.getRawX() - downRawX) < touchSlop
                && Math.abs(event.getRawY() - downRawY) < touchSlop;
    }

    private boolean isTouchOutsideView(View view, MotionEvent event) {
        Rect outRect = new Rect();
        view.getGlobalVisibleRect(outRect);
        return !outRect.contains((int) event.getRawX(), (int) event.getRawY());
    }

    private void toggleMode() {
        isSignup = !isSignup;
        renderMode();
        if (tvMsg != null) tvMsg.setText("");
        if (cvInfo != null) cvInfo.setVisibility(View.GONE);
    }

    private void renderMode() {
        if (etName != null) {
            etName.setVisibility(isSignup ? View.VISIBLE : View.GONE);
        }
        if (etConfirmPass != null) {
            etConfirmPass.setVisibility(isSignup ? View.VISIBLE : View.GONE);
        }
        if (btnPrimary != null) {
            btnPrimary.setText(isSignup ? "Signup" : "Login");
        }
        if (tvToggle != null) {
            tvToggle.setText(isSignup ? "Already have an account? Login" : "No account? Signup");
        }
        if (tvTitleSubtitle != null) {
            tvTitleSubtitle.setText(isSignup ? "Signup To Your Account" : "Login To Your Account");
        }
        if (ivTopBackground != null) {
            ivTopBackground.setImageResource(isSignup ? R.drawable.top_background2 : R.drawable.top_background1);
        }
    }

    private void onPrimary() {
        AppDatabase db = AppDatabase.getInstance(this);
        String username = etUser.getText().toString().trim().toLowerCase();
        String pass = etPass.getText().toString();

        if (username.isEmpty() || pass.isEmpty()) {
            if (tvMsg != null) tvMsg.setText("Fill all required fields.");
            return;
        }

        if (isSignup) {
            String confirmPass = etConfirmPass.getText().toString();
            if (!pass.equals(confirmPass)) {
                if (tvMsg != null) tvMsg.setText("Passwords do not match.");
                return;
            }

            String displayName = etName.getText().toString().trim();
            if (displayName.isEmpty()) {
                if (tvMsg != null) tvMsg.setText("Enter your name.");
                return;
            }

            if (db.userDao().getByUsername(username) != null) {
                if (tvMsg != null) tvMsg.setText("User already exists.");
                return;
            }

            byte[] salt = PasswordUtils.generateSalt();
            byte[] hash = PasswordUtils.hashPassword(pass.toCharArray(), salt);

            UserEntity user = new UserEntity();
            user.userId = UUID.randomUUID().toString();
            user.username = username;
            user.displayName = displayName;
            user.passwordHash = hash;
            user.salt = salt;
            user.createdAt = System.currentTimeMillis();

            db.userDao().insert(user);

            SessionManager session = new SessionManager(this);
            session.setLoggedIn(true);
            session.setActiveUserId(user.userId);
            session.setDisplayName(user.displayName);

            goToHome();
        } else {
            UserEntity user = db.userDao().getByUsername(username);
            if (user == null) {
                if (tvMsg != null) tvMsg.setText("User not found.");
                return;
            }

            byte[] inputHash = PasswordUtils.hashPassword(pass.toCharArray(), user.salt);
            if (!PasswordUtils.verify(user.passwordHash, inputHash)) {
                if (tvMsg != null) tvMsg.setText("Wrong password.");
                return;
            }

            SessionManager session = new SessionManager(this);
            session.setLoggedIn(true);
            session.setActiveUserId(user.userId);
            session.setDisplayName(user.displayName);

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
