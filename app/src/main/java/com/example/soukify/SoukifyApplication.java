package com.example.soukify;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;

public class SoukifyApplication extends Application {
    private static final String TAG = "SoukifyApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        try {
            // Initialize Firebase with error handling
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
            }
            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase: " + e.getMessage(), e);
        }
    }
}
