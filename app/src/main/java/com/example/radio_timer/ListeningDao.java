package com.example.radio_timer;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ListeningDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(ListeningLog log);

    @Query("SELECT * FROM listening_logs WHERE date = :targetDate LIMIT 1")
    ListeningLog getLogByDate(String targetDate);

    // Belirli bir ayın toplam dinleme süresini milisaniye cinsinden getirir
    @Query("SELECT SUM(durationMillis) FROM listening_logs WHERE date LIKE :monthPattern || '%'")
    long getTotalTimeForMonth(String monthPattern); // monthPattern örneği: "2026-03"

    @Query("SELECT * FROM listening_logs ORDER BY date DESC")
    List<ListeningLog> getAllLogs();
}