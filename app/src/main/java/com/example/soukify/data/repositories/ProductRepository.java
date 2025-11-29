package com.example.soukify.data.repositories;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.soukify.data.remote.FirebaseManager;
import com.example.soukify.data.remote.firebase.FirebaseProductService;
import com.example.soukify.data.remote.firebase.FirebaseStorageService;
import com.example.soukify.data.models.ProductModel;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Product Repository - Firebase implementation
 * Follows MVVM pattern by abstracting data operations from ViewModels
 */
public class ProductRepository {
    private final FirebaseProductService productService;
    private final FirebaseStorageService storageService;
    private final MutableLiveData<List<ProductModel>> shopProducts = new MutableLiveData<>();
    private final MutableLiveData<List<ProductModel>> allProducts = new MutableLiveData<>();
    private final MutableLiveData<ProductModel> currentProduct = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    
    public ProductRepository(Application application) {
        FirebaseManager firebaseManager = FirebaseManager.getInstance(application);
        this.productService = new FirebaseProductService(firebaseManager.getFirestore());
        this.storageService = new FirebaseStorageService(firebaseManager.getStorage());
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
    
    public void createProduct(String shopId, String name, String description, String category, 
                            double price, int stock) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        ProductModel product = new ProductModel(shopId, name, description, price, "USD");
        
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
        
        productService.getProductsByShop(shopId).get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ProductModel> products = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        ProductModel product = document.toObject(ProductModel.class);
                        product.setProductId(document.getId());
                        products.add(product);
                    }
                    shopProducts.postValue(products);
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
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
}
