package com.example.radio_timer;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final Locale LOCALE_TR = new Locale("tr", "TR");

    private StationAdapter stationAdapter;
    private TextView stationNameTop;
    private TextView playerStatusText;
    private TextView percentageText;
    private TextView totalDailyTime;
    private TextView goalLabelText;
    private TextView currentDateText;
    private TextView streakText;
    private ImageView playIcon;
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
            hasServiceState = true;

            long millisPlayed = intent.getLongExtra(RadioService.EXTRA_MILLIS_PLAYED, 0L);
            long dailyGoal = intent.getLongExtra(RadioService.EXTRA_DAILY_GOAL, 0L);
            if (dailyGoal <= 0) dailyGoal = preferenceHelper.getDailyGoal();
            boolean goalReached = intent.getBooleanExtra(RadioService.EXTRA_GOAL_REACHED, false);
            String dateKey = intent.getStringExtra(RadioService.EXTRA_DATE_KEY);
            boolean isPlaying = intent.getBooleanExtra(RadioService.EXTRA_IS_PLAYING, false);
            String playerState = intent.getStringExtra(RadioService.EXTRA_PLAYER_STATE);
            String errorCode = intent.getStringExtra(RadioService.EXTRA_ERROR_CODE);
            String stationUrl = intent.getStringExtra(RadioService.EXTRA_STATION_URL);
            String stationName = intent.getStringExtra(RadioService.EXTRA_STATION_NAME);

            if (stationName != null) stationNameTop.setText(stationName);
            showProgress(millisPlayed, dailyGoal, goalReached);
            showPlayerState(playerState, errorCode, isPlaying);
            stationAdapter.setPlaybackState(stationUrl, isPlaying, playerState);

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

        requestNotificationPermission();
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
        } else {
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

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.perm_notif_title)
                            .setMessage(R.string.perm_notif_message)
                            .setPositiveButton(R.string.perm_notif_allow, (dialog, which) ->
                                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                            PERMISSION_REQUEST_CODE))
                            .setNegativeButton(R.string.perm_notif_cancel, null)
                            .show();
                } else {
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (!granted) {
                Toast.makeText(this, R.string.perm_notif_denied, Toast.LENGTH_LONG).show();
            }
        }
    }
}
