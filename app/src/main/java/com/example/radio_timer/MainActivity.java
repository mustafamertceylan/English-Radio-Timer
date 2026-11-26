package com.example.radio_timer;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.net.Uri;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final Locale LOCALE_TR = new Locale("tr", "TR");
    /** Uyku zamanlayıcı seçenekleri (dakika). */
    private static final int[] SLEEP_OPTIONS = {15, 30, 45, 60};

    private StationAdapter stationAdapter;
    private TextView stationNameTop;
    private TextView playerStatusText;
    private TextView percentageText;
    private TextView totalDailyTime;
    private TextView goalLabelText;
    private TextView currentDateText;
    private TextView streakText;
    private ImageView playIcon;
    private ImageView sleepIcon;
    private TextView sleepRemainingText;
    private TextView notifWarningText;
    private ProgressBar goalProgressBar;
    private PreferenceHelper preferenceHelper;

    /** Bu onResume döngüsünde servisten durum broadcast'i alındı mı (DB sonucunun üzerine yazmasını engeller). */
    private boolean hasServiceState = false;
    @Nullable private String shownDateKey;
    @Nullable private Boolean lastGoalReached;
    /** Son yüklenen seri değeri; -1 = henüz yüklenmedi. */
    private int lastStreak = -1;
    /** Bugün hedefe ulaşıldı mı (broadcast'ten, yoksa DB'den). K7 seri metni için. */
    private boolean todayReached = false;
    /** Son bilinen oynatma durumu (uyku zamanlayıcı yalnız idle değilken etkin). */
    private String currentPlayerState = RadioService.STATE_IDLE;
    /** Son bilinen uyku zamanlayıcı kalanı (ms); 0 = yok. */
    private long sleepRemainingMs = 0L;

    /** Y8: Activity açıkken gece yarısında tarih kartını, ilerlemeyi ve seriyi yeniler. */
    private final Handler midnightHandler = new Handler(Looper.getMainLooper());
    private final Runnable midnightRunnable = new Runnable() {
        @Override
        public void run() {
            hasServiceState = false;
            lastGoalReached = null;
            todayReached = false;
            showDate(DateKeys.today());
            loadTodayFromDb(preferenceHelper.getDailyGoal());
            loadStreak();
            scheduleMidnightRefresh();
        }
    };

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!RadioService.ACTION_STATE_UPDATE.equals(intent.getAction())) return;
            String dateKey = intent.getStringExtra(RadioService.EXTRA_DATE_KEY);
            // T5: gösterilen günden eski bir güne ait (geç gelmiş) broadcast yok sayılır.
            if (dateKey != null && shownDateKey != null && dateKey.compareTo(shownDateKey) < 0) return;
            hasServiceState = true;

            long millisPlayed = intent.getLongExtra(RadioService.EXTRA_MILLIS_PLAYED, 0L);
            long dailyGoal = intent.getLongExtra(RadioService.EXTRA_DAILY_GOAL, 0L);
            if (dailyGoal <= 0) dailyGoal = preferenceHelper.getDailyGoal();
            boolean goalReached = intent.getBooleanExtra(RadioService.EXTRA_GOAL_REACHED, false);
            boolean isPlaying = intent.getBooleanExtra(RadioService.EXTRA_IS_PLAYING, false);
            String playerState = intent.getStringExtra(RadioService.EXTRA_PLAYER_STATE);
            String errorCode = intent.getStringExtra(RadioService.EXTRA_ERROR_CODE);
            String stationUrl = intent.getStringExtra(RadioService.EXTRA_STATION_URL);
            String stationName = intent.getStringExtra(RadioService.EXTRA_STATION_NAME);
            long sleepRemaining = intent.getLongExtra(RadioService.EXTRA_SLEEP_REMAINING_MS, 0L);

            if (stationName != null) stationNameTop.setText(stationName);
            showProgress(millisPlayed, dailyGoal, goalReached);
            showPlayerState(playerState, errorCode, isPlaying);
            stationAdapter.setPlaybackState(stationUrl, isPlaying, playerState);
            showSleepTimer(sleepRemaining);

            boolean refreshStreak = false;
            if (dateKey != null && !dateKey.equals(shownDateKey)) {
                showDate(dateKey);
                refreshStreak = true;
            }
            if (lastGoalReached == null || lastGoalReached != goalReached) {
                lastGoalReached = goalReached;
                todayReached = goalReached;
                refreshStreak = true;
            }
            if (refreshStreak) loadStreak();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferenceHelper = new PreferenceHelper(this);

        stationNameTop = findViewById(R.id.stationNameTop);
        playerStatusText = findViewById(R.id.playerStatusText);
        percentageText = findViewById(R.id.percentageText);
        totalDailyTime = findViewById(R.id.totalDailyTime);
        goalLabelText = findViewById(R.id.goalLabelText);
        currentDateText = findViewById(R.id.currentDateText);
        streakText = findViewById(R.id.streakText);
        playIcon = findViewById(R.id.playIcon);
        sleepIcon = findViewById(R.id.sleepIcon);
        sleepRemainingText = findViewById(R.id.sleepRemainingText);
        notifWarningText = findViewById(R.id.notifWarningText);
        goalProgressBar = findViewById(R.id.goalProgressBar);
        RecyclerView stationRecyclerView = findViewById(R.id.stationRecyclerView);
        CardView nowPlayingCard = findViewById(R.id.nowPlayingCard);
        CardView dateCard = findViewById(R.id.dateCard);
        ImageView settingsIcon = findViewById(R.id.settingsIcon);

        // İstasyon listesi tek kaynaktan (H6)
        stationRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        stationAdapter = new StationAdapter(StationRepository.getStations(), station -> {
            Intent serviceIntent = new Intent(MainActivity.this, RadioService.class);
            serviceIntent.setAction(RadioService.ACTION_PLAY_STATION);
            serviceIntent.putExtra(RadioService.EXTRA_STATION_URL, station.getUrl());
            serviceIntent.putExtra(RadioService.EXTRA_STATION_NAME, station.getName());
            startForegroundServiceCompat(serviceIntent);
        });
        stationRecyclerView.setAdapter(stationAdapter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        settingsIcon.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SettingsActivity.class)));

        dateCard.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, CalendarActivity.class)));

        // Üst karta tıklanınca mevcut/son istasyonu toggle et
        nowPlayingCard.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(this, RadioService.class);
            serviceIntent.setAction(RadioService.ACTION_TOGGLE);
            startForegroundServiceCompat(serviceIntent);
        });

        sleepIcon.setOnClickListener(v -> showSleepTimerDialog());
        notifWarningText.setOnClickListener(v -> openNotificationSettings());

        maybeRequestNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ContextCompat.registerReceiver(this, stateReceiver,
                new IntentFilter(RadioService.ACTION_STATE_UPDATE),
                ContextCompat.RECEIVER_NOT_EXPORTED);

        hasServiceState = false;
        lastGoalReached = null;
        showInitialState();
        requestServiceState();
        loadStreak();
        scheduleMidnightRefresh();
        updateNotificationWarning();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(stateReceiver);
        midnightHandler.removeCallbacks(midnightRunnable);
    }

    private void scheduleMidnightRefresh() {
        midnightHandler.removeCallbacks(midnightRunnable);
        Calendar next = Calendar.getInstance();
        next.add(Calendar.DAY_OF_MONTH, 1);
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 1);
        next.set(Calendar.MILLISECOND, 0);
        long delay = Math.max(1000L, next.getTimeInMillis() - System.currentTimeMillis());
        midnightHandler.postDelayed(midnightRunnable, delay);
    }

    /** Broadcast gelmeden önce: son istasyon adı, prefs'teki hedef ve DB'deki bugünkü ilerleme (H10, U6). */
    private void showInitialState() {
        // K9: servisle aynı kural — listede olmayan kayıtlı URL ⇒ varsayılan istasyon.
        Station last = StationRepository.findByUrl(preferenceHelper.getLastStationUrl());
        if (last == null) last = StationRepository.getDefault();
        stationNameTop.setText(last.getName());

        showDate(DateKeys.today());

        final long dailyGoal = preferenceHelper.getDailyGoal();
        showProgress(0L, dailyGoal, false);
        showPlayerState(RadioService.STATE_IDLE, null, false);
        stationAdapter.setPlaybackState(last.getUrl(), false, RadioService.STATE_IDLE);
        showSleepTimer(0L);
        loadTodayFromDb(dailyGoal);
    }

    /** Bugünkü ilerlemeyi DB'den yükler; bu arada servisten broadcast geldiyse sonuç yok sayılır. */
    private void loadTodayFromDb(final long dailyGoal) {
        final String today = DateKeys.today();
        final Context appContext = getApplicationContext();
        AppExecutors.diskIO().execute(() -> {
            ListeningLog log = AppDatabase.getInstance(appContext).listeningDao().getLogByDate(today);
            final long played = log != null ? log.durationMillis : 0L;
            final boolean reached = log != null && (log.isGoalReached || played >= dailyGoal);
            runOnUiThread(() -> {
                if (hasServiceState || isFinishing()) return;
                showProgress(played, dailyGoal, played >= dailyGoal);
                todayReached = reached;
                renderStreak();
            });
        });
    }

    private void requestServiceState() {
        Intent intent = new Intent(this, RadioService.class);
        intent.setAction(RadioService.ACTION_REQUEST_STATE);
        try {
            startService(intent);
        } catch (IllegalStateException e) {
            // Arka plan kısıtı: DB'den yüklenen değerler gösterilmeye devam eder.
        }
    }

    private void startForegroundServiceCompat(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void showProgress(long played, long goal, boolean goalReached) {
        totalDailyTime.setText(getString(R.string.time_played_of_goal,
                TimeFormatter.formatShort(played), TimeFormatter.formatGoal(goal)));
        int percent = StatsHelper.progressPercent(played, goal);
        goalProgressBar.setProgress(percent);
        percentageText.setText(goalReached
                ? getString(R.string.goal_reached_label)
                : getString(R.string.percent_done, percent));
        goalLabelText.setText(getString(R.string.goal_label, TimeFormatter.formatGoal(goal)));
    }

    private void showPlayerState(@Nullable String state, @Nullable String errorCode, boolean isPlaying) {
        currentPlayerState = state != null ? state : RadioService.STATE_IDLE;
        boolean sleepEnabled = !RadioService.STATE_IDLE.equals(currentPlayerState);
        sleepIcon.setEnabled(sleepEnabled);
        sleepIcon.setAlpha(sleepEnabled ? 1f : 0.4f);

        playIcon.setImageResource(isPlaying
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play);
        playIcon.setContentDescription(getString(isPlaying ? R.string.cd_pause : R.string.cd_play));

        if (RadioService.STATE_ERROR.equals(state)) {
            int msg;
            if (RadioService.ERROR_NETWORK.equals(errorCode)) {
                msg = R.string.error_network;
            } else if (RadioService.ERROR_SOURCE.equals(errorCode)) {
                msg = R.string.error_source;
            } else {
                msg = R.string.error_unknown;
            }
            // K2: hata metni + tekrar deneme ipucu (karta dokunmak TOGGLE ⇒ yeniden prepare)
            playerStatusText.setText(getString(R.string.error_with_hint,
                    getString(msg), getString(R.string.error_retry_hint)));
            playerStatusText.setTextColor(ContextCompat.getColor(this, R.color.on_accent));
            playerStatusText.setBackgroundResource(R.drawable.bg_status_error);
            return;
        }

        int msg;
        if (RadioService.STATE_CONNECTING.equals(state)) {
            msg = R.string.state_connecting;
        } else if (RadioService.STATE_BUFFERING.equals(state)) {
            msg = R.string.state_buffering;
        } else if (RadioService.STATE_PLAYING.equals(state)) {
            msg = R.string.state_playing;
        } else if (RadioService.STATE_PAUSED.equals(state)) {
            msg = R.string.state_paused;
        } else if (RadioService.STATE_INTERRUPTED.equals(state)) {
            msg = R.string.state_interrupted;
        } else {
            // IDLE ve bilinmeyen değerler (ileriye dönük güvenlik)
            msg = R.string.state_idle;
        }
        playerStatusText.setText(msg);
        playerStatusText.setTextColor(ContextCompat.getColor(this, R.color.on_accent_secondary));
        playerStatusText.setBackground(null);
        playerStatusText.setPadding(0, 0, 0, 0);
    }

    private void showDate(String dateKey) {
        shownDateKey = dateKey;
        int[] ymd = DateKeys.parse(dateKey);
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(ymd[0], ymd[1] - 1, ymd[2]);
        currentDateText.setText(new SimpleDateFormat("EEEE, d MMMM yyyy", LOCALE_TR).format(cal.getTime()));
    }

    private void loadStreak() {
        final Context appContext = getApplicationContext();
        AppExecutors.diskIO().execute(() -> {
            final int streak = StatsHelper.loadStreak(appContext);
            runOnUiThread(() -> {
                lastStreak = streak;
                renderStreak();
            });
        });
    }

    /** K7: 0 ⇒ streak_none; >0 ve bugün ulaşılmadı ⇒ streak_at_risk; ulaşıldı ⇒ streak_label. */
    private void renderStreak() {
        if (lastStreak < 0) return;
        if (lastStreak == 0) {
            streakText.setText(R.string.streak_none);
        } else if (todayReached) {
            streakText.setText(getString(R.string.streak_label, lastStreak));
        } else {
            streakText.setText(getString(R.string.streak_at_risk, lastStreak));
        }
    }

    // ===== Uyku zamanlayıcı (B3) =====

    private void showSleepTimer(long remainingMs) {
        sleepRemainingMs = Math.max(0L, remainingMs);
        if (sleepRemainingMs > 0) {
            sleepRemainingText.setText(getString(R.string.sleep_remaining,
                    TimeFormatter.formatGoal(TimeFormatter.roundUpToMinute(sleepRemainingMs))));
            sleepRemainingText.setVisibility(View.VISIBLE);
        } else {
            sleepRemainingText.setVisibility(View.GONE);
        }
    }

    private void showSleepTimerDialog() {
        if (RadioService.STATE_IDLE.equals(currentPlayerState)) return;
        final boolean active = sleepRemainingMs > 0;
        String[] items = new String[SLEEP_OPTIONS.length + (active ? 1 : 0)];
        for (int i = 0; i < SLEEP_OPTIONS.length; i++) {
            items[i] = getString(R.string.sleep_option_minutes, SLEEP_OPTIONS[i]);
        }
        if (active) items[SLEEP_OPTIONS.length] = getString(R.string.sleep_cancel);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.sleep_dialog_title)
                .setSingleChoiceItems(items, -1, (dialog, which) -> {
                    dialog.dismiss();
                    int minutes = which < SLEEP_OPTIONS.length ? SLEEP_OPTIONS[which] : 0;
                    sendSleepTimer(minutes);
                })
                .show();
    }

    /** @param minutes 15/30/45/60; 0 = iptal. */
    private void sendSleepTimer(int minutes) {
        if (RadioService.STATE_IDLE.equals(currentPlayerState)) return;
        Intent intent = new Intent(this, RadioService.class);
        intent.setAction(RadioService.ACTION_SLEEP_TIMER);
        intent.putExtra(RadioService.EXTRA_SLEEP_MINUTES, minutes);
        try {
            startService(intent);
        } catch (IllegalStateException e) {
            return;
        }
        if (minutes > 0) {
            Toast.makeText(this, getString(R.string.sleep_set_toast, minutes), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.sleep_cancelled_toast, Toast.LENGTH_SHORT).show();
        }
    }

    // ===== Bildirim izni (T1, T10) =====

    /** API 33+: izin yoksa ve daha önce sorulmadıysa gerekçe diyaloğu olmadan bir kez sorar. */
    private void maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return;
        if (preferenceHelper.wasNotificationPermissionAsked()) return;
        preferenceHelper.setNotificationPermissionAsked();
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
    }

    /** Bildirimler kapalıysa (tüm API'lerde) kalıcı uyarı satırı gösterilir. */
    private void updateNotificationWarning() {
        boolean enabled = NotificationManagerCompat.from(this).areNotificationsEnabled();
        notifWarningText.setVisibility(enabled ? View.GONE : View.VISIBLE);
    }

    private void openNotificationSettings() {
        Uri packageUri = Uri.fromParts("package", getPackageName(), null);
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri);
        }
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            try {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri));
            } catch (ActivityNotFoundException ignored) {
                // Ayarlar ekranı açılamıyor; satır görünür kalır.
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            updateNotificationWarning();
        }
    }
}
