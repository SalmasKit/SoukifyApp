package com.example.soukify.ui.favorites;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.soukify.data.repositories.SessionRepository;
import com.example.soukify.data.repositories.FavoritesRepository;
import com.example.soukify.data.models.ShopModel;

import java.util.List;

public class FavoritesViewModel extends AndroidViewModel {
    private final SessionRepository sessionRepository;
    private final FavoritesRepository favoritesRepository;
    
    public FavoritesViewModel(Application application) {
        super(application);
        sessionRepository = SessionRepository.getInstance(application);
        favoritesRepository = new FavoritesRepository(application);
    }
    
    public LiveData<String> getCurrentUserId() {
        return sessionRepository.getCurrentUserId();
    }
    
    public LiveData<Boolean> isLoggedIn() {
        return sessionRepository.isLoggedIn();
    }
    
    public int getUserId() {
        String userIdStr = sessionRepository.getCurrentUserId().getValue();
        if (userIdStr != null && !userIdStr.isEmpty()) {
            try {
                return Integer.parseInt(userIdStr);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }
    
    // Favorites management methods
    
    /**
     * Get favorite shops LiveData
     */
    public LiveData<List<ShopModel>> getFavoriteShops() {
        return favoritesRepository.getFavoriteShops();
    }
    
    /**
     * Load favorite shops for current user
     */
    public void loadFavoriteShops() {
        favoritesRepository.loadFavoriteShops();
    }
    
    /**
     * Toggle favorite status for a shop
     */
    public void toggleFavorite(ShopModel shop) {
        favoritesRepository.toggleFavorite(shop);
    }
    
    /**
     * Check if a shop is in favorites
     */
    public LiveData<Boolean> isFavorite(String shopId) {
        return favoritesRepository.isFavorite(shopId);
    }
    
    /**
     * Get error messages
     */
    public LiveData<String> getErrorMessage() {
        return favoritesRepository.getErrorMessage();
    }
    
    /**
     * Get loading state
     */
    public LiveData<Boolean> getIsLoading() {
        return favoritesRepository.getIsLoading();
    }
    
    /**
     * Clear error message
     */
    public void clearError() {
        favoritesRepository.clearError();
    }
    
    /**
     * Update user ID when session changes
     */
    public void updateUserId(String userId) {
        favoritesRepository.updateUserId(userId);
    }
}
