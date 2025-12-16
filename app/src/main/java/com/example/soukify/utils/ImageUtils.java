package com.example.soukify.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility class for handling image operations
 * Uses local storage for persistent storage on the device
 */
public class ImageUtils {
    
    private static final String TAG = "ImageUtils";
    private static final String IMAGES_DIR = "images";
    
    /**
     * Saves an image to local storage for persistent storage
     * @param context Application context
     * @param sourceUri Original URI from gallery
     * @param imageType Type of image (profile, shop, or product)
     * @param entityId Entity ID (userId, shopId, or productId)
     * @return Task containing the local file URI
     */
    /**
     * Checks if a URI points to a video file
     * @param context Application context
     * @param uri URI to check
     * @return true if the URI is a video, false if it's an image
     */
    public static boolean isVideo(Context context, Uri uri) {
        try {
            String mimeType = context.getContentResolver().getType(uri);
            if (mimeType != null && mimeType.startsWith("video/")) {
                Log.d(TAG, "Detected VIDEO by MIME type: " + mimeType);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting MIME type", e);
        }
        
        // Fallback to extension check
        String uriString = uri.toString().toLowerCase();
        boolean isVideo = uriString.endsWith(".mp4") || 
                         uriString.endsWith(".3gp") || 
                         uriString.endsWith(".avi") || 
                         uriString.endsWith(".mov") ||
                         uriString.endsWith(".wmv") ||
                         uriString.endsWith(".mkv") ||
                         uriString.endsWith(".webm") ||
                         uriString.endsWith(".flv") ||
                         uriString.endsWith(".m4v");
        
        if (isVideo) {
            Log.d(TAG, "Detected VIDEO by extension");
        } else {
            Log.d(TAG, "Detected IMAGE");
        }
        
        return isVideo;
    }

    /**
     * Uploads media (image or video) to local storage with proper file extension
     * @param context Application context
     * @param sourceUri Source URI of the media
     * @param imageType Type of media (profile, shop, or product)
     * @param entityId Entity ID (userId, shopId, or productId)
     * @return Task containing the uploaded media URI
     */
    public static Task<Uri> uploadMediaToLocalStorage(Context context, Uri sourceUri, String imageType, String entityId) {
        return Tasks.call(() -> {
            try {
                boolean isVideoFile = isVideo(context, sourceUri);
                String fileExtension = isVideoFile ? ".mp4" : ".jpg";
                
                Log.d(TAG, "Starting local save for " + (isVideoFile ? "VIDEO" : "IMAGE") + " URI: " + sourceUri + ", type: " + imageType + ", entityId: " + entityId);

                // Create a temporary file from the URI to avoid permission issues
                File tempFile = createTempFileFromUri(context, sourceUri);
                if (tempFile == null) {
                    Log.e(TAG, "Failed to create temporary file from URI: " + sourceUri);
                    throw new IOException("Failed to create temporary file from URI");
                }

                // Verify file exists and has content before proceeding
                if (!tempFile.exists()) {
                    Log.e(TAG, "Temporary file does not exist: " + tempFile.getAbsolutePath());
                    throw new IOException("Temporary file does not exist");
                }

                if (tempFile.length() == 0) {
                    Log.e(TAG, "Temporary file is empty: " + tempFile.getAbsolutePath());
                    throw new IOException("Temporary file is empty");
                }

                // Create permanent local file with proper extension
                File permanentFile = createPermanentFileWithExtension(context, imageType, entityId, fileExtension);

                // Copy from temp to permanent location
                copyFile(tempFile, permanentFile);

                // Clean up temporary file
                cleanupTempFile(tempFile);

                Uri localUri = Uri.fromFile(permanentFile);
                Log.d(TAG, "Successfully saved " + (isVideoFile ? "VIDEO" : "IMAGE") + " locally: " + localUri + ", size: " + permanentFile.length() + " bytes");

                return localUri;

            } catch (Exception e) {
                Log.e(TAG, "Error saving media locally", e);
                throw e;
            }
        });
    }

    /**
     * Creates a permanent file with the specified extension
     */
    private static File createPermanentFileWithExtension(Context context, String imageType, String entityId, String extension) throws IOException {
        File imagesDir = new File(context.getFilesDir(), "images/" + imageType);
        if (!imagesDir.exists()) {
            imagesDir.mkdirs();
        }
        
        String fileName = imageType + "_" + entityId + "_" + System.currentTimeMillis() + extension;
        return new File(imagesDir, fileName);
    }

    public static Task<Uri> uploadImageToFirebaseStorage(Context context, Uri sourceUri, String imageType, String entityId) {
        return Tasks.call(() -> {
            try {
                Log.d(TAG, "Starting local save for URI: " + sourceUri + ", type: " + imageType + ", entityId: " + entityId);
                
                // Create a temporary file from the URI to avoid permission issues
                File tempFile = createTempFileFromUri(context, sourceUri);
                if (tempFile == null) {
                    Log.e(TAG, "Failed to create temporary file from URI: " + sourceUri);
                    throw new IOException("Failed to create temporary file from URI");
                }
                
                // Verify file exists and has content before proceeding
                if (!tempFile.exists()) {
                    Log.e(TAG, "Temporary file does not exist: " + tempFile.getAbsolutePath());
                    throw new IOException("Temporary file does not exist");
                }
                
                if (tempFile.length() == 0) {
                    Log.e(TAG, "Temporary file is empty: " + tempFile.getAbsolutePath());
                    throw new IOException("Temporary file is empty");
                }
                
                // Create permanent local file
                File permanentFile = createPermanentFile(context, imageType, entityId);
                
                // Copy from temp to permanent location
                copyFile(tempFile, permanentFile);
                
                // Clean up temporary file
                cleanupTempFile(tempFile);
                
                Uri localUri = Uri.fromFile(permanentFile);
                Log.d(TAG, "Successfully saved image locally: " + localUri + ", size: " + permanentFile.length() + " bytes");
                
                return localUri;
                
            } catch (Exception e) {
                Log.e(TAG, "Error saving image locally", e);
                throw e;
            }
        });
    }
    
    private static void cleanupTempFile(File tempFile) {
        if (tempFile != null && tempFile.exists()) {
            boolean deleted = tempFile.delete();
            Log.d(TAG, "Cleaned up temporary file: " + tempFile.getAbsolutePath() + " - " + (deleted ? "SUCCESS" : "FAILED"));
        }
    }
    
    /**
     * Creates a temporary file from a URI to avoid permission issues
     */
    private static File createTempFileFromUri(Context context, Uri uri) throws IOException {
        Log.d(TAG, "Starting temporary file creation for URI: " + uri);
        
        try {
            String fileName = "temp_image_" + System.currentTimeMillis() + ".jpg";
            File cacheDir = context.getCacheDir();
            Log.d(TAG, "Cache directory: " + cacheDir.getAbsolutePath() + ", exists: " + cacheDir.exists() + ", writable: " + cacheDir.canWrite());
            
            File tempFile = new File(cacheDir, fileName);
            Log.d(TAG, "Creating temp file: " + tempFile.getAbsolutePath());
            
            InputStream inputStream = null;
            OutputStream outputStream = null;
            
            try {
                inputStream = context.getContentResolver().openInputStream(uri);
                if (inputStream == null) {
                    Log.e(TAG, "Failed to open input stream for URI: " + uri);
                    throw new IOException("Failed to open input stream for URI: " + uri);
                }
                
                Log.d(TAG, "Input stream opened successfully");
                outputStream = new FileOutputStream(tempFile);
                Log.d(TAG, "Output stream opened successfully");
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytes = 0;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                
                outputStream.flush();
                Log.d(TAG, "Data written successfully, bytes copied: " + totalBytes);
                
                // Verify file was created and has content
                if (!tempFile.exists()) {
                    Log.e(TAG, "Temporary file was not created: " + tempFile.getAbsolutePath());
                    throw new IOException("Temporary file creation failed - file does not exist");
                }
                
                if (tempFile.length() == 0) {
                    Log.e(TAG, "Temporary file is empty: " + tempFile.getAbsolutePath());
                    throw new IOException("Temporary file creation failed - file is empty");
                }
                
                Log.d(TAG, "Successfully copied " + totalBytes + " bytes to temporary file: " + tempFile.getAbsolutePath());
                Log.d(TAG, "Final file size: " + tempFile.length() + " bytes");
                return tempFile;
                
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        Log.w(TAG, "Failed to close input stream", e);
                    }
                }
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        Log.w(TAG, "Failed to close output stream", e);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating temporary file from URI: " + uri, e);
            throw new IOException("Failed to create temporary file from URI: " + uri, e);
        }
    }
    
