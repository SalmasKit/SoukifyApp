package com.example.soukify.ui.shop;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.soukify.data.models.ProductModel;
import com.example.soukify.data.models.ProductImageModel;
import com.example.soukify.data.repositories.ProductRepository;
import com.example.soukify.data.remote.firebase.FirebaseProductImageService;
import com.example.soukify.data.remote.FirebaseManager;
import com.example.soukify.utils.ImageUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Centralized manager for all product operations
 * Handles CRUD operations, image management, and data state
 */
public class ProductManager {
    
    private static final String TAG = "ProductManager";
    
    private final ProductRepository productRepository;
    private final FirebaseProductImageService firebaseProductImageService;
    private final Context context;
    private final ExecutorService executorService;
    private final MutableLiveData<List<ProductModel>> products = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    
    private String currentShopId;
    
    public ProductManager(Application application) {
        this.productRepository = new ProductRepository(application);
        FirebaseManager firebaseManager = FirebaseManager.getInstance(application);
        this.firebaseProductImageService = new FirebaseProductImageService(firebaseManager.getFirestore());
        this.context = application.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
    }
    
    // ==================== PRODUCT LOADING ====================
    
    /**
     * Load products for a specific shop
     */
    public void loadProductsForShop(String shopId) {
        if (shopId == null || shopId.isEmpty()) {
            Log.w(TAG, "Shop ID is null or empty, cannot load products");
            return;
        }
        
        Log.d(TAG, "Loading products for shop: " + shopId);
        this.currentShopId = shopId;
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        productRepository.loadShopProducts(shopId);
        observeProductData();
    }
    
    /**
     * Refresh products for the current shop
     */
    public void refreshProducts() {
        if (currentShopId != null && !currentShopId.isEmpty()) {
            loadProductsForShop(currentShopId);
        }
    }
    
    // ==================== PRODUCT CREATION ====================
    
    /**
     * Add a new product without images
     */
    public void addProduct(String name, String description, double price, String productType) {
        addProduct(name, description, price, productType, null, null, null, null, null, null, null);
    }
    
    /**
     * Add a new product with single image
     */
    public void addProduct(String name, String description, double price, String productType, String imageUriString) {
        addProduct(name, description, price, productType, imageUriString, null, null, null, null, null, null);
    }
    
    /**
     * Add a new product with single image and optional details
     */
    public void addProduct(String name, String description, double price, String productType, String imageUriString, 
                          Double weight, Double length, Double width, Double height, String color, String material) {
        if (!validateProductInput(name, description, price, productType)) {
            return;
        }
        
        Log.d(TAG, "Adding product: " + name + " to shop: " + currentShopId);
        
        if (imageUriString != null && !imageUriString.isEmpty()) {
            List<String> imageUris = new ArrayList<>();
            imageUris.add(imageUriString);
            addProductWithMultipleImages(name, description, price, productType, imageUris, weight, length, width, height, color, material);
        } else {
            createProductWithImageIds(name, description, price, productType, null, weight, length, width, height, color, material);
        }
    }
    
    /**
     * Add a new product with multiple images
     */
    public void addProductWithMultipleImages(String name, String description, double price, String productType, List<String> imageUriStrings) {
        addProductWithMultipleImages(name, description, price, productType, imageUriStrings, null, null, null, null, null, null);
    }
    
    /**
     * Add a new product with multiple images and optional details
     */
    public void addProductWithMultipleImages(String name, String description, double price, String productType, List<String> imageUriStrings, 
                                           Double weight, Double length, Double width, Double height, String color, String material) {
        if (!validateProductInput(name, description, price, productType)) {
            return;
        }
        
        if (imageUriStrings == null || imageUriStrings.isEmpty()) {
            Log.d(TAG, "No images provided, creating product without images");
            createProductWithImageIds(name, description, price, productType, null, weight, length, width, height, color, material);
            return;
        }
        
        Log.d(TAG, "Adding product with " + imageUriStrings.size() + " images: " + name);
        
        List<String> imageIds = new ArrayList<>();
        int[] processedCount = {0};
        int totalImages = imageUriStrings.size();
        
        for (String imageUriString : imageUriStrings) {
            processImageAndCollectId(name, imageUriString, imageIds, processedCount, totalImages, description, price, productType, weight, length, width, height, color, material);
        }
    }
    
