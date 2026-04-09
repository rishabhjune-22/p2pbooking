package com.example.roombooking.home;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.roombooking.auth.AuthRepository;
import com.example.roombooking.auth.SessionManager;
import com.example.roombooking.booking.BookingRepository;

public class HomeViewModelFactory implements ViewModelProvider.Factory {

    private final BookingRepository bookingRepository;
    private final AuthRepository authRepository;
    private final SessionManager sessionManager;

    public HomeViewModelFactory(
            BookingRepository bookingRepository,
            AuthRepository authRepository,
            SessionManager sessionManager
    ) {
        this.bookingRepository = bookingRepository;
        this.authRepository = authRepository;
        this.sessionManager = sessionManager;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(HomeViewModel.class)) {
            return (T) new HomeViewModel(bookingRepository, authRepository, sessionManager);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}