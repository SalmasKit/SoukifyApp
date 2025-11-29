package com.example.soukify.ui.session;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.soukify.data.repositories.SessionRepository;

public class SessionViewModel extends AndroidViewModel {
    private final SessionRepository sessionRepository;
    
    public SessionViewModel(Application application) {
        super(application);
        sessionRepository = SessionRepository.getInstance(application);
    }
    
    public LiveData<String> getCurrentUserId() {
        return sessionRepository.getCurrentUserId();
    }
    
    public LiveData<Boolean> isLoggedIn() {
        return sessionRepository.isLoggedIn();
    }
    
    public void createLoginSession(String userId, String email, String name) {
        sessionRepository.createLoginSession(userId, email, name);
    }
    
    public void logout() {
        sessionRepository.logout();
    }
    
    public String getUserEmail() {
        return sessionRepository.getUserEmail();
    }
    
    public String getUserName() {
        return sessionRepository.getUserName();
    }
}
