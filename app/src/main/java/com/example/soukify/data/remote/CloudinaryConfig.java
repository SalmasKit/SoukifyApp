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
            config.put("cloud_name", "duewphf56");
            config.put("api_key", "757982559184685");
            config.put("api_secret", "9cr_yctKQOeJbQ5ceOJ8UfnvA7A");
            config.put("secure", true);
            
            cloudinary = new Cloudinary(config);
        }
        return cloudinary;
    }
}
