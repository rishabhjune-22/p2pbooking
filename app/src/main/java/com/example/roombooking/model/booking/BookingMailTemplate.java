package com.example.roombooking.model.booking;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class BookingMailTemplate {

    @SerializedName("subject")
    @Nullable
    private String subject;

    @SerializedName("body")
    @Nullable
    private String body;

    @SerializedName("html")
    @Nullable
    private String html;

    @Nullable
    public String getSubject() {
        return subject;
    }

    @Nullable
    public String getBody() {
        return body;
    }

    @Nullable
    public String getHtml() {
        return html;
    }

    public String getSafeSubject() {
        return subject != null ? subject : "";
    }

    public String getSafeBody() {
        return body != null ? body : "";
    }

    public String getSafeHtml() {
        return html != null ? html : "";
    }

    public boolean hasContent() {
        return !getSafeSubject().trim().isEmpty()
                || !getSafeBody().trim().isEmpty()
                || !getSafeHtml().trim().isEmpty();
    }
}
