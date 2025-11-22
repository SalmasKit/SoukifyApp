package com.exemple.soukify.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.exemple.soukify.data.entities.*;
import com.exemple.soukify.data.dao.*;
import com.exemple.soukify.data.dao.ProvinceDao;
import com.exemple.soukify.data.dao.UserDao;
import com.exemple.soukify.data.entities.Province;
import com.exemple.soukify.data.entities.User;

@Database(
        entities = {User.class, Province.class /* ajoute toutes tes entités */},
        version = 4,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao();
    public abstract ProvinceDao provinceDao();
    // ajoute les autres DAO ici

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
