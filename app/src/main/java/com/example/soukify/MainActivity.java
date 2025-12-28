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
            

            
            // FCM Token Sync is handled here
            
            // Link user to OneSignal for Chat
            String userId = FirebaseAuth.getInstance().getUid();
            if (userId != null) {
                // Logout first to avoid "Alias claimed by another user" 409 error if IDs got mixed
                com.onesignal.OneSignal.logout();
                com.onesignal.OneSignal.login(userId);
                android.util.Log.d("MainActivity", "OneSignal user linked: " + userId);
            }
        }
        
        // Request Notification Permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != 
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        checkNotificationIntent(getIntent());
        

    }
    
    private void checkNotificationIntent(android.content.Intent intent) {
        if (intent == null) return;
        
        String type = intent.getStringExtra("type");
        if (type == null) return;

        android.util.Log.d("MainActivity", "Notification reçue du type: " + type);

        Bundle args = new Bundle();
        
        switch (type.toLowerCase()) {
            case "message":
                String conversationId = intent.getStringExtra("conversationId");
                if (conversationId != null) {
                    android.content.Intent chatIntent = new android.content.Intent(this, com.example.soukify.ui.chat.ChatActivity.class);
                    chatIntent.putExtra(com.example.soukify.ui.chat.ChatActivity.EXTRA_CONVERSATION_ID, conversationId);
                    startActivity(chatIntent);
                }
                break;

            case "nouveau produit":
                String productId = intent.getStringExtra("productId");
                if (productId != null) {
                    args.putString("productId", productId);
                    navController.navigate(R.id.productDetailFragment, args);
                }
                break;

            case "promotion":
                String shopId = intent.getStringExtra("shopId");
                if (shopId != null) {
                    args.putString("shopId", shopId);
                    navController.navigate(R.id.shopHomeFragment, args);
                }
                break;

            default:
                android.util.Log.d("MainActivity", "Type de notification inconnu ou général: " + type);
                // Fallback handle for GPS if still needed for legacy (unlikely given user's request)
                if (intent.hasExtra("lat") && intent.hasExtra("lng")) {
                    String lat = intent.getStringExtra("lat");
                    String lng = intent.getStringExtra("lng");
                    android.widget.Toast.makeText(this, "Alerte reçue à: " + lat + ", " + lng, android.widget.Toast.LENGTH_LONG).show();
                }
                break;
        }
    }
    
    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkNotificationIntent(intent);
    }
    
    
    /**
     * Helper pour simuler ou sauvegarder la position actuelle de l'utilisateur.
     * Utile pour tester le système de filtrage par distance des notifications.
     */
    public void saveCurrentLocation(float lat, float lng) {
        android.content.SharedPreferences prefs = getSharedPreferences("safe_city_prefs", MODE_PRIVATE);
        prefs.edit().putFloat("last_lat", lat).putFloat("last_lng", lng).apply();
    }



    public void navigateToSearch() {
        if (navController != null && binding.navView != null) {
            // Use bottom navigation to navigate, which preserves proper state
            binding.navView.setSelectedItemId(R.id.navigation_search);
        }
    }

}