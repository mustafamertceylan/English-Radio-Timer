package com.example.radio_timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

public class MainActivity extends AppCompatActivity {

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
                String timeLeft = intent.getStringExtra("time_left");
                timerTextView.setText(timeLeft); // Ekrandaki yazıyı güncelle
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }



        timerTextView = findViewById(R.id.timerTextView);
        startStopButton = findViewById(R.id.startStopButton);



        startStopButton.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(this, RadioService.class);
            serviceIntent.setAction(RadioService.ACTION_TOGGLE); // Durdurma değil, değiştirme komutu gönder
            startForegroundService(serviceIntent);
        });
        registerReceiver(timerReceiver, new IntentFilter(RadioService.TIMER_UPDATE), Context.RECEIVER_EXPORTED);
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