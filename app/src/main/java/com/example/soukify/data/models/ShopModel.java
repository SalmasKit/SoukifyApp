package com.example.soukify.data.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Shop Model - Firebase POJO for shop data
 * Used for Firestore serialization and follows MVVM pattern
 */
public class ShopModel {
    private String shopId;
    private String name;
    private String category;
    private double rating;
    private int reviews;
    private String location;
    private String imageUrl;
    private boolean favorite;
    private boolean liked;
    private int likesCount;
    private boolean hasProducts;
    private String searchableName;
    private String createdAt;
    private String phone;
    private String email;
    private String address;
    private String userId;
    private String regionId;
    private String cityId;
    private boolean hasPromotion;
    private int searchCount;
    
    // Date formatter for consistent date format
    private static final String DATE_FORMAT = "dd/MM/yyyy HH:mm";
    
    // Default constructor required for Firestore
    public ShopModel() {
        this.favorite = false;
        this.liked = false;
        this.likesCount = 0;
        this.hasProducts = false;
        this.rating = 0.0;
        this.reviews = 0;
    }
    
    public ShopModel(String name, String category, String location, String imageUrl) {
        this.name = name;
        this.category = category;
        this.location = location;
        this.imageUrl = imageUrl;
        this.rating = 0.0;
        this.reviews = 0;
        this.favorite = false;
        this.liked = false;
        this.likesCount = 0;
        this.hasProducts = false;
        this.searchableName = name.toLowerCase();
        this.createdAt = formatCurrentDate();
    }
    
    public ShopModel(String userId, String name, String category, String phone, String email, String address, String location, String imageUrl, String regionId, String cityId) {
        this.userId = userId;
        this.name = name;
        this.category = category;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.location = location;
        this.imageUrl = imageUrl;
        this.regionId = regionId;
        this.cityId = cityId;
        this.rating = 0.0;
        this.reviews = 0;
        this.favorite = false;
        this.liked = false;
        this.likesCount = 0;
        this.hasProducts = false;
        this.searchableName = name.toLowerCase();
        this.createdAt = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(new Date());
    }
    
    public ShopModel(String shopId, String userId, String name, String category, String phone, String email, String address, String location, String imageUrl, String regionId, String cityId) {
        this.shopId = shopId;
        this.userId = userId;
        this.name = name;
        this.category = category;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.location = location;
        this.imageUrl = imageUrl;
        this.regionId = regionId;
        this.cityId = cityId;
        this.rating = 0.0;
        this.reviews = 0;
        this.favorite = false;
        this.liked = false;
        this.likesCount = 0;
        this.hasProducts = false;
        this.searchableName = name.toLowerCase();
        this.createdAt = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(new Date());
    }
    
    // Helper method to format current date consistently
    private String formatCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        return sdf.format(new Date());
    }
    
    // Getters and Setters
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
        this.searchableName = name.toLowerCase();
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public double getRating() {
        return rating;
    }
    
    public void setRating(double rating) {
        this.rating = rating;
    }
    
    public int getReviews() {
        return reviews;
    }
    
    public void setReviews(int reviews) {
        this.reviews = reviews;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public boolean isFavorite() {
        return favorite;
    }
    
    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }
    
    public boolean isLiked() {
        return liked;
    }
    
    public void setLiked(boolean liked) {
        this.liked = liked;
    }
    
    public int getLikesCount() {
        return likesCount;
    }
    
    public void setLikesCount(int likesCount) {
        this.likesCount = likesCount;
    }
    
    public boolean isHasProducts() {
        return hasProducts;
    }
    
    public void setHasProducts(boolean hasProducts) {
        this.hasProducts = hasProducts;
    }
    
    public String getSearchableName() {
        return searchableName;
    }
    
    public void setSearchableName(String searchableName) {
        this.searchableName = searchableName;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getRegionId() {
        return regionId;
    }
    
    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }
    
    public String getCityId() {
        return cityId;
    }
    
    public void setCityId(String cityId) {
        this.cityId = cityId;
    }
    
    public boolean isHasPromotion() {
        return hasPromotion;
    }
    
    public void setHasPromotion(boolean hasPromotion) {
        this.hasPromotion = hasPromotion;
    }
    
    public int getSearchCount() {
        return searchCount;
    }
    
    public void setSearchCount(int searchCount) {
        this.searchCount = searchCount;
    }
}
