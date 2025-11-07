package com.exemple.soukify.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    public int user_id;

    public String full_name;
    public String email;
    public String password_hash;
    public String phone_number;
    public String profile_image;
    public String created_at;
}

