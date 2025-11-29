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
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public void setHasShop(boolean hasShop) {
        this.hasShop.setValue(hasShop);
    }
    
    public LiveData<List<RegionModel>> getRegions() {
        return locationRepository.getRegions();
    }
    
    public LiveData<List<CityModel>> getCities() {
        return locationRepository.getCities();
    }
    
    public void loadCitiesByRegion(String regionName) {
        locationRepository.loadCitiesByRegion(regionName);
    }
    
    public void checkShopStatus() {
        if (userRepository.isUserLoggedIn()) {
            loadUserShops();
        } else {
            hasShop.setValue(false);
            isLoading.setValue(false);
        }
    }
    
    private void loadUserShops() {
        android.util.Log.d("ShopViewModel", "loadUserShops called");
        shopRepository.loadUserShops();
    }
    
    public void createShop(String name, String description, String phone, String email, 
                         String regionName, String cityName, String address, String imageUrl, String category) {
        android.util.Log.d("ShopViewModel", "createShop called with: " + name + ", " + category);
        
        // Validate inputs
        if (name == null || name.trim().isEmpty()) {
            errorMessage.setValue("Shop name is required");
            return;
        }
        
        // Copy image to internal storage for persistence
        String copiedImageUrl = imageUrl;
        if (imageUrl != null && !imageUrl.isEmpty()) {
            String userId = userRepository.getCurrentUserId();
            String fileName = ImageUtils.createUniqueFileName("shop", userId.hashCode(), 0);
            copiedImageUrl = ImageUtils.copyImageToInternalStorage(getApplication(), Uri.parse(imageUrl), "shop", fileName);
            
            if (copiedImageUrl == null) {
                errorMessage.setValue("Failed to copy image to internal storage");
                return;
            }
            
            Log.d("ShopViewModel", "Shop image copied to internal storage: " + copiedImageUrl);
        }
        
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
        
        shopRepository.createShop(name, category, phone, email, address, location, copiedImageUrl, regionId, cityId);
        
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
            // Copy image to internal storage for persistence
            String copiedImageUrl = imageUrl;
            if (imageUrl != null && !imageUrl.isEmpty()) {
                String fileName = ImageUtils.createUniqueFileName("shop", 0, currentShopData.getShopId().hashCode());
                copiedImageUrl = ImageUtils.copyImageToInternalStorage(getApplication(), Uri.parse(imageUrl), "shop", fileName);
                
                if (copiedImageUrl == null) {
                    errorMessage.setValue("Failed to copy image to internal storage");
                    return;
                }
                
                // Delete old image if it exists
                if (currentShopData.getImageUrl() != null && !currentShopData.getImageUrl().isEmpty()) {
                    ImageUtils.deleteImageFromInternalStorage(getApplication(), currentShopData.getImageUrl());
                }
                
                Log.d("ShopViewModel", "Shop image copied to internal storage: " + copiedImageUrl);
            }
            
            currentShopData.setImageUrl(copiedImageUrl);
            shopRepository.updateShop(currentShopData);
        }
    }
    
    public void updateShop(ShopModel shop) {
        shopRepository.updateShop(shop);
    }
    
    public void deleteShop(String shopId) {
        shopRepository.deleteShop(shopId);
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
