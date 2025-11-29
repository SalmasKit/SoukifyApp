package com.example.soukify.ui;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.soukify.data.repositories.SessionRepository;

public class BaseViewModel extends AndroidViewModel {
    protected final SessionRepository sessionRepository;
    
    public BaseViewModel(Application application) {
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
    
    public String getUserName() {
        return sessionRepository.getUserName();
    }
    
    public String getUserEmail() {
        return sessionRepository.getUserEmail();
    }
    
    public void logout() {
        sessionRepository.logout();
    }
}
