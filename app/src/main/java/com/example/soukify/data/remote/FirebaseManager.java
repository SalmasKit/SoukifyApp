package com.example.soukify.data.remote;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

/**
 * Firebase Manager - Singleton class to initialize and provide Firebase instances
 * Maintains MVVM architecture by providing centralized Firebase access
 */
public class FirebaseManager {
    private static FirebaseManager instance;
    private final Application application;
    
    // Firebase instances
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    
    private FirebaseManager(Application application) {
        this.application = application;
        initializeFirebase();
    }
    
    public static synchronized FirebaseManager getInstance(Application application) {
        if (instance == null) {
            instance = new FirebaseManager(application);
        }
        return instance;
    }
    
    private void initializeFirebase() {
        if (!FirebaseApp.getApps(application).isEmpty()) {
            FirebaseApp.getInstance();
        }
        
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        
        // Enable offline persistence for Firestore
        firestore.enableNetwork();
    }
    
    public FirebaseAuth getAuth() {
        return auth;
    }
    
    public FirebaseFirestore getFirestore() {
        return firestore;
    }
    
    public FirebaseStorage getStorage() {
        return storage;
    }
    
    public String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }
    
    public boolean isUserLoggedIn() {
        return auth.getCurrentUser() != null;
    }
}
