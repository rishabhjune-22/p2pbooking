package com.example.roombooking.booking;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.roombooking.room.RoomRepository;

public class CreateBookingViewModelFactory implements ViewModelProvider.Factory {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;

    public CreateBookingViewModelFactory(
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
        if (modelClass.isAssignableFrom(CreateBookingViewModel.class)) {
            return (T) new CreateBookingViewModel(
                    bookingRepository,
                    roomRepository
            );
        }

        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
