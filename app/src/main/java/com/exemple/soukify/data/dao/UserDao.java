package com.exemple.soukify.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import com.exemple.soukify.data.entities.User;
import java.util.List;

@Dao
public interface UserDao {
    @Insert
    void insert(User user);

    @Update
    void update(User user);

    @Delete
    void delete(User user);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User findByEmail(String email);

    @Query("SELECT * FROM users ORDER BY created_at DESC")
    List<User> getAllUsers();
}
