package com.example.soukify.data.repositories;

import android.app.Application;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.soukify.data.remote.FirebaseManager;
import com.example.soukify.data.remote.firebase.FirebaseProductService;
import com.example.soukify.data.remote.firebase.FirebaseStorageService;
import com.example.soukify.data.models.ProductModel;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.Query;
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
                    currentProduct.postValue(product);
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to load product: " + e.getMessage());
                    isLoading.postValue(false);
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
}
