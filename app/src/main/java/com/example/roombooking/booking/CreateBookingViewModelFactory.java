package com.example.roombooking.booking;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.roombooking.common.LocalUserManager;
import com.example.roombooking.room.RoomRepository;

public class CreateBookingViewModelFactory implements ViewModelProvider.Factory {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final LocalUserManager localUserManager;

    public CreateBookingViewModelFactory(
            BookingRepository bookingRepository,
            RoomRepository roomRepository,
            LocalUserManager localUserManager
    ) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.localUserManager = localUserManager;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(CreateBookingViewModel.class)) {
            return (T) new CreateBookingViewModel(
                    bookingRepository,
                    roomRepository,
                    localUserManager
            );
        }

        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
