package com.example.radio_timer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.PlaybackException;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@androidx.media3.common.util.UnstableApi
public class RadioService extends Service {
    private static final String TAG = "RadioService";
    private ExoPlayer exoPlayer;
    private CountDownTimer countDownTimer;
    private long timePlayedToday = 0;
    private boolean isPlaying = false;
    private boolean isPlayerReady = false;
    private boolean pendingPlay = false;
    private static final String CHANNEL_ID = "RadioChannel";
    public static final String ACTION_TOGGLE = "ACTION_TOGGLE";
    public static final String TIMER_UPDATE = "TIMER_UPDATE";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        initializePlayer();
        loadInitialProgress();
        // Servis başlar başlamaz ön plan bildirimi gönder
        Intent toggleIntent = new Intent(this, RadioService.class);
        toggleIntent.setAction(ACTION_TOGGLE);
        PendingIntent togglePendingIntent = PendingIntent.getService(this, 0, toggleIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("BBC World Service")
                .setContentText("Radyo hazırlanıyor...")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_media_play, "Başlat", togglePendingIntent)
                .build();

        startForeground(1, notification);
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    isPlayerReady = true;
                    if (pendingPlay) {
                        playRadio();
                        pendingPlay = false;
                    }
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                // Hata durumunda bildirimi güncelle veya kullanıcıya göster
                Log.e("RadioService", "Player hatası: " + error.getMessage());
                // İsterseniz bir bildirimle kullanıcıyı bilgilendirebilirsiniz
            }
        });
    }

    private void initializePlayer() {
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(30000, 60000, 1500, 3000)
                .build();

        exoPlayer = new ExoPlayer.Builder(this)
                .setLoadControl(loadControl)
                .build();

        String url = "https://stream.live.vc.bbcmedia.co.uk/bbc_world_service";
        exoPlayer.setMediaItem(MediaItem.fromUri(url));

        // Hata dinleyicisi
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    Log.d(TAG, "Player hazır");
                    isPlayerReady = true;
                    if (pendingPlay) {
                        playRadio();
                        pendingPlay = false;
                    }
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Log.e(TAG, "Player hatası: " + error.getMessage());
                // Hata durumunda kullanıcıyı bilgilendirebilirsiniz
            }
        });

        exoPlayer.prepare();
    }

    private void loadInitialProgress() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            ListeningLog log = db.listeningDao().getLogByDate(today);
            if (log != null) {
                timePlayedToday = log.durationMillis;
            }
        }).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_TOGGLE.equals(intent.getAction())) {
            if (isPlaying) {
                pauseRadio();
            } else {
                playRadio();
            }
        }
        return START_STICKY;
    }

    private void playRadio() {
        if (!isPlaying) {
            if (!isPlayerReady) {
                pendingPlay = true;
                return;
            }
            exoPlayer.play();
            isPlaying = true;
            startTimer();
            updateNotification();
            sendStateToActivity();
        }
    }

    private void pauseRadio() {
        if (!isPlaying) return;
        exoPlayer.pause();
        isPlaying = false;
        if (countDownTimer != null) countDownTimer.cancel();
        saveProgressToDatabase();
        updateNotification();
        sendStateToActivity();
    }

    private void startTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new CountDownTimer(Long.MAX_VALUE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (exoPlayer != null && exoPlayer.getPlaybackState() == Player.STATE_READY && exoPlayer.getPlayWhenReady()) {
                    timePlayedToday += 1000;
                    if ((timePlayedToday / 1000) % 60 == 0) {
                        saveProgressToDatabase();
                    }
                }
                updateNotification();
                sendStateToActivity();
            }

            @Override
            public void onFinish() {
                // Sonsuz timer olduğu için asla gelmez
            }
        }.start();
    }

    private void updateNotification() {
        Intent toggleIntent = new Intent(this, RadioService.class);
        toggleIntent.setAction(ACTION_TOGGLE);
        PendingIntent togglePendingIntent = PendingIntent.getService(this, 0, toggleIntent, PendingIntent.FLAG_IMMUTABLE);

        long dailyGoal = new PreferenceHelper(this).getDailyGoal();
        long remainingTime = Math.max(0, dailyGoal - timePlayedToday);

        String buttonText = isPlaying ? "Duraklat" : "Devam Et";
        int buttonIcon = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("BBC World Service")
                .setContentText("Kalan Süre: " + formatTime(remainingTime))
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(isPlaying)
                .addAction(buttonIcon, buttonText, togglePendingIntent)
                .setOnlyAlertOnce(true)
                .build();

        startForeground(1, notification);
    }

    private void sendStateToActivity() {
        Intent intent = new Intent(TIMER_UPDATE);
        intent.putExtra("millis_played", timePlayedToday);
        intent.putExtra("is_playing", isPlaying);
        sendBroadcast(intent);
    }

    private void saveProgressToDatabase() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            long dailyGoal = new PreferenceHelper(getApplicationContext()).getDailyGoal();
            boolean goalReached = timePlayedToday >= dailyGoal;
            db.listeningDao().insertOrUpdate(new ListeningLog(today, timePlayedToday, goalReached));
        }).start();
    }

    private String formatTime(long millis) {
        int hours = (int) (millis / 1000) / 3600;
        int minutes = (int) ((millis / 1000) % 3600) / 60;
        int seconds = (int) (millis / 1000) % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Radyo Servisi", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        if (countDownTimer != null) countDownTimer.cancel();
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
        saveProgressToDatabase();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}