package com.example.soukify.data.models;

/**
 * Region Model - Firebase POJO for region data
 * Used for Firestore serialization and follows MVVM pattern
 */
public class RegionModel {
    private String regionId;
    private String name;
    
    // Default constructor required for Firestore
    public RegionModel() {}
    
    public RegionModel(String name) {
        this.name = name;
    }
    
    // Getters and Setters
    public String getRegionId() {
        return regionId;
    }
    
    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
}
