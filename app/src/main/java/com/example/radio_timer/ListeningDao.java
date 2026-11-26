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

    /** Belirli bir ayın toplam dinleme süresi (ms). monthPrefix: "yyyy-MM". Kayıt yoksa 0. */
    @Query("SELECT COALESCE(SUM(durationMillis), 0) FROM listening_logs WHERE date LIKE :monthPrefix || '%'")
    long getTotalTimeForMonth(String monthPrefix);

    /** Hedef bayrağını yalnız yükseltir (asla düşürmez). Etkilenen satır sayısını döndürür. */
    @Query("UPDATE listening_logs SET isGoalReached = 1 WHERE date = :dateKey AND durationMillis >= :goalMillis")
    int markGoalReachedIfAtLeast(String dateKey, long goalMillis);

    /** Ayın kayıtları, tarihe göre artan. monthPrefix: "yyyy-MM". */
    @Query("SELECT * FROM listening_logs WHERE date LIKE :monthPrefix || '%' ORDER BY date ASC")
    List<ListeningLog> getLogsForMonth(String monthPrefix);

    /** Hedefe ulaşılan günler, yeniden eskiye. */
    @Query("SELECT date FROM listening_logs WHERE isGoalReached = 1 ORDER BY date DESC")
    List<String> getReachedDates();
}
