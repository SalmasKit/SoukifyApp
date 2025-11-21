package com.exemple.soukify.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.exemple.soukify.data.entities.*;
import com.exemple.soukify.data.dao.*;

/**
 * Classe AppDatabase pour Room
 */
@Database(
        entities = {User.class, Province.class, Shop.class}, // toutes les entités
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    // DAO
    public abstract UserDao userDao();
    public abstract ProvinceDao provinceDao();
    public abstract ShopDao shopDao(); // DAO Shop ajouté

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "marketplace.db"
                            )
                            .fallbackToDestructiveMigration() // réinitialise la db si version change
                            .allowMainThreadQueries() // uniquement pour test / prototype
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
