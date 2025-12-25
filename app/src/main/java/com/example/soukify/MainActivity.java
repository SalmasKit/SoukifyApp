package com.example.soukify;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.example.soukify.databinding.ActivityMainBinding;
import com.example.soukify.ui.search.SearchFragment;
import com.example.soukify.data.repositories.UserProductPreferencesRepository;
import com.example.soukify.utils.LocaleHelper;
import com.google.firebase.auth.FirebaseAuth;
import android.content.Context;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up navigation
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);

        // Load user's likes from Firestore on app startup if user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            UserProductPreferencesRepository userPreferences = new UserProductPreferencesRepository(this);
            userPreferences.loadUserLikesFromFirebase(new UserProductPreferencesRepository.OnLikesLoadedListener() {
                @Override
                public void onLikesLoaded(java.util.Set<String> likedProductIds) {
                    android.util.Log.d("MainActivity", "Loaded " + likedProductIds.size() + " liked products from Firestore");
                }

                @Override
                public void onError(String error) {
                    android.util.Log.e("MainActivity", "Failed to load likes: " + error);
                }
            });
            
            // Sync FCM Token
            com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    android.util.Log.w("MainActivity", "Fetching FCM registration token failed", task.getException());
                    return;
                }
                String token = task.getResult();
                new com.example.soukify.data.repositories.UserRepository(getApplication()).updateFcmToken(token);
            });
        }
        
        // Request Notification Permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != 
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }
    
    
    public void navigateToSearch() {
        if (navController != null && binding.navView != null) {
            // Use bottom navigation to navigate, which preserves proper state
            binding.navView.setSelectedItemId(R.id.navigation_search);
        }
    }

}