package com.example.roombooking;

public class BookingUpdateRequest {

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
    private Integer room;

    public void setVisitor_name(String visitor_name) {
        this.visitor_name = visitor_name;
    }

    public void setVisitor_designation(String visitor_designation) {
        this.visitor_designation = visitor_designation;
    }

    public void setVisitor_organisation(String visitor_organisation) {
        this.visitor_organisation = visitor_organisation;
    }

    public void setVisitor_gender(String visitor_gender) {
        this.visitor_gender = visitor_gender;
    }

    public void setVisitor_address(String visitor_address) {
        this.visitor_address = visitor_address;
    }

    public void setVisitor_mobile(String visitor_mobile) {
        this.visitor_mobile = visitor_mobile;
    }

    public void setVisitor_email(String visitor_email) {
        this.visitor_email = visitor_email;
    }

    public void setArrival_date(String arrival_date) {
        this.arrival_date = arrival_date;
    }

    public void setArrival_time(String arrival_time) {
        this.arrival_time = arrival_time;
    }

    public void setDeparture_date(String departure_date) {
        this.departure_date = departure_date;
    }

    public void setDeparture_time(String departure_time) {
        this.departure_time = departure_time;
    }

    public void setPurpose_of_visit(String purpose_of_visit) {
        this.purpose_of_visit = purpose_of_visit;
    }

    public void setRequestee_name(String requestee_name) {
        this.requestee_name = requestee_name;
    }

    public void setRequestee_designation(String requestee_designation) {
        this.requestee_designation = requestee_designation;
    }

    public void setRequestee_department(String requestee_department) {
        this.requestee_department = requestee_department;
    }

    public void setRequestee_mobile(String requestee_mobile) {
        this.requestee_mobile = requestee_mobile;
    }

    public void setLogistics_name(String logistics_name) {
        this.logistics_name = logistics_name;
    }

    public void setLogistics_designation(String logistics_designation) {
        this.logistics_designation = logistics_designation;
    }

    public void setLogistics_mobile(String logistics_mobile) {
        this.logistics_mobile = logistics_mobile;
    }

    public void setRoom(Integer room) {
        this.room = room;
    }
}