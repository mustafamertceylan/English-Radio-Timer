package com.example.radio_timer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Radyo oynatma servisi: ExoPlayer, saniyelik sayaç, Room kaydı, bildirimler.
 * Sözleşme: pm_decisions.md §2.1 / §2.2.
 */
public class RadioService extends Service {

    private static final String TAG = "RadioService";

    // ---- Servis -> Activity broadcast (§2.1) ----
    public static final String ACTION_STATE_UPDATE = "com.example.radio_timer.STATE_UPDATE";

    public static final String EXTRA_MILLIS_PLAYED = "millis_played";
    public static final String EXTRA_DAILY_GOAL = "daily_goal";
    public static final String EXTRA_GOAL_REACHED = "goal_reached";
    public static final String EXTRA_DATE_KEY = "date_key";
    public static final String EXTRA_IS_PLAYING = "is_playing";
    public static final String EXTRA_PLAYER_STATE = "player_state";
    public static final String EXTRA_ERROR_CODE = "error_code";
    public static final String EXTRA_ERROR_MESSAGE = "error_message";
    public static final String EXTRA_STATION_URL = "station_url";
    public static final String EXTRA_STATION_NAME = "station_name";

    public static final String STATE_IDLE = "idle";
    public static final String STATE_CONNECTING = "connecting";
    public static final String STATE_BUFFERING = "buffering";
    public static final String STATE_PLAYING = "playing";
    public static final String STATE_PAUSED = "paused";
    public static final String STATE_ERROR = "error";

    public static final String ERROR_NETWORK = "network";
    public static final String ERROR_SOURCE = "source";
    public static final String ERROR_UNKNOWN = "unknown";

    // ---- Activity -> Servis action'ları (§2.2) ----
    public static final String ACTION_PLAY_STATION = "com.example.radio_timer.action.PLAY_STATION";
    public static final String ACTION_TOGGLE = "com.example.radio_timer.action.TOGGLE";
    public static final String ACTION_STOP = "com.example.radio_timer.action.STOP";
    public static final String ACTION_REQUEST_STATE = "com.example.radio_timer.action.REQUEST_STATE";

    // ---- Bildirim ----
    private static final String CHANNEL_PLAYBACK = "radio_playback";
    private static final String CHANNEL_GOAL = "goal_events";
    private static final int NOTIFICATION_ID_PLAYBACK = 1;
    private static final int NOTIFICATION_ID_GOAL = 2;
    private static final int REQUEST_CODE_TOGGLE = 1;
    private static final int REQUEST_CODE_STOP = 2;

