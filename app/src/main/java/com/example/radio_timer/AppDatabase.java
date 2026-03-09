package com.example.radio_timer;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// entities listesinin ListeningLog ile uyumlu olduğundan emin ol
@Database(entities = {ListeningLog.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    // Volatile anahtar kelimesi, değişkenin değerinin tüm thread'lerde güncel kalmasını sağlar
    private static volatile AppDatabase instance;

    public abstract ListeningDao listeningDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            // "Double-checked locking" yöntemi ile thread safety sağlıyoruz
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "radio_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}