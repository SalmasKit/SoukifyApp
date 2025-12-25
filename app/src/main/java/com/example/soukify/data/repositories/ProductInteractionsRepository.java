package com.example.soukify.data.repositories;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.HashMap;
import java.util.Map;

/**
 * Unified repository for product likes and favorites
 * Clean, simple implementation using Firestore
 * 
 * Structure:
 * - userProductInteractions/{userId}/products/{productId}
 *   {
 *     liked: boolean,
 *     favorited: boolean,
 *     timestamp: timestamp
 *   }
 * - products/{productId}
 *   {
 *     likesCount: number,
 *     favoritesCount: number
 *   }
 */
public class ProductInteractionsRepository {
    private static final String TAG = "ProductInteractions";
    
    private static final String INTERACTIONS_COLLECTION = "userProductInteractions";
    private static final String PRODUCTS_COLLECTION = "products";
    private static final String USER_PRODUCTS_SUBCOLLECTION = "products";
    
    private final FirebaseFirestore firestore;
    private final FirebaseAuth firebaseAuth;
    
    public ProductInteractionsRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.firebaseAuth = FirebaseAuth.getInstance();
    }
    
    /**
     * Get current user ID
     */
    private String getCurrentUserId() {
        return firebaseAuth.getCurrentUser() != null ? 
            firebaseAuth.getCurrentUser().getUid() : null;
    }
    
    /**
     * Check if user is authenticated
     */
    public boolean isUserAuthenticated() {
        return firebaseAuth.getCurrentUser() != null;
    }
    
    /**
     * Toggle like for a product
     */
    public Task<Boolean> toggleLike(String productId) {
        String userId = getCurrentUserId();
        if (userId == null || productId == null) {
            return Tasks.forException(new Exception("User not authenticated or invalid product ID"));
        }
        
        DocumentReference userProductRef = firestore
            .collection(INTERACTIONS_COLLECTION)
            .document(userId)
            .collection(USER_PRODUCTS_SUBCOLLECTION)
            .document(productId);
        
        return firestore.runTransaction(transaction -> {
            // Get current state
            boolean currentlyLiked = false;
            try {
                var snapshot = transaction.get(userProductRef);
                if (snapshot.exists()) {
                    Boolean liked = snapshot.getBoolean("liked");
                    currentlyLiked = liked != null && liked;
                }
            } catch (Exception e) {
                Log.d(TAG, "Document doesn't exist yet, treating as not liked");
            }
            
            boolean newLikedState = !currentlyLiked;
            
            // Update user interaction
            Map<String, Object> data = new HashMap<>();
            data.put("liked", newLikedState);
            data.put("timestamp", FieldValue.serverTimestamp());
            transaction.set(userProductRef, data, SetOptions.merge());
            
            // Update product likes count
            DocumentReference productRef = firestore
                .collection(PRODUCTS_COLLECTION)
                .document(productId);
            
            if (newLikedState) {
                transaction.update(productRef, "likesCount", FieldValue.increment(1));
            } else {
                transaction.update(productRef, "likesCount", FieldValue.increment(-1));
            }
            
            Log.d(TAG, "✅ Toggled like: " + productId + " -> " + newLikedState);
            return newLikedState;
        });
    }
    
    /**
     * Toggle favorite for a product
     */
    public Task<Boolean> toggleFavorite(String productId) {
        String userId = getCurrentUserId();
        if (userId == null || productId == null) {
            return Tasks.forException(new Exception("User not authenticated or invalid product ID"));
        }
        
        DocumentReference userProductRef = firestore
            .collection(INTERACTIONS_COLLECTION)
            .document(userId)
            .collection(USER_PRODUCTS_SUBCOLLECTION)
            .document(productId);
        
        return firestore.runTransaction(transaction -> {
            // Get current state
            boolean currentlyFavorited = false;
            try {
                var snapshot = transaction.get(userProductRef);
                if (snapshot.exists()) {
                    Boolean favorited = snapshot.getBoolean("favorited");
                    currentlyFavorited = favorited != null && favorited;
                }
            } catch (Exception e) {
                Log.d(TAG, "Document doesn't exist yet, treating as not favorited");
            }
            
            boolean newFavoritedState = !currentlyFavorited;
            
            // Update user interaction
            Map<String, Object> data = new HashMap<>();
            data.put("favorited", newFavoritedState);
            data.put("timestamp", FieldValue.serverTimestamp());
            transaction.set(userProductRef, data, SetOptions.merge());
            
            // Update product favorites count
            DocumentReference productRef = firestore
                .collection(PRODUCTS_COLLECTION)
                .document(productId);
            
            if (newFavoritedState) {
                transaction.update(productRef, "favoritesCount", FieldValue.increment(1));
            } else {
                transaction.update(productRef, "favoritesCount", FieldValue.increment(-1));
            }
            
            Log.d(TAG, "⭐ Toggled favorite: " + productId + " -> " + newFavoritedState);
            return newFavoritedState;
        });
    }
    
    /**
     * Check if product is liked by current user
     */
    public Task<Boolean> isProductLiked(String productId) {
        String userId = getCurrentUserId();
        if (userId == null || productId == null) {
            return Tasks.forResult(false);
        }
        
        return firestore
            .collection(INTERACTIONS_COLLECTION)
            .document(userId)
            .collection(USER_PRODUCTS_SUBCOLLECTION)
            .document(productId)
            .get()
            .continueWith(task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    Boolean liked = task.getResult().getBoolean("liked");
                    return liked != null && liked;
                }
                return false;
            });
    }
    
    /**
     * Check if product is favorited by current user
     */
    public Task<Boolean> isProductFavorited(String productId) {
        String userId = getCurrentUserId();
        if (userId == null || productId == null) {
            return Tasks.forResult(false);
        }
        
        return firestore
            .collection(INTERACTIONS_COLLECTION)
            .document(userId)
            .collection(USER_PRODUCTS_SUBCOLLECTION)
            .document(productId)
            .get()
            .continueWith(task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    Boolean favorited = task.getResult().getBoolean("favorited");
                    return favorited != null && favorited;
                }
                return false;
            });
    }
    
    /**
     * Get user's product interaction state (liked, favorited)
     */
    public Task<Map<String, Boolean>> getProductInteractionState(String productId) {
        String userId = getCurrentUserId();
        if (userId == null || productId == null) {
            Map<String, Boolean> defaultState = new HashMap<>();
            defaultState.put("liked", false);
            defaultState.put("favorited", false);
            return Tasks.forResult(defaultState);
        }
        
        return firestore
            .collection(INTERACTIONS_COLLECTION)
            .document(userId)
            .collection(USER_PRODUCTS_SUBCOLLECTION)
            .document(productId)
            .get()
            .continueWith(task -> {
                Map<String, Boolean> state = new HashMap<>();
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    Boolean liked = task.getResult().getBoolean("liked");
                    Boolean favorited = task.getResult().getBoolean("favorited");
                    state.put("liked", liked != null && liked);
                    state.put("favorited", favorited != null && favorited);
                } else {
                    state.put("liked", false);
                    state.put("favorited", false);
                }
                return state;
            });
    }
}
