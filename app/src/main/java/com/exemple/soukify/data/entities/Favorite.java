package com.exemple.soukify.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorites", primaryKeys = {"user_id", "product_id"})
public class Favorite {
    public int user_id;   // FK vers User
    public int product_id; // FK vers Product
    public String created_at;
}
