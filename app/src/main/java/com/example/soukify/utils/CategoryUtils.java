package com.example.soukify.utils;

import android.content.Context;
import com.example.soukify.R;
import java.util.HashMap;
import java.util.Map;

public class CategoryUtils {

    private static final Map<String, Integer> CATEGORY_MAP = new HashMap<>();

    static {
        CATEGORY_MAP.put("textile_tapestry", R.string.cat_textile_tapestry);
        CATEGORY_MAP.put("gourmet_foods", R.string.cat_gourmet_foods);
        CATEGORY_MAP.put("pottery_ceramics", R.string.cat_pottery_ceramics);
        CATEGORY_MAP.put("traditional_wear", R.string.cat_traditional_wear);
        CATEGORY_MAP.put("leather_crafts", R.string.cat_leather_crafts);
        CATEGORY_MAP.put("wellness_products", R.string.cat_wellness_products);
        CATEGORY_MAP.put("jewelry_accessories", R.string.cat_jewelry_accessories);
        CATEGORY_MAP.put("metal_brass", R.string.cat_metal_brass);
        CATEGORY_MAP.put("painting_calligraphy", R.string.cat_painting_calligraphy);
        CATEGORY_MAP.put("woodwork", R.string.cat_woodwork);
    }

    public static String getLocalizedCategory(Context context, String categoryKey) {
        if (categoryKey == null || categoryKey.isEmpty()) {
            return context.getString(R.string.category);
        }

        Integer resId = CATEGORY_MAP.get(categoryKey);
        if (resId != null) {
            return context.getString(resId);
        }

        // Return the key itself if not found in map (fallback for legacy data)
        return categoryKey;
    }

    public static String getCategoryKey(Context context, String localizedName) {
        for (Map.Entry<String, Integer> entry : CATEGORY_MAP.entrySet()) {
            if (context.getString(entry.getValue()).equals(localizedName)) {
                return entry.getKey();
            }
        }
        return localizedName; // Fallback to raw name if no match
    }

    public static String[] getCategoryKeys() {
        return CATEGORY_MAP.keySet().toArray(new String[0]);
    }
}
