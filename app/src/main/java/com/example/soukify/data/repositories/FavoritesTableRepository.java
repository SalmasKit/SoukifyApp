package com.example.soukify.data.repositories;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.soukify.data.models.FavoriteModel;
import com.example.soukify.data.models.ShopModel;
import com.example.soukify.data.models.ProductModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Favorites Table Repository - Manages user favorites with complete privacy
 * Uses Firestore favorites collection to ensure each user only sees their own favorites
 */
public class FavoritesTableRepository {
    private static final String TAG = "FavoritesTableRepository";
    private static final String FAVORITES_COLLECTION = "favorites";
    
    private final FirebaseFirestore firestore;
    private final FirebaseAuth firebaseAuth;
    private String currentUserId;
    
    // LiveData for favorites
    private final MutableLiveData<List<ShopModel>> favoriteShops = new MutableLiveData<>();
    private final MutableLiveData<List<ProductModel>> favoriteProducts = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    
    public FavoritesTableRepository(Application application) {
        this.firestore = FirebaseFirestore.getInstance();
        this.firebaseAuth = FirebaseAuth.getInstance();
        updateCurrentUser();
    }
    
    /**
     * Update current user ID when authentication state changes
     */
    public void updateCurrentUser() {
        updateCurrentUserId();
    }
    
    private void updateCurrentUserId() {
        String newUserId = firebaseAuth.getCurrentUser() != null ? 
            firebaseAuth.getCurrentUser().getUid() : null;
        
        if (!java.util.Objects.equals(currentUserId, newUserId)) {
            currentUserId = newUserId;
            Log.d(TAG, "Current user updated: " + currentUserId);
            
            // Clear favorites when user changes
            if (currentUserId == null) {
                favoriteShops.postValue(new ArrayList<>());
                favoriteProducts.postValue(new ArrayList<>());
            }
        }
    }
    
    /**
     * Get current user ID (private to ensure privacy)
     */
    private String getCurrentUserId() {
        updateCurrentUser();
        return currentUserId;
    }
    
    // LiveData getters
    public LiveData<List<ShopModel>> getFavoriteShops() {
        return favoriteShops;
    }
    
    public LiveData<List<ProductModel>> getFavoriteProducts() {
        return favoriteProducts;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    /**
     * Load favorite shops for current user only
     */
    public void loadFavoriteShops() {
        String userId = getCurrentUserId();
        if (userId == null) {
            errorMessage.postValue("User not logged in");
            favoriteShops.postValue(new ArrayList<>());
            return;
        }
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        Log.d(TAG, "Loading favorite shops for user: " + userId);
        
        // Query favorites collection for this user's shop favorites only
        firestore.collection(FAVORITES_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("itemType", "shop")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<String> shopIds = new ArrayList<>();
                for (QueryDocumentSnapshot document : querySnapshot) {
                    String shopId = document.getString("itemId");
                    if (shopId != null) {
                        shopIds.add(shopId);
                    }
                }
                
                Log.d(TAG, "Found " + shopIds.size() + " favorite shop IDs");
                loadShopDetails(shopIds);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading favorite shops", e);
                errorMessage.postValue("Failed to load favorite shops: " + e.getMessage());
                isLoading.setValue(false);
            });
    }
    
