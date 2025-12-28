package com.example.soukify.data.remote;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.util.HashMap;
import java.util.Map;

public class CloudinaryConfig {
    private static Cloudinary cloudinary;
    
    public static Cloudinary getInstance() {
        if (cloudinary == null) {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);
            config.put("api_key", BuildConfig.CLOUDINARY_API_KEY);
            config.put("api_secret", BuildConfig.CLOUDINARY_API_SECRET);
            config.put("secure", true);
            
            cloudinary = new Cloudinary(config);
        }
        return cloudinary;
    }
}
