package com.example.soukify.data.models;

import java.util.List;

/**
 * Product Model - Firebase POJO for product data
 * Used for Firestore serialization and follows MVVM pattern
 */
public class ProductModel {
    private String productId;
    private String shopId;
    private String name;
    private String description;
    private double price;
    private String currency;
    private String createdAt;
    private String typeId;
    private String typeName;
    private String images;
    
    // Default constructor required for Firestore
    public ProductModel() {}
    
    public ProductModel(String shopId, String name, String description, double price, String currency) {
        this.shopId = shopId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.currency = currency;
        this.createdAt = String.valueOf(System.currentTimeMillis());
    }
    
    // Getters and Setters
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public String getShopId() {
        return shopId;
    }
    
    public void setShopId(String shopId) {
        this.shopId = shopId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public double getPrice() {
        return price;
    }
    
    public void setPrice(double price) {
        this.price = price;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getTypeId() {
        return typeId;
    }
    
    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }
    
    public String getTypeName() {
        return typeName;
    }
    
    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }
    
    public String getImages() {
        return images;
    }
    
    public void setImages(String images) {
        this.images = images;
    }
}
