package com.example.soukify.data.repositories;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.soukify.data.remote.FirebaseManager;
import com.example.soukify.data.remote.firebase.FirebaseFavoritesService;
import com.example.soukify.data.remote.firebase.FirebaseShopService;
import com.example.soukify.data.models.ShopModel;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Favorites Repository - Firebase implementation for favorites functionality
 * Follows MVVM pattern by abstracting data operations from ViewModels
 * Uses dedicated favorites collection for better data organization
 */
public class FavoritesRepository {
    private final FirebaseFavoritesService favoritesService;
    private final FirebaseShopService shopService;
    private final MutableLiveData<List<ShopModel>> favoriteShops = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private String currentUserId;
    
    public FavoritesRepository(Application application) {
        FirebaseManager firebaseManager = FirebaseManager.getInstance(application);
        this.favoritesService = new FirebaseFavoritesService(firebaseManager.getFirestore());
        this.shopService = new FirebaseShopService(firebaseManager.getFirestore());
        this.currentUserId = firebaseManager.getCurrentUserId();
    }
    
    public LiveData<List<ShopModel>> getFavoriteShops() {
        return favoriteShops;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    /**
     * Load favorite shops for the current user
     */
    public void loadFavoriteShops() {
        if (currentUserId == null) {
            errorMessage.setValue("User not logged in");
            return;
        }
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        favoritesService.getUserFavorites(currentUserId)
                .addOnSuccessListener(querySnapshot -> {
                    List<String> shopIds = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        String shopId = document.getString("shopId");
                        if (shopId != null) {
                            shopIds.add(shopId);
                        }
                    }
                    
                    // Now load full shop data for each favorite
                    loadShopsData(shopIds);
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Failed to load favorites: " + e.getMessage());
                    isLoading.setValue(false);
                });
    }
    
    /**
     * Load full shop data for the given shop IDs
     */
    private void loadShopsData(List<String> shopIds) {
        if (shopIds.isEmpty()) {
            favoriteShops.postValue(new ArrayList<>());
            isLoading.setValue(false);
            return;
        }
        
        // Load shops in batches to avoid "in" query limitations
        List<ShopModel> shops = new ArrayList<>();
        final int[] loadedCount = {0};
        
        for (String shopId : shopIds) {
            shopService.getShopById(shopId)
                    .addOnSuccessListener(document -> {
                        if (document != null && document.exists()) {
                            ShopModel shop = document.toObject(ShopModel.class);
                            if (shop != null) {
                                shops.add(shop);
                            }
                        }
                        
                        loadedCount[0]++;
                        if (loadedCount[0] == shopIds.size()) {
                            // All shops loaded
                            favoriteShops.postValue(shops);
                            isLoading.setValue(false);
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Continue loading other shops even if one fails
                        loadedCount[0]++;
                        if (loadedCount[0] == shopIds.size()) {
                            favoriteShops.postValue(shops);
                            isLoading.setValue(false);
                        }
                    });
        }
    }
    
    /**
     * Toggle favorite status for a shop
     * @param shop The shop to toggle
     */
    public void toggleFavorite(ShopModel shop) {
        if (currentUserId == null) {
            errorMessage.setValue("User not logged in");
            return;
        }
        
        if (shop.getShopId() == null) {
            errorMessage.setValue("Invalid shop ID");
            return;
        }
        
        // Check if currently favorite
        favoritesService.isFavorite(currentUserId, shop.getShopId())
                .addOnSuccessListener(querySnapshot -> {
                    boolean isFavorite = !querySnapshot.isEmpty();
                    
                    favoritesService.toggleFavorite(currentUserId, shop.getShopId(), shop, isFavorite)
                            .addOnSuccessListener(aVoid -> {
                                // Toggle the favorite status in the shop model
                                shop.setFavorite(!isFavorite);
                                
                                // Reload favorites list
                                loadFavoriteShops();
                            })
                            .addOnFailureListener(e -> {
                                errorMessage.setValue("Failed to toggle favorite: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Failed to check favorite status: " + e.getMessage());
                });
    }
    
    /**
     * Check if a shop is in favorites
     * @param shopId The shop ID to check
     * @return LiveData with boolean result
     */
    public LiveData<Boolean> isFavorite(String shopId) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        
        if (currentUserId == null || shopId == null) {
            result.setValue(false);
            return result;
        }
        
        favoritesService.isFavorite(currentUserId, shopId)
                .addOnSuccessListener(querySnapshot -> {
                    result.setValue(!querySnapshot.isEmpty());
                })
                .addOnFailureListener(e -> {
                    result.setValue(false);
                });
        
        return result;
    }
    
    /**
     * Update current user ID (for session changes)
     */
    public void updateUserId(String userId) {
        this.currentUserId = userId;
        if (userId != null) {
            loadFavoriteShops();
        } else {
            favoriteShops.postValue(new ArrayList<>());
        }
    }
    
    /**
     * Clear error message
     */
    public void clearError() {
        errorMessage.setValue(null);
    }
}
