package com.example.soukify.data.repositories;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashSet;
import java.util.Set;

/**
 * Repository to manage user-specific product likes and favorites
 * Uses both SharedPreferences for local persistence and Firestore for cloud sync
 * Enhanced with device-based fallback for anonymous users
 */
public class UserProductPreferencesRepository {
    private static final String TAG = "UserProductPreferencesRepository";
    private static final String PREFS_NAME = "user_product_preferences";
    private static final String LIKED_PRODUCTS_KEY = "liked_products_";
    private static final String FAVORITED_PRODUCTS_KEY = "favorited_products_";
    private static final String DEVICE_LIKED_PRODUCTS_KEY = "device_liked_products";
    private static final String DEVICE_FAVORITED_PRODUCTS_KEY = "device_favorited_products";
    
    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final FirebaseFirestore firestore;
    private String deviceUniqueId;
    
    public UserProductPreferencesRepository(Context context) {
        this.context = context.getApplicationContext();
        this.sharedPreferences = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.firestore = FirebaseFirestore.getInstance();
        this.deviceUniqueId = getDeviceUniqueId();
    }
    
    /**
     * Get unique device identifier for anonymous users
     */
    private String getDeviceUniqueId() {
        try {
            // Use Android ID as device identifier (persists across app reinstallation)
            String androidId = Settings.Secure.getString(
                context.getContentResolver(), 
                Settings.Secure.ANDROID_ID
            );
            return "device_" + androidId;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get device ID", e);
            return "device_fallback";
        }
    }
    
