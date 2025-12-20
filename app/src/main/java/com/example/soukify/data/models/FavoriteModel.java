package com.example.soukify.data.models;

/**
 * Favorite Model - Database entity for favorite data
 * Supports both shops and products with proper privacy
 */
public class FavoriteModel {
    private String favoriteId;
    private String userId;
    private String itemId; // Can be shopId or productId
    private String itemType; // "shop" or "product"
    private String createdAt;
    private long createdAtTimestamp;
    
    // Default constructor required for Room/SQLite
    public FavoriteModel() {}
    
    public FavoriteModel(String userId, String itemId, String itemType) {
        this.userId = userId;
        this.itemId = itemId;
        this.itemType = itemType;
        this.createdAt = String.valueOf(System.currentTimeMillis());
        this.createdAtTimestamp = System.currentTimeMillis();
    }
    
    // Convenience constructors
    public static FavoriteModel forShop(String userId, String shopId) {
        return new FavoriteModel(userId, shopId, "shop");
    }
    
    public static FavoriteModel forProduct(String userId, String productId) {
        return new FavoriteModel(userId, productId, "product");
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
    
    public String getItemId() {
        return itemId;
    }
    
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    
    public String getItemType() {
        return itemType;
    }
    
    public void setItemType(String itemType) {
        this.itemType = itemType;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getCreatedAtTimestamp() {
        return createdAtTimestamp;
    }
    
    public void setCreatedAtTimestamp(long createdAtTimestamp) {
        this.createdAtTimestamp = createdAtTimestamp;
    }
    
    // Helper methods
    public boolean isShopFavorite() {
        return "shop".equals(itemType);
    }
    
    public boolean isProductFavorite() {
        return "product".equals(itemType);
    }
}
