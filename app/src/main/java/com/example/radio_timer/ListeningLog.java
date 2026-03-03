package com.example.radio_timer;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "listening_logs")
public class ListeningLog {
    @PrimaryKey
    @NonNull
    public String date; // Örn: "2026-03-03"
    public long durationMillis;
    public boolean isGoalReached;

    // 1. Oda (Room) için boş constructor (Gerekli)
    public ListeningLog() {}

    // 2. Senin RadioService içinde kullandığın constructor (Hatanın çözümü)
    public ListeningLog(@NonNull String date, long durationMillis, boolean isGoalReached) {
        this.date = date;
        this.durationMillis = durationMillis;
        this.isGoalReached = isGoalReached;
    }
}