package com.example.soukify.data.models;

/**
 * Product Image Model - Firebase POJO for product image data
 * Used for Firestore serialization and follows MVVM pattern
 */
public class ProductImageModel {
    private String imageId;
    private String productId;
    private String imageUrl;
    
    // Default constructor required for Firestore
    public ProductImageModel() {}
    
    public ProductImageModel(String productId, String imageUrl) {
        this.productId = productId;
        this.imageUrl = imageUrl;
    }
    
    // Getters and Setters
    public String getImageId() {
        return imageId;
    }
    
    public void setImageId(String imageId) {
        this.imageId = imageId;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
