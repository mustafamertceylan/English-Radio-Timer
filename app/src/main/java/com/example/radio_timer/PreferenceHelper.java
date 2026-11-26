package com.example.radio_timer;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

public class PreferenceHelper {
    private static final String PREF_NAME = "radio_prefs";
    private static final String KEY_DAILY_GOAL = "daily_goal";
    private static final String KEY_LAST_STATION_URL = "last_station_url";
    private static final String KEY_LAST_STATION_NAME = "last_station_name";
    private static final String KEY_LAST_CELEBRATED_DATE = "last_celebrated_date";
    private static final String KEY_NOTIF_PERMISSION_ASKED = "notif_permission_asked";
    private static final long DEFAULT_GOAL = 14_400_000L; // 4 saat (varsayılan)

    public static final long MIN_GOAL_MILLIS = 60_000L;        // 1 dk
    public static final long MAX_GOAL_MILLIS = 86_400_000L;    // 24 sa

    private final SharedPreferences prefs;

    public PreferenceHelper(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static long clampGoal(long millis) {
        return Math.max(MIN_GOAL_MILLIS, Math.min(MAX_GOAL_MILLIS, millis));
    }

    /** Hedefi MIN..MAX aralığına sıkıştırarak kaydeder (ms). */
    public void setDailyGoal(long millis) {
        prefs.edit().putLong(KEY_DAILY_GOAL, clampGoal(millis)).apply();
    }

    /** Hedef (ms); her zaman MIN..MAX aralığında. */
    public long getDailyGoal() {
        return clampGoal(prefs.getLong(KEY_DAILY_GOAL, DEFAULT_GOAL));
    }

    public float getDailyGoalInHours() {
        return getDailyGoal() / (1000f * 60 * 60);
    }

    /** setDailyGoal üzerinden clamp edilir. */
    public void setDailyGoalInHours(float hours) {
        if (Float.isNaN(hours)) return;
        setDailyGoal(Math.round((double) hours * 60 * 60 * 1000));
    }

    @Nullable
    public String getLastStationUrl() {
        return prefs.getString(KEY_LAST_STATION_URL, null);
    }

    @Nullable
    public String getLastStationName() {
        return prefs.getString(KEY_LAST_STATION_NAME, null);
    }

    public void setLastStation(String url, String name) {
        prefs.edit()
                .putString(KEY_LAST_STATION_URL, url)
                .putString(KEY_LAST_STATION_NAME, name)
                .apply();
    }

    /** U4: aynı gün ikinci kutlamayı engellemek için. */
    @Nullable
    public String getLastCelebratedDate() {
        return prefs.getString(KEY_LAST_CELEBRATED_DATE, null);
    }

    public void setLastCelebratedDate(String dateKey) {
        prefs.edit().putString(KEY_LAST_CELEBRATED_DATE, dateKey).apply();
    }

    /** T10: bildirim izni daha önce soruldu mu (yalnız bir kez sorulur). */
    public boolean wasNotificationPermissionAsked() {
        return prefs.getBoolean(KEY_NOTIF_PERMISSION_ASKED, false);
    }

    public void setNotificationPermissionAsked() {
        prefs.edit().putBoolean(KEY_NOTIF_PERMISSION_ASKED, true).apply();
    }
}
