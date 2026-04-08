package com.example.roombooking.booking;

import java.io.Serializable;

public class BookingItem implements Serializable {
    private int id;
    private String room_name;
    private String created_by_username;
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
    private String status;
    private int room;

    public int getId() { return id; }
    public String getRoom_name() { return room_name; }
    public String getCreated_by_username() { return created_by_username; }
    public String getVisitor_name() { return visitor_name; }
    public String getVisitor_designation() { return visitor_designation; }
    public String getVisitor_organisation() { return visitor_organisation; }
    public String getVisitor_gender() { return visitor_gender; }
    public String getVisitor_address() { return visitor_address; }
    public String getVisitor_mobile() { return visitor_mobile; }
    public String getVisitor_email() { return visitor_email; }
    public String getArrival_date() { return arrival_date; }
    public String getArrival_time() { return arrival_time; }
    public String getDeparture_date() { return departure_date; }
    public String getDeparture_time() { return departure_time; }
    public String getPurpose_of_visit() { return purpose_of_visit; }
    public String getRequestee_name() { return requestee_name; }
    public String getRequestee_designation() { return requestee_designation; }
    public String getRequestee_department() { return requestee_department; }
    public String getRequestee_mobile() { return requestee_mobile; }
    public String getLogistics_name() { return logistics_name; }
    public String getLogistics_designation() { return logistics_designation; }
    public String getLogistics_mobile() { return logistics_mobile; }
    public String getStatus() { return status; }
    public int getRoom() { return room; }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setVisitor_name(String visitor_name) {
        this.visitor_name = visitor_name;
    }

    public void setVisitor_mobile(String visitor_mobile) {
        this.visitor_mobile = visitor_mobile;
    }

    public void setPurpose_of_visit(String purpose_of_visit) {
        this.purpose_of_visit = purpose_of_visit;
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
}