package com.example.soukify.data.repositories;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.soukify.data.remote.FirebaseManager;
import com.example.soukify.data.remote.firebase.FirebaseShopService;
import com.example.soukify.data.remote.firebase.FirebaseStorageService;
import com.example.soukify.data.models.ShopModel;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Shop Repository - Firebase implementation
 * Follows MVVM pattern by abstracting data operations from ViewModels
 */
public class ShopRepository {
    private final FirebaseShopService shopService;
    private final FirebaseStorageService storageService;
    private final MutableLiveData<ShopModel> currentShop = new MutableLiveData<>();
    private final MutableLiveData<List<ShopModel>> userShops = new MutableLiveData<>();
    private final MutableLiveData<List<ShopModel>> allShops = new MutableLiveData<>();
    private final MutableLiveData<List<ShopModel>> favoriteShops = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    
    public ShopRepository(Application application) {
        FirebaseManager firebaseManager = FirebaseManager.getInstance(application);
        this.shopService = new FirebaseShopService(firebaseManager.getFirestore());
        this.storageService = new FirebaseStorageService(firebaseManager.getStorage());
    }
    
    public LiveData<ShopModel> getCurrentShop() {
        return currentShop;
    }
    
    public LiveData<List<ShopModel>> getUserShops() {
        return userShops;
    }
    
