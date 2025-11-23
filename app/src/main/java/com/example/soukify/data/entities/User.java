package com.example.soukify.data.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    public int user_id;

    @NonNull
    public String full_name = "";
    
    @Nullable
    public String email;
    
    @NonNull
    public String password_hash = "";
    
    @Nullable
    public String phone_number;
    
    @Nullable
    public String profile_image;
    
    @Nullable
    public String created_at;

    @Override
    public String toString() {
        return "User{" +
                "user_id=" + user_id +
                ", full_name='" + full_name + '\'' +
                ", email='" + (email != null ? email : "null") + '\'' +
                ", password_hash='[PROTECTED]'" +
                ", phone_number='" + (phone_number != null ? phone_number : "null") + '\'' +
                ", profile_image='" + (profile_image != null ? profile_image : "null") + '\'' +
                ", created_at='" + (created_at != null ? created_at : "null") + '\'' +
                '}';
    }
}

