package com.example.soukify.data.models;

import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Shop Model - Firebase POJO for shop data
 * Used for Firestore serialization and follows MVVM pattern
 */
public class ShopModel {
    private String shopId;
    private String name;
    private String category;
    private String description;
    private double rating;
    private int reviews;
    private String location;
    private String imageUrl;
    private boolean favorite;
    private boolean liked;
    private int likesCount;
    private int favoritesCount;
    private String searchableName;
    private Object createdAt;   // Peut être String ou Timestamp
    private String phone;
    private String email;
    private String address;
    private String userId;
    private String regionId;
    private String cityId;
    private boolean hasPromotion;
    private int searchCount;    // <-- Champ pour le tri "Tendance"
    private String workingHours;
    private String workingDays;
    private String instagram;
    private String facebook;
    private String website;
    private boolean hasLivraison;
    private ArrayList<String> likedByUserIds;
    private Map<String, Float> userRatings;

    // Date formatter for consistent date format
    private static final String DATE_FORMAT = "dd/MM/yyyy HH:mm";

    // Default constructor required for Firestore
    public ShopModel() {
        this.favorite = false;
        this.liked = false;
        this.likesCount = 0;
        this.favoritesCount = 0;
        this.rating = 0.0;
        this.reviews = 0;
        this.hasLivraison = false;
        this.likedByUserIds = new ArrayList<>();
        this.userRatings = new HashMap<>();
        this.searchCount = 0; // initialiser à 0
    }

    // Constructeur simplifié
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
        this.searchableName = name.toLowerCase();
        this.createdAt = formatCurrentDate();
        this.searchCount = 0;
    }

    // Constructeur complet pour Firestore
    public ShopModel(String shopId, String userId, String name, String category, String phone, String email,
                     String address, String location, String imageUrl, String regionId, String cityId) {
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
        this.searchableName = name.toLowerCase();
        this.createdAt = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(new Date());
        this.searchCount = 0;
        this.workingHours = "";
        this.workingDays = "";
        this.instagram = "";
        this.facebook = "";
        this.website = "";
        this.hasPromotion = false;
        this.hasLivraison = false;
        this.likedByUserIds = new ArrayList<>();
        this.userRatings = new HashMap<>();
    }

    // Helper method to format current date consistently
    private String formatCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        return sdf.format(new Date());
    }

    // ---------- Getters & Setters ----------

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    @com.google.firebase.firestore.Exclude
    public boolean isFavorite() {
        return favorite;
    }

    @com.google.firebase.firestore.Exclude
    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    @com.google.firebase.firestore.Exclude
    public boolean isLiked() {
        return liked;
    }

    @com.google.firebase.firestore.Exclude
    public void setLiked(boolean liked) {
        this.liked = liked;
    }

    public int getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(int likesCount) {
        this.likesCount = likesCount;
    }

    public int getFavoritesCount() {
        return favoritesCount;
    }

    public void setFavoritesCount(int favoritesCount) {
        this.favoritesCount = favoritesCount;
    }

    public String getSearchableName() {
        return searchableName;
    }

    public void setSearchableName(String searchableName) {
        this.searchableName = searchableName;
    }

    @com.google.firebase.firestore.PropertyName("createdAt")
    public Object getCreatedAt() {
        return createdAt;
    }

    @com.google.firebase.firestore.Exclude
    public String getCreatedAtString() {
        if (createdAt == null) return null;
        if (createdAt instanceof String) return (String) createdAt;
        if (createdAt instanceof Timestamp) {
            return new SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(((Timestamp) createdAt).toDate());
        }
        return createdAt.toString();
    }

    @com.google.firebase.firestore.PropertyName("createdAt")
    public void setCreatedAt(Object createdAt) {
        this.createdAt = createdAt;
    }

    public long getCreatedAtTimestamp() {
        String dateStr = getCreatedAtString();
        if (dateStr == null || dateStr.isEmpty()) {
            return System.currentTimeMillis();
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
            Date date = sdf.parse(dateStr);
            return date != null ? date.getTime() : System.currentTimeMillis();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getRegionId() { return regionId; }
    public void setRegionId(String regionId) { this.regionId = regionId; }
    public String getCityId() { return cityId; }
    public void setCityId(String cityId) { this.cityId = cityId; }
    public boolean isHasPromotion() { return hasPromotion; }
    public void setHasPromotion(boolean hasPromotion) { this.hasPromotion = hasPromotion; }
    public int getSearchCount() { return searchCount; }
    public void setSearchCount(int searchCount) { this.searchCount = searchCount; }

    public String getWorkingHours() { return workingHours; }
    public void setWorkingHours(String workingHours) { this.workingHours = workingHours; }
    public String getWorkingDays() { return workingDays; }
    public void setWorkingDays(String workingDays) { this.workingDays = workingDays; }
    public String getInstagram() { return instagram; }
    public void setInstagram(String instagram) { this.instagram = instagram; }
    public String getFacebook() { return facebook; }
    public void setFacebook(String facebook) { this.facebook = facebook; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public boolean isHasLivraison() { return hasLivraison; }
    public void setHasLivraison(boolean hasLivraison) { this.hasLivraison = hasLivraison; }
    public boolean hasLivraison() { return hasLivraison; }
    public ArrayList<String> getLikedByUserIds() { return likedByUserIds; }
    public void setLikedByUserIds(ArrayList<String> likedByUserIds) { this.likedByUserIds = likedByUserIds != null ? likedByUserIds : new ArrayList<>(); }
    public Map<String, Float> getUserRatings() { return userRatings; }
    public void setUserRatings(Map<String, Float> userRatings) { this.userRatings = userRatings != null ? userRatings : new HashMap<>(); }

    // Méthode pratique pour incrémenter le compteur de recherche
    public void incrementSearchCount() { this.searchCount++; }

    public void calculateAverageRating() {
        if (userRatings == null || userRatings.isEmpty()) {
            this.rating = 0.0;
            this.reviews = 0;
            return;
        }

        float sum = 0.0f;
        int count = 0;
        for (Float ratingValue : userRatings.values()) {
            if (ratingValue != null) {
                sum += ratingValue;
                count++;
            }
        }

        this.rating = count > 0 ? (double) sum / count : 0.0;
        this.reviews = count;
    }
}
