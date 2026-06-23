package com.example.roombooking.admin;

import com.google.gson.annotations.SerializedName;

public class AccountRequestItem {

    @SerializedName("id")
    private int id;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("role")
    private String role;

    @SerializedName("approval_status")
    private String approvalStatus;

    @SerializedName("designation")
    private String designation;

    @SerializedName("department")
    private String department;

    @SerializedName("mobile")
    private String mobile;

    @SerializedName("approved_by")
    private Integer approvedBy;

    @SerializedName("approved_by_name")
    private String approvedByName;

    @SerializedName("approved_at")
    private String approvedAt;

    @SerializedName("remarks")
    private String remarks;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public String getName() {
        return clean(name);
    }

    public String getEmail() {
        return clean(email);
    }

    public String getRole() {
        return clean(role);
    }

    public String getApprovalStatus() {
        return clean(approvalStatus);
    }

    public String getDesignation() {
        return clean(designation);
    }

    public String getDepartment() {
        return clean(department);
    }

    public String getMobile() {
        return clean(mobile);
    }

    public Integer getApprovedBy() {
        return approvedBy;
    }

    public String getApprovedByName() {
        return clean(approvedByName);
    }

    public String getApprovedAt() {
        return clean(approvedAt);
    }

    public String getRemarks() {
        return clean(remarks);
    }

    public String getCreatedAt() {
        return clean(createdAt);
    }

    public String getUpdatedAt() {
        return clean(updatedAt);
    }

    private static String clean(String value) {
        return value != null ? value.trim() : "";
    }
}