    // ==================== PRODUCT UPDATE ====================
    
    /**
     * Update a product (without changing images)
     */
    public void updateProduct(ProductModel product) {
        updateProduct(product, null);
    }
    
    /**
     * Update a product with optional new image
     */
    public void updateProduct(ProductModel product, String newImageUriString) {
        if (product == null || product.getProductId() == null) {
            Log.w(TAG, "Cannot update null product or product without ID");
            errorMessage.setValue("Invalid product");
            return;
        }
        
        Log.d(TAG, "Updating product: " + product.getName() + " (ID: " + product.getProductId() + ")");
        
        if (newImageUriString != null && !newImageUriString.isEmpty()) {
            uploadNewImageAndUpdate(product, newImageUriString);
        } else {
            productRepository.updateProduct(product);
            successMessage.setValue("Product updated successfully");
            Log.d(TAG, "Product updated in repository");
        }
    }
    
    /**
     * Update product type
     */
    public void updateProductType(ProductModel product, String newProductType) {
        if (product == null || product.getProductId() == null) {
            Log.w(TAG, "Cannot update null product or product without ID");
            errorMessage.setValue("Invalid product");
            return;
        }
        
        if (newProductType == null || newProductType.trim().isEmpty()) {
            Log.w(TAG, "Cannot update product with empty type name");
            errorMessage.setValue("Product type cannot be empty");
            return;
        }
        
        Log.d(TAG, "Updating product type to: " + newProductType);
        product.setProductType(newProductType.trim());
        updateProduct(product);
    }
    
    // ==================== PRODUCT DELETION ====================
    
    /**
     * Delete a product and its associated images/videos
     */
    public void deleteProduct(ProductModel product) {
        if (product == null || product.getProductId() == null) {
            Log.w(TAG, "Cannot delete null product or product without ID");
            errorMessage.setValue("Invalid product");
            return;
        }
        
        Log.d(TAG, "Deleting product: " + product.getName() + " (ID: " + product.getProductId() + ")");
        
        // Delete product images/videos if exist
        if (product.hasImages()) {
            deleteProductImages(product);
        }
        
        // Delete product from repository
        productRepository.deleteProduct(product.getProductId(), product.getShopId());
        successMessage.setValue("Product deleted successfully");
        Log.d(TAG, "Product deleted from repository");
    }
    
    // ==================== IMAGE OPERATIONS ====================
    
    /**
     * Get product image URL by ID
     */
    public void getProductImageUrl(String imageId, ImageUrlCallback callback) {
        if (imageId == null || imageId.isEmpty()) {
            callback.onResult(null);
            return;
        }
        
        firebaseProductImageService.getProductImage(imageId)
            .addOnSuccessListener(productImage -> {
                String url = productImage != null ? productImage.getImageUrl() : null;
                callback.onResult(url);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting product image URL: " + e.getMessage(), e);
                callback.onResult(null);
            });
    }
    
    /**
     * Get all product image URLs for a product
     */
    public void getProductImageUrls(ProductModel product, ImageUrlsCallback callback) {
        List<String> imageUrls = new ArrayList<>();
        
        if (product == null || !product.hasImages()) {
            callback.onResult(imageUrls);
            return;
        }
        
        List<String> imageIds = product.getImageIds();
        int[] processedCount = {0};
        
        for (String imageId : imageIds) {
            getProductImageUrl(imageId, url -> {
                if (url != null) {
                    synchronized (imageUrls) {
                        imageUrls.add(url);
                    }
                }
                
                synchronized (processedCount) {
                    processedCount[0]++;
                    if (processedCount[0] == imageIds.size()) {
                        callback.onResult(imageUrls);
                    }
                }
            });
        }
    }
    
    // ==================== VALIDATION ====================
    
