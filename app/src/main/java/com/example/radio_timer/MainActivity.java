package com.example.radio_timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.content.IntentFilter;
import android.os.Bundle;

import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;


public class MainActivity extends AppCompatActivity {

    private ImageView playIcon;      // Üstteki yeşil kartın ikonu
    private ImageView playIconList;  // Alttaki istasyon listesinin ikonu
    private TextView totalDailyTime, bbcTimer;
    private ProgressBar goalProgressBar;
    private CardView dateCard;


    // Hedef süre: 4 saat (milisaniye cinsinden: 4 * 60 * 60 * 1000)
    private long timeLeftInMillis = 14400000;
    private boolean isTimerRunning = false;

    // BBC World Service Canlı Yayın Linki
    private final String RADIO_URL = "http://stream.live.vc.bbcmedia.co.uk/bbc_world_service";
    private BroadcastReceiver timerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(RadioService.TIMER_UPDATE)) {
                long millisPlayed = intent.getLongExtra("millis_played", 0);
                boolean isPlaying = intent.getBooleanExtra("is_playing", false);

                // Metinleri ve Progress'i güncelle
                String formattedTime = formatToShortTime(millisPlayed);
                totalDailyTime.setText(formattedTime + " / 4 saat");
                bbcTimer.setText(formattedTime);
                goalProgressBar.setProgress((int) ((millisPlayed * 100) / 14400000));

                // İKONLARI GÜNCELLE (Burayı ekledik)
                int iconRes = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
                playIcon.setImageResource(iconRes);
                playIconList.setImageResource(iconRes);
            }
        }
    };

    // Yardımcı metod: Milisaniyeyi "X s Y dk" formatına çevirir
    private String formatToShortTime(long millis) {
        long hours = millis / 3600000;
        long minutes = (millis % 3600000) / 60000;
        long seconds = (millis % 60000) / 1000; // Milisaniyeden saniyeyi hesaplar

        return hours + "s " + minutes + "d " + seconds + "sn";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. UI Elemanlarını Bağlama
        playIcon = findViewById(R.id.playIcon);
        playIconList = findViewById(R.id.playIconList);
        totalDailyTime = findViewById(R.id.totalDailyTime);
        bbcTimer = findViewById(R.id.bbcTimer);
        goalProgressBar = findViewById(R.id.goalProgressBar);
        dateCard = findViewById(R.id.dateCard);
        CardView bbcCard = findViewById(R.id.bbcCard);
        CardView bbcListItem = findViewById(R.id.bbcStationCard); // XML ID kontrolü

        // 2. Takvim Geçişi
        dateCard.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CalendarActivity.class);
            startActivity(intent);
        });

        // 3. Ortak Radyo Kontrol Görevi (Listener)
        View.OnClickListener toggleAction = v -> {
            Intent serviceIntent = new Intent(this, RadioService.class);
            serviceIntent.setAction("ACTION_TOGGLE");

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        };

        // 4. KRİTİK EKSİK: Görevi butonlara atıyoruz
        bbcCard.setOnClickListener(toggleAction);
        if (bbcListItem != null) {
            bbcListItem.setOnClickListener(toggleAction);
        }

        // 5. KRİTİK EKSİK: Veritabanından bugünün süresini yükle
        // Bu metodun MainActivity içinde tanımlı olduğundan emin ol!
        loadTodayProgress();
    }




    @Override
    protected void onResume() {
        super.onResume();
        // Servisten gelen güncellemeleri dinlemeye başla
        IntentFilter filter = new IntentFilter(RadioService.TIMER_UPDATE);
        registerReceiver(timerReceiver, filter, Context.RECEIVER_EXPORTED);

        // Uygulama açıldığında veritabanındaki eski veriyi yükle (Sıfırlanma çözümü)
        loadTodayProgress();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Uygulama arka plana geçince dinlemeyi bırak (Performans için)
        unregisterReceiver(timerReceiver);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(timerReceiver);
        super.onDestroy();
    }
    private void loadTodayProgress() {
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            ListeningLog log = db.listeningDao().getLogByDate(today);

            if (log != null) {
                runOnUiThread(() -> {
                    // Veritabanındaki süreyi arayüze yansıt
                    String formatted = formatToShortTime(log.durationMillis);
                    totalDailyTime.setText(formatted + " / 4 saat");
                    bbcTimer.setText(formatted);
                    goalProgressBar.setProgress((int) ((log.durationMillis * 100) / 14400000));
                });
            }
        }).start();
    }
}