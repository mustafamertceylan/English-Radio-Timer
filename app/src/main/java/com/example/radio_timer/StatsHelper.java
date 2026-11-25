package com.example.radio_timer;

import android.content.Context;

import androidx.annotation.WorkerThread;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class StatsHelper {
    private StatsHelper() {}

    /** 0..100 sınırlı; goal<=0 ⇒ 0. */
    public static int progressPercent(long playedMillis, long goalMillis) {
        if (goalMillis <= 0 || playedMillis <= 0) return 0;
        long pct = (playedMillis * 100L) / goalMillis;
        return (int) Math.min(100L, pct);
    }

    /**
     * Ardışık hedef günleri. Bugün ulaşıldıysa bugünden geriye, ulaşılmadıysa dünden geriye sayar.
     * Saf fonksiyon.
     */
    public static int computeStreak(List<String> reachedDatesDesc, String todayKey) {
        if (reachedDatesDesc == null || reachedDatesDesc.isEmpty() || todayKey == null) return 0;
        Set<String> reached = new HashSet<>(reachedDatesDesc);
        String day = reached.contains(todayKey) ? todayKey : DateKeys.previousDay(todayKey);
        int streak = 0;
        while (reached.contains(day)) {
            streak++;
            day = DateKeys.previousDay(day);
        }
        return streak;
    }

    /** DAO + computeStreak. ANA THREAD'DE ÇAĞRILMAZ. */
    @WorkerThread
    public static int loadStreak(Context context) {
        List<String> dates = AppDatabase.getInstance(context.getApplicationContext())
                .listeningDao().getReachedDates();
        return computeStreak(dates, DateKeys.today());
    }

    /**
     * Güncel hedefle o günün bayrağını yeniden değerlendirir (yalnız false -> true).
     * ANA THREAD'DE ÇAĞRILMAZ; AppExecutors.diskIO() içinde çağrılır.
     */
    @WorkerThread
    public static void recomputeGoalFlag(Context context, String dateKey) {
        Context app = context.getApplicationContext();
        long goal = new PreferenceHelper(app).getDailyGoal();
        AppDatabase.getInstance(app).listeningDao().markGoalReachedIfAtLeast(dateKey, goal);
    }
}
