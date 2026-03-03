package com.example.radio_timer;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    // 1. Değişkeni burada tanımla
    private MaterialCalendarView calendarView;
    private TextView monthlyTotalText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        // 2. Değişkeni XML'deki ID ile eşleştir (Başlatma)
        calendarView = findViewById(R.id.calendarView);

        // 3. Şimdi decorator ekleyebilirsin
        setupCalendarDecorators();

        // Geri tuşu mantığı
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupCalendarDecorators() {
        // "Bugün"ü işaretleyen çerçeve
        calendarView.addDecorator(new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
                return day.equals(CalendarDay.today());
            }

            @Override
            public void decorate(DayViewFacade view) {
                // today_selector.xml dosyasını drawable olarak kullanıyoruz
                view.setBackgroundDrawable(ContextCompat.getDrawable(CalendarActivity.this, R.drawable.today_selector));
            }
        });
    }
    private void loadMonthlyStats(String currentMonth) {
        new Thread(() -> {
            // currentMonth örneği: "2026-03"
            long monthlyTotal = AppDatabase.getInstance(this).listeningDao().getTotalTimeForMonth(currentMonth);

            runOnUiThread(() -> {
                // "2s 56dk" formatına çevirip ekrana basıyoruz
                monthlyTotalText.setText(formatToShortTime(monthlyTotal));
            });
        }).start();
    }
    private String formatToShortTime(long millis) {
        long hours = millis / 3600000;
        long minutes = (millis % 3600000) / 60000;
        long seconds = (millis % 60000) / 1000; // Milisaniyeden saniyeyi hesaplar

        return hours + "s " + minutes + "d " + seconds + "sn";
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Sayfa her açıldığında verileri güncelle
        loadMonthlyData();
    }

    private void loadMonthlyData() {
        // 2026-03 formatında bugünün ay bilgisini alalım
        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            // Veritabanından o aya ait tüm sürelerin toplamını çek
            long totalMillis = db.listeningDao().getTotalTimeForMonth(currentMonth);

            runOnUiThread(() -> {
                // "2s 56dk" formatına çevirip ekrana bas
                monthlyTotalText.setText(formatToShortTime(totalMillis));
            });
        }).start();
    }
}