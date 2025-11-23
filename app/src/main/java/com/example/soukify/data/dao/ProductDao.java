package com.example.soukify.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;
import com.example.soukify.data.entities.Product;
import java.util.List;

@Dao
public interface ProductDao {

    // Ajouter un produit
    @Insert
    void insert(Product product);

    // Mettre à jour un produit
    @Update
    void update(Product product);

    // Supprimer un produit
    @Delete
    void delete(Product product);

    // Récupérer tous les produits
    @Query("SELECT * FROM products ORDER BY created_at DESC")
    List<Product> getAllProducts();

    // Récupérer un produit par ID
    @Query("SELECT * FROM products WHERE product_id = :id LIMIT 1")
    Product getProductById(int id);

    // Récupérer les produits d’un shop
    @Query("SELECT * FROM products WHERE shop_id = :shopId ORDER BY created_at DESC")
    List<Product> getProductsByShop(int shopId);

    // Récupérer les produits par type
    @Query("SELECT * FROM products WHERE type_id = :typeId ORDER BY created_at DESC")
    List<Product> getProductsByType(int typeId);
}