    /**
     * Validate product input
     */
    public boolean validateProductInput(String name, String description, double price, String type) {
        if (currentShopId == null || currentShopId.isEmpty()) {
            errorMessage.setValue("No shop selected");
            return false;
        }
        
        if (name == null || name.trim().isEmpty()) {
            errorMessage.setValue("Product name is required");
            return false;
        }
        
        if (description == null || description.trim().isEmpty()) {
            errorMessage.setValue("Product description is required");
            return false;
        }
        
        if (price <= 0) {
            errorMessage.setValue("Product price must be greater than 0");
            return false;
        }
        
        if (type == null || type.trim().isEmpty()) {
            errorMessage.setValue("Product type is required");
            return false;
        }
        
        return true;
    }
    
    // ==================== LIVEDATA GETTERS ====================
    
    public LiveData<List<ProductModel>> getProducts() {
        return products;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }
    
    public void clearError() {
        errorMessage.setValue(null);
    }
    
    public void clearSuccess() {
        successMessage.setValue(null);
    }
    
    public String getCurrentShopId() {
        return currentShopId;
    }
    
    public String getProductTypeName(ProductModel product) {
        return product != null ? product.getProductType() : null;
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private void processImageAndCollectId(String productName, String imageUriString, List<String> imageIds, 
                                         int[] processedCount, int totalImages, String description, 
                                         double price, String productType, Double weight, Double length, 
                                         Double width, Double height, String color, String material) {
        try {
            Uri mediaUri = Uri.parse(imageUriString);
            
            ImageUtils.uploadMediaToLocalStorage(context, mediaUri, "product", productName)
                .addOnSuccessListener(uri -> {
                    Log.d(TAG, "Product media uploaded: " + uri.toString());
                    
                    ProductImageModel productImage = new ProductImageModel();
                    productImage.setImageUrl(uri.toString());
                    
                    firebaseProductImageService.createProductImage(productImage)
                        .addOnSuccessListener(documentReference -> {
                            String createdImageId = documentReference.getId();
                            Log.d(TAG, "Product image created with ID: " + createdImageId);
                            
                            synchronized (imageIds) {
                                imageIds.add(createdImageId);
                                processedCount[0]++;
                                
                                if (processedCount[0] == totalImages) {
                                    Log.d(TAG, "All " + totalImages + " images processed");
                                    createProductWithImageIds(productName, description, price, productType, imageIds, weight, length, width, height, color, material);
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to create product image: " + e.getMessage(), e);
                            ImageUtils.deleteImageFromFirebaseStorage("product", productName);
                            handleImageProcessingFailure(imageIds, processedCount, totalImages, productName, description, price, productType, weight, length, width, height, color, material);
                        });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload product media: " + e.getMessage(), e);
                    handleImageProcessingFailure(imageIds, processedCount, totalImages, productName, description, price, productType, weight, length, width, height, color, material);
                });
        } catch (Exception e) {
            Log.e(TAG, "Error parsing image URI: " + e.getMessage(), e);
            handleImageProcessingFailure(imageIds, processedCount, totalImages, productName, description, price, productType, weight, length, width, height, color, material);
        }
    }
    
    private void handleImageProcessingFailure(List<String> imageIds, int[] processedCount, int totalImages,
                                             String productName, String description, double price, String productType,
                                             Double weight, Double length, Double width, Double height, String color, String material) {
        synchronized (imageIds) {
            processedCount[0]++;
            
            if (processedCount[0] == totalImages) {
                Log.d(TAG, "All images processed, creating product with " + imageIds.size() + " images");
                if (imageIds.isEmpty()) {
                    createProductWithImageIds(productName, description, price, productType, null, weight, length, width, height, color, material);
                } else {
                    createProductWithImageIds(productName, description, price, productType, imageIds, weight, length, width, height, color, material);
                }
            }
        }
    }
    
    private void createProductWithImageIds(String name, String description, double price, String productType, List<String> imageIds,
                                         Double weight, Double length, Double width, Double height, String color, String material) {
        ProductModel product = new ProductModel(
            currentShopId,
            name.trim(),
            description != null ? description.trim() : "",
            productType,
            price,
            "MAD",
            imageIds
        );
        
        product.setWeight(weight);
        product.setLength(length);
        product.setWidth(width);
        product.setHeight(height);
        product.setColor(color);
        product.setMaterial(material);
        
        Log.d(TAG, "Creating product with productType: " + productType + ", imageCount: " + 
            (imageIds != null ? imageIds.size() : 0));
        
        productRepository.createProduct(product);
        successMessage.setValue("Product added successfully!");
    }
    
    private void uploadNewImageAndUpdate(ProductModel product, String newImageUriString) {
        try {
            Uri newImageUri = Uri.parse(newImageUriString);
            
            ImageUtils.uploadMediaToLocalStorage(context, newImageUri, "product", product.getName())
                .addOnSuccessListener(uri -> {
                    Log.d(TAG, "New product image uploaded: " + uri.toString());
                    
                    // Delete old images
                    if (product.hasImages()) {
                        deleteProductImages(product);
                    }
                    
                    // Create new image document
                    ProductImageModel newImageModel = new ProductImageModel();
                    newImageModel.setImageUrl(uri.toString());
                    
                    firebaseProductImageService.createProductImage(newImageModel)
                        .addOnSuccessListener(documentReference -> {
                            String newImageId = documentReference.getId();
                            Log.d(TAG, "New product image created with ID: " + newImageId);
                            
                            List<String> newImageIds = new ArrayList<>();
                            newImageIds.add(newImageId);
                            product.setImageIds(newImageIds);
                            productRepository.updateProduct(product);
                            successMessage.setValue("Product updated successfully");
                            Log.d(TAG, "Product updated with new image");
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to create new image document: " + e.getMessage(), e);
                            ImageUtils.deleteImageFromFirebaseStorage("product", product.getName());
                            errorMessage.setValue("Failed to update product image");
                        });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload new product image: " + e.getMessage(), e);
                    errorMessage.setValue("Failed to upload product image");
                });
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing new image URI: " + e.getMessage(), e);
            errorMessage.setValue("Invalid image");
        }
    }
    
    private void deleteProductImages(ProductModel product) {
        List<String> imageIds = product.getImageIds();
        Log.d(TAG, "Deleting " + imageIds.size() + " product images");
        
        for (String imageId : imageIds) {
            if (imageId != null && !imageId.isEmpty()) {
                firebaseProductImageService.getProductImage(imageId)
                    .addOnSuccessListener(productImage -> {
                        if (productImage != null && productImage.getImageUrl() != null) {
                            String mediaUrl = productImage.getImageUrl();
                            
                            ImageUtils.deleteMediaFromLocalStorage(mediaUrl)
                                .addOnSuccessListener(deleted -> {
                                    if (deleted) {
                                        Log.d(TAG, "Local media file deleted: " + mediaUrl);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error deleting local media file: " + mediaUrl, e);
                                });
                        }
                        
                        firebaseProductImageService.deleteProductImage(imageId)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Product image document deleted: " + imageId);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to delete product image document: " + imageId, e);
                            });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get product image for deletion: " + imageId, e);
                    });
            }
        }
        
        // Cleanup any remaining files
        ImageUtils.deleteAllMediaForEntity(context, "product", product.getName())
            .addOnSuccessListener(deletedCount -> {
                Log.d(TAG, "Cleaned up " + deletedCount + " additional media files");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error cleaning up media files", e);
            });
    }
    
    private void observeProductData() {
        productRepository.getShopProducts().observeForever(productList -> {
            Log.d(TAG, "Products received: " + (productList != null ? productList.size() : 0) + " items");
            products.postValue(productList);
            isLoading.postValue(false);
        });
        
        productRepository.getIsLoading().observeForever(loading -> {
            if (Boolean.TRUE.equals(loading)) {
                isLoading.postValue(true);
            }
        });
        
        productRepository.getErrorMessage().observeForever(error -> {
            if (error != null && !error.isEmpty()) {
                Log.e(TAG, "Repository error: " + error);
                errorMessage.postValue(error);
                isLoading.postValue(false);
            }
        });
    }
    
    // ==================== CALLBACK INTERFACES ====================
    
    public interface ImageUrlCallback {
        void onResult(String url);
    }
    
    public interface ImageUrlsCallback {
        void onResult(List<String> urls);
    }
}

