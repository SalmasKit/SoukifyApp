package com.example.soukify.data.remote.firebase;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.example.soukify.data.models.ShopModel;
import com.example.soukify.data.models.FavoriteModel;

import java.util.HashMap;
import java.util.Map;

/**
 * Firebase Favorites Service - Handles favorites collection operations
 * Follows MVVM pattern by providing clean separation from UI layer
 */
public class FirebaseFavoritesService {
    private final FirebaseFirestore firestore;
    private final CollectionReference favoritesCollection;
    
    private static final String FAVORITES_COLLECTION = "favorites";
    
    public FirebaseFavoritesService(FirebaseFirestore firestore) {
        this.firestore = firestore;
        this.favoritesCollection = firestore.collection(FAVORITES_COLLECTION);
    }
    
    /**
     * Add a shop to user's favorites
     * @param userId The user ID
     * @param shopId The shop ID
     * @param shop The shop object (for caching shop data)
     * @return Task for the operation
     */
    public Task<Void> addToFavorites(String userId, String shopId, ShopModel shop) {
        // Create favorite document with composite key
        String favoriteId = userId + "_" + shopId;
        
        // Create favorite data
        Map<String, Object> favoriteData = new HashMap<>();
        favoriteData.put("userId", userId);
        favoriteData.put("shopId", shopId);
        favoriteData.put("addedAt", System.currentTimeMillis());
        
        // Add shop data for easy retrieval
        favoriteData.put("shopName", shop.getName());
        favoriteData.put("shopCategory", shop.getCategory());
        favoriteData.put("shopImageUrl", shop.getImageUrl());
        favoriteData.put("shopRating", shop.getRating());
        favoriteData.put("shopLocation", shop.getAddress());
        
        return favoritesCollection.document(favoriteId).set(favoriteData, SetOptions.merge());
    }
    
    /**
     * Remove a shop from user's favorites
     * @param userId The user ID
     * @param shopId The shop ID
     * @return Task for the operation
     */
    public Task<Void> removeFromFavorites(String userId, String shopId) {
        String favoriteId = userId + "_" + shopId;
        return favoritesCollection.document(favoriteId).delete();
    }
    
    /**
     * Get all favorite shops for a user
     * @param userId The user ID
     * @return Task with QuerySnapshot of favorite shops
     */
    public Task<QuerySnapshot> getUserFavorites(String userId) {
        return favoritesCollection
                .whereEqualTo("userId", userId)
                .orderBy("addedAt", Query.Direction.DESCENDING)
                .get();
    }
    
    /**
     * Check if a shop is in user's favorites
     * @param userId The user ID
     * @param shopId The shop ID
     * @return Task with QuerySnapshot (empty if not favorite)
     */
    public Task<QuerySnapshot> isFavorite(String userId, String shopId) {
        String favoriteId = userId + "_" + shopId;
        return favoritesCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("shopId", shopId)
                .limit(1)
                .get();
    }
    
    /**
     * Toggle favorite status
     * @param userId The user ID
     * @param shopId The shop ID
     * @param shop The shop object (if adding to favorites)
     * @param isFavorite Current favorite status
     * @return Task for the operation
     */
    public Task<Void> toggleFavorite(String userId, String shopId, ShopModel shop, boolean isFavorite) {
        if (isFavorite) {
            return removeFromFavorites(userId, shopId);
        } else {
            return addToFavorites(userId, shopId, shop);
        }
    }
    
    /**
     * Get favorite count for a shop
     * @param shopId The shop ID
     * @return Task with QuerySnapshot containing count
     */
    public Task<QuerySnapshot> getFavoriteCount(String shopId) {
        return favoritesCollection
                .whereEqualTo("shopId", shopId)
                .get();
    }
}
