 package com.example.soukify.data.repositories;

 import android.app.Application;
 import android.util.Log;

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
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;
 import com.example.soukify.services.NotificationSenderService;
import com.google.firebase.auth.FirebaseAuth;

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
     private final NotificationSenderService notificationSenderService;
     private final ExecutorService executor = Executors.newFixedThreadPool(2);
    
     public ShopRepository(Application application) {
         FirebaseManager firebaseManager = FirebaseManager.getInstance(application);
         this.shopService = new FirebaseShopService(firebaseManager.getFirestore());
         this.storageService = new FirebaseStorageService(firebaseManager.getStorage());
         this.productRepository = ProductRepository.getInstance(application);
         this.productImageRepository = new ProductImageRepository(application);
         this.notificationSenderService = new NotificationSenderService();
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
                           String website, boolean hasPromotion, boolean hasLivraison) {
         isLoading.postValue(true);
         errorMessage.postValue(null);
        
         String userId = FirebaseManager.getInstance(null).getCurrentUserId();
         if (userId == null) {
             errorMessage.postValue("User not logged in");
             isLoading.postValue(false);
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
         shop.setHasPromotion(hasPromotion);
         shop.setHasLivraison(hasLivraison);
        
        // Debug logging to verify field values before Firestore save
        android.util.Log.d("ShopRepository", "Before Firestore save - hasPromotion: " + shop.isHasPromotion() + ", hasLivraison: " + shop.isHasLivraison());
        
         shopService.createShop(shop)
                 .addOnSuccessListener(documentReference -> {
                     // Update with the actual Firestore document ID
                     String actualShopId = documentReference.getId();
                     shop.setShopId(actualShopId);
                    
                     // Update the document in Firestore with the correct ID and all fields
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
         isLoading.postValue(true);
         errorMessage.postValue(null);
        
         android.util.Log.d("ShopRepository", "=== FIRESTORE UPDATE DEBUG ===");
        android.util.Log.d("ShopRepository", "Updating shop in Firestore: " + shop.getName());
        android.util.Log.d("ShopRepository", "Toggle values being sent to Firestore - hasPromotion: " + shop.isHasPromotion() + ", hasLivraison: " + shop.isHasLivraison());
        
        // 🔍 Récupérer l'état actuel pour détecter l'activation d'une promotion
        shopService.getShopById(shop.getShopId()).addOnSuccessListener(doc -> {
            boolean previouslyHadPromotion = false;
            if (doc.exists() && doc.contains("hasPromotion")) {
                Boolean val = doc.getBoolean("hasPromotion");
                previouslyHadPromotion = val != null && val;
            }
            
            final boolean isNewPromotion = !previouslyHadPromotion && shop.isHasPromotion();
            
            shopService.updateShop(shop.getShopId(), shop)
                 .addOnSuccessListener(aVoid -> {
                     currentShop.postValue(shop);
                     loadUserShops(); // Refresh user shops list
                     isLoading.postValue(false);
                     
                     if (isNewPromotion) {
                         android.util.Log.d("ShopRepository", "🔔 Promotion activée ! Envoi des notifications...");
                         notificationSenderService.sendPromotionNotification(
                             shop.getShopId(), 
                             shop.getName(), 
                             "Profitez de nos nouvelles offres promotionnelles !"
                         );
                     }
                 })
                 .addOnFailureListener(e -> {
                     errorMessage.postValue("Failed to update shop: " + e.getMessage());
                     isLoading.postValue(false);
                 });
        }).addOnFailureListener(e -> {
            // Fallback: update anyway even if fetch fails
            shopService.updateShop(shop.getShopId(), shop)
                 .addOnSuccessListener(aVoid -> {
                     currentShop.postValue(shop);
                     loadUserShops();
                     isLoading.postValue(false);
                 })
                 .addOnFailureListener(err -> {
                     errorMessage.postValue("Failed to update shop: " + err.getMessage());
                     isLoading.postValue(false);
                 });
        });
     }
    
     public Task<Void> deleteShop(String shopId) {
         isLoading.postValue(true);
         errorMessage.postValue(null);
        
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
         android.util.Log.d("ShopRepository", "=== loadUserShops STARTED ===");
         isLoading.postValue(true);
         errorMessage.postValue(null);
        
         String userId = FirebaseManager.getInstance(null).getCurrentUserId();
         android.util.Log.d("ShopRepository", "Current userId: " + userId);
         if (userId == null) {
             android.util.Log.e("ShopRepository", "User not logged in");
             errorMessage.postValue("User not logged in");
             isLoading.postValue(false);
             return;
         }
        
         android.util.Log.d("ShopRepository", "Fetching shops for user: " + userId);
         android.util.Log.d("ShopRepository", "Calling shopService.getShopsByUser(" + userId + ")");
        
         shopService.getShopsByUser(userId).get()
                 .addOnSuccessListener(executor, querySnapshot -> {
                     android.util.Log.d("ShopRepository", "=== Direct Query SUCCESS ===");
                     android.util.Log.d("ShopRepository", "Processing " + querySnapshot.size() + " documents on background thread");
                     List<ShopModel> shops = new ArrayList<>();
                     for (QueryDocumentSnapshot document : querySnapshot) {
                         try {
                             ShopModel shop = deserializeShop(document);
                             shops.add(shop);
                         } catch (Exception e) {
                             Log.e("ShopRepository", "Error deserializing shop: " + document.getId(), e);
                         }
                     }
                     
                     userShops.postValue(shops);
                     if (!shops.isEmpty()) {
                         currentShop.postValue(shops.get(0));
                     } else {
                         currentShop.postValue(null);
                     }
                     isLoading.postValue(false);
                 })
                 .addOnFailureListener(e -> {
                     android.util.Log.e("ShopRepository", "Direct query failed, trying fallback", e);
                     
                     shopService.getAllShops().get()
                         .addOnSuccessListener(executor, allShopsSnapshot -> {
                             List<ShopModel> userShopsList = new ArrayList<>();
                             for (QueryDocumentSnapshot document : allShopsSnapshot) {
                                 try {
                                     ShopModel shop = deserializeShop(document);
                                     if (userId.equals(shop.getUserId())) {
                                         userShopsList.add(shop);
                                     }
                                 } catch (Exception ex) {
                                     Log.e("ShopRepository", "Error in fallback deserialization", ex);
                                 }
                             }
                             userShops.postValue(userShopsList);
                             if (!userShopsList.isEmpty()) {
                                 currentShop.postValue(userShopsList.get(0));
                             } else {
                                 currentShop.postValue(null);
                             }
                             isLoading.postValue(false);
                         })
                         .addOnFailureListener(fallbackE -> {
                             errorMessage.postValue("Failed to load shops: " + fallbackE.getMessage());
                             isLoading.postValue(false);
                         });
                 });
         android.util.Log.d("ShopRepository", "=== loadUserShops METHOD COMPLETED (async) ===");
     }
    
     public Task<DocumentSnapshot> getShopById(String shopId) {
         android.util.Log.d("ShopRepository", "getShopById called for shopId: " + shopId);
         return shopService.getShopById(shopId);
     }
    
     public void loadAllShops() {
         isLoading.postValue(true);
         errorMessage.postValue(null);
        
         shopService.getAllShops().get()
                 .addOnSuccessListener(executor, querySnapshot -> {
                     List<ShopModel> shops = new ArrayList<>();
                     for (QueryDocumentSnapshot document : querySnapshot) {
                         try {
                             shops.add(deserializeShop(document));
                         } catch (Exception e) {
                             Log.e("ShopRepository", "Error deserializing", e);
                         }
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
         isLoading.postValue(true);
         errorMessage.postValue(null);
        
         shopService.searchShops(query).get()
                 .addOnSuccessListener(executor, querySnapshot -> {
                     List<ShopModel> shops = new ArrayList<>();
                     for (QueryDocumentSnapshot document : querySnapshot) {
                         try {
                             shops.add(deserializeShop(document));
                         } catch (Exception e) {
                             Log.e("ShopRepository", "Error deserializing", e);
                         }
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
         isLoading.postValue(true);
         errorMessage.postValue(null);
        
         shopService.getShopsByCategory(category).get()
                 .addOnSuccessListener(executor, querySnapshot -> {
                     List<ShopModel> shops = new ArrayList<>();
                     for (QueryDocumentSnapshot document : querySnapshot) {
                         try {
                             shops.add(deserializeShop(document));
                         } catch (Exception e) {
                             Log.e("ShopRepository", "Error deserializing", e);
                         }
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
         currentShop.postValue(shop);
     }
    
     // Helper method to handle both old (Long) and new (String) createdAt formats
     public ShopModel deserializeShop(QueryDocumentSnapshot document) {
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
         
         // Handle likedByUserIds
         if (document.contains("likedByUserIds")) {
             Object likedByUserIdsObj = document.get("likedByUserIds");
             if (likedByUserIdsObj instanceof List) {
                 ArrayList<String> likedByUserIds = new ArrayList<>((List<String>) likedByUserIdsObj);
                 shop.setLikedByUserIds(likedByUserIds);
                 
                 String currentUserId = FirebaseAuth.getInstance().getUid();
                 if (currentUserId != null && likedByUserIds.contains(currentUserId)) {
                     shop.setLiked(true);
                 } else if (currentUserId != null) {
                     // Only overwrite if we have a user and they are NOT in the list
                     // Otherwise keep the simple 'liked' boolean if it was true (though it shouldn't be relied upon)
                     shop.setLiked(false);
                 }
             }
         }
         if (document.contains("hasPromotion")) {
            shop.setHasPromotion(document.getBoolean("hasPromotion"));
        }
        if (document.contains("hasLivraison")) {
            shop.setHasLivraison(document.getBoolean("hasLivraison"));
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
     public ShopModel deserializeShop(DocumentSnapshot document) {
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

         // Handle likedByUserIds
         if (document.contains("likedByUserIds")) {
             Object likedByUserIdsObj = document.get("likedByUserIds");
             if (likedByUserIdsObj instanceof List) {
                 ArrayList<String> likedByUserIds = new ArrayList<>((List<String>) likedByUserIdsObj);
                 shop.setLikedByUserIds(likedByUserIds);
                 
                 String currentUserId = FirebaseAuth.getInstance().getUid();
                 if (currentUserId != null && likedByUserIds.contains(currentUserId)) {
                     shop.setLiked(true);
                 } else if (currentUserId != null) {
                     shop.setLiked(false);
                 }
             }
         }
         if (document.contains("hasPromotion")) {
            shop.setHasPromotion(document.getBoolean("hasPromotion"));
        }
        if (document.contains("hasLivraison")) {
            shop.setHasLivraison(document.getBoolean("hasLivraison"));
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

