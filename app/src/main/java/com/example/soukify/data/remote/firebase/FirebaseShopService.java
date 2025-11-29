package com.example.soukify.data.remote.firebase;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.example.soukify.data.models.ShopModel;

import java.util.List;

/**
 * Firebase Shop Service - Handles shop data operations
 * Follows MVVM pattern by providing clean separation from UI layer
 */
public class FirebaseShopService {
    private final FirebaseFirestore firestore;
    
    private static final String SHOPS_COLLECTION = "shops";
    
    public FirebaseShopService(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }
    
    public Task<DocumentReference> createShop(ShopModel shop) {
        return firestore.collection(SHOPS_COLLECTION).add(shop);
    }
    
    public Task<Void> updateShop(String shopId, ShopModel shop) {
        return firestore.collection(SHOPS_COLLECTION).document(shopId).set(shop);
    }
    
    public Task<Void> deleteShop(String shopId) {
        return firestore.collection(SHOPS_COLLECTION).document(shopId).delete();
    }
    
    public Task<ShopModel> getShop(String shopId) {
        return firestore.collection(SHOPS_COLLECTION).document(shopId).get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        return task.getResult().toObject(ShopModel.class);
                    }
                    return null;
                });
    }
    
    public Query getShopsByUser(String userId) {
        return firestore.collection(SHOPS_COLLECTION).whereEqualTo("userId", userId);
    }
    
    public Query getAllShops() {
        return firestore.collection(SHOPS_COLLECTION).orderBy("createdAt", Query.Direction.DESCENDING);
    }
    
    public Query searchShops(String query) {
        return firestore.collection(SHOPS_COLLECTION)
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + "\uf8ff");
    }
    
    public Query getShopsByCategory(String category) {
        return firestore.collection(SHOPS_COLLECTION)
                .whereEqualTo("category", category)
                .orderBy("createdAt", Query.Direction.DESCENDING);
    }
    
    public Query getShopsByLocation(String location) {
        return firestore.collection(SHOPS_COLLECTION)
                .whereEqualTo("location", location)
                .orderBy("createdAt", Query.Direction.DESCENDING);
    }
    
    public Query getShopsByRegion(String regionId) {
        return firestore.collection(SHOPS_COLLECTION)
                .whereEqualTo("regionId", regionId)
                .orderBy("createdAt", Query.Direction.DESCENDING);
    }
    
    public Query getShopsByCity(String cityId) {
        return firestore.collection(SHOPS_COLLECTION)
                .whereEqualTo("cityId", cityId)
                .orderBy("createdAt", Query.Direction.DESCENDING);
    }
    
    public Task<QuerySnapshot> getFeaturedShops() {
        return firestore.collection(SHOPS_COLLECTION)
                .whereEqualTo("hasPromotion", true)
                .orderBy("rating", Query.Direction.DESCENDING)
                .limit(10)
                .get();
    }
    
    public Task<Void> incrementShopLikes(String shopId) {
        return firestore.collection(SHOPS_COLLECTION).document(shopId)
                .update("likesCount", com.google.firebase.firestore.FieldValue.increment(1));
    }
    
    public Task<Void> incrementShopViews(String shopId) {
        return firestore.collection(SHOPS_COLLECTION).document(shopId)
                .update("searchCount", com.google.firebase.firestore.FieldValue.increment(1));
    }
    
    public Task<Void> updateShopRating(String shopId, double newRating) {
        return firestore.collection(SHOPS_COLLECTION).document(shopId)
                .update("rating", newRating);
    }
    
    public Task<DocumentSnapshot> getShopById(String shopId) {
        return firestore.collection(SHOPS_COLLECTION).document(shopId).get();
    }
    
    public Task<Void> toggleLike(String shopId, boolean isLiked, int likesCount) {
        return firestore.collection(SHOPS_COLLECTION).document(shopId)
                .update("liked", isLiked, 
                        "likesCount", likesCount,
                        "updatedAt", System.currentTimeMillis());
    }
}
