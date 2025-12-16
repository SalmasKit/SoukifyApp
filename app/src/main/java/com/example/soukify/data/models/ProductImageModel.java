package com.example.soukify.data.models;

/**
 * Product Image Model - Firebase POJO matching SQL structure
 * Used for Firestore serialization and follows MVVM pattern
 */
public class ProductImageModel {
    private String imageId;      // SERIAL PRIMARY KEY
    private String imageUrl;     // VARCHAR(255) NOT NULL
    
    // Default constructor required for Firestore
    public ProductImageModel() {}
    
    public ProductImageModel(String imageId, String imageUrl) {
        this.imageId = imageId;
        this.imageUrl = imageUrl;
    }
    
    // Getters and Setters
    public String getImageId() {
        return imageId;
    }
    
    public void setImageId(String imageId) {
        this.imageId = imageId;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
