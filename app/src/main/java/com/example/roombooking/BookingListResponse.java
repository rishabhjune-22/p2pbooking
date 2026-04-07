package com.example.roombooking;

import java.util.List;

public class BookingListResponse {
    private int count;
    private String next;
    private String previous;
    private List<BookingItem> results;

    public int getCount() {
        return count;
    }

    public String getNext() {
        return next;
    }

    public String getPrevious() {
        return previous;
    }

    public List<BookingItem> getResults() {
        return results;
    }
}