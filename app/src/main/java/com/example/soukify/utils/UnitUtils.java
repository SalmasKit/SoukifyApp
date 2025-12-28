package com.example.soukify.utils;

import android.content.Context;
import com.example.soukify.R;

public class UnitUtils {
    
    public static String getLocalizedUnit(Context context, String key) {
        if (key == null) return "";
        switch (key.toLowerCase()) {
            case "kg": return context.getString(R.string.unit_kg);
            case "g": return context.getString(R.string.unit_g);
            case "mg": return context.getString(R.string.unit_mg);
            case "cm": return context.getString(R.string.unit_cm);
            case "km": return context.getString(R.string.unit_km);
            case "m": return context.getString(R.string.unit_m);
            case "mm": return context.getString(R.string.unit_mm);
            default: return key;
        }
    }

    public static String getUnitKey(Context context, String localizedName) {
        if (localizedName == null) return "";
        
        if (localizedName.equals(context.getString(R.string.unit_kg))) return "kg";
        if (localizedName.equals(context.getString(R.string.unit_g))) return "g";
        if (localizedName.equals(context.getString(R.string.unit_mg))) return "mg";
        if (localizedName.equals(context.getString(R.string.unit_cm))) return "cm";
        if (localizedName.equals(context.getString(R.string.unit_km))) return "km";
        if (localizedName.equals(context.getString(R.string.unit_m))) return "m";
        if (localizedName.equals(context.getString(R.string.unit_mm))) return "mm";
        
        return localizedName.toLowerCase();
    }
}
