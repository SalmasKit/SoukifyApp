package com.example.soukify.data;

import android.content.Context;
import android.util.Log;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.example.soukify.data.entities.*;
import com.example.soukify.data.dao.*;

@Database(
        entities = {
            User.class,
            Region.class,
            City.class,
            Shop.class,
            ProductType.class,
            Product.class,
            ProductImage.class,
            Favorite.class,
            UserSetting.class
        },
        version = 7,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static final String TAG = "AppDatabase";
    private static volatile AppDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract RegionDao regionDao();
    public abstract CityDao cityDao();
    public abstract ShopDao shopDao();
    public abstract ProductTypeDao productTypeDao();
    public abstract ProductDao productDao();
    public abstract ProductImageDao productImageDao();
    public abstract FavoriteDao favoriteDao();
    public abstract UserSettingDao userSettingDao();

    // Migration from version 5 to 6 - Update users table schema
    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // 1. Create a new table with the correct schema
            database.execSQL("CREATE TABLE users_new (" +
                    "user_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "full_name TEXT NOT NULL, " +
                    "email TEXT, " +
                    "password_hash TEXT NOT NULL, " +
                    "phone_number TEXT, " +
                    "profile_image TEXT, " +
                    "created_at INTEGER NOT NULL, " +
                    "updated_at INTEGER)");
            
            // 2. Copy data from old table to new table
            database.execSQL("INSERT INTO users_new (user_id, full_name, email, password_hash, phone_number, profile_image, created_at) " +
                           "SELECT id, name, email, password, phone, image, created_at FROM users");
            
            // 3. Remove the old table
            database.execSQL("DROP TABLE users");
            
            // 4. Rename the new table
            database.execSQL("ALTER TABLE users_new RENAME TO users");
            
            Log.d(TAG, "Migration from 5 to 6 completed");
        }
    };

    // Migration from version 6 to 7 - Add new fields to shops table
    private static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            try {
                // Add new columns to shops table if they don't exist
                database.execSQL("ALTER TABLE shops ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0");
                database.execSQL("ALTER TABLE shops ADD COLUMN liked INTEGER NOT NULL DEFAULT 0");
                database.execSQL("ALTER TABLE shops ADD COLUMN likes_count INTEGER NOT NULL DEFAULT 0");
                database.execSQL("ALTER TABLE shops ADD COLUMN search_count INTEGER NOT NULL DEFAULT 0");
                Log.d(TAG, "Migration from 6 to 7 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during migration 6 to 7", e);
                throw e;
            }
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "soukify-db"
                    )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()  // Only for development, remove in production
                    .addCallback(new RoomDatabase.Callback() {
                        @Override
                        public void onCreate(@androidx.annotation.NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            Log.d(TAG, "Database created");
                        }

                        @Override
                        public void onOpen(@androidx.annotation.NonNull SupportSQLiteDatabase db) {
                            super.onOpen(db);
                            Log.d(TAG, "Database opened");
                        }
                    })
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}