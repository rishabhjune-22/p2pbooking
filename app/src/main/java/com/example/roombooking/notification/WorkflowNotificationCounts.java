package com.example.roombooking.notification;

import com.google.gson.annotations.SerializedName;

public class WorkflowNotificationCounts {
    @SerializedName("booking_requests") private int bookingRequests;
    @SerializedName("requester_accounts") private int requesterAccounts;
    @SerializedName("admin_accounts") private int adminAccounts;
    @SerializedName("my_requests") private int myRequests;
    @SerializedName("total") private int total;

    public int getBookingRequests() { return bookingRequests; }
    public int getRequesterAccounts() { return requesterAccounts; }
    public int getAdminAccounts() { return adminAccounts; }
    public int getMyRequests() { return myRequests; }
    public int getTotal() { return total; }
}
