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

    private static FavoritesTableRepository instance;
    private final FirebaseFirestore firestore;
    private final FirebaseAuth firebaseAuth;
    private final UserProductPreferencesRepository userPreferences;
    private String currentUserId;
    
    // Cache for favorite IDs to allow fast enrichment
    private final java.util.Set<String> favoriteShopIds = new java.util.HashSet<>();
    private final java.util.Set<String> favoriteProductIds = new java.util.HashSet<>();
    
    // LiveData for favorites
    private final MutableLiveData<List<ShopModel>> favoriteShops = new MutableLiveData<>();
    private final MutableLiveData<List<ProductModel>> favoriteProducts = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    
    private FavoritesTableRepository(Application application) {
        this.firestore = FirebaseFirestore.getInstance();
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.userPreferences = new UserProductPreferencesRepository(application);
        updateCurrentUser();
    }

    public static synchronized FavoritesTableRepository getInstance(Application application) {
        if (instance == null) {
            instance = new FavoritesTableRepository(application);
        }
        return instance;
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
                favoriteShopIds.clear();
                favoriteProductIds.clear();
            } else {
                // Pre-load favorite IDs for fast enrichment
                preloadFavoriteIds();
            }
        }
    }
    
    private void preloadFavoriteIds() {
        String userId = getCurrentUserId();
        if (userId == null) return;
        
        firestore.collection(FAVORITES_COLLECTION)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                favoriteShopIds.clear();
                favoriteProductIds.clear();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    String type = doc.getString("itemType");
                    String id = doc.getString("itemId");
                    if ("shop".equals(type)) favoriteShopIds.add(id);
                    else if ("product".equals(type)) favoriteProductIds.add(id);
                }
                Log.d(TAG, "Preloaded " + favoriteShopIds.size() + " shop IDs and " + favoriteProductIds.size() + " product IDs");
            });
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
                java.util.Set<String> newIds = new java.util.HashSet<>();
                for (QueryDocumentSnapshot document : querySnapshot) {
                    String shopId = document.getString("itemId");
                    if (shopId != null) {
                        shopIds.add(shopId);
                        newIds.add(shopId);
                    }
                }
                
                synchronized (favoriteShopIds) {
                    favoriteShopIds.clear();
                    favoriteShopIds.addAll(newIds);
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
                java.util.Set<String> newIds = new java.util.HashSet<>();
                for (QueryDocumentSnapshot document : querySnapshot) {
                    String productId = document.getString("itemId");
                    if (productId != null) {
                        productIds.add(productId);
                        newIds.add(productId);
                    }
                }
                
                synchronized (favoriteProductIds) {
                    favoriteProductIds.clear();
                    favoriteProductIds.addAll(newIds);
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
                favoriteShopIds.add(shop.getShopId());
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
                favoriteProductIds.add(product.getProductId());
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
                            favoriteShopIds.remove(shopId);
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
                            favoriteProductIds.remove(productId);
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
        
        if (favoriteShopIds.contains(shopId)) {
            result.setValue(true);
            return result;
        }
        
        // Final fallback to Firestore
        firestore.collection(FAVORITES_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("itemId", shopId)
            .whereEqualTo("itemType", "shop")
            .limit(1)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                boolean isFav = !querySnapshot.isEmpty();
                if (isFav) favoriteShopIds.add(shopId);
                result.setValue(isFav);
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
        
        if (favoriteProductIds.contains(productId)) {
            result.setValue(true);
            return result;
        }
        
        firestore.collection(FAVORITES_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("itemId", productId)
            .whereEqualTo("itemType", "product")
            .limit(1)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                boolean isFav = !querySnapshot.isEmpty();
                if (isFav) favoriteProductIds.add(productId);
                result.setValue(isFav);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking product favorite status", e);
                result.setValue(false);
            });
        
        return result;
    }

    public boolean isProductFavoriteSync(String productId) {
        return productId != null && favoriteProductIds.contains(productId);
    }
    
    public boolean isShopFavoriteSync(String shopId) {
        return shopId != null && favoriteShopIds.contains(shopId);
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
                boolean isFav = !querySnapshot.isEmpty();
                if (isFav) favoriteProductIds.add(productId);
                if (listener != null) listener.onChecked(isFav);
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
        final List<ProductModel> products = java.util.Collections.synchronizedList(new ArrayList<>());
        final int batchSize = 10;
        final int numBatches = (int) Math.ceil((double) productIds.size() / batchSize);
        final java.util.concurrent.atomic.AtomicInteger batchesProcessed = new java.util.concurrent.atomic.AtomicInteger(0);
        
        for (int i = 0; i < productIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, productIds.size());
            List<String> batch = productIds.subList(i, endIndex);
            
            firestore.collection("products")
                .whereIn("productId", batch)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            ProductModel product = document.toObject(ProductModel.class);
                            if (product != null) {
                                product.setProductId(document.getId());
                                enrichProduct(product);
                                products.add(product);
                            }
                        }
                    } else {
                        Log.e(TAG, "Error loading product batch", task.getException());
                    }

                    if (batchesProcessed.incrementAndGet() == numBatches) {
                        // All batches done
                        favoriteProducts.postValue(new ArrayList<>(products));
                        isLoading.setValue(false);
                        Log.d(TAG, "Finished loading " + products.size() + " favorite products");
                    }
                });
        }
    }
    
    /**
     * Clear error message
     */
    /**
     * Notifie le repository qu'un produit a changé.
     */
    public void notifyProductChanged(ProductModel updatedProduct) {
        if (updatedProduct == null || updatedProduct.getProductId() == null) return;
        
        List<ProductModel> currentList = favoriteProducts.getValue();
        if (currentList != null) {
            List<ProductModel> updatedList = new ArrayList<>(currentList);
            boolean changed = false;
            for (int i = 0; i < updatedList.size(); i++) {
                if (updatedList.get(i).getProductId().equals(updatedProduct.getProductId())) {
                    // Update the product in the local list
                    updatedList.set(i, updatedProduct);
                    changed = true;
                    break;
                }
            }
            if (changed) {
                favoriteProducts.postValue(updatedList);
                Log.d(TAG, "Synchronized product in favorites list: " + updatedProduct.getName());
            }
        }
    }

    /**
     * Enrichit un produit avec l'état utilisateur (Liked, Favorite)
     */
    private void enrichProduct(ProductModel product) {
        if (product == null || product.getProductId() == null) return;
        
        // Favorite state from our own cache
        product.setFavoriteByUser(favoriteProductIds.contains(product.getProductId()));
        
        // Like state from user preferences
        product.setLikedByUser(userPreferences.isProductLiked(product.getProductId()));
    }
    
    public void clearError() {
        errorMessage.setValue(null);
    }
}
