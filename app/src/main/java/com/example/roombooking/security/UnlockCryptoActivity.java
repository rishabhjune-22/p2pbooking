package com.example.roombooking.security;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.roombooking.R;
import com.example.roombooking.api.RetrofitClient;
import com.example.roombooking.home.HomeActivity;
import com.example.roombooking.model.common.ApiResponse;

import org.json.JSONObject;

import javax.crypto.SecretKey;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UnlockCryptoActivity extends AppCompatActivity {

    private EditText etPassphrase;
    private Button btnUnlock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unlock_crypto);

        etPassphrase = findViewById(R.id.etPassphrase);
        btnUnlock = findViewById(R.id.btnUnlock);

        btnUnlock.setOnClickListener(v -> unlockCrypto());
    }

    private void unlockCrypto() {
        String passphraseText = etPassphrase.getText() != null
                ? etPassphrase.getText().toString()
                : "";

        if (TextUtils.isEmpty(passphraseText)) {
            showToast("Enter your encryption passphrase.");
            return;
        }

        btnUnlock.setEnabled(false);
        btnUnlock.setText("Unlocking...");

        RetrofitClient.getApiService(this)
                .getEncryptionMaterial()
                .enqueue(new Callback<ApiResponse<EncryptionMaterialData>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<ApiResponse<EncryptionMaterialData>> call,
                            @NonNull Response<ApiResponse<EncryptionMaterialData>> response
                    ) {
                        btnUnlock.setEnabled(true);
                        btnUnlock.setText("Unlock");

                        if (!response.isSuccessful() || response.body() == null) {
                            showToast("Unable to fetch encryption material.");
                            return;
                        }

                        ApiResponse<EncryptionMaterialData> apiResponse = response.body();
                        if (!apiResponse.isSuccess() || apiResponse.getData() == null) {
                            showToast(apiResponse.getFirstErrorMessage());
                            return;
                        }

                        try {
                            EncryptionMaterialData data = apiResponse.getData();

                            CryptoManager cryptoManager = new CryptoManager();
                            CryptoManager.KdfMetadata metadata = data.getKdfMetadata();
                            char[] passphraseChars = passphraseText.toCharArray();

                            try {

                                SecretKey dek = cryptoManager.unwrapDek(
                                        passphraseChars,
                                        data.getEncryptedDek(),
                                        data.getDekWrapNonce(),
                                        metadata
                                );
                                Log.d("CRYPTO_DEBUG", "Decrypted Dek: " + base64Encode(dek.getEncoded()));


                                KeystoreBackedCryptoSessionManager
                                        .getInstance(getApplicationContext())
                                        .setDek(dek);

                            } finally {

                                // Zeroize passphrase from memory
                                java.util.Arrays.fill(passphraseChars, '\0');
                            }

                            etPassphrase.setText("");
                            Intent intent = new Intent(UnlockCryptoActivity.this, HomeActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();

                        } catch (Exception e) {
                            showToast("Invalid passphrase or corrupted key material.");
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<ApiResponse<EncryptionMaterialData>> call,
                            @NonNull Throwable t
                    ) {
                        btnUnlock.setEnabled(true);
                        btnUnlock.setText("Unlock");
                        showToast("Please check your internet connection.");
                    }
                });
    }
    private String base64Encode(byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}