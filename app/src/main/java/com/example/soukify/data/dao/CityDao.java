package com.example.soukify.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.soukify.data.entities.City;
import java.util.List;

@Dao
public interface CityDao {
    @Insert
    void insert(City city);
    
    @Query("SELECT * FROM cities WHERE region_id = :regionId")
    List<City> getCitiesByRegion(int regionId);
    
    @Query("SELECT * FROM cities WHERE city_id = :id")
    City getCityById(int id);
}
