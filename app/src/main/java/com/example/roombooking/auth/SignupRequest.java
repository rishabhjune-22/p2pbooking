package com.example.roombooking.auth;

import com.example.roombooking.security.CryptoManager;
import com.example.roombooking.security.KdfMetadata;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public class SignupRequest {

    @SerializedName("username")
    private final String username;

    @SerializedName("email")
    private final String email;

    @SerializedName("password")
    private final String password;

    @SerializedName("encrypted_dek")
    private final String encryptedDek;

    @SerializedName("dek_wrap_nonce")
    private final String dekWrapNonce;

    @SerializedName("kdf_metadata")
    private final CryptoManager.KdfMetadata kdfMetadata;
    public SignupRequest(
            String username,
            String email,
            String password,
            String encryptedDek,
            String dekWrapNonce,
            CryptoManager.KdfMetadata kdfMetadata
    ) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.encryptedDek = encryptedDek;
        this.dekWrapNonce = dekWrapNonce;
        this.kdfMetadata = kdfMetadata;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getEncryptedDek() {
        return encryptedDek;
    }

    public String getDekWrapNonce() {
        return dekWrapNonce;
    }

    public CryptoManager.KdfMetadata getKdfMetadata() {
        return kdfMetadata;
    }
}