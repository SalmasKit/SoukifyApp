package com.example.soukify.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.soukify.data.entities.ProductType;
import java.util.List;

@Dao
public interface ProductTypeDao {
    @Insert
    void insert(ProductType productType);
    
    @Query("SELECT * FROM product_types")
    List<ProductType> getAllProductTypes();
    
    @Query("SELECT * FROM product_types WHERE type_id = :id")
    ProductType getProductTypeById(int id);
}
