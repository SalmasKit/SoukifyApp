package com.example.soukify;

import android.app.Application;
import android.util.Log;

import com.example.soukify.data.repositories.AuthPreferenceManager;
import com.example.soukify.utils.LocaleHelper;
import com.google.firebase.FirebaseApp;
import android.content.Context;

public class SoukifyApplication extends Application {
    private static final String TAG = "SoukifyApplication";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        try {
            // Initialize Firebase with error handling
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
            }
            Log.d(TAG, "Firebase initialized successfully");
            
            // Initialize AuthPreferenceManager for persistent like/favorite state
            AuthPreferenceManager.getInstance(this);
            Log.d(TAG, "AuthPreferenceManager initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing app components: " + e.getMessage(), e);
        }
    }
}
