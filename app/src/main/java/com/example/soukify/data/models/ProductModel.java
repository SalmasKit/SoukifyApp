package com.example.soukify.data.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

import java.util.List;
import java.util.ArrayList;

/**
 * Product Model - Firebase POJO matching SQL structure
 * Used for Firestore serialization and follows MVVM pattern
 */
public class ProductModel implements Parcelable {
    private String productId;        // SERIAL PRIMARY KEY
    private String shopId;           // INT REFERENCES shops(shop_id) ON DELETE CASCADE
    private String name;             // VARCHAR(120) NOT NULL
    private String description;      // TEXT
    private String productType;      // VARCHAR(50) NOT NULL - direct product type storage
    private double price;            // DECIMAL(10,2)
    private String currency;         // VARCHAR(10) DEFAULT 'MAD'
    private List<String> imageIds;   // List of image IDs for carousel - NEW FIELD
    private String createdAt;        // TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    private int likesCount;         // INTEGER DEFAULT 0 - Number of likes
    
    // Optional product details
    private Double weight;            // Weight in kg
    private Double length;            // Length in cm
    private Double width;             // Width in cm
    private Double height;            // Height in cm
    private String color;             // Color
    private String material;          // Material
    
    // Default constructor required for Firestore
    public ProductModel() {
        this.imageIds = new ArrayList<>(); // Initialize empty list
        this.likesCount = 0;         // Initialize likes count
    }
    
    public ProductModel(String shopId, String name, String description, String productType, 
                        double price, String currency) {
        this.shopId = shopId;
        this.name = name;
        this.description = description;
        this.productType = productType;
        this.price = price;
        this.currency = currency != null ? currency : "MAD";
        this.imageIds = new ArrayList<>(); // Initialize empty list
        this.createdAt = String.valueOf(System.currentTimeMillis());
    }
    
    // Constructor with timestamp
    public ProductModel(String shopId, String name, String description, String productType, 
                        double price, String currency, long createdAt) {
        this.shopId = shopId;
        this.name = name;
        this.description = description;
        this.productType = productType;
        this.price = price;
        this.currency = currency != null ? currency : "MAD";
        this.imageIds = new ArrayList<>(); // Initialize empty list
        this.createdAt = String.valueOf(createdAt);
    }
    
