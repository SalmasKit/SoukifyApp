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

import java.util.List;
import java.util.HashMap;
import java.util.Map;

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
    private final MutableLiveData<Boolean> likeOperationInProgress = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> favoriteOperationInProgress = new MutableLiveData<>(false);
    private final MutableLiveData<Map<String, Boolean>> productFavoriteStates = new MutableLiveData<>(new HashMap<>());
    
    public ProductViewModel(Application application) {
        super(application);
        this.productRepository = new ProductRepository(application);
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.userPreferences = new UserProductPreferencesRepository(application);
        this.favoritesTableRepository = new FavoritesTableRepository(application);
    }
    
    public void setupObservers(androidx.lifecycle.LifecycleOwner lifecycleOwner) {
        if (lifecycleOwner != null && productRepository != null) {
            removeObservers();
            
            if (productRepository.getCurrentProduct() != null) {
                productRepository.getCurrentProduct().observe(lifecycleOwner, product -> {
                    if (product != null) {
                        loadUserPreferencesForProduct(product);
                        currentProduct.postValue(product);
                    }
                });
            }
            
            if (productRepository.getErrorMessage() != null) {
                productRepository.getErrorMessage().observe(lifecycleOwner, errorMessage::postValue);
            }
            
            if (productRepository.getIsLoading() != null) {
                productRepository.getIsLoading().observe(lifecycleOwner, isLoading::postValue);
            }
        }
    }
    
    private void removeObservers() {
        if (productRepository != null) {
            if (productRepository.getCurrentProduct() != null) {
                productRepository.getCurrentProduct().removeObservers(null);
            }
            if (productRepository.getErrorMessage() != null) {
                productRepository.getErrorMessage().removeObservers(null);
            }
            if (productRepository.getIsLoading() != null) {
                productRepository.getIsLoading().removeObservers(null);
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
    
    public LiveData<Boolean> getLikeOperationInProgress() {
        return likeOperationInProgress;
    }
    
    public LiveData<Boolean> getFavoriteOperationInProgress() {
        return favoriteOperationInProgress;
    }
    
    public LiveData<Map<String, Boolean>> getProductFavoriteStates() {
        return productFavoriteStates;
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
                    loadUserPreferencesForProduct(product);
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
                    loadUserPreferencesForProducts(productList);
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
    
    public void toggleLikeProduct(String productId) {
        if (likeOperationInProgress.getValue() == Boolean.TRUE) {
            Log.d(TAG, "Like operation already in progress");
            return;
        }
        
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            errorMessage.postValue("Please log in to like products");
            return;
        }
        
        String userId = currentUser.getUid();
        likeOperationInProgress.postValue(true);
        
        Log.d(TAG, "Toggling like for product: " + productId + ", user: " + userId);
        
        productRepository.toggleLikeProduct(productId, userId, new ProductRepository.OnLikeToggledListener() {
            @Override
            public void onLikeToggled(ProductModel updatedProduct, boolean isNowLiked) {
                likeOperationInProgress.postValue(false);
                
                if (currentProduct.getValue() != null && 
                    currentProduct.getValue().getProductId().equals(productId)) {
                    currentProduct.postValue(updatedProduct);
                }
                
                updateProductInList(updatedProduct);
                
                Log.d(TAG, "Like toggled successfully. Product now " + (isNowLiked ? "liked" : "unliked"));
            }
            
            @Override
            public void onError(String error) {
                likeOperationInProgress.postValue(false);
                errorMessage.postValue(error);
                Log.e(TAG, "Error toggling like: " + error);
            }
        });
    }
    
    public void toggleFavoriteProduct(String productId) {
        if (favoriteOperationInProgress.getValue() == Boolean.TRUE) {
            Log.d(TAG, "Favorite operation already in progress");
            return;
        }
        
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            errorMessage.postValue("Please log in to favorite products");
            return;
        }
        
        String userId = currentUser.getUid();
        favoriteOperationInProgress.postValue(true);
        
        Log.d(TAG, "Toggling favorite for product: " + productId + ", user: " + userId);
        
        favoritesTableRepository.isProductFavorite(productId).observeForever(isFavorited -> {
            if (isFavorited != null) {
                // Update the favorite states map
                Map<String, Boolean> currentStates = productFavoriteStates.getValue();
                if (currentStates == null) {
                    currentStates = new HashMap<>();
                }
                currentStates.put(productId, !isFavorited);
                productFavoriteStates.postValue(currentStates);
                
                if (isFavorited) {
                    favoritesTableRepository.removeProductFromFavorites(productId);
                    Log.d(TAG, "Removed product from favorites: " + productId);
                } else {
                    productRepository.loadProductById(productId, new ProductRepository.OnProductLoadedListener() {
                        @Override
                        public void onProductLoaded(ProductModel product) {
                            if (product != null) {
                                favoritesTableRepository.addProductToFavorites(product);
                                Log.d(TAG, "Added product to favorites: " + productId);
                            } else {
                                errorMessage.postValue("Product not found");
                            }
                            favoriteOperationInProgress.postValue(false);
                        }
                        
                        @Override
                        public void onError(String error) {
                            errorMessage.postValue("Failed to load product: " + error);
                            favoriteOperationInProgress.postValue(false);
                        }
                    });
                    return;
                }
                favoriteOperationInProgress.postValue(false);
            }
        });
    }
    
    public void likeProduct(String productId) {
        toggleLikeProduct(productId);
    }
    
    public void unlikeProduct(String productId) {
        toggleLikeProduct(productId);
    }
    
    public void favoriteProduct(String productId) {
        toggleFavoriteProduct(productId);
    }
    
    public void unfavoriteProduct(String productId) {
        toggleFavoriteProduct(productId);
    }
    
    public void updateProduct(ProductModel product) {
        productRepository.updateProduct(product);
    }
    
    public void deleteProduct(String productId, String shopId) {
        productRepository.deleteProduct(productId, shopId);
    }
    
    private void updateProductInList(ProductModel updatedProduct) {
        List<ProductModel> currentList = products.getValue();
        if (currentList != null) {
            for (int i = 0; i < currentList.size(); i++) {
                if (currentList.get(i).getProductId().equals(updatedProduct.getProductId())) {
                    currentList.set(i, updatedProduct);
                    products.postValue(currentList);
                    Log.d(TAG, "Updated product in list at position: " + i);
                    break;
                }
            }
        }
    }
    
    private void loadUserPreferencesForProduct(ProductModel product) {
        if (product == null) return;
        
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            currentProduct.postValue(product);
            return;
        }
        
        favoritesTableRepository.isProductFavorite(product.getProductId()).observeForever(isFavorited -> {
            if (isFavorited != null) {
                Log.d(TAG, "Loaded persistent favorite state for product: " + product.getName() + 
                           " - Favorited: " + isFavorited);
                currentProduct.postValue(product);
            }
        });
        
        Log.d(TAG, "Loaded preferences for product: " + product.getName());
    }
    
    private void loadUserPreferencesForProducts(List<ProductModel> productList) {
        if (productList == null) {
            products.postValue(null);
            return;
        }
        
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            products.postValue(productList);
            return;
        }
        
        for (ProductModel product : productList) {
            favoritesTableRepository.isProductFavorite(product.getProductId()).observeForever(isFavorited -> {
                if (isFavorited != null) {
                    Log.d(TAG, "Loaded persistent favorite state for product: " + product.getName() + 
                               " - Favorited: " + isFavorited);
                    products.postValue(productList);
                }
            });
            
            Log.d(TAG, "Product: " + product.getName());
        }
        
        products.postValue(productList);
    }
    
    public void setCurrentProduct(ProductModel product) {
        if (product != null) {
            loadUserPreferencesForProduct(product);
        } else {
            currentProduct.postValue(null);
        }
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
