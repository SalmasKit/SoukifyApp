package com.example.soukify.data.repositories;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.soukify.data.remote.FirebaseManager;
import com.example.soukify.data.remote.firebase.FirebaseShopService;
import com.example.soukify.data.remote.firebase.FirebaseStorageService;
import com.example.soukify.data.remote.firebase.FirebaseProductService;
import com.example.soukify.data.remote.firebase.FirebaseProductImageService;
import com.example.soukify.data.models.ShopModel;
import com.example.soukify.data.repositories.ProductRepository;
import com.example.soukify.data.repositories.ProductImageRepository;
import com.google.android.gms.tasks.Task;
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
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
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
        this.productRepository = new ProductRepository(application);
        this.productImageRepository = new ProductImageRepository(application);
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
    
    public void createShop(String name, String description, String category, String phone, String email, 
                          String address, String location, String imageUrl, String regionId, String cityId,
                          String workingHours, String workingDays, String instagram, String facebook, 
                          String website) {
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
        shop.setDescription(description);
        shop.setWorkingHours(workingHours);
        shop.setWorkingDays(workingDays);
        shop.setInstagram(instagram);
        shop.setFacebook(facebook);
        shop.setWebsite(website);
        
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
    
    public Task<Void> deleteShop(String shopId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        // First delete all products for this shop (cascade delete)
        return productRepository.deleteAllProductsForShop(shopId)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        errorMessage.postValue("Failed to delete products: " + task.getException().getMessage());
                        isLoading.postValue(false);
                        throw task.getException();
                    }
                    
                    // After deleting products, delete the shop image
                    return storageService.deleteShopImage(shopId)
                            .continueWithTask(imageTask -> {
                                // Log error but continue with shop deletion
                                if (!imageTask.isSuccessful()) {
                                    android.util.Log.w("ShopRepository", "Failed to delete shop image: " + imageTask.getException().getMessage());
                                }
                                
                                // After deleting image, delete the shop
                                return shopService.deleteShop(shopId)
                                        .continueWithTask(shopTask -> {
                                            if (!shopTask.isSuccessful()) {
                                                errorMessage.postValue("Failed to delete shop: " + shopTask.getException().getMessage());
                                                isLoading.postValue(false);
                                                throw shopTask.getException();
                                            }
                                            
                                            // All deletions completed successfully
                                            currentShop.postValue(null);
                                            loadUserShops(); // Refresh user shops list
                                            isLoading.postValue(false);
                                            return com.google.android.gms.tasks.Tasks.forResult((Void) null);
                                        });
                            });
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to delete shop: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void loadUserShops() {
        android.util.Log.d("ShopRepository", "loadUserShops called");
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        String userId = FirebaseManager.getInstance(null).getCurrentUserId();
        android.util.Log.d("ShopRepository", "Current userId: " + userId);
        if (userId == null) {
            android.util.Log.e("ShopRepository", "User not logged in");
            errorMessage.setValue("User not logged in");
            isLoading.setValue(false);
            return;
        }
        
        android.util.Log.d("ShopRepository", "Fetching shops for user: " + userId);
        shopService.getShopsByUser(userId).get()
                .addOnSuccessListener(querySnapshot -> {
                    android.util.Log.d("ShopRepository", "Query successful, documents count: " + querySnapshot.size());
                    List<ShopModel> shops = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        ShopModel shop = deserializeShop(document);
                        shops.add(shop);
                        android.util.Log.d("ShopRepository", "Found shop: " + shop.getName() + " (ID: " + shop.getShopId() + ")");
                    }
                    userShops.postValue(shops);
                    android.util.Log.d("ShopRepository", "Posted " + shops.size() + " shops to userShops LiveData");
                    
                    // Set current shop if not already set and list is not empty
                    if (currentShop.getValue() == null && !shops.isEmpty()) {
                        currentShop.postValue(shops.get(0));
                        android.util.Log.d("ShopRepository", "Set current shop to: " + shops.get(0).getName());
                    }
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ShopRepository", "Failed to load shops: " + e.getMessage(), e);
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
        if (document.contains("description")) {
            shop.setDescription(document.getString("description"));
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
        if (document.contains("workingHours")) {
            shop.setWorkingHours(document.getString("workingHours"));
        }
        if (document.contains("workingDays")) {
            shop.setWorkingDays(document.getString("workingDays"));
        }
        if (document.contains("instagram")) {
            shop.setInstagram(document.getString("instagram"));
        }
        if (document.contains("facebook")) {
            shop.setFacebook(document.getString("facebook"));
        }
        if (document.contains("website")) {
            shop.setWebsite(document.getString("website"));
        }
        
        // Add missing likesCount and rating fields
        if (document.contains("likesCount")) {
            Object likesCountValue = document.get("likesCount");
            if (likesCountValue instanceof Long) {
                shop.setLikesCount(((Long) likesCountValue).intValue());
            } else if (likesCountValue instanceof Integer) {
                shop.setLikesCount((Integer) likesCountValue);
            }
        }
        if (document.contains("rating")) {
            Object ratingValue = document.get("rating");
            if (ratingValue instanceof Double) {
                shop.setRating((Double) ratingValue);
            } else if (ratingValue instanceof Long) {
                shop.setRating(((Long) ratingValue).doubleValue());
            }
        }
        
        // Handle boolean fields
        if (document.contains("favorite")) {
            shop.setFavorite(document.getBoolean("favorite"));
        }
        if (document.contains("liked")) {
            shop.setLiked(document.getBoolean("liked"));
        }
        if (document.contains("reviews")) {
            Object reviewsValue = document.get("reviews");
            if (reviewsValue instanceof Long) {
                shop.setReviews(((Long) reviewsValue).intValue());
            } else if (reviewsValue instanceof Integer) {
                shop.setReviews((Integer) reviewsValue);
            }
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
        if (document.contains("description")) {
            shop.setDescription(document.getString("description"));
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
        if (document.contains("workingHours")) {
            shop.setWorkingHours(document.getString("workingHours"));
        }
        if (document.contains("workingDays")) {
            shop.setWorkingDays(document.getString("workingDays"));
        }
        if (document.contains("instagram")) {
            shop.setInstagram(document.getString("instagram"));
        }
        if (document.contains("facebook")) {
            shop.setFacebook(document.getString("facebook"));
        }
        if (document.contains("website")) {
            shop.setWebsite(document.getString("website"));
        }
        
        // Add missing likesCount and rating fields
        if (document.contains("likesCount")) {
            Object likesCountValue = document.get("likesCount");
            if (likesCountValue instanceof Long) {
                shop.setLikesCount(((Long) likesCountValue).intValue());
            } else if (likesCountValue instanceof Integer) {
                shop.setLikesCount((Integer) likesCountValue);
            }
        }
        if (document.contains("rating")) {
            Object ratingValue = document.get("rating");
            if (ratingValue instanceof Double) {
                shop.setRating((Double) ratingValue);
            } else if (ratingValue instanceof Long) {
                shop.setRating(((Long) ratingValue).doubleValue());
            }
        }
        
        // Handle boolean fields
        if (document.contains("favorite")) {
            shop.setFavorite(document.getBoolean("favorite"));
        }
        if (document.contains("liked")) {
            shop.setLiked(document.getBoolean("liked"));
        }
        if (document.contains("reviews")) {
            Object reviewsValue = document.get("reviews");
            if (reviewsValue instanceof Long) {
                shop.setReviews(((Long) reviewsValue).intValue());
            } else if (reviewsValue instanceof Integer) {
                shop.setReviews((Integer) reviewsValue);
            }
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
