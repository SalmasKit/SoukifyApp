package com.example.soukify.data.repositories;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashSet;
import java.util.Set;

/**
 * Repository to manage user-specific product likes and favorites
 * Uses both SharedPreferences for local persistence and Firestore for cloud sync
 */
public class UserProductPreferencesRepository {
    private static final String TAG = "UserProductPreferencesRepository";
    private static final String PREFS_NAME = "user_product_preferences";
    private static final String LIKED_PRODUCTS_KEY = "liked_products_";
    private static final String FAVORITED_PRODUCTS_KEY = "favorited_products_";
    
    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final FirebaseFirestore firestore;
    
    public UserProductPreferencesRepository(Context context) {
        this.context = context.getApplicationContext();
        this.sharedPreferences = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.firestore = FirebaseFirestore.getInstance();
    }
    
    /**
     * Get current user ID
     */
    private String getCurrentUserId() {
        return FirebaseAuth.getInstance().getCurrentUser() != null ? 
            FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }
    
    /**
     * Get user-specific liked products set
     */
    public Set<String> getLikedProducts() {
        String userId = getCurrentUserId();
        if (userId == null) return new HashSet<>();
        
        return sharedPreferences.getStringSet(LIKED_PRODUCTS_KEY + userId, new HashSet<>());
    }
    
    /**
     * Get user-specific favorited products set
     */
    public Set<String> getFavoritedProducts() {
        String userId = getCurrentUserId();
        if (userId == null) return new HashSet<>();
        
        return sharedPreferences.getStringSet(FAVORITED_PRODUCTS_KEY + userId, new HashSet<>());
    }
    
    /**
     * Toggle like status for a product
     */
    public void toggleLike(String productId) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.w(TAG, "User not logged in, cannot toggle like");
            return;
        }
        
        Set<String> likedProducts = getLikedProducts();
        boolean isLiked = likedProducts.contains(productId);
        
        if (isLiked) {
            likedProducts.remove(productId);
        } else {
            likedProducts.add(productId);
        }
        
        // Save locally
        sharedPreferences.edit()
            .putStringSet(LIKED_PRODUCTS_KEY + userId, likedProducts)
            .apply();
        
        // Update in Firestore
        updateProductLikeCount(productId, isLiked);
    }
    
    /**
     * Toggle favorite status for a product
     */
    public void toggleFavorite(String productId) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.w(TAG, "User not logged in, cannot toggle favorite");
            return;
        }
        
        Set<String> favoritedProducts = getFavoritedProducts();
        boolean isFavorited = favoritedProducts.contains(productId);
        
        if (isFavorited) {
            favoritedProducts.remove(productId);
        } else {
            favoritedProducts.add(productId);
        }
        
        // Save locally
        sharedPreferences.edit()
            .putStringSet(FAVORITED_PRODUCTS_KEY + userId, favoritedProducts)
            .apply();
        
        // Update in Firestore
        updateProductFavoriteCount(productId, isFavorited);
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
}
