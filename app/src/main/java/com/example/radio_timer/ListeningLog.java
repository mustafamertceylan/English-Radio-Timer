package com.example.radio_timer;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/** Günlük dinleme kaydı: tarih başına bir satır. */
@Entity(tableName = "listening_logs")
public class ListeningLog {
    @PrimaryKey
    @NonNull
    public String date; // "yyyy-MM-dd" (DateKeys)
    public long durationMillis;
    public boolean isGoalReached;

    /** Room tarafından kullanılır. */
    public ListeningLog() {}

    @Ignore
    public ListeningLog(@NonNull String date, long durationMillis, boolean isGoalReached) {
        this.date = date;
        this.durationMillis = durationMillis;
        this.isGoalReached = isGoalReached;
    }
}