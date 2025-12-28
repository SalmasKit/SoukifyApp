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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.example.soukify.services.NotificationSenderService;
import com.google.firebase.firestore.FirebaseFirestore;

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

    public interface OnProductDeletedListener {
        void onProductDeleted();
        void onError(String error);
    }

    private static ProductRepository instance;
    private final FirebaseProductService productService;
    private final FirebaseStorageService storageService;
    private final com.example.soukify.data.remote.firebase.FirebaseProductImageService productImageService;
    private final UserProductPreferencesRepository userPreferences;
    private final Application application;
    private final MutableLiveData<List<ProductModel>> shopProducts = new MutableLiveData<>();
    private final MutableLiveData<List<ProductModel>> allProducts = new MutableLiveData<>();
    private final MutableLiveData<ProductModel> currentProduct = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final NotificationSenderService notificationSenderService;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private ProductRepository(Application application) {
        FirebaseManager firebaseManager = FirebaseManager.getInstance(application);
        this.productService = new FirebaseProductService(firebaseManager.getFirestore());
        this.storageService = new FirebaseStorageService(firebaseManager.getStorage());
        this.productImageService = new com.example.soukify.data.remote.firebase.FirebaseProductImageService(firebaseManager.getFirestore());
        this.userPreferences = new UserProductPreferencesRepository(application);
        this.application = application;
        this.notificationSenderService = new NotificationSenderService();
    }

    public static synchronized ProductRepository getInstance(Application application) {
        if (instance == null) {
            instance = new ProductRepository(application);
        }
        return instance;
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
                                
                                // 🔔 Envoi de notification aux followers
                                fetchShopNameAndNotify(product);
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

    public void deleteProduct(String productId, String shopId, OnProductDeletedListener listener) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        // Optimistic update: Remove from list immediately
        List<ProductModel> currentList = shopProducts.getValue();
        if (currentList != null) {
            List<ProductModel> optimisticList = new ArrayList<>(currentList);
            boolean removed = optimisticList.removeIf(p -> p.getProductId().equals(productId));
            if (removed) {
                Log.d(TAG, "⚡ Optimistically removed product " + productId + " from list");
                shopProducts.postValue(optimisticList);
            }
        }

        productService.deleteProduct(productId)
                .addOnSuccessListener(aVoid -> {
                    currentProduct.postValue(null);
                    // Reload products and wait for completion (to confirm sync)
                    loadShopProducts(shopId, new OnProductsLoadedListener() {
                        @Override
                        public void onProductsLoaded(List<ProductModel> products) {
                            isLoading.postValue(false);
                            if (listener != null) {
                                listener.onProductDeleted();
                            }
                        }

                        @Override
                        public void onError(String error) {
                            isLoading.postValue(false);
                            // Even if reload fails, deletion succeeded
                            if (listener != null) {
                                listener.onProductDeleted();
                            }
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to delete product: " + e.getMessage());
                    isLoading.postValue(false);
                    if (listener != null) {
                        listener.onError(e.getMessage());
                    }
                });
    }

    public void deleteProduct(String productId, String shopId) {
        deleteProduct(productId, shopId, null);
    }

    public void loadShopProducts(String shopId) {
        loadShopProducts(shopId, null);
    }

    public void loadShopProducts(String shopId, final OnProductsLoadedListener listener) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        Log.d(TAG, "Loading products for shop: " + shopId);

        Task<QuerySnapshot> task = productService.getProductsByShop(shopId);
        task.addOnSuccessListener(executor, querySnapshot -> {
            Log.d(TAG, "Query successful, processing " + querySnapshot.size() + " documents on background thread");
            List<ProductModel> products = new ArrayList<>();
            for (QueryDocumentSnapshot document : querySnapshot) {
                try {
                    ProductModel product = document.toObject(ProductModel.class);
                    product.setProductId(document.getId());
                    enrichProductWithUserState(product);
                    products.add(product);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing product document: " + document.getId(), e);
                }
            }
            
            // Sort products by createdAt locally (descending)
            products.sort((p1, p2) -> {
                String s1 = p1.getCreatedAtString();
                String s2 = p2.getCreatedAtString();
                if (s1 == null && s2 == null) return 0;
                if (s1 == null) return 1;
                if (s2 == null) return -1;
                try {
                    long t1 = Long.parseLong(s1);
                    long t2 = Long.parseLong(s2);
                    return Long.compare(t2, t1); // Descending: newer (larger) first
                } catch (NumberFormatException e) {
                    return s2.compareTo(s1);
                }
            });
            
            Log.d(TAG, "Processing complete, posting " + products.size() + " products to LiveData");
            shopProducts.postValue(products);
            isLoading.postValue(false);
            if (listener != null) {
                listener.onProductsLoaded(products);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to load products", e);
            errorMessage.postValue("Failed to load products: " + e.getMessage());
            isLoading.postValue(false);
            if (listener != null) {
                listener.onError(e.getMessage());
            }
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
                String d1 = p1.getCreatedAtString();
                String d2 = p2.getCreatedAtString();
                if (d1 == null && d2 == null) return 0;
                if (d1 == null) return 1;
                if (d2 == null) return -1;
                return d2.compareTo(d1);
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

                                        // ✅ Synchronisation globale
                                        notifyProductChanged(updatedProduct);

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
    public void enrichProductWithUserState(ProductModel product) {
        if (product == null || product.getProductId() == null) {
            return;
        }

        try {
            // Charger l'état "liked" depuis les préférences locales
            boolean isLiked = userPreferences.isProductLiked(product.getProductId());
            product.setLikedByUser(isLiked);

            // Enricher les favoris (depuis FavoritesTableRepository Singleton)
            FavoritesTableRepository favoritesRepo = FavoritesTableRepository.getInstance(application);
            product.setFavoriteByUser(favoritesRepo.isProductFavoriteSync(product.getProductId()));

            Log.d(TAG, "✅ Enriched product " + product.getName() +
                    " with user state: liked=" + isLiked + ", favorite=" + product.isFavoriteByUser());
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to enrich product with user state", e);
            product.setLikedByUser(false);
            product.setFavoriteByUser(false); // Also set favorite to false on error
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
     * Notifie le repository qu'un produit a changé pour synchroniser tous les observateurs.
     */
    public void notifyProductChanged(ProductModel product) {
        if (product == null || product.getProductId() == null) return;
        
        // Mettre à jour le produit courant
        ProductModel current = currentProduct.getValue();
        if (current != null && current.getProductId().equals(product.getProductId())) {
            currentProduct.postValue(product);
        }
        
        // Mettre à jour dans toutes les listes
        updateProductInList(product);
        
        // Synchroniser également avec FavoritesTableRepository
        FavoritesTableRepository.getInstance(application).notifyProductChanged(product);
    }

    /**
     * Helper pour mettre à jour un produit dans la liste
     */
    private void updateProductInList(ProductModel updatedProduct) {
        // Update shop products list
        List<ProductModel> currentShopList = shopProducts.getValue();
        if (currentShopList != null) {
            List<ProductModel> updatedList = new ArrayList<>(currentShopList);
            for (int i = 0; i < updatedList.size(); i++) {
                if (updatedList.get(i).getProductId().equals(updatedProduct.getProductId())) {
                    updatedList.set(i, updatedProduct);
                    shopProducts.postValue(updatedList);
                    Log.d(TAG, "Updated product in shop list at position: " + i);
                    break;
                }
            }
        }

        // Update all products list (Search results etc)
        List<ProductModel> currentAllList = allProducts.getValue();
        if (currentAllList != null) {
            List<ProductModel> updatedList = new ArrayList<>(currentAllList);
            for (int i = 0; i < updatedList.size(); i++) {
                if (updatedList.get(i).getProductId().equals(updatedProduct.getProductId())) {
                    updatedList.set(i, updatedProduct);
                    allProducts.postValue(updatedList);
                    Log.d(TAG, "Updated product in all products list at position: " + i);
                    break;
                }
            }
        }
    }

    private void fetchShopNameAndNotify(ProductModel product) {
        if (product == null || product.getShopId() == null) return;

        FirebaseFirestore.getInstance().collection("shops").document(product.getShopId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String shopName = documentSnapshot.getString("name");
                        if (shopName != null) {
                            notificationSenderService.sendNewProductNotification(
                                    product.getShopId(),
                                    shopName,
                                    product.getName(),
                                    product.getProductId()
                            );
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch shop name for notification", e));
    }
}
