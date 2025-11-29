package com.example.soukify.data.remote.firebase;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.soukify.data.models.ProductImageModel;

import java.util.List;
import java.util.ArrayList;

/**
 * Firebase Product Image Service - Handles product image operations
 * Follows MVVM pattern by providing clean separation from UI layer
 */
public class FirebaseProductImageService {
    private final FirebaseFirestore firestore;
    
    private static final String PRODUCT_IMAGES_COLLECTION = "product_images";
    
    public FirebaseProductImageService(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }
    
    // CRUD Operations
    
    /**
     * Create a product image
     */
    public Task<DocumentReference> createProductImage(ProductImageModel productImage) {
        return firestore.collection(PRODUCT_IMAGES_COLLECTION).add(productImage);
    }
    
    /**
     * Update a product image
     */
    public Task<Void> updateProductImage(String imageId, ProductImageModel productImage) {
        return firestore.collection(PRODUCT_IMAGES_COLLECTION).document(imageId).set(productImage);
    }
    
    /**
     * Delete a product image
     */
    public Task<Void> deleteProductImage(String imageId) {
        return firestore.collection(PRODUCT_IMAGES_COLLECTION).document(imageId).delete();
    }
    
    // Query Operations
    
    /**
     * Get all product images
     */
    public Query getAllProductImages() {
        return firestore.collection(PRODUCT_IMAGES_COLLECTION).orderBy("createdAt");
    }
    
    /**
     * Get product image by ID
     */
    public Task<ProductImageModel> getProductImage(String imageId) {
        return firestore.collection(PRODUCT_IMAGES_COLLECTION).document(imageId).get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        return task.getResult().toObject(ProductImageModel.class);
                    }
                    return null;
                });
    }
    
    /**
     * Get images by product ID
     */
    public Query getImagesByProductId(String productId) {
        return firestore.collection(PRODUCT_IMAGES_COLLECTION)
                .whereEqualTo("productId", productId)
                .orderBy("createdAt");
    }
    
    /**
     * Get images by product ID as Task for async operations
     */
    public Task<List<ProductImageModel>> getImagesByProductIdAsync(String productId) {
        return firestore.collection(PRODUCT_IMAGES_COLLECTION)
                .whereEqualTo("productId", productId)
                .orderBy("createdAt")
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<ProductImageModel> images = new ArrayList<>();
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            ProductImageModel image = doc.toObject(ProductImageModel.class);
                            if (image != null) {
                                image.setImageId(doc.getId());
                                images.add(image);
                            }
                        }
                        return images;
                    }
                    return new ArrayList<>();
                });
    }
    
    /**
     * Delete all images for a product
     */
    public Task<Void> deleteAllImagesForProduct(String productId) {
        return getImagesByProductIdAsync(productId).continueWithTask(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<ProductImageModel> images = task.getResult();
                List<Task<Void>> deleteTasks = new ArrayList<>();
                
                for (ProductImageModel image : images) {
                    deleteTasks.add(deleteProductImage(image.getImageId()));
                }
                
                return com.google.android.gms.tasks.Tasks.whenAll(deleteTasks);
            }
            return com.google.android.gms.tasks.Tasks.forResult(null);
        });
    }
    
    // Utility Operations
    
    /**
     * Create multiple images for a product
     */
    public Task<List<String>> createProductImages(String productId, List<String> imageUrls) {
        List<Task<DocumentReference>> createTasks = new ArrayList<>();
        
        for (String imageUrl : imageUrls) {
            ProductImageModel productImage = new ProductImageModel();
            productImage.setProductId(productId);
            productImage.setImageUrl(imageUrl);
            
            createTasks.add(createProductImage(productImage));
        }
        
        return com.google.android.gms.tasks.Tasks.whenAllSuccess(createTasks)
                .continueWith(task -> {
                    List<String> imageIds = new ArrayList<>();
                    for (Object result : task.getResult()) {
                        if (result instanceof DocumentReference) {
                            imageIds.add(((DocumentReference) result).getId());
                        }
                    }
                    return imageIds;
                });
    }
}
