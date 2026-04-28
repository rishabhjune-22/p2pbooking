package com.example.roombooking.security;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import android.util.Log;
public class CryptoManager {

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";

    private static final int AES_KEY_SIZE_BITS = 256;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_NONCE_LENGTH_BYTES = 12;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int PBKDF2_ITERATIONS = 120_000;

    private final SecureRandom secureRandom = new SecureRandom();

    public static class KdfMetadata {
        private final String salt;
        private final int iterations;
        private final int key_length_bits;

        public KdfMetadata(String salt, int iterations, int keyLengthBits) {
            this.salt = salt;
            this.iterations = iterations;
            this.key_length_bits = keyLengthBits;
        }

        public String getSalt() {
            return salt;
        }

        public int getIterations() {
            return iterations;
        }

        public int getKeyLengthBits() {
            return key_length_bits;
        }
    }

    public static class WrappedDekResult {
        private final String encryptedDekBase64;
        private final String dekWrapNonceBase64;
        private final KdfMetadata kdfMetadata;

        public WrappedDekResult(String encryptedDekBase64, String dekWrapNonceBase64, KdfMetadata kdfMetadata) {
            this.encryptedDekBase64 = encryptedDekBase64;
            this.dekWrapNonceBase64 = dekWrapNonceBase64;
            this.kdfMetadata = kdfMetadata;
        }

        public String getEncryptedDekBase64() {
            return encryptedDekBase64;
        }

        public String getDekWrapNonceBase64() {
            return dekWrapNonceBase64;
        }

        public KdfMetadata getKdfMetadata() {
            return kdfMetadata;
        }
    }

    public static class EncryptionResult {
        private final String ciphertextBase64;
        private final String nonceBase64;

        public EncryptionResult(String ciphertextBase64, String nonceBase64) {
            this.ciphertextBase64 = ciphertextBase64;
            this.nonceBase64 = nonceBase64;
        }

        public String getCiphertextBase64() {
            return ciphertextBase64;
        }

        public String getNonceBase64() {
            return nonceBase64;
        }
    }

    public SecretKey generateDek() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
        keyGenerator.init(AES_KEY_SIZE_BITS, secureRandom);
        return keyGenerator.generateKey();
    }

    public WrappedDekResult createAndWrapDek(char[] passphrase) throws Exception {


        byte[] salt = generateRandomBytes(SALT_LENGTH_BYTES);

        Log.d("CRYPTO_DEBUG", "Passphrase: " + new String(passphrase));
        Log.d("CRYPTO_DEBUG", "Salt (Base64): " + base64Encode(salt));

        KdfMetadata metadata = new KdfMetadata(
                base64Encode(salt),
                PBKDF2_ITERATIONS,
                AES_KEY_SIZE_BITS
        );
        Log.d("CRYPTO_DEBUG", "Salt (Base64): " + metadata.getSalt());

        Log.d("CRYPTO_DEBUG", "PBKDF2 Iterations: " +
                metadata.getIterations());

        Log.d("CRYPTO_DEBUG", "AES Key Length: " +
                metadata.getKeyLengthBits());

        Log.d("CRYPTO_DEBUG", "KDF metadata: " + metadata.toString());


        SecretKey dek = generateDek();

        Log.d("CRYPTO_DEBUG",
                "Generated DEK: " +
                        base64Encode(dek.getEncoded()));

        SecretKey kek = deriveKek(passphrase, salt, PBKDF2_ITERATIONS, AES_KEY_SIZE_BITS);
        Log.d("CRYPTO_DEBUG",
                "Generated KEK: " +
                        base64Encode(kek.getEncoded()));

        byte[] wrapNonce = generateRandomBytes(GCM_NONCE_LENGTH_BYTES);
        byte[] encryptedDek = encryptBytes(dek.getEncoded(), kek, wrapNonce);
        Log.d("CRYPTO_DEBUG",
                "Encrypted DEK: " +
                        base64Encode(encryptedDek));


        zeroizeKey(kek);

        return new WrappedDekResult(
                base64Encode(encryptedDek),
                base64Encode(wrapNonce),
                metadata
        );
    }

    public SecretKey unwrapDek(
            char[] passphrase,
            String encryptedDekBase64,
            String dekWrapNonceBase64,
            KdfMetadata metadata
    ) throws Exception {
        byte[] salt = base64Decode(metadata.getSalt());
        SecretKey kek = deriveKek(
                passphrase,
                salt,
                metadata.getIterations(),
                metadata.getKeyLengthBits()
        );
        Log.d("CRYPTO_DEBUG", "Derived KEK: " + base64Encode(kek.getEncoded()));

        byte[] encryptedDek = base64Decode(encryptedDekBase64);
        byte[] wrapNonce = base64Decode(dekWrapNonceBase64);

        byte[] dekBytes = decryptBytes(encryptedDek, kek, wrapNonce);
        zeroizeKey(kek);

        return new SecretKeySpec(dekBytes, AES_ALGORITHM);
    }

    public EncryptionResult encryptPayload(String plaintextJson, SecretKey dek) throws Exception {
        byte[] nonce = generateRandomBytes(GCM_NONCE_LENGTH_BYTES);
        byte[] ciphertext = encryptBytes(
                plaintextJson.getBytes(StandardCharsets.UTF_8),
                dek,
                nonce
        );
        return new EncryptionResult(base64Encode(ciphertext), base64Encode(nonce));
    }

    public String decryptPayload(String ciphertextBase64, String nonceBase64, SecretKey dek) throws Exception {
        byte[] ciphertext = base64Decode(ciphertextBase64);
        byte[] nonce = base64Decode(nonceBase64);
        byte[] plaintext = decryptBytes(ciphertext, dek, nonce);
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    public SecretKey deriveKek(char[] passphrase, byte[] salt, int iterations, int keyLengthBits) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        KeySpec spec = new PBEKeySpec(passphrase, salt, iterations, keyLengthBits);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, AES_ALGORITHM);
    }

    private byte[] encryptBytes(byte[] plaintext, SecretKey key, byte[] nonce) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        return cipher.doFinal(plaintext);
    }

    private byte[] decryptBytes(byte[] ciphertext, SecretKey key, byte[] nonce) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        return cipher.doFinal(ciphertext);
    }

    private byte[] generateRandomBytes(int length) {
        byte[] out = new byte[length];
        secureRandom.nextBytes(out);
        return out;
    }

    private String base64Encode(byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    private byte[] base64Decode(String value) {
        return Base64.decode(value, Base64.NO_WRAP);
    }

    private void zeroizeKey(SecretKey key) {
        if (key == null || key.getEncoded() == null) {
            return;
        }
        byte[] bytes = key.getEncoded();
        Arrays.fill(bytes, (byte) 0);
    }
}