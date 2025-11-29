package com.example.soukify.data.repositories;

import android.app.Application;
import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.soukify.data.SessionManager;

public class SessionRepository {
    private static SessionRepository instance;
    private final SessionManager sessionManager;
    private final MutableLiveData<String> currentUserId;
    private final MutableLiveData<Boolean> isLoggedIn;
    
    private SessionRepository(Application application) {
        sessionManager = SessionManager.getInstance(application);
        currentUserId = new MutableLiveData<>();
        isLoggedIn = new MutableLiveData<>();
        
        // Initialize with current session state
        currentUserId.setValue(sessionManager.getUserId());
        isLoggedIn.setValue(sessionManager.isLoggedIn());
    }
    
    public static synchronized SessionRepository getInstance(Application application) {
        if (instance == null) {
            instance = new SessionRepository(application);
        }
        return instance;
    }
    
    public LiveData<String> getCurrentUserId() {
        return currentUserId;
    }
    
    public LiveData<Boolean> isLoggedIn() {
        return isLoggedIn;
    }
    
    public void createLoginSession(String userId, String email, String name) {
        sessionManager.createLoginSession(userId, email, name);
        currentUserId.postValue(userId);
        isLoggedIn.postValue(true);
    }
    
    public void logout() {
        sessionManager.logout();
        currentUserId.setValue("");
        isLoggedIn.setValue(false);
    }
    
    public String getUserEmail() {
        return sessionManager.getUserEmail();
    }
    
    public String getUserName() {
        return sessionManager.getUserName();
    }
    
    public void updateUserEmail(String email, String name) {
        sessionManager.updateUserEmail(email, name);
    }
}
