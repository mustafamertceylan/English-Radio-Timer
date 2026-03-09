package com.example.radio_timer;

import android.graphics.Color;
import androidx.core.graphics.Insets;
import android.graphics.drawable.Drawable;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;;


import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;


import java.util.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private TextView monthlyTotalText;

    // CalendarActivity.java

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Üst kısım (Şarj göstergesi) için boşluk ver
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            return insets;
        });

        calendarView = findViewById(R.id.calendarView);
        // 1. KRİTİK ADIM: TextView'ı mutlaka bağla (XML'deki ID ile aynı olmalı)
        monthlyTotalText = findViewById(R.id.monthlyTotalTime);

        View topBackLayout = findViewById(R.id.topBackLayout);
        topBackLayout.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 2. KRİTİK ADIM: Her şeyi tek seferde yükleyen metodu çağırıyoruz
        loadCalendarAndStats();
    }

    private void loadCalendarAndStats() {
        // 9 Mart 2026 itibariyle "2026-03" formatını alıyoruz
        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);

            // Veritabanı sorguları (Senin DAO'daki metodun kullanılıyor)
            List<ListeningLog> allLogs = db.listeningDao().getAllLogs();
            long monthlyTotalMillis = db.listeningDao().getTotalTimeForMonth(currentMonth);

            // Takvim günlerini işleme mantığı (Burayı zaten doğru yapmıştın)
            List<CalendarDay> reachedDays = new ArrayList<>();
            List<CalendarDay> missedDays = new ArrayList<>();

            for (ListeningLog log : allLogs) {
                String[] parts = log.date.split("-");
                CalendarDay day = CalendarDay.from(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2])
                );

                if (log.durationMillis >= 14400000) { // 4 saat hedefi
                    reachedDays.add(day);
                } else {
                    missedDays.add(day);
                }
            }

            runOnUiThread(() -> {
                // 3. KRİTİK ADIM: Veriyi burada ekrana basıyoruz
                if (monthlyTotalText != null) {
                    monthlyTotalText.setText(formatToShortTime(monthlyTotalMillis));
                }

                calendarView.removeDecorators();

                // Dolu daire dekoratörlerini ekliyoruz
                calendarView.addDecorator(new GoalReachedDecorator(
                        ContextCompat.getDrawable(this, R.drawable.circle_filled_green), reachedDays));
                calendarView.addDecorator(new GoalReachedDecorator(
                        ContextCompat.getDrawable(this, R.drawable.circle_filled_red), missedDays));
            });
        }).start();
    }

    private String formatToShortTime(long millis) {
        long hours = millis / 3600000;
        long minutes = (millis % 3600000) / 60000;
        long seconds = (millis % 60000) / 1000;
        return hours + "s " + minutes + "d " + seconds + "sn";
    }

    private static class GoalReachedDecorator implements DayViewDecorator {
        private final Drawable drawable;
        private final HashSet<CalendarDay> dates;

        public GoalReachedDecorator(Drawable drawable, Collection<CalendarDay> dates) {
            this.drawable = drawable;
            this.dates = new HashSet<>(dates);
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return dates.contains(day);
        }

        @Override
        public void decorate(DayViewFacade view) {
            // Nokta yerine arka planı (dolu daire) ayarlıyoruz
            view.setBackgroundDrawable(drawable);
            // Yazıyı beyaz yaparak kontrastı sağlıyoruz
            view.addSpan(new ForegroundColorSpan(Color.WHITE));
        }
    }
}