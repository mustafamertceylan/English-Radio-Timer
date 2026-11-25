package com.example.radio_timer;

import android.app.Application;

import com.jakewharton.threetenabp.AndroidThreeTen;

/** Uygulama sınıfı: ThreeTen saat dilimi verisini başlatır (material-calendarview için). */
public class RadioTimerApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AndroidThreeTen.init(this);
    }
}
