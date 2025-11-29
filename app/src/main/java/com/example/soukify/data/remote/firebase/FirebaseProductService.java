package com.example.soukify.data.remote.firebase;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.example.soukify.data.models.ProductModel;

/**
 * Firebase Product Service - Handles product data operations
 * Follows MVVM pattern by providing clean separation from UI layer
 */
public class FirebaseProductService {
    private final FirebaseFirestore firestore;
    
    private static final String PRODUCTS_COLLECTION = "products";
    
    public FirebaseProductService(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }
    
    public Task<DocumentReference> createProduct(ProductModel product) {
        return firestore.collection(PRODUCTS_COLLECTION).add(product);
    }
    
    public Task<Void> updateProduct(String productId, ProductModel product) {
        return firestore.collection(PRODUCTS_COLLECTION).document(productId).set(product);
    }
    
    public Task<Void> deleteProduct(String productId) {
        return firestore.collection(PRODUCTS_COLLECTION).document(productId).delete();
    }
    
    public Task<ProductModel> getProduct(String productId) {
        return firestore.collection(PRODUCTS_COLLECTION).document(productId).get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        return task.getResult().toObject(ProductModel.class);
                    }
                    return null;
                });
    }
    
    public Query getProductsByShop(String shopId) {
        return firestore.collection(PRODUCTS_COLLECTION)
                .whereEqualTo("shopId", shopId)
                .orderBy("createdAt", Query.Direction.DESCENDING);
    }
    
    public Query getAllProducts() {
        return firestore.collection(PRODUCTS_COLLECTION).orderBy("createdAt", Query.Direction.DESCENDING);
    }
    
    public Query searchProducts(String query) {
        return firestore.collection(PRODUCTS_COLLECTION)
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + "\uf8ff");
    }
    
    public Query getProductsByCategory(String category) {
        return firestore.collection(PRODUCTS_COLLECTION)
                .whereEqualTo("category", category)
                .orderBy("createdAt", Query.Direction.DESCENDING);
    }
    
    public Task<QuerySnapshot> getFeaturedProducts() {
        return firestore.collection(PRODUCTS_COLLECTION)
                .whereEqualTo("featured", true)
                .orderBy("rating", Query.Direction.DESCENDING)
                .limit(20)
                .get();
    }
    
    public Task<Void> updateProductStock(String productId, int newStock) {
        return firestore.collection(PRODUCTS_COLLECTION).document(productId)
                .update("stock", newStock);
    }
    
    public Task<Void> updateProductRating(String productId, double newRating) {
        return firestore.collection(PRODUCTS_COLLECTION).document(productId)
                .update("rating", newRating);
    }
}
