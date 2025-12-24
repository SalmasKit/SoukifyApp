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

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply saved language before calling super.onCreate()
        applySavedLanguage();
        
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
        }
    }
    
    /**
     * Apply the saved language preference to the app
     */
    private void applySavedLanguage() {
        String language = LocaleHelper.getLanguage(this);
        if (language != null && !language.isEmpty()) {
            if ("device".equalsIgnoreCase(language)) {
                language = LocaleHelper.getDeviceLanguage();
            }
            LocaleHelper.updateLocale(this, language);
        }
    }
    
    public void navigateToSearch() {
        if (navController != null && binding.navView != null) {
            // Use bottom navigation to navigate, which preserves proper state
            binding.navView.setSelectedItemId(R.id.navigation_search);
        }
    }

}