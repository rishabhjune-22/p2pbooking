package com.example.roombooking.security;

import com.google.gson.annotations.SerializedName;

public class EncryptionMaterialData {

    @SerializedName("encrypted_dek")
    private String encryptedDek;

    @SerializedName("dek_wrap_nonce")
    private String dekWrapNonce;

    @SerializedName("kdf_metadata")
    private CryptoManager.KdfMetadata kdfMetadata;

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