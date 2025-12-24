package com.example.soukify.data.repositories;

import android.app.Application;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.soukify.data.models.ProductModel;
import com.example.soukify.data.remote.FirebaseManager;
import com.example.soukify.data.remote.firebase.FirebaseProductService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.soukify.data.remote.firebase.FirebaseStorageService;
import com.google.android.gms.tasks.Task;
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
    private final UserProductPreferencesRepository userPreferences;
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
        this.userPreferences = new UserProductPreferencesRepository(application);
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

                    productService.updateProduct(productId, product)
                            .addOnSuccessListener(aVoid -> {
                                currentProduct.postValue(product);
                                loadShopProducts(product.getShopId());
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

    public void updateProduct(ProductModel product) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        productService.updateProduct(product.getProductId(), product)
                .addOnSuccessListener(aVoid -> {
                    currentProduct.postValue(product);
                    loadShopProducts(product.getShopId());
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
                    loadShopProducts(shopId);
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

        Task<QuerySnapshot> task = productService.getProductsByShop(shopId);
        task.addOnSuccessListener(querySnapshot -> {
            Log.d(TAG, "Query successful, documents count: " + querySnapshot.size());
            List<ProductModel> products = new ArrayList<>();
            for (QueryDocumentSnapshot document : querySnapshot) {
                ProductModel product = document.toObject(ProductModel.class);
                product.setProductId(document.getId());
                enrichProductWithUserState(product);
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
                        enrichProductWithUserState(product);
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

    public void loadProduct(String productId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        productService.getProduct(productId)
                .addOnSuccessListener(product -> {
                    enrichProductWithUserState(product);
                    currentProduct.postValue(product);
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to load product: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }

    public void loadProductById(String productId, OnProductLoadedListener listener) {
        if (productId == null || productId.isEmpty()) {
            listener.onError("Invalid product ID");
            return;
        }

        productService.getProduct(productId)
                .addOnSuccessListener(product -> {
                    if (product != null) {
                        enrichProductWithUserState(product);
                        listener.onProductLoaded(product);
                    } else {
                        listener.onError("Product not found");
                    }
                })
                .addOnFailureListener(e -> {
                    listener.onError("Failed to load product: " + e.getMessage());
                });
    }

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
                enrichProductWithUserState(product);
                products.add(product);
            }
            // Sort products by createdAt
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

    /**
     * Toggle like pour un produit
     */
    public void toggleLikeProduct(String productId, String userId, OnLikeToggledListener listener) {
        Log.d(TAG, "❤️ toggleLikeProduct: productId=" + productId);

        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            Log.e(TAG, "❌ User not authenticated");
            listener.onError("User not authenticated");
            return;
        }

        try {
            // Vérifier l'état actuel
            boolean wasLiked = userPreferences.isProductLiked(productId);
            Log.d(TAG, "❤️ Current state: wasLiked=" + wasLiked);

            // Toggle local state
            userPreferences.toggleLike(productId);
            final boolean isNowLiked = !wasLiked;

            Log.d(TAG, "❤️ New state: isNowLiked=" + isNowLiked);

            // Sync to Firebase
            userPreferences.syncLikesToFirebase();

            // Mettre à jour le compteur dans Firestore
            userPreferences.updateProductLikeCountAsync(productId, wasLiked)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "❤️ Like count updated in Firestore");
                        // Récupérer le produit mis à jour
                        productService.getProduct(productId)
                                .addOnSuccessListener(updatedProduct -> {
                                    if (updatedProduct != null) {
                                        // Enrichir avec l'état utilisateur
                                        updatedProduct.setLikedByUser(isNowLiked);

                                        Log.d(TAG, "❤️ Success: liked=" + isNowLiked +
                                                ", count=" + updatedProduct.getLikesCount());
                                        listener.onLikeToggled(updatedProduct, isNowLiked);
                                    } else {
                                        Log.e(TAG, "❌ Product not found after update");
                                        listener.onError("Product not found");
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "❌ Failed to get updated product", e);
                                    listener.onError("Failed to get product: " + e.getMessage());
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Failed to update like count", e);
                        // Revert local change
                        userPreferences.revertLikeChange(productId, wasLiked);
                        listener.onError("Failed to update: " + e.getMessage());
                    });
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception in toggleLikeProduct", e);
            listener.onError("Error: " + e.getMessage());
        }
    }

    /**
     * Enrichit un produit avec l'état utilisateur (liked, favorite)
     */
    private void enrichProductWithUserState(ProductModel product) {
        if (product == null || product.getProductId() == null) {
            return;
        }

        try {
            // Charger l'état "liked" depuis les préférences locales
            boolean isLiked = userPreferences.isProductLiked(product.getProductId());
            product.setLikedByUser(isLiked);

            Log.d(TAG, "✅ Enriched product " + product.getName() +
                    " with user state: liked=" + isLiked);
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to enrich product with user state", e);
            product.setLikedByUser(false);
        }
    }

    /**
     * Obtenir l'ID de l'utilisateur connecté
     */
    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public void deleteProductWithCascade(String productId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        productService.deleteProductWithCascade(productId, productImageService)
                .addOnSuccessListener(aVoid -> {
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

    public Task<Void> deleteAllProductsForShop(String shopId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        Log.d(TAG, "deleteAllProductsForShop: " + shopId);

        return productService.getProductsByShop(shopId)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "Failed to get products for shop", task.getException());
                        throw task.getException();
                    }

                    return productService.deleteAllProductsForShop(shopId, productImageService)
                            .continueWithTask(deleteTask -> {
                                if (!deleteTask.isSuccessful()) {
                                    errorMessage.postValue("Failed to delete products: " +
                                            deleteTask.getException().getMessage());
                                    isLoading.postValue(false);
                                    throw deleteTask.getException();
                                }

                                return deleteAllProductImagesForShop(shopId)
                                        .continueWithTask(deleteImagesTask -> {
                                            shopProducts.postValue(new ArrayList<>());
                                            isLoading.postValue(false);
                                            return com.google.android.gms.tasks.Tasks.forResult((Void) null);
                                        });
                            });
                });
    }

    private Task<Void> deleteAllProductImagesForShop(String shopId) {
        Log.d(TAG, "deleteAllProductImagesForShop: " + shopId);

        return productService.getProductsByShop(shopId)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    List<String> allImageIds = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        ProductModel product = document.toObject(ProductModel.class);
                        String imageId = product.getPrimaryImageId();
                        if (imageId != null && !imageId.isEmpty() && !allImageIds.contains(imageId)) {
                            allImageIds.add(imageId);
                        }
                    }

                    Log.d(TAG, "Total images to delete: " + allImageIds.size());
                    return productImageService.deleteProductImagesByIds(allImageIds);
                });
    }

    public void setCurrentProduct(ProductModel product) {
        currentProduct.setValue(product);
    }

    /**
     * Helper pour mettre à jour un produit dans la liste
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