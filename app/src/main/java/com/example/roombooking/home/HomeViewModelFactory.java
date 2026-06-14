package com.example.roombooking.home;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.roombooking.booking.BookingRepository;

public class HomeViewModelFactory implements ViewModelProvider.Factory {

    private final BookingRepository bookingRepository;

    public HomeViewModelFactory(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(HomeViewModel.class)) {
            return (T) new HomeViewModel(bookingRepository);
        }

        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}