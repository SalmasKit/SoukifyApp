package com.example.soukify.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Query;
import com.example.soukify.data.entities.UserSetting;

@Dao
public interface UserSettingDao {

    @Insert
    void insert(UserSetting setting);

    @Update
    void update(UserSetting setting);

    @Query("SELECT * FROM user_settings WHERE user_id = :userId LIMIT 1")
    UserSetting getSettingsForUser(int userId);
}
