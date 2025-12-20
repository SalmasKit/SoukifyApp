package com.example.soukify.data.remote.firebase;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.example.soukify.data.models.ProductModel;

import java.util.ArrayList;
import java.util.List;

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
    
    public Task<QuerySnapshot> getProductsByShop(String shopId) {
        return firestore.collection(PRODUCTS_COLLECTION)
                .whereEqualTo("shopId", shopId)
                .get();
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
    
    /**
     * Delete product with cascade - deletes product and all related images
     */
    public Task<Void> deleteProductWithCascade(String productId, FirebaseProductImageService productImageService) {
        // Get the product first to find its imageId
        return getProduct(productId).continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                android.util.Log.w("FirebaseProductService", "Product not found for deletion: " + productId);
                return deleteProduct(productId); // Just delete the product if it exists
            }
            
            ProductModel product = task.getResult();
            String imageId = product.getPrimaryImageId();
            
            if (imageId != null && !imageId.isEmpty()) {
                android.util.Log.d("FirebaseProductService", "Deleting primary image with ID: " + imageId + " for product: " + productId);
                // Delete the specific image by its ID
                return productImageService.deleteProductImageById(imageId)
                        .continueWithTask(deleteImageTask -> {
                            // After deleting image, delete the product
                            android.util.Log.d("FirebaseProductService", "Deleting product: " + productId);
                            return deleteProduct(productId);
                        });
            } else {
                android.util.Log.d("FirebaseProductService", "No image to delete for product: " + productId);
                // No image to delete, just delete the product
                return deleteProduct(productId);
            }
        });
    }
    
    /**
     * Delete all products for a specific shop with cascade
     */
    public Task<Void> deleteAllProductsForShop(String shopId, FirebaseProductImageService productImageService) {
        return getProductsByShop(shopId).continueWithTask(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<String> imageIdsToDelete = new ArrayList<>();
                List<Task<Void>> deleteTasks = new ArrayList<>();
                
                // Collect all imageIds from products
                for (com.google.firebase.firestore.QueryDocumentSnapshot document : task.getResult()) {
                    ProductModel product = document.toObject(ProductModel.class);
                    String productId = document.getId();
                    String imageId = product.getPrimaryImageId();
                    
                    if (imageId != null && !imageId.isEmpty()) {
                        imageIdsToDelete.add(imageId);
                        android.util.Log.d("FirebaseProductService", "Collected imageId: " + imageId + " for product: " + productId);
                    }
                    
                    // Add product deletion task
                    deleteTasks.add(deleteProduct(productId));
                }
                
                android.util.Log.d("FirebaseProductService", "Found " + imageIdsToDelete.size() + " images to delete for shop: " + shopId);
                
                // Delete all images in batch first, then all products
                if (!imageIdsToDelete.isEmpty()) {
                    return productImageService.deleteProductImagesByIds(imageIdsToDelete)
                            .continueWithTask(deleteImagesTask -> {
                                android.util.Log.d("FirebaseProductService", "Image deletion completed, now deleting products");
                                return com.google.android.gms.tasks.Tasks.whenAll(deleteTasks);
                            });
                } else {
                    android.util.Log.d("FirebaseProductService", "No images to delete, only deleting products");
                    return com.google.android.gms.tasks.Tasks.whenAll(deleteTasks);
                }
            } else {
                return com.google.android.gms.tasks.Tasks.forException(task.getException());
            }
        });
    }
    
    /**
     * Like a product - increments like count and adds user to likedBy array
     */
    public Task<Void> likeProduct(String productId, String userId) {
        return firestore.collection(PRODUCTS_COLLECTION).document(productId)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        return com.google.android.gms.tasks.Tasks.forException(
                                new Exception("Product not found: " + productId));
                    }
                    
                    ProductModel product = task.getResult().toObject(ProductModel.class);
                    if (product == null) {
                        return com.google.android.gms.tasks.Tasks.forException(
                                new Exception("Product data not found: " + productId));
                    }
                    
                    // Like logic is now handled by FavoritesTableRepository
                    // Just return the product as-is
                    return updateProduct(productId, product);
                });
    }
    
    /**
     * Unlike a product - decrements like count and removes user from likedBy array
     */
    public Task<Void> unlikeProduct(String productId, String userId) {
        return firestore.collection(PRODUCTS_COLLECTION).document(productId)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        return com.google.android.gms.tasks.Tasks.forException(
                                new Exception("Product not found: " + productId));
                    }
                    
                    ProductModel product = task.getResult().toObject(ProductModel.class);
                    if (product == null) {
                        return com.google.android.gms.tasks.Tasks.forException(
                                new Exception("Product data not found: " + productId));
                    }
                    
                    // Like logic is now handled by FavoritesTableRepository
                    // Just return the product as-is
                    return updateProduct(productId, product);
                });
    }
    
    /**
     * Favorite a product - increments favorite count and adds user to favoritedBy array
     */
    public Task<Void> favoriteProduct(String productId, String userId) {
        return firestore.collection(PRODUCTS_COLLECTION).document(productId)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        return com.google.android.gms.tasks.Tasks.forException(
                                new Exception("Product not found: " + productId));
                    }
                    
                    ProductModel product = task.getResult().toObject(ProductModel.class);
                    if (product == null) {
                        return com.google.android.gms.tasks.Tasks.forException(
                                new Exception("Product data not found: " + productId));
                    }
                    
                    // Favorite logic is now handled by FavoritesTableRepository
                    // Just return the product as-is
                    return updateProduct(productId, product);
                });
    }
    
    /**
     * Unfavorite a product - decrements favorite count and removes user from favoritedBy array
     */
    public Task<Void> unfavoriteProduct(String productId, String userId) {
        return firestore.collection(PRODUCTS_COLLECTION).document(productId)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        return com.google.android.gms.tasks.Tasks.forException(
                                new Exception("Product not found: " + productId));
                    }
                    
                    ProductModel product = task.getResult().toObject(ProductModel.class);
                    if (product == null) {
                        return com.google.android.gms.tasks.Tasks.forException(
                                new Exception("Product data not found: " + productId));
                    }
                    
                    // Favorite logic is now handled by FavoritesTableRepository
                    // Just return the product as-is
                    return updateProduct(productId, product);
                });
    }
}
