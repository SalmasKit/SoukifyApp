package com.example.soukify.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.soukify.data.entities.ProductImage;
import java.util.List;

@Dao
public interface ProductImageDao {
    @Insert
    void insert(ProductImage productImage);
    
    @Insert
    void insertAll(List<ProductImage> productImages);
    
    @Delete
    void delete(ProductImage productImage);
    
    @Query("SELECT * FROM product_images WHERE product_id = :productId")
    List<ProductImage> getImagesByProductId(int productId);
    
    @Query("DELETE FROM product_images WHERE product_id = :productId")
    void deleteByProductId(int productId);
}
