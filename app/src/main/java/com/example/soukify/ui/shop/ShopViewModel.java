package com.example.soukify.ui.shop;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.AndroidViewModel;
import android.app.Application;

import com.example.soukify.R;
import com.example.soukify.data.repositories.LocationRepository;
import com.example.soukify.data.repositories.ShopRepository;
import com.example.soukify.data.repositories.UserRepository;
import com.example.soukify.data.repositories.ProductRepository;
import com.example.soukify.data.repositories.ProductImageRepository;
import com.example.soukify.data.models.RegionModel;
import com.example.soukify.data.models.CityModel;
import com.example.soukify.data.models.ShopModel;
import com.example.soukify.data.models.ProductModel;
import com.example.soukify.data.models.UserModel;
import com.example.soukify.utils.ImageUtils;
import java.util.List;

import android.net.Uri;
import android.util.Log;

/**
 * ViewModel for ShopFragment that manages shop data and status
 * Updated for Firebase integration while maintaining MVVM pattern
 */
public class ShopViewModel extends AndroidViewModel {
    
    private LocationRepository locationRepository;
    private ShopRepository shopRepository;
    private UserRepository userRepository;
    private final MutableLiveData<Boolean> hasShop = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<ShopModel> currentShop = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    
    public ShopViewModel(Application application) {
        super(application);
        locationRepository = new LocationRepository(application);
        shopRepository = new ShopRepository(application);
        userRepository = new UserRepository(application);
        
        // Initialize with default values
        hasShop.setValue(false);
        isLoading.setValue(false);
        
        // Load regions data
        locationRepository.loadRegions();
        
        // Check if user is already logged in and load their shops immediately
        if (userRepository.isUserLoggedIn()) {
            android.util.Log.d("ShopViewModel", "User already logged in, loading shops immediately");
            loadUserShops();
        }
        
        // Observe repository changes
        observeRepositories();
    }
    
    private void observeRepositories() {
        android.util.Log.d("ShopViewModel", "observeRepositories called");
        
        // Observe shop repository
        shopRepository.getCurrentShop().observeForever(shop -> {
            android.util.Log.d("ShopViewModel", "Repository shop changed: " + (shop != null ? "EXISTS - " + shop.getName() : "NULL"));
            currentShop.setValue(shop);
            hasShop.setValue(shop != null);
            android.util.Log.d("ShopViewModel", "hasShop updated to: " + (shop != null));
        });
        
        shopRepository.getErrorMessage().observeForever(error -> {
            if (error != null) {
                errorMessage.setValue(error);
            }
        });
        
        shopRepository.getIsLoading().observeForever(loading -> {
            if (loading != null) {
                isLoading.setValue(loading);
            }
        });
        
        // Observe user shops to determine if user has a shop
        shopRepository.getUserShops().observeForever(shops -> {
            android.util.Log.d("ShopViewModel", "User shops LiveData changed: " + (shops != null ? shops.size() + " shops" : "null"));
            isLoading.setValue(false); // Loading finished
            
            if (shops != null && !shops.isEmpty()) {
                android.util.Log.d("ShopViewModel", "User has shops, setting hasShop to true");
                hasShop.setValue(true);
                if (currentShop.getValue() == null) {
                    currentShop.setValue(shops.get(0));
                    android.util.Log.d("ShopViewModel", "Set current shop to: " + shops.get(0).getName());
                }
            } else {
                android.util.Log.d("ShopViewModel", "User has no shops, setting hasShop to false");
                hasShop.setValue(false);
                currentShop.setValue(null);
            }
        });
        
        // Observe user repository for authentication state
        userRepository.getCurrentUser().observeForever(user -> {
            if (user != null) {
                // User is logged in, load their shops
                loadUserShops();
            } else {
                // User is logged out, clear shop data
                hasShop.setValue(false);
                currentShop.setValue(null);
            }
        });
    }
    
    public LiveData<Boolean> getHasShop() {
        return hasShop;
    }
    
    public LiveData<ShopModel> getShop() {
        return currentShop;
    }
    
    public LiveData<ShopModel> getRepositoryShop() {
        return shopRepository.getCurrentShop();
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }
    
