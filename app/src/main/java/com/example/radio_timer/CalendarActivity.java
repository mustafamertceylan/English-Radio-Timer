package com.example.radio_timer;

import android.os.Bundle;
import android.widget.ImageButton;

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

public class CalendarActivity extends AppCompatActivity {

    // 1. Değişkeni burada tanımla
    private MaterialCalendarView calendarView;

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
}