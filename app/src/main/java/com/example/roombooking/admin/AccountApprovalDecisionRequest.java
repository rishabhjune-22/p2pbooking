package com.example.roombooking.admin;

import com.google.gson.annotations.SerializedName;

public class AccountApprovalDecisionRequest {

    @SerializedName("remarks")
    private final String remarks;

    public AccountApprovalDecisionRequest(String remarks) {
        this.remarks = remarks;
    }
}
