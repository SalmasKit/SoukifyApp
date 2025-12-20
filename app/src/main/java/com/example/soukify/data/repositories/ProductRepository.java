package com.example.soukify.data.repositories;

import android.app.Application;
import android.os.Handler;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.soukify.data.models.ProductModel;
import com.example.soukify.data.remote.FirebaseManager;
import com.example.soukify.data.remote.firebase.FirebaseProductService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.soukify.data.remote.firebase.FirebaseStorageService;
import com.example.soukify.data.models.ProductModel;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.Query;
import com.example.soukify.data.remote.firebase.FirebaseProductImageService;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Product Repository - Firebase implementation
 * Follows MVVM pattern by abstracting data operations from ViewModels
 */
public class ProductRepository {
    private static final String TAG = "ProductRepository";
    
    // Callback interfaces for async operations
    public interface OnProductLoadedListener {
        void onProductLoaded(ProductModel product);
        void onError(String error);
    }
    
    public interface OnProductsLoadedListener {
        void onProductsLoaded(List<ProductModel> products);
        void onError(String error);
    }
    
    public interface OnLikeToggledListener {
        void onLikeToggled(ProductModel updatedProduct, boolean isNowLiked);
        void onError(String error);
    }
    
    public interface OnFavoriteToggledListener {
        void onFavoriteToggled(ProductModel updatedProduct, boolean isNowFavorited);
        void onError(String error);
    }
    
    private final FirebaseProductService productService;
    private final FirebaseStorageService storageService;
    private final com.example.soukify.data.remote.firebase.FirebaseProductImageService productImageService;
    private final MutableLiveData<List<ProductModel>> shopProducts = new MutableLiveData<>();
    private final MutableLiveData<List<ProductModel>> allProducts = new MutableLiveData<>();
    private final MutableLiveData<ProductModel> currentProduct = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    
    public ProductRepository(Application application) {
        FirebaseManager firebaseManager = FirebaseManager.getInstance(application);
        this.productService = new FirebaseProductService(firebaseManager.getFirestore());
        this.storageService = new FirebaseStorageService(firebaseManager.getStorage());
        this.productImageService = new com.example.soukify.data.remote.firebase.FirebaseProductImageService(firebaseManager.getFirestore());
    }
    
    public LiveData<List<ProductModel>> getShopProducts() {
        return shopProducts;
    }
    
    public LiveData<List<ProductModel>> getAllProducts() {
        return allProducts;
    }
    
