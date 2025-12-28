package com.example.soukify.ui.shop;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.soukify.data.models.ProductModel;
import com.example.soukify.data.repositories.ProductRepository;
import com.example.soukify.data.repositories.UserProductPreferencesRepository;
import com.example.soukify.data.repositories.FavoritesTableRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class ProductViewModel extends AndroidViewModel {
    private static final String TAG = "ProductViewModel";

    private final ProductRepository productRepository;
    private final FirebaseAuth firebaseAuth;
    private final UserProductPreferencesRepository userPreferences;
    private final FavoritesTableRepository favoritesTableRepository;

    private final MutableLiveData<ProductModel> currentProduct = new MutableLiveData<>();
    private final MutableLiveData<List<ProductModel>> products = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    public ProductViewModel(Application application) {
        super(application);
        this.productRepository = ProductRepository.getInstance(application);
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.userPreferences = new UserProductPreferencesRepository(application);
        this.favoritesTableRepository = FavoritesTableRepository.getInstance(application);
    }

    public void setupObservers(androidx.lifecycle.LifecycleOwner lifecycleOwner) {
        if (lifecycleOwner != null && productRepository != null) {
            if (productRepository.getCurrentProduct() != null) {
                productRepository.getCurrentProduct().observe(lifecycleOwner, product -> {
                    if (product != null) {
                        enrichProductWithUserState(product);
                    }
                });
            }

            if (productRepository.getErrorMessage() != null) {
                productRepository.getErrorMessage().observe(lifecycleOwner, errorMessage::postValue);
            }

            if (productRepository.getIsLoading() != null) {
                productRepository.getIsLoading().observe(lifecycleOwner, isLoading::postValue);
            }

            if (productRepository.getShopProducts() != null) {
                productRepository.getShopProducts().observe(lifecycleOwner, productsList -> {
                    if (productsList != null) {
                        Log.d(TAG, "Syncing products from repository, count: " + productsList.size());
                        enrichProductsWithUserState(productsList);
                    }
                });
            }
        }
    }



    public LiveData<ProductModel> getCurrentProduct() {
        return currentProduct;
    }

    public LiveData<List<ProductModel>> getProducts() {
        return products;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void loadProduct(String productId) {
        if (productId == null || productId.isEmpty()) {
            errorMessage.postValue("Invalid product ID");
            return;
        }

        isLoading.postValue(true);
        productRepository.loadProductById(productId, new ProductRepository.OnProductLoadedListener() {
            @Override
            public void onProductLoaded(ProductModel product) {
                isLoading.postValue(false);
                if (product != null) {
                    enrichProductWithUserState(product);
                } else {
                    errorMessage.postValue("Product not found");
                }
            }

            @Override
            public void onError(String error) {
                isLoading.postValue(false);
                errorMessage.postValue(error);
            }
        });
    }

    public void loadProductsForShop(String shopId) {
        if (shopId == null || shopId.isEmpty()) {
            products.postValue(null);
            return;
        }

        isLoading.postValue(true);
        productRepository.loadProductsByShopId(shopId, new ProductRepository.OnProductsLoadedListener() {
            @Override
            public void onProductsLoaded(List<ProductModel> productList) {
                isLoading.postValue(false);
                if (productList != null) {
                    enrichProductsWithUserState(productList);
                } else {
                    products.postValue(null);
                }
            }

            @Override
            public void onError(String error) {
                isLoading.postValue(false);
                errorMessage.postValue(error);
            }
        });
    }

    /**
     * Toggle like pour un produit
     * Mise à jour optimiste + persistance backend
     */
    public void toggleLikeProduct(String productId) {
        Log.d(TAG, "❤️ toggleLikeProduct called for product: " + productId);

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "❌ User not logged in");
            errorMessage.postValue("Please log in to like products");
            return;
        }

        String userId = currentUser.getUid();

        // Trouver le produit dans la liste ou le produit courant
        ProductModel product = findProductById(productId);
        if (product == null) {
            Log.e(TAG, "❌ Product not found: " + productId);
            return;
        }

        // Mise à jour optimiste du modèle
        boolean newLikedState = !product.isLikedByUser();
        int newLikesCount = newLikedState ?
                product.getLikesCount() + 1 :
                Math.max(0, product.getLikesCount() - 1);

        Log.d(TAG, "❤️ Optimistic update: liked=" + newLikedState + ", likes=" + newLikesCount);

        product.setLikedByUser(newLikedState);
        product.setLikesCount(newLikesCount);

        // Notifier l'UI immédiatement (mise à jour optimiste)
        notifyProductUpdated(product);

        // Persister dans le backend
        productRepository.toggleLikeProduct(productId, userId, new ProductRepository.OnLikeToggledListener() {
            @Override
            public void onLikeToggled(ProductModel updatedProduct, boolean isNowLiked) {
                Log.d(TAG, "❤️ Backend confirmation: liked=" + isNowLiked + ", likes=" + updatedProduct.getLikesCount());

                // Mettre à jour avec les données réelles du backend
                if (updatedProduct != null) {
                    updatedProduct.setLikedByUser(isNowLiked);
                    notifyProductUpdated(updatedProduct);
                    
                    // ✅ Synchronisation globale via le Repository
                    productRepository.notifyProductChanged(updatedProduct);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error toggling like: " + error);

                // Revert la mise à jour optimiste en cas d'erreur
                product.setLikedByUser(!newLikedState);
                product.setLikesCount(newLikedState ? newLikesCount - 1 : newLikesCount + 1);
                notifyProductUpdated(product);

                errorMessage.postValue("Failed to like product: " + error);
            }
        });
    }

    /**
     * Toggle favorite pour un produit
     * Mise à jour optimiste + persistance backend
     */
    public void toggleFavoriteProduct(String productId) {
        Log.d(TAG, "⭐ toggleFavoriteProduct called for product: " + productId);

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            errorMessage.postValue("Please log in to favorite products");
            return;
        }

        // Trouver le produit
        ProductModel product = findProductById(productId);
        if (product == null) {
            Log.e(TAG, "❌ Product not found: " + productId);
            return;
        }

        // Vérifier l'état actuel et basculer
        favoritesTableRepository.checkProductFavoriteOnce(productId, new FavoritesTableRepository.OnFavoriteCheckedListener() {
            @Override
            public void onChecked(boolean isFavorited) {
                boolean newFavoriteState = !isFavorited;

                Log.d(TAG, "⭐ Current favorite state: " + isFavorited + ", new state: " + newFavoriteState);

                // Mise à jour optimiste
                product.setFavoriteByUser(newFavoriteState);
                notifyProductUpdated(product);
                
                // ✅ Synchronisation globale via le Repository
                productRepository.notifyProductChanged(product);

                if (newFavoriteState) {
                    // Ajouter aux favoris
                    favoritesTableRepository.addProductToFavorites(product);
                    Log.d(TAG, "⭐ Added product to favorites: " + productId);
                } else {
                    // Retirer des favoris
                    favoritesTableRepository.removeProductFromFavorites(productId);
                    Log.d(TAG, "⭐ Removed product from favorites: " + productId);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error checking favorite state: " + error);
                errorMessage.postValue("Failed to favorite product: " + error);
            }
        });
    }

    /**
     * Trouve un produit par son ID dans la liste ou le produit courant
     */
    private ProductModel findProductById(String productId) {
        // Vérifier le produit courant
        ProductModel current = currentProduct.getValue();
        if (current != null && current.getProductId().equals(productId)) {
            return current;
        }

        // Chercher dans la liste de produits
        List<ProductModel> productList = products.getValue();
        if (productList != null) {
            for (ProductModel p : productList) {
                if (p.getProductId().equals(productId)) {
                    return p;
                }
            }
        }

        return null;
    }

    /**
     * Notifie l'UI qu'un produit a été mis à jour
     */
    private void notifyProductUpdated(ProductModel updatedProduct) {
        // Mettre à jour le produit courant si c'est le même
        ProductModel current = currentProduct.getValue();
        if (current != null && current.getProductId().equals(updatedProduct.getProductId())) {
            currentProduct.postValue(updatedProduct);
        }

        // Mettre à jour le produit dans la liste
        List<ProductModel> productList = products.getValue();
        if (productList != null) {
            List<ProductModel> updatedList = new ArrayList<>(productList);
            for (int i = 0; i < updatedList.size(); i++) {
                if (updatedList.get(i).getProductId().equals(updatedProduct.getProductId())) {
                    updatedList.set(i, updatedProduct);
                    products.postValue(updatedList);
                    Log.d(TAG, "✅ Updated product in list at position: " + i);
                    break;
                }
            }
        }
    }

    /**
     * Enrichit un produit avec l'état utilisateur (liked, favorite)
     */
    private void enrichProductWithUserState(ProductModel product) {
        if (product == null) {
            currentProduct.postValue(null);
            return;
        }

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            currentProduct.postValue(product);
            return;
        }

        String productId = product.getProductId();

        // ✅ Charger l'état "liked" - utilise seulement productId
        boolean isLiked = userPreferences.isProductLiked(productId);
        product.setLikedByUser(isLiked);
        Log.d(TAG, "✅ Loaded like state for " + product.getName() + ": " + isLiked);

        // ✅ Charger l'état "favorite"
        favoritesTableRepository.isProductFavorite(productId).observeForever(isFavorite -> {
            product.setFavoriteByUser(isFavorite != null && isFavorite);
            Log.d(TAG, "✅ Loaded favorite state for " + product.getName() + ": " + isFavorite);
            currentProduct.postValue(product);
        });
    }

    /**
     * Enrichit une liste de produits avec l'état utilisateur
     */
    private void enrichProductsWithUserState(List<ProductModel> productList) {
        if (productList == null || productList.isEmpty()) {
            products.postValue(productList);
            return;
        }

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            products.postValue(productList);
            return;
        }

        // Create a new list to avoid concurrent modification issues
        // and to serve as the source of truth for the adapter
        List<ProductModel> enrichedProducts = new ArrayList<>(productList);

        // First pass: Apply synchronous updates (Likes) and post IMMEDIATELY
        // This ensures the user sees the products (including the new one) right away.
        for (ProductModel product : enrichedProducts) {
            String productId = product.getProductId();
            if (productId != null) {
                // Load like state (synchronous from SharedPreferences)
                boolean isLiked = userPreferences.isProductLiked(productId);
                product.setLikedByUser(isLiked);
                Log.d(TAG, "✅ Loaded like state for " + product.getName() + ": " + isLiked);
            }
        }
        
        // Post the list immediately!
        products.postValue(enrichedProducts);

        // Second pass: Apply asynchronous updates (Favorites)
        // We only trigger a refresh if the state actually changes (i.e. isFavorited is true)
        for (ProductModel product : enrichedProducts) {
            String productId = product.getProductId();
            if (productId == null) continue;

            favoritesTableRepository.checkProductFavoriteOnce(productId, new FavoritesTableRepository.OnFavoriteCheckedListener() {
                @Override
                public void onChecked(boolean isFavorited) {
                    // Update state
                    boolean stateChanged = product.isFavoriteByUser() != isFavorited;
                    product.setFavoriteByUser(isFavorited);
                    
                    Log.d(TAG, "✅ Loaded favorite state for " + product.getName() + ": " + isFavorited);
                    
                    // Only refresh the list if the state is 'true' (since default is false)
                    // or if it somehow changed from what was effectively displayed.
                    // To be safe and ensure UI consistency: if it's favorited, we definitely need to show it.
                    if (isFavorited || stateChanged) {
                        // We must post the SAME list instance (or a copy of it) so the adapter updates.
                        // Since 'product' is a specific object reference INSIDE enrichedProducts,
                        // modifying it and reposting the list works.
                        products.postValue(enrichedProducts);
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "❌ Error checking favorite state for " + product.getName() + ": " + error);
                    // No need to block UI, just keep default false
                }
            });
        }
    }

    public void updateProduct(ProductModel product) {
        productRepository.updateProduct(product);
    }

    public void deleteProduct(String productId, String shopId) {
        productRepository.deleteProduct(productId, shopId);
    }

    public void setCurrentProduct(ProductModel product) {
        if (product != null) {
            enrichProductWithUserState(product);
        } else {
            currentProduct.postValue(null);
        }
    }

    public void clearProducts() {
        Log.d(TAG, "Clearing products list");
        products.postValue(new ArrayList<>());
        currentProduct.postValue(null);
        errorMessage.postValue(null);
    }

    public void clearError() {
        errorMessage.postValue(null);
    }

    public void clearSuccess() {
        successMessage.postValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "ProductViewModel cleared");
    }

    public static class Factory extends ViewModelProvider.NewInstanceFactory {
        private final Application application;

        public Factory(Application application) {
            this.application = application;
        }

        @Override
        public <T extends ViewModel> T create(Class<T> modelClass) {
            if (modelClass.isAssignableFrom(ProductViewModel.class)) {
                return (T) new ProductViewModel(application);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}