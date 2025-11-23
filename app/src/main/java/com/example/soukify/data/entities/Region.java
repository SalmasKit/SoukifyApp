package com.example.soukify.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "regions")
public class Region {
    @PrimaryKey(autoGenerate = true)
    public int region_id;
    public String name;
}
