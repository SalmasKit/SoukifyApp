package com.example.soukify.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import androidx.room.Index;

@Entity(
    tableName = "cities",
    foreignKeys = @ForeignKey(
        entity = Region.class,
        parentColumns = "region_id",
        childColumns = "region_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("region_id")}
)
public class City {
    @PrimaryKey(autoGenerate = true)
    public int city_id;
    public int region_id;
    public String name;
}
