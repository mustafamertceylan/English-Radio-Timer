package com.example.radio_timer;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "listening_logs")
public class ListeningLog {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String date; // Örn: "2026-03-01"
    public long durationMillis; // Kaç milisaniye dinlendi
    public boolean isGoalReached; // 4 saati tamamladı mı?
}