    /**
     * Creates a permanent file in the app's internal storage
     * @param context Application context
     * @param imageType Type of image (profile, shop, or product)
     * @param entityId Entity ID (userId, shopId, or productId)
     * @return File object for the permanent storage location
     */
    private static File createPermanentFile(Context context, String imageType, String entityId) {
        // Create images directory in app's internal storage
        File imagesDir = new File(context.getFilesDir(), IMAGES_DIR);
        if (!imagesDir.exists()) {
            boolean created = imagesDir.mkdirs();
            Log.d(TAG, "Created images directory: " + imagesDir.getAbsolutePath() + " - " + (created ? "SUCCESS" : "FAILED"));
        }
        
        // Create type-specific subdirectory
        File typeDir = new File(imagesDir, imageType);
        if (!typeDir.exists()) {
            boolean created = typeDir.mkdirs();
            Log.d(TAG, "Created " + imageType + " directory: " + typeDir.getAbsolutePath() + " - " + (created ? "SUCCESS" : "FAILED"));
        }
        
        // Create unique filename
        String fileName = createUniqueFileName(imageType, entityId);
        return new File(typeDir, fileName + ".jpg");
    }
    
    /**
     * Copies a file from source to destination
     * @param source Source file
     * @param destination Destination file
     * @throws IOException if copy fails
     */
    private static void copyFile(File source, File destination) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        
        try {
            in = new FileInputStream(source);
            out = new FileOutputStream(destination);
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            
            out.flush();
            Log.d(TAG, "Successfully copied " + source.length() + " bytes from " + source.getAbsolutePath() + " to " + destination.getAbsolutePath());
            
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.w(TAG, "Failed to close input stream", e);
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    Log.w(TAG, "Failed to close output stream", e);
                }
            }
        }
    }
    
    /**
     * Gets the local file URI for an image
     * @param imageType Type of image (profile, shop, or product)
     * @param entityId Entity ID (userId, shopId, or productId)
     * @return Task containing the local file URI, or null if not found
     */
    public static Task<Uri> getImageUrlFromFirebaseStorage(String imageType, String entityId) {
        return Tasks.call(() -> {
            try {
                // Try to find the most recent file for this entity
                File imagesDir = new File(IMAGES_DIR, imageType);
                // Note: This is a simplified approach. In a real app, you might want to
                // store the file path in the database or use a more sophisticated naming scheme
                Log.d(TAG, "Looking for local image: " + imageType + "/" + entityId);
                return null; // Return null for now - this would need proper implementation
            } catch (Exception e) {
                Log.e(TAG, "Error getting local image URL", e);
                throw e;
            }
        });
    }
    
    /**
     * Deletes a media file from local storage
     * @param mediaUrl The local file URI to delete (file:// format)
     * @return Task for the deletion operation
     */
    public static Task<Boolean> deleteMediaFromLocalStorage(String mediaUrl) {
        return Tasks.call(() -> {
            try {
                if (mediaUrl == null || mediaUrl.isEmpty()) {
                    Log.w(TAG, "Media URL is null or empty, nothing to delete");
                    return false;
                }
                
                Uri uri = Uri.parse(mediaUrl);
                if (!"file".equals(uri.getScheme())) {
                    Log.w(TAG, "Not a local file URI, cannot delete: " + mediaUrl);
                    return false;
                }
                
                String filePath = uri.getPath();
                if (filePath == null) {
                    Log.w(TAG, "File path is null, cannot delete: " + mediaUrl);
                    return false;
                }
                
                File mediaFile = new File(filePath);
                if (!mediaFile.exists()) {
                    Log.w(TAG, "Media file does not exist: " + filePath);
                    return false;
                }
                
                boolean deleted = mediaFile.delete();
                Log.d(TAG, "Deleted media file: " + filePath + " - " + (deleted ? "SUCCESS" : "FAILED"));
                
                return deleted;
                
            } catch (Exception e) {
                Log.e(TAG, "Error deleting media file: " + mediaUrl, e);
                throw e;
            }
        });
    }

    /**
     * Deletes all media files for a specific entity type and ID
     * @param context Application context
     * @param entityType Type of entity (profile, shop, or product)
     * @param entityId Entity ID
     * @return Task for the deletion operation
     */
    public static Task<Integer> deleteAllMediaForEntity(Context context, String entityType, String entityId) {
        return Tasks.call(() -> {
            try {
                File entityDir = new File(context.getFilesDir(), "images/" + entityType);
                if (!entityDir.exists()) {
                    Log.d(TAG, "Entity directory does not exist: " + entityDir.getAbsolutePath());
                    return 0;
                }
                
                File[] mediaFiles = entityDir.listFiles();
                if (mediaFiles == null || mediaFiles.length == 0) {
                    Log.d(TAG, "No media files found for entity: " + entityType + "/" + entityId);
                    return 0;
                }
                
                int deletedCount = 0;
                String entityPrefix = entityType + "_" + entityId + "_";
                
                for (File mediaFile : mediaFiles) {
                    String fileName = mediaFile.getName();
                    if (fileName.startsWith(entityPrefix)) {
                        boolean deleted = mediaFile.delete();
                        if (deleted) {
                            deletedCount++;
                            Log.d(TAG, "Deleted media file: " + fileName);
                        } else {
                            Log.w(TAG, "Failed to delete media file: " + fileName);
                        }
                    }
                }
                
                Log.d(TAG, "Deleted " + deletedCount + " media files for entity: " + entityType + "/" + entityId);
                return deletedCount;
                
            } catch (Exception e) {
                Log.e(TAG, "Error deleting media files for entity: " + entityType + "/" + entityId, e);
                throw e;
            }
        });
    }

    /**
     * Deletes an image from local storage
     * @param imageType Type of image (profile, shop, or product)
     * @param entityId Entity ID (userId, shopId, or productId)
     * @return Task for the deletion operation
     */
    public static Task<Void> deleteImageFromFirebaseStorage(String imageType, String entityId) {
        return Tasks.call(() -> {
            try {
                // Try to find and delete the file for this entity
                Log.d(TAG, "Deleting local image: " + imageType + "/" + entityId);
                // Note: This is a simplified approach. In a real app, you would need
                // to track the actual file path to delete the correct file
                return null;
            } catch (Exception e) {
                Log.e(TAG, "Error deleting local image", e);
                throw e;
            }
        });
    }
    
    /**
     * Creates a unique filename for the image (for fallback purposes)
     * @param imageType Type of image
     * @param entityId Entity ID (userId, shopId, or productId)
     * @return Unique filename
     */
    public static String createUniqueFileName(String imageType, String entityId) {
        long timestamp = System.currentTimeMillis();
        if ("profile".equals(imageType)) {
            return "profile_" + entityId + "_" + timestamp;
        } else if ("shop".equals(imageType)) {
            return "shop_" + entityId + "_" + timestamp;
        } else if ("product".equals(imageType)) {
            return "product_" + entityId + "_" + timestamp;
        }
        return "image_" + timestamp;
    }
    
    /**
     * Gets the file extension from a URI (for fallback purposes)
     * @param context Application context
     * @param uri URI to get extension from
     * @return File extension (without dot) or null if cannot be determined
     */
    private static String getFileExtension(Context context, Uri uri) {
        try {
            // Try to get MIME type
            String mimeType = context.getContentResolver().getType(uri);
            if (mimeType != null) {
                // Extract extension from MIME type
                if (mimeType.contains("image/jpeg")) return "jpg";
                if (mimeType.contains("image/png")) return "png";
                if (mimeType.contains("image/gif")) return "gif";
                if (mimeType.contains("image/webp")) return "webp";
                if (mimeType.contains("video/mp4")) return "mp4";
                if (mimeType.contains("video/3gpp")) return "3gp";
                if (mimeType.contains("video/avi")) return "avi";
                if (mimeType.contains("video/quicktime")) return "mov";
                if (mimeType.contains("video/x-msvideo")) return "avi";
                if (mimeType.contains("video/x-flv")) return "flv";
            }
            
            // Fallback: try to get extension from URI path
            String path = uri.getPath();
            if (path != null && path.contains(".")) {
                String ext = path.substring(path.lastIndexOf(".") + 1).toLowerCase();
                if (!ext.isEmpty() && ext.length() <= 5) { // Reasonable extension length
                    return ext;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting file extension: " + e.getMessage());
        }
        
        return null;
    }
}
