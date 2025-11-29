package com.example.soukify.data.remote.firebase;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.soukify.data.models.ProductTypeModel;

import java.util.List;
import java.util.ArrayList;

/**
 * Firebase Product Type Service - Handles product type operations
 * Follows MVVM pattern by providing clean separation from UI layer
 */
public class FirebaseProductTypeService {
    private final FirebaseFirestore firestore;
    
    private static final String PRODUCT_TYPES_COLLECTION = "product_types";
    
    public FirebaseProductTypeService(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }
    
    // CRUD Operations
    
    /**
     * Create a product type
     */
    public Task<DocumentReference> createProductType(ProductTypeModel productType) {
        return firestore.collection(PRODUCT_TYPES_COLLECTION).add(productType);
    }
    
    /**
     * Update a product type
     */
    public Task<Void> updateProductType(String typeId, ProductTypeModel productType) {
        return firestore.collection(PRODUCT_TYPES_COLLECTION).document(typeId).set(productType);
    }
    
    /**
     * Delete a product type
     */
    public Task<Void> deleteProductType(String typeId) {
        return firestore.collection(PRODUCT_TYPES_COLLECTION).document(typeId).delete();
    }
    
    // Query Operations
    
    /**
     * Get all product types
     */
    public Query getAllProductTypes() {
        return firestore.collection(PRODUCT_TYPES_COLLECTION).orderBy("name");
    }
    
    /**
     * Get product type by ID
     */
    public Task<ProductTypeModel> getProductType(String typeId) {
        return firestore.collection(PRODUCT_TYPES_COLLECTION).document(typeId).get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        return task.getResult().toObject(ProductTypeModel.class);
                    }
                    return null;
                });
    }
    
    /**
     * Get product type by name
     */
    public Query getProductTypeByName(String name) {
        return firestore.collection(PRODUCT_TYPES_COLLECTION)
                .whereEqualTo("name", name);
    }
    
    // Utility Operations
    
    /**
     * Initialize default product types
     */
    public Task<Void> initializeDefaultProductTypes() {
        List<ProductTypeModel> defaultTypes = getDefaultProductTypes();
        List<Task<Void>> tasks = new ArrayList<>();
        
        for (ProductTypeModel type : defaultTypes) {
            Task<DocumentReference> createTask = createProductType(type);
            tasks.add(createTask.continueWithTask(task1 -> {
                // Return a completed void task
                return com.google.android.gms.tasks.Tasks.forResult(null);
            }));
        }
        
        return com.google.android.gms.tasks.Tasks.whenAll(tasks);
    }
    
    /**
     * Get default product types for initialization
     */
    private List<ProductTypeModel> getDefaultProductTypes() {
        List<ProductTypeModel> types = new ArrayList<>();
        
        types.add(new ProductTypeModel("Textile & Tapestry"));
        types.add(new ProductTypeModel("Gourmet & Local Foods"));
        types.add(new ProductTypeModel("Pottery & Ceramics"));
        types.add(new ProductTypeModel("Natural Wellness Products"));
        types.add(new ProductTypeModel("Jewelry & Accessories"));
        types.add(new ProductTypeModel("Metal & Brass Crafts"));
        types.add(new ProductTypeModel("Painting & Calligraphy"));
        types.add(new ProductTypeModel("Woodwork"));
        
        return types;
    }
}
