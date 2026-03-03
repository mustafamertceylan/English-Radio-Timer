package com.example.radio_timer;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ListeningDao {
    @Insert
    void insert(ListeningLog log);

    @Query("SELECT * FROM listening_logs")
    List<ListeningLog> getAllLogs();

    @Query("SELECT * FROM listening_logs WHERE date = :targetDate LIMIT 1")
    ListeningLog getLogByDate(String targetDate);
}