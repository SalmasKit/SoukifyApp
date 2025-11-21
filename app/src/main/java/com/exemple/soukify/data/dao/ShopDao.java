package com.exemple.soukify.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;

import com.exemple.soukify.data.entities.Shop;

import java.util.List;

@Dao
public interface ShopDao {

    // Ajouter une boutique
    @Insert
    void insert(Shop shop);

    // Mettre à jour une boutique
    @Update
    void update(Shop shop);

    // Supprimer une boutique
    @Delete
    void delete(Shop shop);

    // Récupérer toutes les boutiques
    @Query("SELECT * FROM shops ORDER BY createdAt DESC")
    List<Shop> getAllShops();

    // Récupérer une boutique par ID
    @Query("SELECT * FROM shops WHERE shopId = :id LIMIT 1")
    Shop getShopById(int id);

    // Récupérer toutes les boutiques d’un utilisateur
    @Query("SELECT * FROM shops WHERE userId = :userId ORDER BY createdAt DESC")
    List<Shop> getShopsByUser(int userId);

    // Récupérer toutes les boutiques dans une province
    @Query("SELECT * FROM shops WHERE provinceId = :provinceId ORDER BY createdAt DESC")
    List<Shop> getShopsByProvince(int provinceId);

    // Récupérer toutes les boutiques dans une ville
    @Query("SELECT * FROM shops WHERE cityId = :cityId ORDER BY createdAt DESC")
    List<Shop> getShopsByCity(int cityId);

    // Récupérer toutes les boutiques favorites
    @Query("SELECT * FROM shops WHERE favorite = 1 ORDER BY createdAt DESC")
    List<Shop> getFavoriteShops();
}