    // Constructor with imageIds list
    public ProductModel(String shopId, String name, String description, String productType, 
                        double price, String currency, List<String> imageIds) {
        this.shopId = shopId;
        this.name = name;
        this.description = description;
        this.productType = productType;
        this.price = price;
        this.currency = currency != null ? currency : "MAD";
        this.imageIds = imageIds != null ? new ArrayList<>(imageIds) : new ArrayList<>();
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
    
    public String getProductType() {
        return productType;
    }
    
    public void setProductType(String productType) {
        this.productType = productType;
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
    
    public List<String> getImageIds() {
        return imageIds;
    }
    
    public void setImageIds(List<String> imageIds) {
        this.imageIds = imageIds != null ? new ArrayList<>(imageIds) : new ArrayList<>();
    }
    
    // Convenience methods for managing image list
    public void addImageId(String imageId) {
        if (imageId != null && !imageId.trim().isEmpty()) {
            if (this.imageIds == null) {
                this.imageIds = new ArrayList<>();
            }
            this.imageIds.add(imageId);
        }
    }
    
    public void removeImageId(String imageId) {
        if (this.imageIds != null && imageId != null) {
            this.imageIds.remove(imageId);
        }
    }
    
    public boolean hasImages() {
        return imageIds != null && !imageIds.isEmpty();
    }
    
    public int getImageCount() {
        return imageIds != null ? imageIds.size() : 0;
    }
    
    public String getPrimaryImageId() {
        return (imageIds != null && !imageIds.isEmpty()) ? imageIds.get(0) : null;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    // Optional product details getters and setters
    public Double getWeight() {
        return weight;
    }
    
    public void setWeight(Double weight) {
        this.weight = weight;
    }
    
    public Double getLength() {
        return length;
    }
    
    public void setLength(Double length) {
        this.length = length;
    }
    
    public Double getWidth() {
        return width;
    }
    
    public void setWidth(Double width) {
        this.width = width;
    }
    
    public Double getHeight() {
        return height;
    }
    
    public void setHeight(Double height) {
        this.height = height;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public String getMaterial() {
        return material;
    }
    
    public void setMaterial(String material) {
        this.material = material;
    }
    
    // Likes and favorites getters and setters
    public int getLikesCount() {
        return likesCount;
    }
    
    public void setLikesCount(int likesCount) {
        this.likesCount = likesCount;
    }
    
        
    // Helper method to check if product has any details
    public boolean hasDetails() {
        return (weight != null && weight > 0) ||
               (length != null && length > 0) ||
               (width != null && width > 0) ||
               (height != null && height > 0) ||
               (color != null && !color.trim().isEmpty()) ||
               (material != null && !material.trim().isEmpty());
    }
    
    // Helper method to get formatted dimensions string
    public String getFormattedDimensions() {
        StringBuilder dimensions = new StringBuilder();
        
        if (length != null && length > 0) {
            dimensions.append("Length: ").append(String.format("%.1f cm", length));
        }
        if (width != null && width > 0) {
            if (dimensions.length() > 0) dimensions.append("\n");
            dimensions.append("Width: ").append(String.format("%.1f cm", width));
        }
        if (height != null && height > 0) {
            if (dimensions.length() > 0) dimensions.append("\n");
            dimensions.append("Height: ").append(String.format("%.1f cm", height));
        }
        
        return dimensions.length() > 0 ? dimensions.toString() : null;
    }
    
    // Helper method to get individual dimension strings
    public String getFormattedLength() {
        return length != null && length > 0 ? String.format("%.1f cm", length) : null;
    }
    
    public String getFormattedWidth() {
        return width != null && width > 0 ? String.format("%.1f cm", width) : null;
    }
    
    public String getFormattedHeight() {
        return height != null && height > 0 ? String.format("%.1f cm", height) : null;
    }
    
    // Parcelable implementation
    protected ProductModel(Parcel in) {
        productId = in.readString();
        shopId = in.readString();
        name = in.readString();
        description = in.readString();
        productType = in.readString();
        price = in.readDouble();
        currency = in.readString();
        imageIds = in.createStringArrayList();
        createdAt = in.readString();
        likesCount = in.readInt();
        // Optional details
        weight = in.readByte() == 1 ? in.readDouble() : null;
        length = in.readByte() == 1 ? in.readDouble() : null;
        width = in.readByte() == 1 ? in.readDouble() : null;
        height = in.readByte() == 1 ? in.readDouble() : null;
        color = in.readString();
        material = in.readString();
    }
    
    public static final Creator<ProductModel> CREATOR = new Creator<ProductModel>() {
        @Override
        public ProductModel createFromParcel(Parcel in) {
            return new ProductModel(in);
        }
        
        @Override
        public ProductModel[] newArray(int size) {
            return new ProductModel[size];
        }
    };
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(productId);
        dest.writeString(shopId);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(productType);
        dest.writeDouble(price);
        dest.writeString(currency);
        dest.writeStringList(imageIds);
        dest.writeString(createdAt);
        dest.writeInt(likesCount);
        // Optional details
        if (weight != null) {
            dest.writeByte((byte) 1);
            dest.writeDouble(weight);
        } else {
            dest.writeByte((byte) 0);
        }
        if (length != null) {
            dest.writeByte((byte) 1);
            dest.writeDouble(length);
        } else {
            dest.writeByte((byte) 0);
        }
        if (width != null) {
            dest.writeByte((byte) 1);
            dest.writeDouble(width);
        } else {
            dest.writeByte((byte) 0);
        }
        if (height != null) {
            dest.writeByte((byte) 1);
            dest.writeDouble(height);
        } else {
            dest.writeByte((byte) 0);
        }
        dest.writeString(color);
        dest.writeString(material);
    }
}
