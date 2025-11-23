package com.example.soukify.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import androidx.room.Index;

@Entity(
    tableName = "product_images",
    foreignKeys = @ForeignKey(
        entity = Product.class,
        parentColumns = "product_id",
        childColumns = "product_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("product_id")}
)
public class ProductImage {
    @PrimaryKey(autoGenerate = true)
    public int image_id;
    public int product_id;
    public String image_url;
}