    /**
     * Load favorite products for current user only
     */
    public void loadFavoriteProducts() {
        String userId = getCurrentUserId();
        if (userId == null) {
            errorMessage.postValue("User not logged in");
            favoriteProducts.postValue(new ArrayList<>());
            return;
        }
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        Log.d(TAG, "Loading favorite products for user: " + userId);
        
        // Query favorites collection for this user's product favorites only
        firestore.collection(FAVORITES_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("itemType", "product")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<String> productIds = new ArrayList<>();
                for (QueryDocumentSnapshot document : querySnapshot) {
                    String productId = document.getString("itemId");
                    if (productId != null) {
                        productIds.add(productId);
                    }
                }
                
                Log.d(TAG, "Found " + productIds.size() + " favorite product IDs");
                loadProductDetails(productIds);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading favorite products", e);
                errorMessage.postValue("Failed to load favorite products: " + e.getMessage());
                isLoading.setValue(false);
            });
    }
    
    /**
     * Add shop to user's favorites
     */
    public void addShopToFavorites(ShopModel shop) {
        String userId = getCurrentUserId();
        if (userId == null || shop == null || shop.getShopId() == null) {
            errorMessage.postValue("Cannot add shop to favorites - invalid data");
            return;
        }
        
        FavoriteModel favorite = FavoriteModel.forShop(userId, shop.getShopId());
        
        firestore.collection(FAVORITES_COLLECTION)
            .add(favorite)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "Shop added to favorites: " + shop.getName());
                favorite.setFavoriteId(documentReference.getId());
                loadFavoriteShops(); // Refresh list
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error adding shop to favorites", e);
                errorMessage.postValue("Failed to add shop to favorites: " + e.getMessage());
            });
    }
    
    /**
     * Add product to user's favorites
     */
    public void addProductToFavorites(ProductModel product) {
        String userId = getCurrentUserId();
        if (userId == null || product == null || product.getProductId() == null) {
            errorMessage.postValue("Cannot add product to favorites - invalid data");
            return;
        }
        
        FavoriteModel favorite = FavoriteModel.forProduct(userId, product.getProductId());
        
        firestore.collection(FAVORITES_COLLECTION)
            .add(favorite)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "Product added to favorites: " + product.getName());
                favorite.setFavoriteId(documentReference.getId());
                loadFavoriteProducts(); // Refresh list
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error adding product to favorites", e);
                errorMessage.postValue("Failed to add product to favorites: " + e.getMessage());
            });
    }
    
    /**
     * Remove shop from user's favorites
     */
    public void removeShopFromFavorites(String shopId) {
        String userId = getCurrentUserId();
        if (userId == null || shopId == null) {
            errorMessage.postValue("Cannot remove shop from favorites - invalid data");
            return;
        }
        
        firestore.collection(FAVORITES_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("itemId", shopId)
            .whereEqualTo("itemType", "shop")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                for (QueryDocumentSnapshot document : querySnapshot) {
                    document.getReference().delete()
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Shop removed from favorites: " + shopId);
                            loadFavoriteShops(); // Refresh list
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error removing shop favorite", e);
                            errorMessage.postValue("Failed to remove shop from favorites: " + e.getMessage());
                        });
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error finding shop favorite to remove", e);
                errorMessage.postValue("Failed to remove shop from favorites: " + e.getMessage());
            });
    }
    
    /**
     * Remove product from user's favorites
     */
    public void removeProductFromFavorites(String productId) {
        String userId = getCurrentUserId();
        if (userId == null || productId == null) {
            errorMessage.postValue("Cannot remove product from favorites - invalid data");
            return;
        }
        
        firestore.collection(FAVORITES_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("itemId", productId)
            .whereEqualTo("itemType", "product")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                for (QueryDocumentSnapshot document : querySnapshot) {
                    document.getReference().delete()
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Product removed from favorites: " + productId);
                            loadFavoriteProducts(); // Refresh list
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error removing product favorite", e);
                            errorMessage.postValue("Failed to remove product from favorites: " + e.getMessage());
                        });
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error finding product favorite to remove", e);
                errorMessage.postValue("Failed to remove product from favorites: " + e.getMessage());
            });
    }
    
    /**
     * Check if shop is in user's favorites
     */
    public LiveData<Boolean> isShopFavorite(String shopId) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        String userId = getCurrentUserId();
        
        if (userId == null || shopId == null) {
            result.setValue(false);
            return result;
        }
        
        firestore.collection(FAVORITES_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("itemId", shopId)
            .whereEqualTo("itemType", "shop")
            .limit(1)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                result.setValue(!querySnapshot.isEmpty());
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking shop favorite status", e);
                result.setValue(false);
            });
        
        return result;
    }
    
    /**
     * Check if product is in user's favorites
     */
    public LiveData<Boolean> isProductFavorite(String productId) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        String userId = getCurrentUserId();
        
        if (userId == null || productId == null) {
            result.setValue(false);
            return result;
        }
        
        firestore.collection(FAVORITES_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("itemId", productId)
            .whereEqualTo("itemType", "product")
            .limit(1)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                result.setValue(!querySnapshot.isEmpty());
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking product favorite status", e);
                result.setValue(false);
            });
        
        return result;
    }

    /**
     * One-shot favorite check with callback to avoid callers using observeForever.
     */
    public interface OnFavoriteCheckedListener {
        void onChecked(boolean isFavorited);
        void onError(String error);
    }

    public void checkProductFavoriteOnce(String productId, OnFavoriteCheckedListener listener) {
        String userId = getCurrentUserId();
        if (userId == null || productId == null) {
            if (listener != null) listener.onChecked(false);
            return;
        }

        firestore.collection(FAVORITES_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("itemId", productId)
            .whereEqualTo("itemType", "product")
            .limit(1)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (listener != null) listener.onChecked(!querySnapshot.isEmpty());
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking product favorite status", e);
                if (listener != null) listener.onError(e.getMessage());
            });
    }
    
    /**
     * Load shop details from IDs
     */
    private void loadShopDetails(List<String> shopIds) {
        if (shopIds.isEmpty()) {
            favoriteShops.postValue(new ArrayList<>());
            isLoading.setValue(false);
            return;
        }
        
        // Load shops in batches to avoid Firestore limits
        List<ShopModel> shops = new ArrayList<>();
        int batchSize = 10;
        final int[] totalLoaded = {0}; // Use array to make effectively final
        
        for (int i = 0; i < shopIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, shopIds.size());
            List<String> batch = shopIds.subList(i, endIndex);
            
            firestore.collection("shops")
                .whereIn("shopId", batch)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        ShopModel shop = document.toObject(ShopModel.class);
                        if (shop != null) {
                            shop.setShopId(document.getId());
                            shops.add(shop);
                        }
                    }
                    
                    totalLoaded[0] += batch.size();
                    if (totalLoaded[0] >= shopIds.size()) {
                        favoriteShops.postValue(shops);
                        isLoading.setValue(false);
                        Log.d(TAG, "Loaded " + shops.size() + " favorite shops");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading shop details", e);
                    errorMessage.postValue("Failed to load shop details: " + e.getMessage());
                    isLoading.setValue(false);
                });
        }
    }
    
    /**
     * Load product details from IDs
     */
    private void loadProductDetails(List<String> productIds) {
        if (productIds.isEmpty()) {
            favoriteProducts.postValue(new ArrayList<>());
            isLoading.setValue(false);
            return;
        }
        
        // Load products in batches to avoid Firestore limits
        List<ProductModel> products = new ArrayList<>();
        int batchSize = 10;
        final int[] totalLoaded = {0}; // Use array to make effectively final
        
        for (int i = 0; i < productIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, productIds.size());
            List<String> batch = productIds.subList(i, endIndex);
            
            firestore.collection("products")
                .whereIn("productId", batch)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        ProductModel product = document.toObject(ProductModel.class);
                        if (product != null) {
                            product.setProductId(document.getId());
                            products.add(product);
                        }
                    }
                    
                    totalLoaded[0] += batch.size();
                    if (totalLoaded[0] >= productIds.size()) {
                        favoriteProducts.postValue(products);
                        isLoading.setValue(false);
                        Log.d(TAG, "Loaded " + products.size() + " favorite products");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading product details", e);
                    errorMessage.postValue("Failed to load product details: " + e.getMessage());
                    isLoading.setValue(false);
                });
        }
    }
    
    /**
     * Clear error message
     */
    public void clearError() {
        errorMessage.setValue(null);
    }
}
