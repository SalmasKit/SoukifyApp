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
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        ProductModel product = task.getResult().toObject(ProductModel.class);
                        if (product != null) {
                            // Assigner l'ID du document au produit
                            product.setProductId(task.getResult().getId());
                        }
                        return product;
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
        return getProduct(productId).continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                android.util.Log.w("FirebaseProductService", "Product not found for deletion: " + productId);
                return deleteProduct(productId);
            }

            ProductModel product = task.getResult();

            // Utiliser la méthode helper getPrimaryImageId() qui existe dans ProductModel
            String primaryImageId = product.getPrimaryImageId();

            if (primaryImageId != null && !primaryImageId.isEmpty()) {
                android.util.Log.d("FirebaseProductService", "Deleting primary image with ID: " + primaryImageId + " for product: " + productId);
                return productImageService.deleteProductImageById(primaryImageId)
                        .continueWithTask(deleteImageTask -> {
                            android.util.Log.d("FirebaseProductService", "Deleting product: " + productId);
                            return deleteProduct(productId);
                        });
            } else {
                android.util.Log.d("FirebaseProductService", "No image to delete for product: " + productId);
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

                for (com.google.firebase.firestore.QueryDocumentSnapshot document : task.getResult()) {
                    ProductModel product = document.toObject(ProductModel.class);
                    product.setProductId(document.getId());

                    String productId = document.getId();

                    // Utiliser la méthode helper getPrimaryImageId()
                    String primaryImageId = product.getPrimaryImageId();

                    if (primaryImageId != null && !primaryImageId.isEmpty()) {
                        imageIdsToDelete.add(primaryImageId);
                        android.util.Log.d("FirebaseProductService", "Collected imageId: " + primaryImageId + " for product: " + productId);
                    }

                    deleteTasks.add(deleteProduct(productId));
                }

                android.util.Log.d("FirebaseProductService", "Found " + imageIdsToDelete.size() + " images to delete for shop: " + shopId);

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
     * Toggle like for a product
     * @param productId Product ID
     * @param userId User ID
     * @param isLiked New like state
     * @return Task with updated product
     */
    public Task<ProductModel> toggleLike(String productId, String userId, boolean isLiked) {
        return firestore.collection(PRODUCTS_COLLECTION).document(productId)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                        return com.google.android.gms.tasks.Tasks.forException(
                                new Exception("Product not found: " + productId));
                    }

                    ProductModel product = task.getResult().toObject(ProductModel.class);
                    if (product == null) {
                        return com.google.android.gms.tasks.Tasks.forException(
                                new Exception("Product data not found: " + productId));
                    }

                    product.setProductId(productId);

                    // Mettre à jour le compteur de likes
                    int currentLikes = product.getLikesCount();
                    if (isLiked) {
                        product.setLikesCount(currentLikes + 1);
                    } else {
                        product.setLikesCount(Math.max(0, currentLikes - 1));
                    }

                    // Sauvegarder dans Firestore
                    return firestore.collection(PRODUCTS_COLLECTION)
                            .document(productId)
                            .update("likesCount", product.getLikesCount())
                            .continueWith(updateTask -> {
                                if (updateTask.isSuccessful()) {
                                    android.util.Log.d("FirebaseProductService",
                                            "✅ Updated likesCount for product " + productId + " to " + product.getLikesCount());
                                    return product;
                                } else {
                                    android.util.Log.e("FirebaseProductService",
                                            "❌ Failed to update likesCount for product " + productId,
                                            updateTask.getException());
                                    throw updateTask.getException();
                                }
                            });
                });
    }

    /**
     * Ces méthodes sont maintenant dépréciées - utilisez toggleLike à la place
     */
    @Deprecated
    public Task<Void> likeProduct(String productId, String userId) {
        return toggleLike(productId, userId, true).continueWith(task -> null);
    }

    @Deprecated
    public Task<Void> unlikeProduct(String productId, String userId) {
        return toggleLike(productId, userId, false).continueWith(task -> null);
    }

    /**
     * Ces méthodes sont gérées par FavoritesTableRepository maintenant
     */
    @Deprecated
    public Task<Void> favoriteProduct(String productId, String userId) {
        android.util.Log.d("FirebaseProductService", "favoriteProduct called - use FavoritesTableRepository instead");
        return com.google.android.gms.tasks.Tasks.forResult(null);
    }

    @Deprecated
    public Task<Void> unfavoriteProduct(String productId, String userId) {
        android.util.Log.d("FirebaseProductService", "unfavoriteProduct called - use FavoritesTableRepository instead");
        return com.google.android.gms.tasks.Tasks.forResult(null);
    }
}