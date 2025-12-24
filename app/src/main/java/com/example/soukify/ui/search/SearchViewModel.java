package com.example.soukify.ui.search;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.soukify.data.repositories.SessionRepository;

public class SearchViewModel extends AndroidViewModel {
    private final SessionRepository sessionRepository;

    public SearchViewModel(Application application) {
        super(application);
        sessionRepository = SessionRepository.getInstance(application);
    }

    public LiveData<String> getCurrentUserId() {
        return sessionRepository.getCurrentUserId();
    }

    public LiveData<Boolean> isLoggedIn() {
        return sessionRepository.isLoggedIn();
    }

    public int getUserId() {
        String userIdStr = sessionRepository.getCurrentUserId().getValue();
        if (userIdStr != null && !userIdStr.isEmpty()) {
            try {
                return Integer.parseInt(userIdStr);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }


}