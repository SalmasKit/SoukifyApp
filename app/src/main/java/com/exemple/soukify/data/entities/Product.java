package com.exemple.soukify.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import java.util.List;

@Entity(tableName = "products")
@TypeConverters(Product.ImageListConverter.class)
public class Product {

    @PrimaryKey(autoGenerate = true)
    public int product_id;

    // Infos produit
    public int shop_id;
    public String name;
    public String description;
    public double price;
    public String currency;
    public String created_at;

    // Type de produit
    public int type_id;
    public String type_name;

    // Images du produit (JSON)
    public List<String> images;

    // Convertisseur pour Room (liste <-> JSON)
    public static class ImageListConverter {
        @androidx.room.TypeConverter
        public static List<String> fromString(String value) {
            if (value == null || value.isEmpty()) return null;
            return java.util.Arrays.asList(value.split(","));
        }

        @androidx.room.TypeConverter
        public static String fromList(List<String> list) {
            if (list == null || list.isEmpty()) return "";
            return String.join(",", list);
        }
    }
}

