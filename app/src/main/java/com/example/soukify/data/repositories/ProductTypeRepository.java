package com.example.soukify.data.repositories;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.soukify.data.remote.FirebaseManager;
import com.example.soukify.data.remote.firebase.FirebaseProductTypeService;
import com.example.soukify.data.models.ProductTypeModel;

import java.util.List;
import java.util.ArrayList;

/**
 * Product Type Repository - Firebase implementation
 * Follows MVVM pattern by abstracting data operations from ViewModels
 */
public class ProductTypeRepository {
    private final FirebaseProductTypeService productTypeService;
    private final MutableLiveData<List<ProductTypeModel>> productTypes = new MutableLiveData<>();
    private final MutableLiveData<ProductTypeModel> currentProductType = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    
    public ProductTypeRepository(Application application) {
        FirebaseManager firebaseManager = FirebaseManager.getInstance(application);
        this.productTypeService = new FirebaseProductTypeService(firebaseManager.getFirestore());
    }
    
    // LiveData getters
    public LiveData<List<ProductTypeModel>> getProductTypes() {
        return productTypes;
    }
    
    public LiveData<ProductTypeModel> getCurrentProductType() {
        return currentProductType;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    // CRUD Operations
    
    public void createProductType(String name) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        ProductTypeModel productType = new ProductTypeModel(name);
        
        productTypeService.createProductType(productType)
                .addOnSuccessListener(documentReference -> {
                    productType.setTypeId(documentReference.getId());
                    loadProductTypes(); // Refresh the list
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to create product type: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void updateProductType(ProductTypeModel productType) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        productTypeService.updateProductType(productType.getTypeId(), productType)
                .addOnSuccessListener(aVoid -> {
                    loadProductTypes(); // Refresh the list
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to update product type: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void deleteProductType(String typeId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        productTypeService.deleteProductType(typeId)
                .addOnSuccessListener(aVoid -> {
                    loadProductTypes(); // Refresh the list
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to delete product type: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    // Query Operations
    
    public void loadProductTypes() {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        productTypeService.getAllProductTypes().get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ProductTypeModel> types = new ArrayList<>();
                    for (var doc : querySnapshot.getDocuments()) {
                        ProductTypeModel productType = doc.toObject(ProductTypeModel.class);
                        if (productType != null) {
                            productType.setTypeId(doc.getId());
                            types.add(productType);
                        }
                    }
                    productTypes.postValue(types);
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to load product types: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void getProductTypeById(String typeId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        productTypeService.getProductType(typeId)
                .addOnSuccessListener(productType -> {
                    currentProductType.postValue(productType);
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to load product type: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    public void getProductTypeByName(String name) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        productTypeService.getProductTypeByName(name).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        ProductTypeModel productType = querySnapshot.getDocuments().get(0).toObject(ProductTypeModel.class);
                        if (productType != null) {
                            productType.setTypeId(querySnapshot.getDocuments().get(0).getId());
                            currentProductType.postValue(productType);
                        }
                    }
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to load product type: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    // Utility Operations
    
    public void initializeDefaultProductTypes() {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        productTypeService.initializeDefaultProductTypes()
                .addOnSuccessListener(aVoid -> {
                    loadProductTypes(); // Refresh the list
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Failed to initialize default product types: " + e.getMessage());
                    isLoading.postValue(false);
                });
    }
    
    // Synchronous methods for backward compatibility
    
    public ProductTypeModel getProductTypeModelByName(String name) {
        List<ProductTypeModel> allTypes = productTypes.getValue();
        if (allTypes != null) {
            for (ProductTypeModel type : allTypes) {
                if (type.getName().equals(name)) {
                    return type;
                }
            }
        }
        return null;
    }
    
    public List<ProductTypeModel> getAllProductTypesSync() {
        List<ProductTypeModel> result = productTypes.getValue();
        return result != null ? result : new ArrayList<>();
    }
}
