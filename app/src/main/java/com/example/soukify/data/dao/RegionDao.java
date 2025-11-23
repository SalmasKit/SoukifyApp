package com.example.soukify.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.soukify.data.entities.Region;
import java.util.List;

@Dao
public interface RegionDao {
    @Insert
    void insert(Region region);
    
    @Query("SELECT * FROM regions")
    List<Region> getAllRegions();
    
    @Query("SELECT * FROM regions WHERE region_id = :id")
    Region getRegionById(int id);
}
