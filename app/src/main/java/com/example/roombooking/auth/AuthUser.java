package com.example.roombooking.auth;

import com.google.gson.annotations.SerializedName;

public class AuthUser {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("role")
    private String role;

    @SerializedName("approval_status")
    private String approvalStatus;

    @SerializedName("remarks")
    private String remarks;

    @SerializedName("designation")
    private String designation;

    @SerializedName("department")
    private String department;

    @SerializedName("mobile")
    private String mobile;

    public int getId() {
        return id;
    }

    public String getName() {
        return name != null ? name : "";
    }

    public String getEmail() {
        return email != null ? email : "";
    }

    public String getRole() {
        return role != null ? role.trim() : "";
    }

    public String getApprovalStatus() {
        return approvalStatus != null ? approvalStatus.trim() : "";
    }

    public String getRemarks() {
        return remarks != null ? remarks.trim() : "";
    }

    public String getDesignation() {
        return designation != null ? designation : "";
    }

    public String getDepartment() {
        return department != null ? department : "";
    }

    public String getMobile() {
        return mobile != null ? mobile : "";
    }
}
