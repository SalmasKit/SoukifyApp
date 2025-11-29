package com.example.soukify.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility class for handling image operations
 * Copies images from gallery to app's internal storage for persistence
 */
public class ImageUtils {
    
    private static final String TAG = "ImageUtils";
    private static final String PROFILE_IMAGES_DIR = "profile_images";
    private static final String SHOP_IMAGES_DIR = "shop_images";
    
    /**
     * Copies an image from URI to app's internal storage
     * @param context Application context
     * @param sourceUri Original URI from gallery
     * @param imageType Type of image (profile or shop)
     * @param fileName Unique filename
     * @return URI of the copied image in internal storage
     */
    public static String copyImageToInternalStorage(Context context, Uri sourceUri, String imageType, String fileName) {
        try {
            // Create directory if it doesn't exist
            File directory = getImageDirectory(context, imageType);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            // Create destination file
            File destinationFile = new File(directory, fileName + ".jpg");
            
            // Copy the image
            copyFile(context, sourceUri, destinationFile);
            
            // Return the file URI
            return Uri.fromFile(destinationFile).toString();
            
        } catch (Exception e) {
            Log.e(TAG, "Error copying image to internal storage", e);
            return null;
        }
    }
    
    /**
     * Creates a unique filename for the image
     * @param imageType Type of image
     * @param userId User ID for profile images
     * @param shopId Shop ID for shop images
     * @return Unique filename
     */
    public static String createUniqueFileName(String imageType, int userId, int shopId) {
        long timestamp = System.currentTimeMillis();
        if ("profile".equals(imageType)) {
            return "profile_" + userId + "_" + timestamp;
        } else if ("shop".equals(imageType)) {
            return "shop_" + shopId + "_" + timestamp;
        }
        return "image_" + timestamp;
    }
    
    /**
     * Gets the appropriate directory for storing images
     * @param context Application context
     * @param imageType Type of image (profile or shop)
     * @return Directory file
     */
    private static File getImageDirectory(Context context, String imageType) {
        File appDir = context.getFilesDir();
        if ("profile".equals(imageType)) {
            return new File(appDir, PROFILE_IMAGES_DIR);
        } else if ("shop".equals(imageType)) {
            return new File(appDir, SHOP_IMAGES_DIR);
        }
        return appDir;
    }
    
    /**
     * Copies file from URI to destination file
     * @param context Application context
     * @param sourceUri Source URI
     * @param destinationFile Destination file
     * @throws IOException If copy fails
     */
    private static void copyFile(Context context, Uri sourceUri, File destinationFile) throws IOException {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        
        try {
            // Open input stream from source URI
            inputStream = context.getContentResolver().openInputStream(sourceUri);
            
            // Create output stream to destination
            outputStream = new FileOutputStream(destinationFile);
            
            // Copy bytes
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            outputStream.flush();
            Log.d(TAG, "Image copied successfully to: " + destinationFile.getAbsolutePath());
            
        } finally {
            // Close streams
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing input stream", e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing output stream", e);
                }
            }
        }
    }
    
    /**
     * Deletes an image file from internal storage
     * @param context Application context
     * @param imageUri URI of the image to delete
     * @return True if deletion was successful
     */
    public static boolean deleteImageFromInternalStorage(Context context, String imageUri) {
        try {
            if (imageUri != null && !imageUri.isEmpty()) {
                Uri uri = Uri.parse(imageUri);
                File imageFile = new File(uri.getPath());
                if (imageFile.exists()) {
                    boolean deleted = imageFile.delete();
                    Log.d(TAG, "Image deletion " + (deleted ? "successful" : "failed") + ": " + imageUri);
                    return deleted;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting image", e);
        }
        return false;
    }
    
    /**
     * Checks if an image file exists in internal storage
     * @param imageUri URI of the image to check
     * @return True if file exists
     */
    public static boolean imageExists(String imageUri) {
        try {
            if (imageUri != null && !imageUri.isEmpty()) {
                Uri uri = Uri.parse(imageUri);
                File imageFile = new File(uri.getPath());
                return imageFile.exists();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking if image exists", e);
        }
        return false;
    }
}
