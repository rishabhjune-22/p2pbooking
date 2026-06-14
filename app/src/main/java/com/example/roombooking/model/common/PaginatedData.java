package com.example.roombooking.model.common;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PaginatedData<T> {

    @SerializedName("count")
    private int count;

    @SerializedName("next")
    @Nullable
    private String next;

    @SerializedName("previous")
    @Nullable
    private String previous;

    @SerializedName("results")
    @Nullable
    private List<T> results;

    public int getCount() {
        return count;
    }

    @Nullable
    public String getNext() {
        return next;
    }

    @Nullable
    public String getPrevious() {
        return previous;
    }

    @Nullable
    public List<T> getResults() {
        return results;
    }

    public boolean hasResults() {
        return results != null && !results.isEmpty();
    }

    public boolean hasNextPage() {
        return next != null && !next.trim().isEmpty();
    }

    public boolean hasPreviousPage() {
        return previous != null && !previous.trim().isEmpty();
    }
}