    public LiveData<ProductModel> getCurrentProduct() {
        return currentProduct;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public void createProduct(ProductModel product) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        productService.createProduct(product)
                .addOnSuccessListener(documentReference -> {
                    String productId = documentReference.getId();
                    product.setProductId(productId);
                    
                    // Update the document to include the productId field
                    product.setProductId(productId);
                    productService.updateProduct(productId, product)
                            .addOnSuccessListener(aVoid -> {
                                currentProduct.postValue(product);
                                loadShopProducts(product.getShopId()); // Refresh shop products
                                isLoading.postValue(false);
                            })
                            .addOnFailureListener(e -> {
                                errorMessage.postValue("Failed to update product with ID: " + e.getMessage());
                                isLoading.postValue(false);
                            });
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to create product: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void createProduct(String shopId, String name, String description, String productType, 
                            double price, int stock) {
        createProduct(shopId, name, description, productType, price, stock, "MAD");
    }
    
    public void createProduct(String shopId, String name, String description, String productType, 
                            double price, int stock, String currency) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        ProductModel product = new ProductModel(shopId, name, description, productType, price, currency);
        
        productService.createProduct(product)
                .addOnSuccessListener(documentReference -> {
                    product.setProductId(documentReference.getId());
                    currentProduct.postValue(product);
                    loadShopProducts(shopId); // Refresh shop products
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to create product: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    
    public void updateProduct(ProductModel product) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        productService.updateProduct(product.getProductId(), product)
                .addOnSuccessListener(aVoid -> {
                    currentProduct.postValue(product);
                    loadShopProducts(product.getShopId()); // Refresh shop products
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to update product: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void deleteProduct(String productId, String shopId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        productService.deleteProduct(productId)
                .addOnSuccessListener(aVoid -> {
                    currentProduct.postValue(null);
                    loadShopProducts(shopId); // Refresh shop products
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to delete product: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void loadShopProducts(String shopId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        Log.d(TAG, "Loading products for shop: " + shopId);
        
        // Use proper Firebase async pattern - service now returns Task directly
        Task<QuerySnapshot> task = productService.getProductsByShop(shopId);
        task.addOnSuccessListener(querySnapshot -> {
            Log.d(TAG, "Query successful, documents count: " + querySnapshot.size());
            List<ProductModel> products = new ArrayList<>();
            for (QueryDocumentSnapshot document : querySnapshot) {
                ProductModel product = document.toObject(ProductModel.class);
                product.setProductId(document.getId());
                updateUserSpecificStatus(product); // Update user-specific like/favorite status
                products.add(product);
                Log.d(TAG, "Found product: " + product.getName() + " (ID: " + product.getProductId() + ")");
            }
            // Sort products by createdAt locally (descending)
            products.sort((p1, p2) -> {
                if (p1.getCreatedAt() == null && p2.getCreatedAt() == null) return 0;
                if (p1.getCreatedAt() == null) return 1;
                if (p2.getCreatedAt() == null) return -1;
                return p2.getCreatedAt().compareTo(p1.getCreatedAt());
            });
            Log.d(TAG, "Posting " + products.size() + " products to LiveData");
            shopProducts.postValue(products);
            isLoading.postValue(false);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to load products", e);
            errorMessage.postValue("Failed to load products: " + e.getMessage());
            isLoading.postValue(false);
        });
    }
    
    public void loadAllProducts() {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        productService.getAllProducts().get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ProductModel> products = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        ProductModel product = document.toObject(ProductModel.class);
                        product.setProductId(document.getId());
                        updateUserSpecificStatus(product); // Update user-specific like/favorite status
                        products.add(product);
                    }
                    allProducts.postValue(products);
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to load all products: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void searchProducts(String query) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        productService.searchProducts(query).get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ProductModel> products = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        ProductModel product = document.toObject(ProductModel.class);
                        product.setProductId(document.getId());
                        updateUserSpecificStatus(product); // Update user-specific like/favorite status
                        products.add(product);
                    }
                    allProducts.postValue(products);
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to search products: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void getProductsByCategory(String category) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        productService.getProductsByCategory(category).get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ProductModel> products = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        ProductModel product = document.toObject(ProductModel.class);
                        product.setProductId(document.getId());
                        updateUserSpecificStatus(product); // Update user-specific like/favorite status
                        products.add(product);
                    }
                    allProducts.postValue(products);
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to load products by category: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void loadProduct(String productId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        productService.getProduct(productId)
                .addOnSuccessListener(product -> {
                    updateUserSpecificStatus(product); // Update user-specific like/favorite status
                    currentProduct.postValue(product);
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to load product: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    // New method with callback for ProductViewModel
    public void loadProductById(String productId, OnProductLoadedListener listener) {
        if (productId == null || productId.isEmpty()) {
            listener.onError("Invalid product ID");
            return;
        }
        
        productService.getProduct(productId)
                .addOnSuccessListener(product -> {
                    if (product != null) {
                        updateUserSpecificStatus(product);
                        listener.onProductLoaded(product);
                    } else {
                        listener.onError("Product not found");
                    }
                })
                .addOnFailureListener(e -> {
                    listener.onError("Failed to load product: " + e.getMessage());
                });
    }
    
    // New method for loading products by shop with callback
    public void loadProductsByShopId(String shopId, OnProductsLoadedListener listener) {
        if (shopId == null || shopId.isEmpty()) {
            listener.onError("Invalid shop ID");
            return;
        }
        
        Task<QuerySnapshot> task = productService.getProductsByShop(shopId);
        task.addOnSuccessListener(querySnapshot -> {
            List<ProductModel> products = new ArrayList<>();
            for (QueryDocumentSnapshot document : querySnapshot) {
                ProductModel product = document.toObject(ProductModel.class);
                product.setProductId(document.getId());
                updateUserSpecificStatus(product);
                products.add(product);
            }
            // Sort products by createdAt locally (descending)
            products.sort((p1, p2) -> {
                if (p1.getCreatedAt() == null && p2.getCreatedAt() == null) return 0;
                if (p1.getCreatedAt() == null) return 1;
                if (p2.getCreatedAt() == null) return -1;
                return p2.getCreatedAt().compareTo(p1.getCreatedAt());
            });
            listener.onProductsLoaded(products);
        }).addOnFailureListener(e -> {
            listener.onError("Failed to load products: " + e.getMessage());
        });
    }
    
    // New toggle methods with callbacks
    public void toggleLikeProduct(String productId, String userId, OnLikeToggledListener listener) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            listener.onError("User not authenticated");
            return;
        }
        
        // Check if user already liked this product
        productService.getProduct(productId)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        return com.google.android.gms.tasks.Tasks.forException(
                                new Exception("Product not found: " + productId));
                    }
                    
                    // Like logic is now handled by FavoritesTableRepository
                    // Just toggle the like state
                    ProductModel product = task.getResult();
                    boolean isLiked = false; // Default to false since we don't track likes in ProductModel anymore
                    
                    if (isLiked) {
                        // Unlike the product
                        return productService.unlikeProduct(productId, currentUserId)
                                .continueWithTask(unlikeTask -> {
                                    if (!unlikeTask.isSuccessful()) {
                                        return com.google.android.gms.tasks.Tasks.forException(unlikeTask.getException());
                                    }
                                    return productService.getProduct(productId);
                                });
                    } else {
                        // Like the product
                        return productService.likeProduct(productId, currentUserId)
                                .continueWithTask(likeTask -> {
                                    if (!likeTask.isSuccessful()) {
                                        return com.google.android.gms.tasks.Tasks.forException(likeTask.getException());
                                    }
                                    return productService.getProduct(productId);
                                });
                    }
                })
                .addOnSuccessListener(updatedProduct -> {
                    if (updatedProduct != null) {
                        // Like state is now handled by repository
                        boolean isNowLiked = false; // Default to false since we don't track likes in ProductModel anymore
                        listener.onLikeToggled(updatedProduct, isNowLiked);
                    } else {
                        listener.onError("Failed to get updated product");
                    }
                })
                .addOnFailureListener(e -> {
                    listener.onError("Failed to toggle like: " + e.getMessage());
                });
    }
    
    public void toggleFavoriteProduct(String productId, String userId, OnFavoriteToggledListener listener) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            listener.onError("User not authenticated");
            return;
        }
        
        // Check if user already favorited this product
        productService.getProduct(productId)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        return com.google.android.gms.tasks.Tasks.forException(
                                new Exception("Product not found: " + productId));
                    }
                    
                    // Favorite logic is now handled by FavoritesTableRepository
                    // Just toggle the favorite state
                    ProductModel product = task.getResult();
                    boolean isFavorited = false; // Default to false since we don't track favorites in ProductModel anymore
                    
                    if (isFavorited) {
                        // Unfavorite the product
                        return productService.unfavoriteProduct(productId, currentUserId)
                                .continueWithTask(unfavoriteTask -> {
                                    if (!unfavoriteTask.isSuccessful()) {
                                        return com.google.android.gms.tasks.Tasks.forException(unfavoriteTask.getException());
                                    }
                                    return productService.getProduct(productId);
                                });
                    } else {
                        // Favorite the product
                        return productService.favoriteProduct(productId, currentUserId)
                                .continueWithTask(favoriteTask -> {
                                    if (!favoriteTask.isSuccessful()) {
                                        return com.google.android.gms.tasks.Tasks.forException(favoriteTask.getException());
                                    }
                                    return productService.getProduct(productId);
                                });
                    }
                })
                .addOnSuccessListener(updatedProduct -> {
                    if (updatedProduct != null) {
                        // Favorite state is now handled by repository
                        boolean isNowFavorited = false; // Default to false since we don't track favorites in ProductModel anymore
                        listener.onFavoriteToggled(updatedProduct, isNowFavorited);
                    } else {
                        listener.onError("Failed to get updated product");
                    }
                })
                .addOnFailureListener(e -> {
                    listener.onError("Failed to toggle favorite: " + e.getMessage());
                });
    }
    
    public void updateProductStock(String productId, int newStock) {
        productService.updateProductStock(productId, newStock)
                .addOnSuccessListener(aVoid -> {
                    // Stock field removed from ProductModel, no need to update local cache
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to update product stock: " + e.getMessage());
                });
    }
    
    public void setCurrentProduct(ProductModel product) {
        currentProduct.setValue(product);
    }
    
    public void deleteProductWithCascade(String productId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        productService.deleteProductWithCascade(productId, productImageService)
                .addOnSuccessListener(aVoid -> {
                    // Refresh the products list
                    ProductModel currentProduct = this.currentProduct.getValue();
                    if (currentProduct != null) {
                        loadShopProducts(currentProduct.getShopId());
                    }
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to delete product: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    /**
     * Delete all products for a specific shop with cascade (includes product types and images)
     */
    public Task<Void> deleteAllProductsForShop(String shopId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        android.util.Log.d("ProductRepository", "deleteAllProductsForShop called for shop: " + shopId);
        
        // First, collect all unique product type IDs used by products in this shop
        return productService.getProductsByShop(shopId)
                .continueWithTask(task -> {
                    android.util.Log.d("ProductRepository", "Got products query result for shop: " + shopId);
                    if (!task.isSuccessful()) {
                        android.util.Log.e("ProductRepository", "Failed to get products for shop: " + shopId, task.getException());
                        throw task.getException();
                    }
                    
                    // Product type is now stored directly, no need to track orphaned types
                    android.util.Log.d("ProductRepository", "Product types are now stored directly in products");
                    
                    // Delete all products for this shop (includes product images)
                    return productService.deleteAllProductsForShop(shopId, productImageService)
                            .continueWithTask(deleteTask -> {
                                android.util.Log.d("ProductRepository", "Products deletion completed for shop: " + shopId);
                                if (!deleteTask.isSuccessful()) {
                                    errorMessage.postValue("Failed to delete products for shop: " + deleteTask.getException().getMessage());
                                    isLoading.postValue(false);
                                    throw deleteTask.getException();
                                }
                                
                                // After deleting products, delete ALL product images for this shop (including orphaned ones)
                                return deleteAllProductImagesForShop(shopId)
                                        .continueWithTask(deleteImagesTask -> {
                                            android.util.Log.d("ProductRepository", "All product images deletion completed for shop: " + shopId);
                                
                                // Product types are now stored directly, no orphaned types to delete
                                shopProducts.postValue(new ArrayList<>());
                                isLoading.postValue(false);
                                android.util.Log.d("ProductRepository", "Task completed for shop: " + shopId);
                                return com.google.android.gms.tasks.Tasks.forResult((Void) null);
                                        });
                            });
        });
    }
    
    /**
     * Delete ALL product images for a shop (including orphaned ones not linked to products)
     */
    private Task<Void> deleteAllProductImagesForShop(String shopId) {
        android.util.Log.d("ProductRepository", "deleteAllProductImagesForShop called for shop: " + shopId);
        
        // Get all products in the shop first to find their imageIds
        return productService.getProductsByShop(shopId)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        android.util.Log.e("ProductRepository", "Failed to get products for image cleanup: " + shopId, task.getException());
                        throw task.getException();
                    }
                    
                    List<String> allImageIds = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        ProductModel product = document.toObject(ProductModel.class);
                        String imageId = product.getPrimaryImageId();
                        if (imageId != null && !imageId.isEmpty() && !allImageIds.contains(imageId)) {
                            allImageIds.add(imageId);
                            android.util.Log.d("ProductRepository", "Found imageId to delete: " + imageId + " for product: " + document.getId());
                        }
                    }
                    
                    android.util.Log.d("ProductRepository", "Total images to delete for shop: " + allImageIds.size());
                    
                    // Delete all these images by their IDs
                    return productImageService.deleteProductImagesByIds(allImageIds);
                });
    }
    
