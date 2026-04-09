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

    public List<T> getResults() {
        return results;
    }
}