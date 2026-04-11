package com.example.roombooking.security;

import com.google.gson.annotations.SerializedName;

public class KdfMetadata {

    @SerializedName("salt")
    private String salt;

    @SerializedName("iterations")
    private int iterations;

    @SerializedName("key_length_bits")
    private int keyLengthBits;

    public KdfMetadata(String salt, int iterations, int keyLengthBits) {
        this.salt = salt;
        this.iterations = iterations;
        this.keyLengthBits = keyLengthBits;
    }

    public String getSalt() { return salt; }
    public int getIterations() { return iterations; }
    public int getKeyLengthBits() { return keyLengthBits; }
}