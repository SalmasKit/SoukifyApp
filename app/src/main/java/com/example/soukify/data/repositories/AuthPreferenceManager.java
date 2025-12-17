package com.example.soukify.data.repositories;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Manages authentication state and preference migration
 * Handles migration from device-based preferences to user-based preferences when user logs in
 */
public class AuthPreferenceManager {
    private static final String TAG = "AuthPreferenceManager";
    private static AuthPreferenceManager instance;
    private final UserProductPreferencesRepository preferencesRepository;
    private final FirebaseAuth auth;
    private String currentUserId = null;
    
    private AuthPreferenceManager(Context context) {
        this.preferencesRepository = new UserProductPreferencesRepository(context);
        this.auth = FirebaseAuth.getInstance();
        setupAuthStateListener();
    }
    
    public static synchronized AuthPreferenceManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthPreferenceManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Set up authentication state listener to handle preference migration
     */
    private void setupAuthStateListener() {
        auth.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                String newUserId = user != null ? user.getUid() : null;
                
                Log.d(TAG, "Auth state changed: " + (newUserId != null ? "User logged in: " + newUserId : "User logged out"));
                
                // Handle user login (migration from device to user preferences)
                if (newUserId != null && currentUserId == null) {
                    // User just logged in
                    Log.d(TAG, "User logged in, checking for device preference migration");
                    preferencesRepository.migrateDevicePreferencesToUser(newUserId);
                }
                
                currentUserId = newUserId;
            }
        });
    }
    
    /**
     * Get current user ID (null if not logged in)
     */
    public String getCurrentUserId() {
        return currentUserId;
    }
    
    /**
     * Check if user is currently authenticated
     */
    public boolean isUserAuthenticated() {
        return currentUserId != null;
    }
    
    /**
     * Get the preferences repository
     */
    public UserProductPreferencesRepository getPreferencesRepository() {
        return preferencesRepository;
    }
}