    /**
     * Get current user ID or device ID for anonymous users
     */
    private String getCurrentUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            return deviceUniqueId;
        }
    }
    
    /**
     * Check if current user is authenticated
     */
    private boolean isUserAuthenticated() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }
    
    /**
     * Get user-specific liked products set
     */
    public Set<String> getLikedProducts() {
        String userId = getCurrentUserId();
        if (userId == null) return new HashSet<>();
        
        Set<String> likedProducts = sharedPreferences.getStringSet(LIKED_PRODUCTS_KEY + userId, new HashSet<>());
        if (!isUserAuthenticated()) {
            Set<String> deviceLikedProducts = sharedPreferences.getStringSet(DEVICE_LIKED_PRODUCTS_KEY, new HashSet<>());
            likedProducts.addAll(deviceLikedProducts);
        }
        return likedProducts;
    }
    
    /**
     * Get user-specific favorited products set
     */
    public Set<String> getFavoritedProducts() {
        String userId = getCurrentUserId();
        if (userId == null) return new HashSet<>();
        
        Set<String> favoritedProducts = sharedPreferences.getStringSet(FAVORITED_PRODUCTS_KEY + userId, new HashSet<>());
        if (!isUserAuthenticated()) {
            Set<String> deviceFavoritedProducts = sharedPreferences.getStringSet(DEVICE_FAVORITED_PRODUCTS_KEY, new HashSet<>());
            favoritedProducts.addAll(deviceFavoritedProducts);
        }
        return favoritedProducts;
    }
    
    /**
     * Toggle like status for a product
     */
    public void toggleLike(String productId) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.w(TAG, "Cannot get user ID, cannot toggle like");
            return;
        }
        
        Set<String> likedProducts = getLikedProducts();
        boolean isLiked = likedProducts.contains(productId);
        
        if (isLiked) {
            likedProducts.remove(productId);
        } else {
            likedProducts.add(productId);
        }
        
        // Save locally with user-specific key
        sharedPreferences.edit()
            .putStringSet(LIKED_PRODUCTS_KEY + userId, likedProducts)
            .apply();
        
        // Also save to device-level storage for anonymous users
        if (!isUserAuthenticated()) {
            Set<String> deviceLikedProducts = sharedPreferences.getStringSet(DEVICE_LIKED_PRODUCTS_KEY, new HashSet<>());
            if (isLiked) {
                deviceLikedProducts.remove(productId);
            } else {
                deviceLikedProducts.add(productId);
            }
            sharedPreferences.edit()
                .putStringSet(DEVICE_LIKED_PRODUCTS_KEY, deviceLikedProducts)
                .apply();
        }
        
        // Update in Firestore only if user is authenticated
        if (isUserAuthenticated()) {
            updateProductLikeCount(productId, isLiked);
        }
        
        Log.d(TAG, "Toggled like for product " + productId + " (now " + !isLiked + ")");
    }
    
    /**
     * Toggle favorite status for a product
     */
    public void toggleFavorite(String productId) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.w(TAG, "Cannot get user ID, cannot toggle favorite");
            return;
        }
        
        Set<String> favoritedProducts = getFavoritedProducts();
        boolean isFavorited = favoritedProducts.contains(productId);
        
        if (isFavorited) {
            favoritedProducts.remove(productId);
        } else {
            favoritedProducts.add(productId);
        }
        
        // Save locally with user-specific key
        sharedPreferences.edit()
            .putStringSet(FAVORITED_PRODUCTS_KEY + userId, favoritedProducts)
            .apply();
        
        // Also save to device-level storage for anonymous users
        if (!isUserAuthenticated()) {
            Set<String> deviceFavoritedProducts = sharedPreferences.getStringSet(DEVICE_FAVORITED_PRODUCTS_KEY, new HashSet<>());
            if (isFavorited) {
                deviceFavoritedProducts.remove(productId);
            } else {
                deviceFavoritedProducts.add(productId);
            }
            sharedPreferences.edit()
                .putStringSet(DEVICE_FAVORITED_PRODUCTS_KEY, deviceFavoritedProducts)
                .apply();
        }
        
        // Update in Firestore only if user is authenticated
        if (isUserAuthenticated()) {
            updateProductFavoriteCount(productId, isFavorited);
        }
        
        Log.d(TAG, "Toggled favorite for product " + productId + " (now " + !isFavorited + ")");
    }
    
    /**
     * Check if product is liked by current user
     */
    public boolean isProductLiked(String productId) {
        return getLikedProducts().contains(productId);
    }
    
    /**
     * Check if product is favorited by current user
     */
    public boolean isProductFavorited(String productId) {
        return getFavoritedProducts().contains(productId);
    }
    
    /**
     * Update product like count in Firestore
     */
    private void updateProductLikeCount(String productId, boolean isCurrentlyLiked) {
        firestore.collection("products")
            .document(productId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Integer currentLikes = documentSnapshot.getLong("likesCount") != null ? 
                        documentSnapshot.getLong("likesCount").intValue() : 0;
                    
                    int newLikesCount = isCurrentlyLiked ? currentLikes - 1 : currentLikes + 1;
                    
                    firestore.collection("products")
                        .document(productId)
                        .update("likesCount", newLikesCount)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Updated like count for product " + productId + " to " + newLikesCount);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to update like count", e);
                            // Revert local change on failure
                            revertLikeChange(productId, isCurrentlyLiked);
                        });
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to get product for like update", e);
            });
    }
    
    /**
     * Update product favorite count in Firestore
     */
    private void updateProductFavoriteCount(String productId, boolean isCurrentlyFavorited) {
        firestore.collection("products")
            .document(productId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Integer currentFavorites = documentSnapshot.getLong("favoritesCount") != null ? 
                        documentSnapshot.getLong("favoritesCount").intValue() : 0;
                    
                    int newFavoritesCount = isCurrentlyFavorited ? currentFavorites - 1 : currentFavorites + 1;
                    
                    firestore.collection("products")
                        .document(productId)
                        .update("favoritesCount", newFavoritesCount)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Updated favorite count for product " + productId + " to " + newFavoritesCount);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to update favorite count", e);
                            // Revert local change on failure
                            revertFavoriteChange(productId, isCurrentlyFavorited);
                        });
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to get product for favorite update", e);
            });
    }
    
    /**
     * Revert like change on failure
     */
    private void revertLikeChange(String productId, boolean wasLiked) {
        String userId = getCurrentUserId();
        if (userId == null) return;
        
        Set<String> likedProducts = getLikedProducts();
        if (wasLiked) {
            likedProducts.add(productId);
        } else {
            likedProducts.remove(productId);
        }
        
        sharedPreferences.edit()
            .putStringSet(LIKED_PRODUCTS_KEY + userId, likedProducts)
            .apply();
    }
    
    /**
     * Revert favorite change on failure
     */
    private void revertFavoriteChange(String productId, boolean wasFavorited) {
        String userId = getCurrentUserId();
        if (userId == null) return;
        
        Set<String> favoritedProducts = getFavoritedProducts();
        if (wasFavorited) {
            favoritedProducts.add(productId);
        } else {
            favoritedProducts.remove(productId);
        }
        
        sharedPreferences.edit()
            .putStringSet(FAVORITED_PRODUCTS_KEY + userId, favoritedProducts)
            .apply();
    }
    
    /**
     * Migrate device preferences to user preferences when user logs in
     */
    public void migrateDevicePreferencesToUser(String userId) {
        Set<String> deviceLikedProducts = sharedPreferences.getStringSet(DEVICE_LIKED_PRODUCTS_KEY, new HashSet<>());
        Set<String> deviceFavoritedProducts = sharedPreferences.getStringSet(DEVICE_FAVORITED_PRODUCTS_KEY, new HashSet<>());
        
        if (!deviceLikedProducts.isEmpty() || !deviceFavoritedProducts.isEmpty()) {
            Log.d(TAG, "Migrating device preferences to user " + userId);
            
            // Get existing user preferences
            Set<String> userLikedProducts = sharedPreferences.getStringSet(LIKED_PRODUCTS_KEY + userId, new HashSet<>());
            Set<String> userFavoritedProducts = sharedPreferences.getStringSet(FAVORITED_PRODUCTS_KEY + userId, new HashSet<>());
            
            // Merge device preferences with user preferences
            userLikedProducts.addAll(deviceLikedProducts);
            userFavoritedProducts.addAll(deviceFavoritedProducts);
            
            // Save merged preferences
            sharedPreferences.edit()
                .putStringSet(LIKED_PRODUCTS_KEY + userId, userLikedProducts)
                .putStringSet(FAVORITED_PRODUCTS_KEY + userId, userFavoritedProducts)
                .apply();
            
            // Clear device preferences
            sharedPreferences.edit()
                .remove(DEVICE_LIKED_PRODUCTS_KEY)
                .remove(DEVICE_FAVORITED_PRODUCTS_KEY)
                .apply();
            
            Log.d(TAG, "Migration completed: " + userLikedProducts.size() + " likes, " + userFavoritedProducts.size() + " favorites");
        }
    }
}
