package com.example.roombooking.model.booking;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class BookingEditHistoryItem implements Parcelable {

    @SerializedName("id")
    private int id;

    @SerializedName("edited_by_name")
    private String editedByName;

    @SerializedName("edited_by_email")
    private String editedByEmail;

    @SerializedName("field_name")
    private String fieldName;

    @SerializedName("field_label")
    private String fieldLabel;

    @SerializedName("old_value")
    private String oldValue;

    @SerializedName("new_value")
    private String newValue;

    @SerializedName("edited_at")
    private String editedAt;

    public BookingEditHistoryItem() {
        // Required for Gson/Retrofit deserialization.
    }

    protected BookingEditHistoryItem(Parcel in) {
        id = in.readInt();
        editedByName = in.readString();
        editedByEmail = in.readString();
        fieldName = in.readString();
        fieldLabel = in.readString();
        oldValue = in.readString();
        newValue = in.readString();
        editedAt = in.readString();
    }

    public static final Creator<BookingEditHistoryItem> CREATOR =
            new Creator<BookingEditHistoryItem>() {
                @Override
                public BookingEditHistoryItem createFromParcel(Parcel in) {
                    return new BookingEditHistoryItem(in);
                }

                @Override
                public BookingEditHistoryItem[] newArray(int size) {
                    return new BookingEditHistoryItem[size];
                }
            };

    public int getId() {
        return id;
    }

    public String getEditedByName() {
        return editedByName;
    }

    public String getEditedByEmail() {
        return editedByEmail;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFieldLabel() {
        return fieldLabel;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public String getEditedAt() {
        return editedAt;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof BookingEditHistoryItem)) return false;
        BookingEditHistoryItem that = (BookingEditHistoryItem) object;
        return id == that.id
                && Objects.equals(editedByName, that.editedByName)
                && Objects.equals(editedByEmail, that.editedByEmail)
                && Objects.equals(fieldName, that.fieldName)
                && Objects.equals(fieldLabel, that.fieldLabel)
                && Objects.equals(oldValue, that.oldValue)
                && Objects.equals(newValue, that.newValue)
                && Objects.equals(editedAt, that.editedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                editedByName,
                editedByEmail,
                fieldName,
                fieldLabel,
                oldValue,
                newValue,
                editedAt
        );
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeInt(id);
        parcel.writeString(editedByName);
        parcel.writeString(editedByEmail);
        parcel.writeString(fieldName);
        parcel.writeString(fieldLabel);
        parcel.writeString(oldValue);
        parcel.writeString(newValue);
        parcel.writeString(editedAt);
    }
}
