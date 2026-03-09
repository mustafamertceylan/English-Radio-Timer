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
import androidx.media3.common.Player;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RadioService extends Service {
    private ExoPlayer exoPlayer;
    private CountDownTimer countDownTimer;
    private long timePlayedToday = 0; // Toplam dinlenen süre (Milisaniye)
    private final long DAILY_GOAL = 14400000; // 4 Saat hedefi
    private boolean isPlaying = false;
    private static final String CHANNEL_ID = "RadioChannel";
    public static final String ACTION_TOGGLE = "ACTION_TOGGLE";
    public static final String TIMER_UPDATE = "TIMER_UPDATE";

    private void saveProgressToDatabase(long currentMillis) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Veritabanı işlemleri ana thread'i kilitlememek için arka planda yapılmalıdır

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            ListeningLog existingLog = db.listeningDao().getLogByDate(today);

            long totalMillisToday = (existingLog != null) ? existingLog.durationMillis + currentMillis : currentMillis;
            boolean goalReached = totalMillisToday >= 14400000; // 4 saat hedefi

            db.listeningDao().insertOrUpdate(new ListeningLog(today, totalMillisToday, goalReached));
        }).start();
    }
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        // 1. 30 Saniyelik Buffer Yapılandırması (Metod ismini güncelledik)
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        30000, // minBufferMs (30 saniye)
                        60000, // maxBufferMs (60 saniye)
                        1500,  // bufferForPlaybackMs
                        3000   // bufferForPlaybackAfterRebufferMs
                )
                .build();

        // 2. ExoPlayer'ı yeni LoadControl ile başlat
        exoPlayer = new ExoPlayer.Builder(this)
                .setLoadControl(loadControl)
                .build();

        // 3. Veritabanından bugünün verisini yükle
        loadInitialProgress();

        // 4. Radyoyu hazırla
        String url = "https://stream.live.vc.bbcmedia.co.uk/bbc_world_service";
        exoPlayer.setMediaItem(MediaItem.fromUri(url));
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
        if (intent != null && "ACTION_TOGGLE".equals(intent.getAction())) {
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
            exoPlayer.play();
            isPlaying = true;
            startTimer();
            updateNotification();
            // Durum gönderilirken toplam süreyi de ekliyoruz
            sendStateToActivity();
        }
    }

    private void pauseRadio() {
        if (isPlaying) {
            exoPlayer.pause();
            isPlaying = false;
            if (countDownTimer != null) countDownTimer.cancel();

            // RADYO DURDUĞUNDA VERİYİ VERİTABANINA MÜHÜRLE
            saveProgressToDatabase();

            updateNotification();
            sendStateToActivity();
        }
    }

    private void sendStateToActivity(boolean isPlaying) {
        Intent intent = new Intent(TIMER_UPDATE);
        intent.putExtra("is_playing", isPlaying);
        sendBroadcast(intent);
    }

    private void startTimer() {
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(14400000 * 10, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Sadece radyo gerçekten çalıyorsa süre ekle
                if (exoPlayer != null &&
                        exoPlayer.getPlaybackState() == Player.STATE_READY &&
                        exoPlayer.getPlayWhenReady()) {

                    timePlayedToday += 1000;

                    // Her 60 saniyede bir otomatik kaydet
                    if ((timePlayedToday / 1000) % 60 == 0) {
                        saveProgressToDatabase();
                    }
                }

                // Arayüzü ve bildirimi her durumda güncelle ki donup kalmasın
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

        // KALAN SÜREYİ HESAPLA: Hedef (14.400.000 ms) - Dinlenen Süre
        // Math.max(0, ...) kullanarak sürenin negatif görünmesini engelliyoruz
        long remainingTime = Math.max(0, DAILY_GOAL - timePlayedToday);

        String buttonText = isPlaying ? "Duraklat" : "Devam Et";
        int buttonIcon = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("BBC World Service")
                // Değişkeni burada güncelledik:
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
        // MainActivity'nin beklediği anahtarlar:
        intent.putExtra("millis_played", timePlayedToday);
        intent.putExtra("is_playing", isPlaying);
        sendBroadcast(intent);
    }

    private void saveProgressToDatabase() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            boolean goalReached = timePlayedToday >= DAILY_GOAL;
            // Mevcut süreyi direkt üzerine yazıyoruz (Üst üste ekleme hatasını önler)
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