package com.exemple.soukify.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.exemple.soukify.data.entities.*;
import com.exemple.soukify.data.dao.*;

@Database(
        entities = {User.class, Province.class, Product.class, Shop.class, Favorite.class, UserSetting.class},
        version = 4,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao();
    public abstract ProvinceDao provinceDao();
    public abstract ProductDao productDao();
    public abstract ShopDao shopDao(); // <-- ajouté

    public abstract FavoriteDao favoriteDao();
    public abstract UserSettingDao userSettingDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "soukify.db"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
