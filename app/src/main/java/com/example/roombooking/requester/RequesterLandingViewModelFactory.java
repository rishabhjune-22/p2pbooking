package com.example.roombooking.requester;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class RequesterLandingViewModelFactory implements ViewModelProvider.Factory {

    private final RequesterAvailabilityRepository availabilityRepository;

    public RequesterLandingViewModelFactory(
            RequesterAvailabilityRepository availabilityRepository
    ) {
        this.availabilityRepository = availabilityRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(RequesterLandingViewModel.class)) {
            return (T) new RequesterLandingViewModel(availabilityRepository);
        }

        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
