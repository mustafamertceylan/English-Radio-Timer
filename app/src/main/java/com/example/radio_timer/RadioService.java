package com.example.radio_timer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;

public class RadioService extends Service {
    private ExoPlayer exoPlayer;
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 14400000; // 4 saat (Sabit tutulmalı, sadece tick ile güncellenmeli)
    private boolean isPlaying = false;

    private static final String CHANNEL_ID = "RadioChannel";
    public static final String ACTION_TOGGLE = "ACTION_TOGGLE";
    public static final String TIMER_UPDATE = "TIMER_UPDATE";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        exoPlayer = new ExoPlayer.Builder(this).build();

        // Radyoyu hazırla ama hemen başlatma
        String url = "https://stream.live.vc.bbcmedia.co.uk/bbc_world_service";
        exoPlayer.setMediaItem(MediaItem.fromUri(url));
        exoPlayer.prepare();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_TOGGLE.equals(intent.getAction())) {
            if (isPlaying) {
                pauseRadio();
            } else {
                playRadio();
            }
        } else {
            // Uygulama içinden ilk defa başlatıldığında
            playRadio();
        }
        return START_STICKY;
    }

    private void playRadio() {
        if (!isPlaying) {
            exoPlayer.play();
            isPlaying = true;
            startTimer();
            updateNotification();
        }
    }

    private void pauseRadio() {
        if (isPlaying) {
            exoPlayer.pause();
            isPlaying = false;
            if (countDownTimer != null) {
                countDownTimer.cancel(); // Önceki timer'ı durdur
            }
            updateNotification();
        }
    }

    private void startTimer() {
        // Eğer zaten bir timer varsa temizle (Sapıtmayı önleyen en kritik yer)
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Kalan süreden devam et
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished; // Kalan süreyi sürekli güncelle
                String timeFormatted = formatTime(millisUntilFinished);

                updateNotification(); // Bildirimi saniyede bir güncelle

                Intent intent = new Intent(TIMER_UPDATE);
                intent.putExtra("time_left", timeFormatted);
                sendBroadcast(intent);
            }

            @Override
            public void onFinish() {
                isPlaying = false;
                updateNotification();
            }
        }.start();
    }

    private void updateNotification() {
        Intent toggleIntent = new Intent(this, RadioService.class);
        toggleIntent.setAction(ACTION_TOGGLE);
        PendingIntent togglePendingIntent = PendingIntent.getService(this, 0, toggleIntent, PendingIntent.FLAG_IMMUTABLE);

        // Buton ikonu ve metni duruma göre değişsin
        String buttonText = isPlaying ? "Duraklat" : "Devam Et";
        int buttonIcon = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("BBC World Service")
                .setContentText("Kalan Süre: " + formatTime(timeLeftInMillis))
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(isPlaying) // Çalıyorsa bildirim silinemez olsun
                .addAction(buttonIcon, buttonText, togglePendingIntent)
                .setOnlyAlertOnce(true)
                .build();

        startForeground(1, notification);
    }

    private String formatTime(long millis) {
        int hours = (int) (millis / 1000) / 3600;
        int minutes = (int) ((millis / 1000) % 3600) / 60;
        int seconds = (int) (millis / 1000) % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID, "Radyo Servisi", NotificationManager.IMPORTANCE_LOW);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(serviceChannel);
    }

    @Override
    public void onDestroy() {
        if (countDownTimer != null) countDownTimer.cancel();
        if (exoPlayer != null) exoPlayer.release();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}