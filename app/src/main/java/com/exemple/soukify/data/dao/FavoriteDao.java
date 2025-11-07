package com.exemple.soukify.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Delete;
import androidx.room.Query;
import com.exemple.soukify.data.entities.Favorite;
import java.util.List;

@Dao
public interface FavoriteDao {

    @Insert
    void insert(Favorite favorite);

    @Delete
    void delete(Favorite favorite);

    @Query("SELECT * FROM favorites WHERE user_id = :userId")
    List<Favorite> getFavoritesForUser(int userId);

    @Query("SELECT * FROM favorites WHERE user_id = :userId AND product_id = :productId LIMIT 1")
    Favorite getFavorite(int userId, int productId);
}
