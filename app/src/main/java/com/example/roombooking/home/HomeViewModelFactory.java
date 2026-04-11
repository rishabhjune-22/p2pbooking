package com.example.roombooking.home;

import android.content.Context;

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
    private final Context context;
    public HomeViewModelFactory(
            Context context,
            BookingRepository bookingRepository,
            AuthRepository authRepository,
            SessionManager sessionManager
    ) {
        this.context = context.getApplicationContext();
        this.bookingRepository = bookingRepository;
        this.authRepository = authRepository;
        this.sessionManager = sessionManager;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(HomeViewModel.class)) {
            return (T) new HomeViewModel( context,bookingRepository, authRepository, sessionManager);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}