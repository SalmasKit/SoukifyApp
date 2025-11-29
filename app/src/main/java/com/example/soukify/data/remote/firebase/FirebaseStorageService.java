package com.example.soukify.data.remote.firebase;

import android.net.Uri;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;

/**
 * Firebase Storage Service - Handles file upload and download operations
 * Follows MVVM pattern by providing clean separation from UI layer
 */
public class FirebaseStorageService {
    private final FirebaseStorage storage;
    
    public FirebaseStorageService(FirebaseStorage storage) {
        this.storage = storage;
    }
    
    public StorageReference getRootReference() {
        return storage.getReference();
    }
    
    public StorageReference getUserImagesReference() {
        return storage.getReference().child("users");
    }
    
    public StorageReference getShopImagesReference() {
        return storage.getReference().child("shops");
    }
    
    public StorageReference getProductImagesReference() {
        return storage.getReference().child("products");
    }
    
    public UploadTask uploadUserImage(String userId, Uri fileUri) {
        StorageReference ref = getUserImagesReference().child(userId).child("profile.jpg");
        return ref.putFile(fileUri);
    }
    
    public UploadTask uploadShopImage(String shopId, Uri fileUri) {
        StorageReference ref = getShopImagesReference().child(shopId).child("shop.jpg");
        return ref.putFile(fileUri);
    }
    
    public UploadTask uploadProductImage(String productId, Uri fileUri) {
        StorageReference ref = getProductImagesReference().child(productId).child("product.jpg");
        return ref.putFile(fileUri);
    }
    
    public UploadTask uploadProductImage(String productId, String imageName, Uri fileUri) {
        StorageReference ref = getProductImagesReference().child(productId).child(imageName);
        return ref.putFile(fileUri);
    }
    
    public Task<Uri> getUserImageUrl(String userId) {
        StorageReference ref = getUserImagesReference().child(userId).child("profile.jpg");
        return ref.getDownloadUrl();
    }
    
    public Task<Uri> getShopImageUrl(String shopId) {
        StorageReference ref = getShopImagesReference().child(shopId).child("shop.jpg");
        return ref.getDownloadUrl();
    }
    
    public Task<Uri> getProductImageUrl(String productId) {
        StorageReference ref = getProductImagesReference().child(productId).child("product.jpg");
        return ref.getDownloadUrl();
    }
    
    public Task<Uri> getProductImageUrl(String productId, String imageName) {
        StorageReference ref = getProductImagesReference().child(productId).child(imageName);
        return ref.getDownloadUrl();
    }
    
    public Task<Void> deleteUserImage(String userId) {
        StorageReference ref = getUserImagesReference().child(userId).child("profile.jpg");
        return ref.delete();
    }
    
    public Task<Void> deleteShopImage(String shopId) {
        StorageReference ref = getShopImagesReference().child(shopId).child("shop.jpg");
        return ref.delete();
    }
    
    public Task<Void> deleteProductImage(String productId) {
        StorageReference ref = getProductImagesReference().child(productId).child("product.jpg");
        return ref.delete();
    }
    
    public Task<Void> deleteProductImage(String productId, String imageName) {
        StorageReference ref = getProductImagesReference().child(productId).child(imageName);
        return ref.delete();
    }
}
