package com.example.roombooking.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class KeystoreBackedCryptoSessionManager {

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "room_booking_local_dek_wrap_key";
    private static final String PREF_NAME = "crypto_session_prefs";
    private static final String PREF_ENCRYPTED_DEK = "pref_encrypted_dek";
    private static final String PREF_DEK_NONCE = "pref_dek_nonce";

    private static final String AES_MODE = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private static volatile KeystoreBackedCryptoSessionManager instance;

    private final Context appContext;
    private SecretKey inMemoryDek;

    private KeystoreBackedCryptoSessionManager(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public static KeystoreBackedCryptoSessionManager getInstance(Context context) {
        if (instance == null) {
            synchronized (KeystoreBackedCryptoSessionManager.class) {
                if (instance == null) {
                    instance = new KeystoreBackedCryptoSessionManager(context);
                }
            }
        }
        return instance;
    }

    public synchronized void setDek(SecretKey dek) throws Exception {
        clearInMemoryOnly();
        this.inMemoryDek = cloneKey(dek);
        persistDekLocally(dek);
    }

    public synchronized SecretKey getDek() {
        if (inMemoryDek == null) {
            return null;
        }
        return cloneKey(inMemoryDek);
    }

    public synchronized boolean isUnlocked() {
        return inMemoryDek != null;
    }

    public synchronized boolean restoreDekFromLocalStore() {
        try {
            SecretKey restored = readPersistedDek();
            if (restored == null) {
                return false;
            }
            clearInMemoryOnly();
            inMemoryDek = restored;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public synchronized void clearAll() {
        clearInMemoryOnly();
        clearPersistedDek();
    }

    public synchronized void clearInMemoryOnly() {
        if (inMemoryDek != null && inMemoryDek.getEncoded() != null) {
            byte[] bytes = inMemoryDek.getEncoded();
            Arrays.fill(bytes, (byte) 0);
        }
        inMemoryDek = null;
    }

    private void persistDekLocally(SecretKey dek) throws Exception {
        SecretKey keystoreKey = getOrCreateKeystoreKey();

        Cipher cipher = Cipher.getInstance(AES_MODE);
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey);

        byte[] dekBytes = dek.getEncoded();
        byte[] encryptedDek = cipher.doFinal(dekBytes);
        byte[] nonce = cipher.getIV();

        getPrefs().edit()
                .putString(PREF_ENCRYPTED_DEK, Base64.encodeToString(encryptedDek, Base64.NO_WRAP))
                .putString(PREF_DEK_NONCE, Base64.encodeToString(nonce, Base64.NO_WRAP))
                .apply();
    }

    private SecretKey readPersistedDek() throws Exception {
        String encryptedDekBase64 = getPrefs().getString(PREF_ENCRYPTED_DEK, null);
        String nonceBase64 = getPrefs().getString(PREF_DEK_NONCE, null);

        if (encryptedDekBase64 == null || nonceBase64 == null) {
            return null;
        }

        SecretKey keystoreKey = getOrCreateKeystoreKey();

        byte[] encryptedDek = Base64.decode(encryptedDekBase64, Base64.NO_WRAP);
        byte[] nonce = Base64.decode(nonceBase64, Base64.NO_WRAP);

        Cipher cipher = Cipher.getInstance(AES_MODE);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce);
        cipher.init(Cipher.DECRYPT_MODE, keystoreKey, spec);

        byte[] dekBytes = cipher.doFinal(encryptedDek);
        return new SecretKeySpec(dekBytes, "AES");
    }

    private void clearPersistedDek() {
        getPrefs().edit()
                .remove(PREF_ENCRYPTED_DEK)
                .remove(PREF_DEK_NONCE)
                .apply();
    }

    private SharedPreferences getPrefs() {
        return appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private SecretKey getOrCreateKeystoreKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);

        if (keyStore.containsAlias(KEY_ALIAS)) {
            KeyStore.SecretKeyEntry entry =
                    (KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null);
            return entry.getSecretKey();
        }

        KeyGenerator keyGenerator =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);

        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build();

        keyGenerator.init(spec);
        return keyGenerator.generateKey();
    }

    private SecretKey cloneKey(SecretKey source) {
        if (source == null || source.getEncoded() == null) {
            return null;
        }
        byte[] original = source.getEncoded();
        byte[] copy = Arrays.copyOf(original, original.length);
        return new SecretKeySpec(copy, source.getAlgorithm());
    }
}