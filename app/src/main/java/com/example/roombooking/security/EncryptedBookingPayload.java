package com.example.roombooking.security;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class EncryptedBookingPayload {

    // ---- Visitor Info ----
    @SerializedName("visitor_name")
    private final String visitorName;

    @SerializedName("visitor_designation")
    private final String visitorDesignation;

    @SerializedName("visitor_organisation")
    private final String visitorOrganisation;

    @SerializedName("visitor_gender")
    private final String visitorGender;

    @SerializedName("visitor_address")
    private final String visitorAddress;

    @SerializedName("visitor_mobile")
    private final String visitorMobile;

    @SerializedName("visitor_email")
    private final String visitorEmail;

    // ---- Booking Info (sensitive) ----
    @SerializedName("purpose_of_visit")
    private final String purposeOfVisit;

    // ---- Versioning (important for future migrations) ----
    @SerializedName("schema_version")
    private final int schemaVersion;

    public EncryptedBookingPayload(
            String visitorName,
            String visitorDesignation,
            String visitorOrganisation,
            String visitorGender,
            String visitorAddress,
            String visitorMobile,
            String visitorEmail,
            String purposeOfVisit
    ) {
        this.visitorName = visitorName;
        this.visitorDesignation = visitorDesignation;
        this.visitorOrganisation = visitorOrganisation;
        this.visitorGender = visitorGender;
        this.visitorAddress = visitorAddress;
        this.visitorMobile = visitorMobile;
        this.visitorEmail = visitorEmail;
        this.purposeOfVisit = purposeOfVisit;
        this.schemaVersion = 1; // fixed for now
    }

    // ---- Getters ----
    public String getVisitorName() { return visitorName; }
    public String getVisitorDesignation() { return visitorDesignation; }
    public String getVisitorOrganisation() { return visitorOrganisation; }
    public String getVisitorGender() { return visitorGender; }
    public String getVisitorAddress() { return visitorAddress; }
    public String getVisitorMobile() { return visitorMobile; }
    public String getVisitorEmail() { return visitorEmail; }
    public String getPurposeOfVisit() { return purposeOfVisit; }
    public int getSchemaVersion() { return schemaVersion; }

    // ---- JSON Helpers ----
    public String toJson(Gson gson) {
        return gson.toJson(this);
    }

    public static EncryptedBookingPayload fromJson(String json, Gson gson) {
        EncryptedBookingPayload payload =
                gson.fromJson(json, EncryptedBookingPayload.class);

        // Safety fallback for backward compatibility
        if (payload == null) {
            return new EncryptedBookingPayload(
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    ""
            );
        }

        return payload;
    }
}