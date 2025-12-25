package com.example.soukify.data.repositories;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.soukify.data.remote.FirebaseManager;
import com.example.soukify.data.remote.firebase.FirebaseProductImageService;
import com.example.soukify.data.models.ProductImageModel;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Product Image Repository - Firebase implementation
 * Follows MVVM pattern by abstracting data operations from ViewModels
 */
public class ProductImageRepository {
    private final FirebaseProductImageService productImageService;
    private final MutableLiveData<List<ProductImageModel>> productImages = new MutableLiveData<>();
    private final MutableLiveData<ProductImageModel> currentProductImage = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(1);
    
    public ProductImageRepository(Application application) {
        FirebaseManager firebaseManager = FirebaseManager.getInstance(application);
        this.productImageService = new FirebaseProductImageService(firebaseManager.getFirestore());
    }
    
    // LiveData getters
    public LiveData<List<ProductImageModel>> getProductImages() {
        return productImages;
    }
    
    public LiveData<ProductImageModel> getCurrentProductImage() {
        return currentProductImage;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    // CRUD Operations
    
    public void createProductImage(String imageUrl) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        ProductImageModel productImage = new ProductImageModel(null, imageUrl);
        
        productImageService.createProductImage(productImage)
                .addOnSuccessListener(documentReference -> {
                    String imageId = documentReference.getId();
                    android.util.Log.d("ProductImageRepository", "Created product image with Firestore ID: " + imageId);
                    
                    productImage.setImageId(imageId);
                    android.util.Log.d("ProductImageRepository", "Set imageId in model: " + imageId);
                    
                    // Update the document to include the imageId field
                    productImageService.updateProductImage(imageId, productImage)
                            .addOnSuccessListener(aVoid -> {
                                android.util.Log.d("ProductImageRepository", "Updated product image with imageId: " + imageId);
                                loadAllProductImages(); // Refresh the list
                                isLoading.postValue(false);
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("ProductImageRepository", "Failed to update product image with ID: " + e.getMessage());
                                errorMessage.postValue("Failed to update product image with ID: " + e.getMessage());
                                isLoading.postValue(false);
                            });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ProductImageRepository", "Failed to create product image: " + e.getMessage());
                    errorMessage.postValue("Failed to create product image: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void updateProductImage(ProductImageModel productImage) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        productImageService.updateProductImage(productImage.getImageId(), productImage)
                .addOnSuccessListener(aVoid -> {
                    loadAllProductImages(); // Refresh the list
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to update product image: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void deleteProductImage(String imageId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        // Get the image first to know which product to refresh
        productImageService.getProductImage(imageId)
                .addOnSuccessListener(productImage -> {
                    if (productImage != null) {
                        productImageService.deleteProductImage(imageId)
                                .addOnSuccessListener(aVoid -> {
                                    loadAllProductImages(); // Refresh the list
                                })
                                .addOnFailureListener(e -> {
                                    errorMessage.postValue("Failed to delete product image: " + e.getMessage());
                                    isLoading.postValue(false);
                                });
                    } else {
                        errorMessage.postValue("Product image not found");
                        isLoading.postValue(false);
                    }
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to find product image: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    // Query Operations
    
    public void loadAllProductImages() {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        productImageService.getAllProductImages().get()
                .addOnSuccessListener(executor, querySnapshot -> {
                    List<ProductImageModel> images = new ArrayList<>();
                    for (var doc : querySnapshot.getDocuments()) {
                        try {
                            ProductImageModel image = doc.toObject(ProductImageModel.class);
                            if (image != null) {
                                image.setImageId(doc.getId());
                                images.add(image);
                            }
                        } catch (Exception e) {
                            Log.e("ProductImageRepository", "Error deserializing", e);
                        }
                    }
                    productImages.postValue(images);
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to load product images: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void loadProductImagesByProductId(String productId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        productImageService.getImagesByProductId(productId).get()
                .addOnSuccessListener(executor, querySnapshot -> {
                    List<ProductImageModel> images = new ArrayList<>();
                    for (var doc : querySnapshot.getDocuments()) {
                        try {
                            ProductImageModel image = doc.toObject(ProductImageModel.class);
                            if (image != null) {
                                image.setImageId(doc.getId());
                                images.add(image);
                            }
                        } catch (Exception e) {
                            Log.e("ProductImageRepository", "Error deserializing", e);
                        }
                    }
                    productImages.postValue(images);
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to load product images: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void getProductImageById(String imageId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        productImageService.getProductImage(imageId)
                .addOnSuccessListener(productImage -> {
                    currentProductImage.postValue(productImage);
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to load product image: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    // Utility Operations
    
    public void createProductImages(List<String> imageUrls) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        productImageService.createProductImages(null, imageUrls)
                .addOnSuccessListener(imageIds -> {
                    loadAllProductImages(); // Refresh the list
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to create product images: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void deleteAllImagesForProduct() {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        productImageService.deleteAllImagesForProduct(null)
                .addOnSuccessListener(aVoid -> {
                    productImages.postValue(new ArrayList<>()); // Clear the list
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to delete product images: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    // Synchronous methods for backward compatibility
    
    public List<ProductImageModel> getProductImagesByProductIdSync(String productId) {
        // Since we removed productId from ProductImageModel, return all images
        // This method is kept for backward compatibility but now returns all images
        return productImages.getValue() != null ? productImages.getValue() : new ArrayList<>();
    }
    
    public ProductImageModel getProductImageModelById(String imageId) {
        List<ProductImageModel> allImages = productImages.getValue();
        if (allImages != null) {
            for (ProductImageModel image : allImages) {
                if (image.getImageId().equals(imageId)) {
                    return image;
                }
            }
        }
        return null;
    }
}
