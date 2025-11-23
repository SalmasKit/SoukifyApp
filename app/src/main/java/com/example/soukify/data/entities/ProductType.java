package com.example.soukify.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "product_types")
public class ProductType {
    @PrimaryKey(autoGenerate = true)
    public int type_id;
    public String name;
}
