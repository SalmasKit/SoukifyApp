package com.exemple.soukify.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "provinces")
public class Province {
    @PrimaryKey(autoGenerate = true)
    public int province_id;
    public String name;

    public Province(String name) { this.name = name; }
}
