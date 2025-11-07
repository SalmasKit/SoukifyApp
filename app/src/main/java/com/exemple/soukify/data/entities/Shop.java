package com.exemple.soukify.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "shops")
public class Shop {
    @PrimaryKey(autoGenerate = true)
    public int shop_id;

    public int user_id; // FK vers User
    public String name;
    public String description;
    public int province_id; // FK vers Province
    public int city_id;     // FK vers City
    public String phone_number;
    public String email;
    public String image_url;
    public String created_at;
}

