package com.example.soukify.data.models;

/**
 * Product Type Model - Firebase model for product types
 * POJO class for Firebase Firestore
 */
public class ProductTypeModel {
    private String typeId;
    private String name;
    
    // Default constructor for Firebase
    public ProductTypeModel() {}
    
    public ProductTypeModel(String name) {
        this.name = name;
    }
    
    // Getters and Setters
    public String getTypeId() {
        return typeId;
    }
    
    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        return "ProductTypeModel{" +
                "typeId='" + typeId + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
