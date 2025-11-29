package com.example.soukify.data.models;

/**
 * Favorite Model - Firebase POJO for favorite data
 * Used for Firestore serialization and follows MVVM pattern
 */
public class FavoriteModel {
    private String favoriteId;
    private String userId;
    private String productId;
    private String createdAt;
    
    // Default constructor required for Firestore
    public FavoriteModel() {}
    
    public FavoriteModel(String userId, String productId) {
        this.userId = userId;
        this.productId = productId;
        this.createdAt = String.valueOf(System.currentTimeMillis());
    }
    
    // Getters and Setters
    public String getFavoriteId() {
        return favoriteId;
    }
    
    public void setFavoriteId(String favoriteId) {
        this.favoriteId = favoriteId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
