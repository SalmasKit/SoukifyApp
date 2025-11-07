package com.exemple.soukify.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_settings")
public class UserSetting {
    @PrimaryKey
    public int user_id; // FK vers User
    public String theme;
    public String language;
    public String currency;
    public boolean notifications;
}