    public void clearSuccessMessage() {
        successMessage.setValue(null);
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public void setHasShop(boolean hasShop) {
        this.hasShop.setValue(hasShop);
    }
    
    public LiveData<List<RegionModel>> getRegions() {
        return locationRepository.getRegions();
    }
    
    public void loadRegions() {
        locationRepository.loadRegions();
    }
    
    public LiveData<List<CityModel>> getCities() {
        return locationRepository.getCities();
    }
    
    public void loadCitiesByRegion(String regionName) {
        locationRepository.loadCitiesByRegion(regionName);
    }
    
    public void checkShopStatus() {
        android.util.Log.d("ShopViewModel", "=== checkShopStatus STARTED ===");
        String userId = getCurrentUserId();
        android.util.Log.d("ShopViewModel", "Current user ID: " + userId);
        if (userId != null && !userId.isEmpty()) {
            android.util.Log.d("ShopViewModel", "User is logged in, calling loadUserShops");
            loadUserShops();
        } else {
            android.util.Log.d("ShopViewModel", "User is not logged in");
            hasShop.setValue(false);
            isLoading.setValue(false);
        }
        android.util.Log.d("ShopViewModel", "=== checkShopStatus COMPLETED ===");
    }
    
    private void loadUserShops() {
        android.util.Log.d("ShopViewModel", "loadUserShops called");
        isLoading.setValue(true);
        shopRepository.loadUserShops();
    }
    
    public void loadShopById(String shopId) {
        android.util.Log.d("ShopViewModel", "loadShopById called for shopId: " + shopId);
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        if (shopId == null || shopId.isEmpty()) {
            errorMessage.setValue("Shop ID is required");
            isLoading.setValue(false);
            return;
        }
        
        shopRepository.getShopById(shopId)
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    ShopModel shop = shopRepository.deserializeShop(documentSnapshot);
                    currentShop.setValue(shop);
                    android.util.Log.d("ShopViewModel", "Shop loaded successfully: " + shop.getName());
                } else {
                    errorMessage.setValue("Shop not found");
                    currentShop.setValue(null);
                }
                isLoading.setValue(false);
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("ShopViewModel", "Failed to load shop: " + e.getMessage(), e);
                errorMessage.setValue("Failed to load shop: " + e.getMessage());
                isLoading.setValue(false);
            });
    }
    
    public void createShop(String name, String description, String phone, String email, 
                         String regionName, String cityName, String address, String imageUrl, String category,
                         String workingHours, String workingDays, String instagram, String facebook, String website) {
        android.util.Log.d("ShopViewModel", "createShop called with: " + name + ", " + category);
        
        // Validate inputs
        if (name == null || name.trim().isEmpty()) {
            errorMessage.setValue("Shop name is required");
            return;
        }
        
        // Upload image to Firebase Storage for persistence
        String finalImageUrl = imageUrl;
        if (imageUrl != null && !imageUrl.isEmpty()) {
            String userId = userRepository.getCurrentUserId();
            if (userId != null) {
                // For shop creation, upload with a temporary userId-based path
                // The image will be re-uploaded with proper shopId after shop creation
                ImageUtils.uploadImageToFirebaseStorage(getApplication(), Uri.parse(imageUrl), "shop", userId)
                    .addOnSuccessListener(uri -> {
                        Log.d("ShopViewModel", "Shop image uploaded to Firebase Storage: " + uri.toString());
                        proceedWithShopCreation(name, description, phone, email, regionName, cityName, address, 
                                              uri.toString(), category, workingHours, workingDays, instagram, facebook, website);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ShopViewModel", "Failed to upload shop image to Firebase Storage", e);
                        errorMessage.setValue("Failed to upload image: " + e.getMessage());
                    });
                return; // Wait for upload completion
            } else {
                errorMessage.setValue("User not logged in");
                return;
            }
        }
        
        // No image or image URL is empty, proceed without image
        proceedWithShopCreation(name, description, phone, email, regionName, cityName, address, 
                              finalImageUrl, category, workingHours, workingDays, instagram, facebook, website);
    }
    
    private void proceedWithShopCreation(String name, String description, String phone, String email,
                                       String regionName, String cityName, String address, String imageUrl, String category,
                                       String workingHours, String workingDays, String instagram, String facebook, String website) {
        
        // Create location string
        String location = address + ", " + cityName + ", " + regionName;
        
        // Get regionId and cityId from the selected names
        String regionId = null;
        String cityId = null;
        
        // Find region ID
        List<RegionModel> regions = locationRepository.getRegions().getValue();
        if (regions != null) {
            for (RegionModel region : regions) {
                if (region.getName().equals(regionName)) {
                    regionId = region.getRegionId();
                    break;
                }
            }
        }
        
        // Find city ID
        List<CityModel> cities = locationRepository.getCities().getValue();
        if (cities != null) {
            for (CityModel city : cities) {
                if (city.getName().equals(cityName)) {
                    cityId = city.getCityId();
                    break;
                }
            }
        }
        
        shopRepository.createShop(name, description, category, phone, email, address, location, imageUrl, regionId, cityId, workingHours, workingDays, instagram, facebook, website);
        
        // Force refresh shop status after creation
        android.util.Log.d("ShopViewModel", "Shop creation initiated, forcing status refresh");
        loadUserShops();
        
        // Add delayed refresh to ensure Firebase has time to update
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            android.util.Log.d("ShopViewModel", "Delayed shop status refresh after creation");
            loadUserShops();
        }, 2000); // 2 second delay
    }
    
    public void updateShopImage(String imageUrl) {
        ShopModel currentShopData = currentShop.getValue();
        if (currentShopData != null) {
            // Upload image to Firebase Storage for persistence
            if (imageUrl != null && !imageUrl.isEmpty()) {
                String shopId = currentShopData.getShopId();
                if (shopId != null) {
                    // Upload to Firebase Storage and get the download URL
                    ImageUtils.uploadImageToFirebaseStorage(getApplication(), Uri.parse(imageUrl), "shop", shopId)
                        .addOnSuccessListener(uri -> {
                            Log.d("ShopViewModel", "Shop image uploaded to Firebase Storage: " + uri.toString());
                            
                            // Delete old image from Firebase Storage if it exists
                            if (currentShopData.getImageUrl() != null && !currentShopData.getImageUrl().isEmpty()) {
                                ImageUtils.deleteImageFromFirebaseStorage("shop", shopId);
                            }
                            
                            // Update shop with new image URL
                            currentShopData.setImageUrl(uri.toString());
                            shopRepository.updateShop(currentShopData);
                            successMessage.setValue("Shop image updated successfully!");
                        })
                        .addOnFailureListener(e -> {
                            Log.e("ShopViewModel", "Failed to upload shop image to Firebase Storage", e);
                            errorMessage.setValue("Failed to upload image: " + e.getMessage());
                        });
                } else {
                    errorMessage.setValue("Shop ID is null");
                }
            } else {
                // No image provided, clear existing image
                currentShopData.setImageUrl(null);
                shopRepository.updateShop(currentShopData);
            }
        }
    }
    
    public void updateShop(ShopModel shop) {
        android.util.Log.d("ShopViewModel", "updateShop called for: " + shop.getName());
        
        // Validate inputs
        if (shop == null) {
            errorMessage.setValue("Invalid shop data");
            return;
        }
        
        if (shop.getName() == null || shop.getName().trim().isEmpty()) {
            errorMessage.setValue("Shop name is required");
            return;
        }
        
        shopRepository.updateShop(shop);
        
        // Show success message
        successMessage.setValue("Shop updated successfully!");
        
        // Force refresh to ensure UI updates
        loadUserShops();
    }
    
    public void deleteShop(String shopId, String password) {
        android.util.Log.d("ShopViewModel", "deleteShop called for: " + shopId + " with password verification");
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        // Get current user email directly from Firebase Auth
        String userEmail = userRepository.getCurrentUserEmail();
        if (userEmail == null || userEmail.isEmpty()) {
            errorMessage.setValue("No user logged in or user email not available");
            isLoading.setValue(false);
            return;
        }
        
        // Verify password before deletion
        userRepository.reauthenticate(userEmail, password)
                .addOnSuccessListener(authResult -> {
                    android.util.Log.d("ShopViewModel", "Password verification successful, proceeding with shop deletion");
                    // Password verified, proceed with deletion
                    performShopDeletion(shopId);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ShopViewModel", "Password verification failed", e);
                    errorMessage.setValue("Incorrect password. Shop deletion cancelled.");
                    isLoading.setValue(false);
                });
    }
    
    private void performShopDeletion(String shopId) {
        // Let the repository handle the deletion and clear shop only after completion
        shopRepository.deleteShop(shopId)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("ShopViewModel", "Shop deletion completed successfully");
                    successMessage.setValue("Shop and all related products deleted successfully!");
                    // Shop is already cleared by repository, just ensure UI state
                    hasShop.setValue(false);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ShopViewModel", "Shop deletion failed", e);
                    errorMessage.setValue("Failed to delete shop: " + e.getMessage());
                    isLoading.setValue(false);
                });
    }
    
    public void deleteShop(String shopId) {
        // Legacy method for backward compatibility - requires password
        android.util.Log.d("ShopViewModel", "Legacy deleteShop called without password - not allowed");
        errorMessage.setValue("Password required for shop deletion. Please use the delete button in the shop interface.");
    }
    
    public void clearErrorMessage() {
        errorMessage.setValue(null);
    }
    
    public void setErrorMessage(String message) {
        errorMessage.setValue(message);
    }
    
    public void addProduct(String productName, String productDescription, double price) {
        // Validate inputs
        if (productName == null || productName.trim().isEmpty()) {
            errorMessage.setValue("Product name is required");
            return;
        }
        
        ShopModel currentShopData = currentShop.getValue();
        if (currentShopData == null) {
            errorMessage.setValue("No shop found to add product to");
            return;
        }
        
        // Implement product addition using ProductRepository
        ProductRepository productRepository = new ProductRepository(getApplication());
        ProductImageRepository productImageRepository = new ProductImageRepository(getApplication());
        
        // Add product using Firebase
        productRepository.createProduct(
            currentShopData.getShopId(),
            productName,
            productDescription,
            "General", // Default category - can be parameterized later
            price,
            0 // Default stock - can be parameterized later
        );
        
        // Observe the result using LiveData
        productRepository.getErrorMessage().observeForever(error -> {
            if (error != null) {
                errorMessage.postValue(error);
            }
        });
        
        productRepository.getCurrentProduct().observeForever(product -> {
            if (product != null && product.getName().equals(productName)) {
                successMessage.postValue("Product added successfully!");
            }
        });
    }
    
    public void updateProduct(ProductModel product) {
        android.util.Log.d("ShopViewModel", "updateProduct called for: " + product.getName());
        
        // Validate inputs
        if (product == null) {
            errorMessage.setValue("Invalid product data");
            return;
        }
        
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            errorMessage.setValue("Product name is required");
            return;
        }
        
        ProductRepository productRepository = new ProductRepository(getApplication());
        
        // Observe errors and success
        productRepository.getErrorMessage().observeForever(error -> {
            if (error != null) {
                errorMessage.postValue(error);
            }
        });
        
        productRepository.updateProduct(product);
        successMessage.setValue("Product updated successfully!");
    }
    
    public void deleteProduct(String productId, String shopId) {
        android.util.Log.d("ShopViewModel", "deleteProduct called for: " + productId);
        
        // Validate inputs
        if (productId == null || productId.trim().isEmpty()) {
            errorMessage.setValue("Invalid product ID");
            return;
        }
        
        if (shopId == null || shopId.trim().isEmpty()) {
            errorMessage.setValue("Invalid shop ID");
            return;
        }
        
        ProductRepository productRepository = new ProductRepository(getApplication());
        
        // Observe errors and success
        productRepository.getErrorMessage().observeForever(error -> {
            if (error != null) {
                errorMessage.postValue(error);
            }
        });
        
        productRepository.deleteProduct(productId, shopId);
        successMessage.setValue("Product deleted successfully!");
    }
    
    public void clearAllShops() {
        android.util.Log.d("ShopViewModel", "Clear all shops called");
        // In Firebase context, this would clear local cache
        hasShop.setValue(false);
        currentShop.setValue(null);
    }
    
    public String getCurrentUserId() {
        return userRepository.getCurrentUserId();
    }
    
    public boolean isUserLoggedIn() {
        return userRepository.isUserLoggedIn();
    }
    
    public void clearAndRepopulateCities() {
        locationRepository.clearAndRepopulateCities();
    }
    
    public void signOut() {
        userRepository.signOut();
    }
}
