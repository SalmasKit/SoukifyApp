package com.example.soukify.ui.home;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.soukify.data.repositories.SessionRepository;

public class HomeViewModel extends AndroidViewModel {
    private final SessionRepository sessionRepository;
    
    public HomeViewModel(Application application) {
        super(application);
        sessionRepository = SessionRepository.getInstance(application);
    }
    
    public LiveData<String> getCurrentUserId() {
        return sessionRepository.getCurrentUserId();
    }
    
    public LiveData<Boolean> isLoggedIn() {
        return sessionRepository.isLoggedIn();
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
