package com.example.radio_timer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RadioService extends Service {
    private ExoPlayer exoPlayer;
    private CountDownTimer countDownTimer;
    private long timePlayedToday = 0; // Toplam dinlenen süre (Milisaniye)
    private boolean isPlaying = false;
    private boolean isPlayerReady = false;
    private boolean pendingPlay = false;

    private static final String CHANNEL_ID = "RadioChannel";
    public static final String ACTION_TOGGLE = "ACTION_TOGGLE";
    public static final String ACTION_PLAY_STATION = "ACTION_PLAY_STATION";
    public static final String TIMER_UPDATE = "TIMER_UPDATE";

    private String currentStationUrl = "https://stream.live.vc.bbcmedia.co.uk/bbc_world_service";
    private String currentStationName = "BBC World Service";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        setupPlayer();
        loadInitialProgress();
    }

    private void setupPlayer() {
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(30000, 60000, 1500, 3000)
                .build();

        exoPlayer = new ExoPlayer.Builder(this)
                .setLoadControl(loadControl)
                .build();

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
        });

        prepareMedia(currentStationUrl);
    }

    private void prepareMedia(String url) {
        // User-Agent ekleyerek DataSource Factory oluştur
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36";
        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent)
                .setAllowCrossProtocolRedirects(true);

        MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(Uri.parse(url)));

        exoPlayer.setMediaSource(mediaSource);
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
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_PLAY_STATION.equals(action)) {
                String url = intent.getStringExtra("station_url");
                String name = intent.getStringExtra("station_name");
                if (url != null && name != null) {
                    if (url.equals(currentStationUrl)) {
                        // Aynı istasyon: toggle yap
                        if (isPlaying) pauseRadio(); else playRadio();
                    } else {
                        // Yeni istasyon
                        currentStationUrl = url;
                        currentStationName = name;
                        if (isPlaying) {
                            exoPlayer.stop();
                            isPlaying = false;
                        }
                        prepareMedia(url);
                        playRadio();
                    }
                }
            } else if (ACTION_TOGGLE.equals(action)) {
                if (isPlaying) pauseRadio(); else playRadio();
            }
        }
        return START_STICKY;
    }

    private void playRadio() {
        if (!isPlaying) {
            if (!isPlayerReady) {
                pendingPlay = true;
                Log.d("RadioService", "Player hazır değil, bekleniyor...");
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
        if (isPlaying) {
            exoPlayer.pause();
            isPlaying = false;
            if (countDownTimer != null) countDownTimer.cancel();
            saveProgressToDatabase();
            updateNotification();
            sendStateToActivity();
        }
    }

    private void startTimer() {
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(Long.MAX_VALUE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (exoPlayer != null &&
                        exoPlayer.getPlaybackState() == Player.STATE_READY &&
                        exoPlayer.getPlayWhenReady()) {

                    timePlayedToday += 1000;

                    if ((timePlayedToday / 1000) % 60 == 0) {
                        saveProgressToDatabase();
                    }
                }

                updateNotification();
                sendStateToActivity();
            }

            @Override
            public void onFinish() { isPlaying = false; }
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
                .setContentTitle(currentStationName)
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
        intent.putExtra("station_url", currentStationUrl);
        intent.putExtra("station_name", currentStationName);
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
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID, "Radyo Servisi", NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(serviceChannel);
    }

    @Override
    public void onDestroy() {
        if (countDownTimer != null) countDownTimer.cancel();
        if (exoPlayer != null) exoPlayer.release();
        saveProgressToDatabase();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}