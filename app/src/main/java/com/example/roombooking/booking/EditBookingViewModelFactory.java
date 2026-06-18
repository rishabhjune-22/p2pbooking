package com.example.roombooking.booking;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.roombooking.room.RoomRepository;

public class EditBookingViewModelFactory implements ViewModelProvider.Factory {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;

    public EditBookingViewModelFactory(
            BookingRepository bookingRepository,
            RoomRepository roomRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(EditBookingViewModel.class)) {
            return (T) new EditBookingViewModel(bookingRepository, roomRepository);
        }

        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
