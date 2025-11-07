package com.exemple.soukify.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.exemple.soukify.data.entities.Province;
import java.util.List;

@Dao
public interface ProvinceDao {
    @Insert
    void insert(Province province);

    @Query("SELECT * FROM provinces")
    List<Province> getAllProvinces();
}


