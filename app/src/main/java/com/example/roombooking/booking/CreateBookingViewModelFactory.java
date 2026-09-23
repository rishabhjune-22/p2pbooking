package com.example.roombooking.booking;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class CreateBookingViewModelFactory implements ViewModelProvider.Factory {

    private final BookingRepository bookingRepository;
    private final AvailabilityRepository availabilityRepository;

    public CreateBookingViewModelFactory(
            BookingRepository bookingRepository,
            AvailabilityRepository availabilityRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.availabilityRepository = availabilityRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(CreateBookingViewModel.class)) {
            return (T) new CreateBookingViewModel(
                    bookingRepository,
                    availabilityRepository
            );
        }

        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
