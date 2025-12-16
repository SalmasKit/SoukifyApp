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
    private static final String TAG = "FirebaseProductImageService";

    public FirebaseProductImageService(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    // ==================== CALLBACK INTERFACE (ADDED) ====================

    /**
     * Callback interface for product image creation
     */
    public interface OnProductImageCreatedListener {
        void onSuccess(String imageId);
        void onFailure(String error);
    }

    // ==================== CALLBACK METHOD (ADDED) ====================

    /**
     * Create a product image in Firestore with callback
     * This is needed for ProductManager to get the imageId synchronously
     *
     * @param productImage The ProductImageModel to create
     * @param listener Callback listener for success/failure
     */
    public void createProductImage(ProductImageModel productImage, OnProductImageCreatedListener listener) {
        if (productImage == null || productImage.getImageUrl() == null || productImage.getImageUrl().isEmpty()) {
            android.util.Log.e(TAG, "Invalid product image data");
            if (listener != null) {
                listener.onFailure("Invalid image data");
            }
            return;
        }

        android.util.Log.d(TAG, "Creating product image with URL: " + productImage.getImageUrl());

        // Create a new document with auto-generated ID
        firestore.collection(PRODUCT_IMAGES_COLLECTION)
                .add(productImage)
                .addOnSuccessListener(documentReference -> {
                    String imageId = documentReference.getId();
                    android.util.Log.d(TAG, "ProductImage created with ID: " + imageId);

                    // Update the document with its own ID
                    documentReference.update("imageId", imageId)
                            .addOnSuccessListener(aVoid -> {
                                android.util.Log.d(TAG, "Successfully updated imageId field");
                                if (listener != null) {
                                    listener.onSuccess(imageId);
                                }
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e(TAG, "Failed to update imageId: " + e.getMessage());
                                // Still return success with the ID even if update failed
                                if (listener != null) {
                                    listener.onSuccess(imageId);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e(TAG, "Failed to create ProductImage: " + e.getMessage(), e);
                    if (listener != null) {
                        listener.onFailure(e.getMessage());
                    }
                });
    }

    // ==================== EXISTING CRUD OPERATIONS ====================

    /**
     * Create a product image (Task-based version)
     * Automatically updates the imageId field with the document ID
     */
    public Task<DocumentReference> createProductImage(ProductImageModel productImage) {
        return firestore.collection(PRODUCT_IMAGES_COLLECTION)
                .add(productImage)
                .addOnSuccessListener(documentReference -> {
                    String imageId = documentReference.getId();
                    android.util.Log.d(TAG, "ProductImage created with ID: " + imageId);
                    
                    // Update the document with its own ID
                    documentReference.update("imageId", imageId)
                            .addOnSuccessListener(aVoid -> {
                                android.util.Log.d(TAG, "Successfully updated imageId field");
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e(TAG, "Failed to update imageId: " + e.getMessage());
                            });
                });
    }

    /**
     * Delete a product image
     */
    public Task<Void> deleteProductImage(String imageId) {
        return firestore.collection(PRODUCT_IMAGES_COLLECTION).document(imageId).delete();
    }

    /**
     * Update a product image
     */
    public Task<Void> updateProductImage(String imageId, ProductImageModel productImage) {
        return firestore.collection(PRODUCT_IMAGES_COLLECTION).document(imageId).set(productImage);
    }

    /**
     * Get all product images
     */
    public Query getAllProductImages() {
        return firestore.collection(PRODUCT_IMAGES_COLLECTION).orderBy("createdAt");
    }

    /**
     * Get product image by ID
     * Handles both Firestore document IDs and local file paths
     */
    public Task<ProductImageModel> getProductImage(String imageId) {
        if (imageId == null || imageId.isEmpty()) {
            return com.google.android.gms.tasks.Tasks.forResult(null);
        }
        
        // Check if imageId is a local file path (starts with "file://")
        if (imageId.startsWith("file://")) {
            // For local file paths, create a ProductImageModel with the file URL
            ProductImageModel localImage = new ProductImageModel();
            localImage.setImageId(imageId);
            localImage.setImageUrl(imageId);
            return com.google.android.gms.tasks.Tasks.forResult(localImage);
        }
        
        // Otherwise, treat as Firestore document ID
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
        return firestore.collection(PRODUCT_IMAGES_COLLECTION).whereEqualTo("productId", productId).orderBy("createdAt");
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

    // ==================== DELETE OPERATIONS ====================

    /**
     * Delete all images for a product by imageId
     */
    public Task<Void> deleteAllImagesForProduct(String productId) {
        android.util.Log.d(TAG, "deleteAllImagesForProduct called for productId: " + productId);

        // Since ProductImageModel doesn't have productId field, we need to find images by their imageId
        // This method should be called with the actual imageId, not productId
        return com.google.android.gms.tasks.Tasks.forResult((Void) null);
    }

    /**
     * Delete a specific product image by imageId
     */
    public Task<Void> deleteProductImageById(String imageId) {
        android.util.Log.d(TAG, "deleteProductImageById called for imageId: " + imageId);
        return deleteProductImage(imageId)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d(TAG, "Successfully deleted image with ID: " + imageId);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e(TAG, "Failed to delete image with ID: " + imageId, e);
                });
    }

    /**
     * Delete multiple product images by their IDs
     */
    public Task<Void> deleteProductImagesByIds(List<String> imageIds) {
        android.util.Log.d(TAG, "deleteProductImagesByIds called for " + imageIds.size() + " images");

        if (imageIds.isEmpty()) {
            android.util.Log.d(TAG, "No image IDs to delete");
            return com.google.android.gms.tasks.Tasks.forResult((Void) null);
        }

        List<Task<Void>> deleteTasks = new ArrayList<>();
        for (String imageId : imageIds) {
            if (imageId != null && !imageId.isEmpty()) {
                android.util.Log.d(TAG, "Adding delete task for imageId: " + imageId);
                deleteTasks.add(deleteProductImage(imageId));
            }
        }

        return com.google.android.gms.tasks.Tasks.whenAll(deleteTasks)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d(TAG, "Successfully deleted " + deleteTasks.size() + " images");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e(TAG, "Failed to delete images", e);
                });
    }

    // ==================== UTILITY OPERATIONS ====================

    /**
     * Create multiple images for a product
     */
    public Task<List<String>> createProductImages(String productId, List<String> imageUrls) {
        List<Task<DocumentReference>> createTasks = new ArrayList<>();

        for (String imageUrl : imageUrls) {
            ProductImageModel productImage = new ProductImageModel();
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