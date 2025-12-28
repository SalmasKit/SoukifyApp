package com.example.soukify.data.models;

/**
 * Region Model - Firebase POJO for region data
 * Used for Firestore serialization and follows MVVM pattern
 */
public class RegionModel {
    private String regionId;
    private String name;
    private String name_fr;
    private String name_ar;
    
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

    public String getName_fr() { return name_fr; }
    public void setName_fr(String name_fr) { this.name_fr = name_fr; }

    public String getName_ar() { return name_ar; }
    public void setName_ar(String name_ar) { this.name_ar = name_ar; }

    public String getLocalizedName() {
        String lang = java.util.Locale.getDefault().getLanguage();
        if ("fr".equals(lang) && name_fr != null && !name_fr.isEmpty()) return name_fr;
        if ("ar".equals(lang) && name_ar != null && !name_ar.isEmpty()) return name_ar;
        return name;
    }
}
