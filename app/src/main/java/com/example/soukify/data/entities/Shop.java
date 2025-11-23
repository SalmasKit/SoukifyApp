package com.example.soukify.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity(tableName = "shops")
public class Shop {

    @PrimaryKey(autoGenerate = true)
    private int shopId;

    private String id;
    private String name;
    private String category;
    private int rating;
    private int reviews;
    private String location;
    private String imageUrl;

    private boolean favorite = false;  // <- gère les favoris
    private boolean liked = false;
    private int likesCount = 0;
    private boolean hasPromotion = false;
    private int searchCount = 0;
    private long createdAt;

    // Colonnes ajoutées
    private int userId;
    private int regionId;
    private int cityId;

    public Shop(String ext1, String laPotterieDeSafae, String potterie, int rating, int reviews, String oujda, String imageUrl, boolean hasPromotion, long createdAt, boolean b, boolean b1, int cityId) {}

    public Shop(String id, String name, String category, int rating, int reviews,
                String location, String imageUrl, boolean hasPromotion, long createdAt,
                int userId, int regionId, int cityId) {

        this.id = id;
        this.name = name;
        this.category = category;
        this.rating = rating;
        this.reviews = reviews;
        this.location = location;
        this.imageUrl = imageUrl;
        this.hasPromotion = hasPromotion;
        this.createdAt = createdAt;
        this.userId = userId;
        this.regionId = regionId;
        this.cityId = cityId;
    }

    // --- Getters / Setters ---
    public int getShopId() { return shopId; }
    public void setShopId(int shopId) { this.shopId = shopId; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public int getReviews() { return reviews; }
    public void setReviews(int reviews) { this.reviews = reviews; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }

    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }

    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }

    public boolean isHasPromotion() { return hasPromotion; }
    public void setHasPromotion(boolean hasPromotion) { this.hasPromotion = hasPromotion; }

    public int getSearchCount() { return searchCount; }
    public void setSearchCount(int searchCount) { this.searchCount = searchCount; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    // --- Getters/Setters ajoutés ---
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getRegionId() { return regionId; }
    public void setRegionId(int regionId) { this.regionId = regionId; }

    public int getCityId() { return cityId; }
    public void setCityId(int cityId) { this.cityId = cityId; }
}