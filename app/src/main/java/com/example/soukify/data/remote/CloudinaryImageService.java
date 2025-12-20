package com.example.soukify.data.remote;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class CloudinaryImageService {
    private static final String TAG = "CloudinaryImageService";
    private final Cloudinary cloudinary;
    private final Context context;

    public interface MediaUploadCallback {
        void onSuccess(String mediaUrl);
        void onError(String error);
        void onProgress(int progress);
    }

    public interface MediaDeleteCallback {
        void onSuccess();
        void onError(String error);
    }

    public CloudinaryImageService(Context context) {
        this.context = context;
        this.cloudinary = CloudinaryConfig.getInstance();
    }

    public void uploadMedia(Uri mediaUri, String publicId, MediaUploadCallback callback) {
        new Thread(() -> {
            try {
                callback.onProgress(10);
                
                // Determine if it's a video or image
                boolean isVideo = isVideoFile(mediaUri);
                String resourceType = isVideo ? "video" : "image";
                
                Log.d(TAG, "Uploading " + resourceType + " with public ID: " + publicId);
                
                callback.onProgress(30);
                
                // Create temporary file
                File tempFile = createFileFromUri(mediaUri, isVideo);
                
                callback.onProgress(50);
                
                // Configure upload options
                Map<String, Object> uploadOptions = new HashMap<>();
                uploadOptions.put("public_id", publicId);
                uploadOptions.put("resource_type", resourceType);
                uploadOptions.put("overwrite", true);
                
                callback.onProgress(70);
                
                // Upload to Cloudinary
                Map<String, Object> uploadResult = cloudinary.uploader().upload(tempFile, uploadOptions);
                
                callback.onProgress(90);
                
                // Get the secure URL
                String mediaUrl = (String) uploadResult.get("secure_url");
                
                // Clean up temporary file
                tempFile.delete();
                
                callback.onProgress(100);
                callback.onSuccess(mediaUrl);
                
                Log.d(TAG, resourceType + " uploaded successfully: " + mediaUrl);
                
            } catch (Exception e) {
                Log.e(TAG, "Error uploading media", e);
                callback.onError("Failed to upload media: " + e.getMessage());
            }
        }).start();
    }

    public void uploadImage(Uri imageUri, String publicId, MediaUploadCallback callback) {
        uploadMedia(imageUri, publicId, callback);
    }

    public void uploadImage(Bitmap bitmap, String publicId, MediaUploadCallback callback) {
        new Thread(() -> {
            try {
                callback.onProgress(10);
                
                // Create temporary file from bitmap
                File tempFile = createFileFromBitmap(bitmap);
                
                callback.onProgress(50);
                
                // Configure upload options
                Map<String, Object> uploadOptions = new HashMap<>();
                uploadOptions.put("public_id", publicId);
                uploadOptions.put("resource_type", "image");
                uploadOptions.put("overwrite", true);
                
                callback.onProgress(70);
                
                // Upload to Cloudinary
                Map<String, Object> uploadResult = cloudinary.uploader().upload(tempFile, uploadOptions);
                
                callback.onProgress(90);
                
                // Get the secure URL
                String imageUrl = (String) uploadResult.get("secure_url");
                
                // Clean up temporary file
                tempFile.delete();
                
                callback.onProgress(100);
                callback.onSuccess(imageUrl);
                
                Log.d(TAG, "Bitmap uploaded successfully: " + imageUrl);
                
            } catch (Exception e) {
                Log.e(TAG, "Error uploading bitmap", e);
                callback.onError("Failed to upload image: " + e.getMessage());
            }
        }).start();
    }

    public void deleteMedia(String publicId, MediaDeleteCallback callback) {
        new Thread(() -> {
            try {
                // Try to delete as image first
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                
                // Also try to delete as video (won't fail if it doesn't exist)
                try {
                    cloudinary.uploader().destroy(publicId, 
                        ObjectUtils.asMap("resource_type", "video"));
                } catch (Exception ignored) {
                    // It's okay if video deletion fails
                }
                
                callback.onSuccess();
                Log.d(TAG, "Media deleted successfully: " + publicId);
                
            } catch (Exception e) {
                Log.e(TAG, "Error deleting media", e);
                callback.onError("Failed to delete media: " + e.getMessage());
            }
        }).start();
    }

    private File createFileFromUri(Uri uri, boolean isVideo) throws IOException {
        String fileName = "temp_media_" + System.currentTimeMillis() + 
                         (isVideo ? ".mp4" : ".jpg");
        File tempFile = new File(context.getCacheDir(), fileName);
        
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        
        return tempFile;
    }

    private File createFileFromBitmap(Bitmap bitmap) throws IOException {
        String fileName = "temp_bitmap_" + System.currentTimeMillis() + ".jpg";
        File tempFile = new File(context.getCacheDir(), fileName);
        
        try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);
        }
        
        return tempFile;
    }

    private boolean isVideoFile(Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        return mimeType != null && mimeType.startsWith("video/");
    }

    public static String generateUniquePublicId(String prefix, String userId) {
        return prefix + "_" + userId + "_" + System.currentTimeMillis();
    }
}