    public LiveData<List<ShopModel>> getAllShops() {
        return allShops;
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
    
    public void createShop(String name, String category, String phone, String email, 
                          String address, String location, String imageUrl, String regionId, String cityId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        String userId = FirebaseManager.getInstance(null).getCurrentUserId();
        if (userId == null) {
            errorMessage.setValue("User not logged in");
            isLoading.setValue(false);
            return;
        }
        
        // Create shop with a generated ID first
        String shopId = java.util.UUID.randomUUID().toString();
        ShopModel shop = new ShopModel(shopId, userId, name, category, phone, email, address, location, imageUrl, regionId, cityId);
        
        shopService.createShop(shop)
                .addOnSuccessListener(documentReference -> {
                    // Update with the actual Firestore document ID
                    String actualShopId = documentReference.getId();
                    shop.setShopId(actualShopId);
                    
                    // Also update the document in Firestore with the correct ID
                    shopService.updateShop(actualShopId, shop)
                            .addOnSuccessListener(updateResult -> {
                                currentShop.postValue(shop);
                                loadUserShops(); // Refresh user shops list
                                isLoading.postValue(false);
                            })
                            .addOnFailureListener(e -> {
                                // Even if update fails, the shop was created, just ID might be different
                                currentShop.postValue(shop);
                                loadUserShops();
                                isLoading.postValue(false);
                            });
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to create shop: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void updateShop(ShopModel shop) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        shopService.updateShop(shop.getShopId(), shop)
                .addOnSuccessListener(aVoid -> {
                    currentShop.postValue(shop);
                    loadUserShops(); // Refresh user shops list
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to update shop: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void deleteShop(String shopId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        shopService.deleteShop(shopId)
                .addOnSuccessListener(aVoid -> {
                    currentShop.postValue(null);
                    loadUserShops(); // Refresh user shops list
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to delete shop: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void loadUserShops() {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        String userId = FirebaseManager.getInstance(null).getCurrentUserId();
        if (userId == null) {
            errorMessage.setValue("User not logged in");
            isLoading.setValue(false);
            return;
        }
        
        shopService.getShopsByUser(userId).get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ShopModel> shops = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        ShopModel shop = deserializeShop(document);
                        shops.add(shop);
                    }
                    userShops.postValue(shops);
                    
                    // Set current shop if not already set and list is not empty
                    if (currentShop.getValue() == null && !shops.isEmpty()) {
                        currentShop.postValue(shops.get(0));
                    }
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to load shops: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void loadAllShops() {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        shopService.getAllShops().get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ShopModel> shops = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        ShopModel shop = deserializeShop(document);
                        shops.add(shop);
                    }
                    allShops.postValue(shops);
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to load all shops: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void searchShops(String query) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        shopService.searchShops(query).get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ShopModel> shops = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        ShopModel shop = deserializeShop(document);
                        shops.add(shop);
                    }
                    allShops.postValue(shops);
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to search shops: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void getShopsByCategory(String category) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        shopService.getShopsByCategory(category).get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ShopModel> shops = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        ShopModel shop = deserializeShop(document);
                        shops.add(shop);
                    }
                    allShops.postValue(shops);
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to load shops by category: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void toggleLike(String shopId, boolean isLiked, int likesCount) {
        shopService.toggleLike(shopId, isLiked, likesCount)
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to toggle like: " + e.getMessage());
                });
    }
    
    public void incrementShopLikes(String shopId) {
        shopService.incrementShopLikes(shopId)
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to increment likes: " + e.getMessage());
                });
    }
    
    public void incrementShopViews(String shopId) {
        shopService.incrementShopViews(shopId)
                .addOnFailureListener(e -> {
                    // Silently fail for view count to not disrupt user experience
                });
    }
    
    public void setCurrentShop(ShopModel shop) {
        currentShop.setValue(shop);
    }
    
    // Helper method to handle both old (Long) and new (String) createdAt formats
    private ShopModel deserializeShop(QueryDocumentSnapshot document) {
        ShopModel shop = new ShopModel();
        shop.setShopId(document.getId());
        
        // Set basic fields
        if (document.contains("name")) {
            shop.setName(document.getString("name"));
        }
        if (document.contains("category")) {
            shop.setCategory(document.getString("category"));
        }
        if (document.contains("location")) {
            shop.setLocation(document.getString("location"));
        }
        if (document.contains("imageUrl")) {
            shop.setImageUrl(document.getString("imageUrl"));
        }
        if (document.contains("userId")) {
            shop.setUserId(document.getString("userId"));
        }
        if (document.contains("phone")) {
            shop.setPhone(document.getString("phone"));
        }
        if (document.contains("email")) {
            shop.setEmail(document.getString("email"));
        }
        if (document.contains("address")) {
            shop.setAddress(document.getString("address"));
        }
        if (document.contains("regionId")) {
            shop.setRegionId(document.getString("regionId"));
        }
        if (document.contains("cityId")) {
            shop.setCityId(document.getString("cityId"));
        }
        
        // Handle createdAt - convert Long to String if needed
        if (document.contains("createdAt")) {
            Object createdAtValue = document.get("createdAt");
            if (createdAtValue instanceof Long) {
                // Convert old timestamp to formatted string
                Long timestamp = (Long) createdAtValue;
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
                shop.setCreatedAt(sdf.format(new java.util.Date(timestamp)));
            } else if (createdAtValue instanceof String) {
                // Use new string format directly
                shop.setCreatedAt((String) createdAtValue);
            }
        }
        
        return shop;
    }
    
    // Helper method for DocumentSnapshot (single document)
    private ShopModel deserializeShop(DocumentSnapshot document) {
        ShopModel shop = new ShopModel();
        shop.setShopId(document.getId());
        
        // Set basic fields
        if (document.contains("name")) {
            shop.setName(document.getString("name"));
        }
        if (document.contains("category")) {
            shop.setCategory(document.getString("category"));
        }
        if (document.contains("location")) {
            shop.setLocation(document.getString("location"));
        }
        if (document.contains("imageUrl")) {
            shop.setImageUrl(document.getString("imageUrl"));
        }
        if (document.contains("userId")) {
            shop.setUserId(document.getString("userId"));
        }
        if (document.contains("phone")) {
            shop.setPhone(document.getString("phone"));
        }
        if (document.contains("email")) {
            shop.setEmail(document.getString("email"));
        }
        if (document.contains("address")) {
            shop.setAddress(document.getString("address"));
        }
        if (document.contains("regionId")) {
            shop.setRegionId(document.getString("regionId"));
        }
        if (document.contains("cityId")) {
            shop.setCityId(document.getString("cityId"));
        }
        
        // Handle createdAt - convert Long to String if needed
        if (document.contains("createdAt")) {
            Object createdAtValue = document.get("createdAt");
            if (createdAtValue instanceof Long) {
                // Convert old timestamp to formatted string
                Long timestamp = (Long) createdAtValue;
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
                shop.setCreatedAt(sdf.format(new java.util.Date(timestamp)));
            } else if (createdAtValue instanceof String) {
                // Use new string format directly
                shop.setCreatedAt((String) createdAtValue);
            }
        }
        
        return shop;
    }
}
