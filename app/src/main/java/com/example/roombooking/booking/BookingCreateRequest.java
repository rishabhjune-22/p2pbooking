package com.example.roombooking.booking;

public class BookingCreateRequest {

    private String visitor_name;
    private String visitor_designation;
    private String visitor_organisation;
    private String visitor_gender;
    private String visitor_address;
    private String visitor_mobile;
    private String visitor_email;
    private String arrival_date;
    private String arrival_time;
    private String departure_date;
    private String departure_time;
    private String purpose_of_visit;
    private String requestee_name;
    private String requestee_designation;
    private String requestee_department;
    private String requestee_mobile;
    private String logistics_name;
    private String logistics_designation;
    private String logistics_mobile;
    private int room;

    public BookingCreateRequest(String visitor_name,
                                String visitor_designation,
                                String visitor_organisation,
                                String visitor_gender,
                                String visitor_address,
                                String visitor_mobile,
                                String visitor_email,
                                String arrival_date,
                                String arrival_time,
                                String departure_date,
                                String departure_time,
                                String purpose_of_visit,
                                String requestee_name,
                                String requestee_designation,
                                String requestee_department,
                                String requestee_mobile,
                                String logistics_name,
                                String logistics_designation,
                                String logistics_mobile,
                                int room) {
        this.visitor_name = visitor_name;
        this.visitor_designation = visitor_designation;
        this.visitor_organisation = visitor_organisation;
        this.visitor_gender = visitor_gender;
        this.visitor_address = visitor_address;
        this.visitor_mobile = visitor_mobile;
        this.visitor_email = visitor_email;
        this.arrival_date = arrival_date;
        this.arrival_time = arrival_time;
        this.departure_date = departure_date;
        this.departure_time = departure_time;
        this.purpose_of_visit = purpose_of_visit;
        this.requestee_name = requestee_name;
        this.requestee_designation = requestee_designation;
        this.requestee_department = requestee_department;
        this.requestee_mobile = requestee_mobile;
        this.logistics_name = logistics_name;
        this.logistics_designation = logistics_designation;
        this.logistics_mobile = logistics_mobile;
        this.room = room;
    }
}