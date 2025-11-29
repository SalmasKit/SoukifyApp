package com.example.soukify.data.remote.firebase;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.example.soukify.data.models.FavoriteModel;

/**
 * Firebase Favorite Service - Handles favorite operations
 * Follows MVVM pattern by providing clean separation from UI layer
 */
public class FirebaseFavoriteService {
    private final FirebaseFirestore firestore;
    
    private static final String FAVORITES_COLLECTION = "favorites";
    
    public FirebaseFavoriteService(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }
    
    public Task<DocumentReference> addFavorite(FavoriteModel favorite) {
        return firestore.collection(FAVORITES_COLLECTION).add(favorite);
    }
    
    public Task<Void> removeFavorite(String favoriteId) {
        return firestore.collection(FAVORITES_COLLECTION).document(favoriteId).delete();
    }
    
    public Query getFavoritesByUser(String userId) {
        return firestore.collection(FAVORITES_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING);
    }
    
    public Query checkIfFavorite(String userId, String shopId) {
        return firestore.collection(FAVORITES_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("shopId", shopId)
                .limit(1);
    }
    
    public Task<Void> removeFavoriteByUserAndShop(String userId, String shopId) {
        return checkIfFavorite(userId, shopId).get()
                .continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        String favoriteId = task.getResult().getDocuments().get(0).getId();
                        return firestore.collection(FAVORITES_COLLECTION).document(favoriteId).delete();
                    }
                    return null;
                });
    }
    
    public Query getFavoriteShopsByUser(String userId) {
        return firestore.collection(FAVORITES_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING);
    }
}
