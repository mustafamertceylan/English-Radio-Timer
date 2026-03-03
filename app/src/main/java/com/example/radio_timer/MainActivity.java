package com.example.radio_timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

public class MainActivity extends AppCompatActivity {

    private ImageView playIcon;      // Üstteki yeşil kartın ikonu
    private ImageView playIconList;  // Alttaki istasyon listesinin ikonu
    private TextView totalDailyTime, bbcTimer;
    private ProgressBar goalProgressBar;
    private CardView dateCard;
    private ExoPlayer player;
    private TextView timerTextView;
    private Button startStopButton;
    private CountDownTimer countDownTimer;

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

                // Yeni format: "0s 45d 12sn"
                String formattedTime = formatToShortTime(millisPlayed);

                // Arayüz bileşenlerini güncelle
                totalDailyTime.setText(formattedTime + " / 4 saat");
                bbcTimer.setText(formattedTime);

                // İlerleme çubuğunu (ProgressBar) güncelle (Hedef: 4 saat = 14.400.000 ms)
                int progress = (int) ((millisPlayed * 100) / 14400000);
                goalProgressBar.setProgress(progress);
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
        playIcon = findViewById(R.id.playIcon);
        playIconList = findViewById(R.id.playIconList); // XML'de bu ID'yi verdiğinden emin ol

        // Yeni ID'leri burada bağlıyoruz
        totalDailyTime = findViewById(R.id.totalDailyTime);
        bbcTimer = findViewById(R.id.bbcTimer);
        goalProgressBar = findViewById(R.id.goalProgressBar);
        dateCard = findViewById(R.id.dateCard);

        // Takvime geçiş butonu (CardView olarak)
        dateCard.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CalendarActivity.class);
            startActivity(intent);
        });
        // MainActivity.java içindeki onCreate metoduna ekle:
        CardView bbcCard = findViewById(R.id.bbcCard);

        bbcCard.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(this, RadioService.class);
            // Servise ne yapacağını söyleyen "Action"
            serviceIntent.setAction("ACTION_TOGGLE");

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        });
    }
    private void initializePlayer() {
        player = new ExoPlayer.Builder(this).build();
        MediaItem mediaItem = MediaItem.fromUri(RADIO_URL);
        player.setMediaItem(mediaItem);
        player.prepare();

        // İşte senin istediğin "Otomatik Başlatma" mantığı burada:
        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) {
                    startTimer(); // Radyo gerçekten çalmaya başlayınca sayacı başlat
                } else {
                    pauseTimer(); // Radyo durunca sayacı durdur
                }
            }
        });
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                timerTextView.setText("00:00:00 - Hedef Tamamlandı!");
            }
        }.start();
        isTimerRunning = true;
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
    }

    private void updateCountDownText() {
        int hours = (int) (timeLeftInMillis / 1000) / 3600;
        int minutes = (int) ((timeLeftInMillis / 1000) % 3600) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;

        String timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        timerTextView.setText(timeFormatted);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(timerReceiver);
        super.onDestroy();
    }
}