package com.example.radio_timer;


import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceHelper {
    private static final String PREF_NAME = "radio_prefs";
    private static final String KEY_DAILY_GOAL = "daily_goal";
    private static final long DEFAULT_GOAL = 14400000; // 4 saat (varsayılan)

    private SharedPreferences prefs;

    public PreferenceHelper(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Hedef süreyi kaydet (milisaniye cinsinden)
    public void setDailyGoal(long millis) {
        prefs.edit().putLong(KEY_DAILY_GOAL, millis).apply();
    }

    // Hedef süreyi oku (milisaniye cinsinden)
    public long getDailyGoal() {
        return prefs.getLong(KEY_DAILY_GOAL, DEFAULT_GOAL);
    }

    // Kullanıcı dostu formatla hedefi saat olarak döndür (örneğin 4.0)
    public float getDailyGoalInHours() {
        return getDailyGoal() / (1000f * 60 * 60);
    }

    // Hedefi saat olarak ayarla (örneğin 2.5 saat = 9000000 ms)
    public void setDailyGoalInHours(float hours) {
        long millis = (long) (hours * 60 * 60 * 1000);
        setDailyGoal(millis);
    }
}