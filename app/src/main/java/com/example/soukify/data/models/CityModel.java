package com.example.soukify.data.models;

/**
 * City Model - Firebase POJO for city data
 * Used for Firestore serialization and follows MVVM pattern
 */
public class CityModel {
    private String cityId;
    private String regionId;
    private String name;
    
    // Default constructor required for Firestore
    public CityModel() {}
    
    public CityModel(String regionId, String name) {
        this.regionId = regionId;
        this.name = name;
    }
    
    // Getters and Setters
    public String getCityId() {
        return cityId;
    }
    
    public void setCityId(String cityId) {
        this.cityId = cityId;
    }
    
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