    private static final long TICK_INTERVAL_MS = 1000L;
    private static final long SAVE_INTERVAL_MS = 60_000L;
    /** Bu süreden uzun duraklatılan canlı yayın devam ettirilirken yeniden hazırlanır. */
    private static final long STALE_PAUSE_MS = 30_000L;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            onTick();
            handler.postDelayed(this, TICK_INTERVAL_MS);
        }
    };
    private boolean tickRunning = false;

    private ExoPlayer exoPlayer;
    private PreferenceHelper prefs;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder playbackBuilder;
    private String lastNotificationKey;

    // Oynatma durumu
    private String playerState = STATE_IDLE;
    @Nullable private String errorCode;
    @Nullable private String errorMessage;
    private boolean inError = false;
    private boolean hasMedia = false;
    private boolean isPlayerReady = false;
    /** Y3: kaynak değişirken/hazırlanırken geçici playWhenReady=false kaynaklı PAUSED yayınını bastırır. */
    private boolean isSwitchingSource = false;
    private long pausedAtElapsed = 0L;
    private boolean isForeground = false;

    private String currentStationUrl;
    private String currentStationName;

    // Sayaç / kalıcılık
    private String currentDayKey;
    private long timePlayedToday = 0L;
    private long lastAccountedElapsed = 0L;
    private long unsavedMillis = 0L;
    private boolean dirty = false;
    private boolean isLoaded = false;
    /** Y4: DB yüklemesi bitmeden çalınan süre; yükleme bitince DB değerine eklenir. */
    private long pendingMillisBeforeLoad = 0L;
    private final List<Integer> pendingStateRequests = new ArrayList<>();

    // ------------------------------------------------------------------ yaşam döngüsü

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new PreferenceHelper(this);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createNotificationChannels();
        restoreLastStation();
        currentDayKey = DateKeys.today();
        loadInitialProgress(currentDayKey);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        String action = intent.getAction();
        if (ACTION_PLAY_STATION.equals(action)) {
            if (!startForegroundCompat()) return START_NOT_STICKY;
            ensurePlayer();
            handlePlayStation(intent.getStringExtra(EXTRA_STATION_URL),
                    intent.getStringExtra(EXTRA_STATION_NAME));
        } else if (ACTION_TOGGLE.equals(action)) {
            if (!startForegroundCompat()) return START_NOT_STICKY;
            ensurePlayer();
            checkDayChange();
            toggle();
        } else if (ACTION_STOP.equals(action)) {
            handleStop();
        } else if (ACTION_REQUEST_STATE.equals(action)) {
            handleRequestState(startId);
        } else if (!isForeground) {
            stopSelfResult(startId);
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        stopTick();
        accountPlayTime();
        if (dirty) {
            if (isLoaded) {
                saveProgress(currentDayKey, timePlayedToday, prefs.getDailyGoal());
            } else {
                saveMerged(currentDayKey, pendingMillisBeforeLoad, prefs.getDailyGoal());
            }
        }
        handler.removeCallbacksAndMessages(null);
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // ------------------------------------------------------------------ kurulum

    private void restoreLastStation() {
        Station station = StationRepository.findByUrl(prefs.getLastStationUrl());
        if (station == null) station = StationRepository.getDefault();
        currentStationUrl = station.getUrl();
        currentStationName = station.getName();
    }

    /** Y7: ExoPlayer yalnız ilk PLAY_STATION/TOGGLE'da kurulur; onCreate ve REQUEST_STATE kurmaz. */
    @OptIn(markerClass = UnstableApi.class)
    private void ensurePlayer() {
        if (exoPlayer != null) return;
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(30000, 60000, 1500, 3000)
                .build();

        DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(USER_AGENT)
                .setAllowCrossProtocolRedirects(true);
        DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(this)
                .setDataSourceFactory(httpFactory);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build();

        exoPlayer = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .build();

        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    isPlayerReady = true;
                    isSwitchingSource = false;
                }
                deriveState();
            }

            @Override
            public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
                // Audio focus kaybı / kulaklık çıkarılması da buradan gelir => PAUSED.
                if (!playWhenReady) pausedAtElapsed = SystemClock.elapsedRealtime();
                deriveState();
            }

            @Override
            public void onPlaybackSuppressionReasonChanged(int playbackSuppressionReason) {
                deriveState();
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                handlePlayerError(error);
            }
        });
    }

    // ------------------------------------------------------------------ komutlar

    private void handlePlayStation(@Nullable String url, @Nullable String name) {
        checkDayChange();
        if (url == null || url.isEmpty()) {
            Log.w(TAG, "PLAY_STATION: station_url eksik");
            broadcastState();
            updateNotification(true);
            return;
        }
        if (name == null || name.isEmpty()) {
            Station known = StationRepository.findByUrl(url);
            name = known != null ? known.getName() : url;
        }
        prefs.setLastStation(url, name);

        if (url.equals(currentStationUrl)) {
            currentStationName = name;
            toggle();
        } else {
            currentStationUrl = url;
            currentStationName = name;
            startPlayback(true);
        }
    }

    private void toggle() {
        if (STATE_PLAYING.equals(playerState)
                || STATE_BUFFERING.equals(playerState)
                || STATE_CONNECTING.equals(playerState)) {
            pausePlayback();
        } else {
            boolean stale = STATE_PAUSED.equals(playerState)
                    && SystemClock.elapsedRealtime() - pausedAtElapsed > STALE_PAUSE_MS;
            boolean needsPrepare = STATE_IDLE.equals(playerState)
                    || STATE_ERROR.equals(playerState)
                    || stale;
            startPlayback(needsPrepare);
        }
    }

    /** Çalmayı başlatır; {@code prepare} true ise kaynak baştan hazırlanır (bekleyen çalma iptal olur). */
    private void startPlayback(boolean prepare) {
        if (exoPlayer == null) return;
        inError = false;
        errorCode = null;
        errorMessage = null;

        int ps = exoPlayer.getPlaybackState();
        if (prepare || !hasMedia || ps == Player.STATE_IDLE || ps == Player.STATE_ENDED) {
            isSwitchingSource = true;
            exoPlayer.setPlayWhenReady(false);
            exoPlayer.stop();
            isPlayerReady = false;
            exoPlayer.setMediaItem(MediaItem.fromUri(currentStationUrl));
            hasMedia = true;
            exoPlayer.prepare();
        }
        exoPlayer.setPlayWhenReady(true);
        deriveState();
        // Durum aynı kalsa bile (ör. istasyon değişti, yine CONNECTING) UI ve bildirim güncellensin.
        broadcastState();
        updateNotification(true);
    }

    private void pausePlayback() {
        if (exoPlayer == null) return;
        isSwitchingSource = false;
        pausedAtElapsed = SystemClock.elapsedRealtime();
        exoPlayer.setPlayWhenReady(false);
        deriveState();
    }

    private void handleStop() {
        if (!isForeground) {
            // Bildirim aksiyonundan getForegroundService ile gelmiş olabilir; zaman aşımı crash'ini önle.
            startForegroundCompat();
        }
        accountPlayTime();
        if (exoPlayer != null) {
            isSwitchingSource = false;
            exoPlayer.setPlayWhenReady(false);
            exoPlayer.stop();
            exoPlayer.release();
            exoPlayer = null;
        }
        hasMedia = false;
        isPlayerReady = false;
        isSwitchingSource = false;
        inError = false;
        errorCode = null;
        errorMessage = null;
        setState(STATE_IDLE);
        saveIfDirty();
        broadcastState();
        stopTick();
        stopForegroundCompat();
        stopSelf();
    }

    private void handleRequestState(int startId) {
        checkDayChange();
        if (isForeground) {
            broadcastState();
            return;
        }
        if (isLoaded) {
            broadcastState();
            stopSelfResult(startId);
        } else {
            pendingStateRequests.add(startId);
        }
    }

    // ------------------------------------------------------------------ durum makinesi

    private void deriveState() {
        if (exoPlayer == null) return;
        if (inError) {
            setState(STATE_ERROR);
            return;
        }
        if (!hasMedia) {
            setState(STATE_IDLE);
            return;
        }
        int ps = exoPlayer.getPlaybackState();
        boolean playWhenReady = exoPlayer.getPlayWhenReady();
        String next;
        if (isSwitchingSource) {
            // Y3: yeni kaynak hazırlanıyor; geçici pwr=false / IDLE durumları CONNECTING sayılır.
            next = STATE_CONNECTING;
        } else if (!playWhenReady) {
            next = STATE_PAUSED;
        } else if (ps == Player.STATE_READY) {
            next = exoPlayer.getPlaybackSuppressionReason() == Player.PLAYBACK_SUPPRESSION_REASON_NONE
                    ? STATE_PLAYING : STATE_PAUSED;
        } else if (ps == Player.STATE_BUFFERING) {
            next = isPlayerReady ? STATE_BUFFERING : STATE_CONNECTING;
        } else if (ps == Player.STATE_ENDED) {
            // Canlı yayın beklenmedik şekilde bitti.
            inError = true;
            errorCode = ERROR_SOURCE;
            errorMessage = "STATE_ENDED";
            next = STATE_ERROR;
        } else {
            // STATE_IDLE + playWhenReady: prepare bekleniyor.
            next = STATE_CONNECTING;
        }
        setState(next);
    }

    private void setState(String next) {
        if (next.equals(playerState)) return;
        String prev = playerState;
        if (STATE_PLAYING.equals(prev)) {
            accountPlayTime();
        }
        playerState = next;
        if (STATE_PLAYING.equals(next)) {
            lastAccountedElapsed = SystemClock.elapsedRealtime();
        } else if (STATE_PLAYING.equals(prev)) {
            saveIfDirty();
        }
        if (isPlayingIntent()) {
            startTick();
        } else {
            stopTick(); // Y5: PAUSED/IDLE/ERROR'da servis her saniye uyanmaz
        }
        Log.d(TAG, "State: " + prev + " -> " + next);
        updateNotification(false);
        broadcastState();
    }

    private void handlePlayerError(PlaybackException error) {
        if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW && exoPlayer != null) {
            // HLS canlı pencerenin gerisinde kaldı: canlı kenara dön ve devam et.
            exoPlayer.seekToDefaultPosition();
            exoPlayer.prepare();
            return;
        }
        Log.w(TAG, "Oynatma hatası: " + error.getErrorCodeName(), error);
        isSwitchingSource = false;
        inError = true;
        errorCode = classifyError(error.errorCode);
        errorMessage = error.getErrorCodeName();
        if (exoPlayer != null) exoPlayer.setPlayWhenReady(false);
        deriveState();
        updateNotification(true);
    }

    private static String classifyError(int code) {
        switch (code) {
            case PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED:
            case PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT:
            case PlaybackException.ERROR_CODE_TIMEOUT:
                return ERROR_NETWORK;
            case PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS:
            case PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE:
            case PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND:
            case PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED:
            case PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED:
            case PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED:
            case PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED:
            case PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED:
            case PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED:
            case PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES:
                return ERROR_SOURCE;
            default:
                return ERROR_UNKNOWN;
        }
    }

    private boolean isPlayingIntent() {
        return STATE_PLAYING.equals(playerState)
                || STATE_BUFFERING.equals(playerState)
                || STATE_CONNECTING.equals(playerState);
    }

    // ------------------------------------------------------------------ sayaç

    private void startTick() {
        if (tickRunning) return;
        tickRunning = true;
        handler.postDelayed(tickRunnable, TICK_INTERVAL_MS);
    }

    private void stopTick() {
        tickRunning = false;
        handler.removeCallbacks(tickRunnable);
    }

    private void onTick() {
        checkDayChange();
        accountPlayTime();
        if (unsavedMillis >= SAVE_INTERVAL_MS) {
            saveIfDirty();
        }
        updateNotification(false);
        broadcastState();
    }

    /** Yalnız STATE_PLAYING'de, son hesaplamadan bu yana geçen gerçek süreyi ekler. */
    private void accountPlayTime() {
        if (!STATE_PLAYING.equals(playerState)) return;
        long now = SystemClock.elapsedRealtime();
        long delta = now - lastAccountedElapsed;
        lastAccountedElapsed = now;
        if (delta <= 0) return;
        if (delta > SAVE_INTERVAL_MS) delta = SAVE_INTERVAL_MS; // güvenlik sınırı

        long goal = prefs.getDailyGoal();
        long prev = timePlayedToday;
        timePlayedToday += delta;
        if (!isLoaded) pendingMillisBeforeLoad += delta;
        unsavedMillis += delta;
        dirty = true;
        if (prev < goal && timePlayedToday >= goal) {
            onGoalReached(goal);
        }
    }

    private void checkDayChange() {
        String today = DateKeys.today();
        if (today.equals(currentDayKey)) return;
        accountPlayTime(); // kalan süre eski güne yazılır
        if (dirty) {
            long goal = prefs.getDailyGoal();
            if (isLoaded) {
                saveProgress(currentDayKey, timePlayedToday, goal);
            } else {
                saveMerged(currentDayKey, pendingMillisBeforeLoad, goal);
            }
        }
        currentDayKey = today;
        timePlayedToday = 0L;
        pendingMillisBeforeLoad = 0L;
        unsavedMillis = 0L;
        dirty = false;
        lastAccountedElapsed = SystemClock.elapsedRealtime();
        loadInitialProgress(today);
        updateNotification(true);
        broadcastState();
    }

    // ------------------------------------------------------------------ kalıcılık

    private void loadInitialProgress(final String dayKey) {
        isLoaded = false;
        final Context appContext = getApplicationContext();
        AppExecutors.diskIO().execute(() -> {
            ListeningLog log = AppDatabase.getInstance(appContext).listeningDao().getLogByDate(dayKey);
            final long dbValue = log != null ? log.durationMillis : 0L;
            handler.post(() -> onInitialProgressLoaded(dayKey, dbValue));
        });
    }

    private void onInitialProgressLoaded(String dayKey, long dbValue) {
        if (!dayKey.equals(currentDayKey)) return; // gün bu arada değişti; yeni yükleme beklenir
        accountPlayTime();
        timePlayedToday = dbValue + pendingMillisBeforeLoad; // Y4: max yerine toplama
        pendingMillisBeforeLoad = 0L;
        isLoaded = true;
        saveIfDirty();
        broadcastState();
        updateNotification(false);
        if (!pendingStateRequests.isEmpty()) {
            List<Integer> ids = new ArrayList<>(pendingStateRequests);
            pendingStateRequests.clear();
            if (!isForeground) {
                for (int id : ids) stopSelfResult(id);
            }
        }
    }

    private void saveIfDirty() {
        if (!isLoaded || !dirty) return;
        saveProgress(currentDayKey, timePlayedToday, prefs.getDailyGoal());
    }

    /** Değerleri parametre olarak alır, yalnız diskIO üzerinde yazar. */
    private void saveProgress(final String dateKey, final long millis, final long goal) {
        dirty = false;
        unsavedMillis = 0L;
        final Context appContext = getApplicationContext();
        AppExecutors.diskIO().execute(() -> {
            ListeningDao dao = AppDatabase.getInstance(appContext).listeningDao();
            ListeningLog existing = dao.getLogByDate(dateKey);
            // Y1: bayrak bir kez true olunca geri false olmaz (old || new).
            boolean reached = (existing != null && existing.isGoalReached) || millis >= goal;
            dao.insertOrUpdate(new ListeningLog(dateKey, millis, reached));
        });
    }

    /** DB yüklemesi bitmeden kaydedilirken: yüklenmemiş süreyi mevcut kayda ekleyerek yaz. */
    private void saveMerged(final String dateKey, final long pendingMillis, final long goal) {
        dirty = false;
        unsavedMillis = 0L;
        final Context appContext = getApplicationContext();
        AppExecutors.diskIO().execute(() -> {
            ListeningDao dao = AppDatabase.getInstance(appContext).listeningDao();
            ListeningLog existing = dao.getLogByDate(dateKey);
            long value = (existing != null ? existing.durationMillis : 0L) + pendingMillis;
            boolean reached = (existing != null && existing.isGoalReached) || value >= goal;
            dao.insertOrUpdate(new ListeningLog(dateKey, value, reached));
        });
    }

    // ------------------------------------------------------------------ broadcast

    private void broadcastState() {
        long goal = prefs.getDailyGoal();
        boolean error = STATE_ERROR.equals(playerState);
        Intent i = new Intent(ACTION_STATE_UPDATE);
        i.setPackage(getPackageName());
        i.putExtra(EXTRA_MILLIS_PLAYED, timePlayedToday);
        i.putExtra(EXTRA_DAILY_GOAL, goal);
        i.putExtra(EXTRA_GOAL_REACHED, timePlayedToday >= goal);
        i.putExtra(EXTRA_DATE_KEY, currentDayKey);
        i.putExtra(EXTRA_IS_PLAYING, isPlayingIntent());
        i.putExtra(EXTRA_PLAYER_STATE, playerState);
        i.putExtra(EXTRA_ERROR_CODE, error ? (errorCode != null ? errorCode : ERROR_UNKNOWN) : null);
        i.putExtra(EXTRA_ERROR_MESSAGE, error ? errorMessage : null);
        i.putExtra(EXTRA_STATION_URL, currentStationUrl);
        i.putExtra(EXTRA_STATION_NAME, currentStationName);
        sendBroadcast(i);
    }

    // ------------------------------------------------------------------ bildirimler

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || notificationManager == null) return;
        NotificationChannel playback = new NotificationChannel(CHANNEL_PLAYBACK,
                getString(R.string.notif_channel_playback), NotificationManager.IMPORTANCE_LOW);
        playback.setShowBadge(false);
        NotificationChannel goal = new NotificationChannel(CHANNEL_GOAL,
                getString(R.string.notif_channel_goal), NotificationManager.IMPORTANCE_DEFAULT);
        notificationManager.createNotificationChannel(playback);
        notificationManager.createNotificationChannel(goal);
    }

    /** Y6: başarısızsa log + stopSelf, false döner (çalma başlatılmaz). */
    private boolean startForegroundCompat() {
        Notification notification = buildPlaybackNotification();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID_PLAYBACK, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            } else {
                startForeground(NOTIFICATION_ID_PLAYBACK, notification);
            }
            isForeground = true;
            return true;
        } catch (RuntimeException e) {
            // API 31+: arka plandan foreground başlatma yasağı (ForegroundServiceStartNotAllowedException).
            Log.e(TAG, "startForeground başarısız", e);
            stopSelf();
            return false;
        }
    }

    private void stopForegroundCompat() {
        stopForeground(STOP_FOREGROUND_REMOVE); // minSdk 24
        isForeground = false;
        lastNotificationKey = null;
    }

    private String currentNotificationText() {
        switch (playerState) {
            case STATE_CONNECTING:
            case STATE_IDLE:
                return getString(R.string.notif_connecting);
            case STATE_BUFFERING:
                return getString(R.string.notif_buffering);
            case STATE_PAUSED:
                return getString(R.string.notif_paused);
            case STATE_ERROR:
                return getString(R.string.notif_error);
            default: {
                long goal = prefs.getDailyGoal();
                if (timePlayedToday >= goal) {
                    return getString(R.string.notif_goal_done_ongoing,
                            TimeFormatter.formatGoal(timePlayedToday));
                }
                // Kalan süreyi dakikaya yukarı yuvarla (30 sn kala "0 dk" görünmesin).
                long remaining = goal - timePlayedToday;
                long roundedUp = ((remaining + 59_999L) / 60_000L) * 60_000L;
                return getString(R.string.notif_remaining, TimeFormatter.formatGoal(roundedUp));
            }
        }
    }

    private Notification buildPlaybackNotification() {
        if (playbackBuilder == null) {
            Intent openApp = new Intent(this, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, openApp,
                    PendingIntent.FLAG_IMMUTABLE);
            playbackBuilder = new NotificationCompat.Builder(this, CHANNEL_PLAYBACK)
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setContentIntent(contentIntent)
                    .setOnlyAlertOnce(true)
                    .setShowWhen(false)
                    .setOngoing(true)
                    .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);
        }
        boolean playing = isPlayingIntent();
        playbackBuilder.clearActions();
        playbackBuilder
                .setContentTitle(currentStationName)
                .setContentText(currentNotificationText())
                .addAction(playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                        getString(playing ? R.string.notif_action_pause : R.string.notif_action_play),
                        serviceIntent(ACTION_TOGGLE, REQUEST_CODE_TOGGLE))
                .addAction(android.R.drawable.ic_menu_close_clear_cancel,
                        getString(R.string.notif_action_stop),
                        serviceIntent(ACTION_STOP, REQUEST_CODE_STOP));
        return playbackBuilder.build();
    }

    private PendingIntent serviceIntent(String action, int requestCode) {
        Intent intent = new Intent(this, RadioService.class).setAction(action);
        int flags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return PendingIntent.getForegroundService(this, requestCode, intent, flags);
        }
        return PendingIntent.getService(this, requestCode, intent, flags);
    }

    /** Yalnız metin veya durum değiştiğinde notify eder (force ile koşulsuz). */
    private void updateNotification(boolean force) {
        if (!isForeground || notificationManager == null) return;
        String key = playerState + '|' + currentStationName + '|' + currentNotificationText();
        if (!force && key.equals(lastNotificationKey)) return;
        lastNotificationKey = key;
        notificationManager.notify(NOTIFICATION_ID_PLAYBACK, buildPlaybackNotification());
    }

    private void onGoalReached(long goal) {
        String today = currentDayKey;
        if (!today.equals(prefs.getLastCelebratedDate())) {
            prefs.setLastCelebratedDate(today);
            postGoalNotification(goal);
        }
        saveIfDirty(); // isGoalReached=true anında kaydedilsin
        updateNotification(true);
    }

    private void postGoalNotification(long goal) {
        if (notificationManager == null
                || !NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            return; // API 33+ izin yoksa sessizce atla
        }
        Intent openApp = new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, openApp,
                PendingIntent.FLAG_IMMUTABLE);
        Notification n = new NotificationCompat.Builder(this, CHANNEL_GOAL)
                .setSmallIcon(android.R.drawable.star_on)
                .setContentTitle(getString(R.string.notif_goal_reached_title))
                .setContentText(getString(R.string.notif_goal_reached_text, TimeFormatter.formatGoal(goal)))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();
        try {
            notificationManager.notify(NOTIFICATION_ID_GOAL, n);
        } catch (SecurityException e) {
            Log.w(TAG, "Kutlama bildirimi gösterilemedi", e);
        }
    }
}