    /**
     * Update user-specific like/favorite status for a product
     * NOTE: This method is no longer needed since FavoritesTableRepository handles this
     */
    private void updateUserSpecificStatus(ProductModel product) {
        // User-specific status is now handled by FavoritesTableRepository
        // This method is kept for compatibility but does nothing
    }
    
    /**
     * Get current user ID
     */
    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }
    
    /**
     * Like a product - increments like count and sets user's like status
     */
    public void likeProduct(String productId) {
        String userId = getCurrentUserId();
        if (userId == null) {
            errorMessage.postValue("User not authenticated");
            return;
        }
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        productService.likeProduct(productId, userId)
                .addOnSuccessListener(aVoid -> {
                    // Refresh current product to get updated counts
                    loadProduct(productId);
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to like product: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    /**
     * Unlike a product - decrements like count and removes user's like status
     */
    public void unlikeProduct(String productId) {
        String userId = getCurrentUserId();
        if (userId == null) {
            errorMessage.postValue("User not authenticated");
            return;
        }
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        productService.unlikeProduct(productId, userId)
                .addOnSuccessListener(aVoid -> {
                    // Refresh current product to get updated counts
                    loadProduct(productId);
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to unlike product: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    /**
     * Favorite a product - increments favorite count and sets user's favorite status
     */
    public void favoriteProduct(String productId) {
        String userId = getCurrentUserId();
        if (userId == null) {
            errorMessage.postValue("User not authenticated");
            return;
        }
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        productService.favoriteProduct(productId, userId)
                .addOnSuccessListener(aVoid -> {
                    // Refresh current product to get updated counts
                    loadProduct(productId);
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to favorite product: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    /**
     * Unfavorite a product - decrements favorite count and removes user's favorite status
     */
    public void unfavoriteProduct(String productId) {
        String userId = getCurrentUserId();
        if (userId == null) {
            errorMessage.postValue("User not authenticated");
            return;
        }
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        productService.unfavoriteProduct(productId, userId)
                .addOnSuccessListener(aVoid -> {
                    // Refresh current product to get updated counts
                    loadProduct(productId);
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to unfavorite product: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    /**
     * Toggle like status for a product
     */
    public void toggleLikeProduct(String productId) {
        ProductModel currentProduct = this.currentProduct.getValue();
        if (currentProduct != null && currentProduct.getProductId().equals(productId)) {
            // Like state is now handled by repository, just toggle
            likeProduct(productId);
        } else {
            // Load product first, then toggle
            loadProduct(productId);
            new Handler().postDelayed(() -> toggleLikeProduct(productId), 500);
        }
    }
    
    /**
     * Toggle favorite status for a product
     */
    public void toggleFavoriteProduct(String productId) {
        ProductModel currentProduct = this.currentProduct.getValue();
        if (currentProduct != null && currentProduct.getProductId().equals(productId)) {
            // Favorite state is now handled by repository, just toggle
            favoriteProduct(productId);
        } else {
            // Load product first, then toggle
            loadProduct(productId);
            new Handler().postDelayed(() -> toggleFavoriteProduct(productId), 500);
        }
    }
















    // Add these helper methods to your ProductRepository class

/**
 * Helper method to refresh product after like toggle
 */
private void refreshProductAfterLikeToggle(String productId) {
    productService.getProduct(productId)
            .addOnSuccessListener(product -> {
                if (product != null) {
                    product.setProductId(productId);
                    updateUserSpecificStatus(product);
                    currentProduct.postValue(product);
                    updateProductInList(product);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to refresh product after like toggle", e);
            });
}

/**
 * Helper method to refresh product after favorite toggle
 */
private void refreshProductAfterFavoriteToggle(String productId) {
    productService.getProduct(productId)
            .addOnSuccessListener(product -> {
                if (product != null) {
                    product.setProductId(productId);
                    updateUserSpecificStatus(product);
                    currentProduct.postValue(product);
                    updateProductInList(product);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to refresh product after favorite toggle", e);
            });
}

/**
 * Helper method to update product in the shop products list
 */
private void updateProductInList(ProductModel updatedProduct) {
    List<ProductModel> currentList = shopProducts.getValue();
    if (currentList != null) {
        for (int i = 0; i < currentList.size(); i++) {
            if (currentList.get(i).getProductId().equals(updatedProduct.getProductId())) {
                currentList.set(i, updatedProduct);
                shopProducts.postValue(currentList);
                Log.d(TAG, "Updated product in list at position: " + i);
                break;
            }
        }
    }
}
}
