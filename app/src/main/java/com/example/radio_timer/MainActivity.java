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
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView stationRecyclerView;
    private StationAdapter stationAdapter;
    private List<Station> stationList;
    private TextView stationNameTop;
    private TextView percentageText; // Tek bir yüzde text'i
    private static final int PERMISSION_REQUEST_CODE = 100;
    private ImageView playIcon;
    private TextView totalDailyTime;
    private ProgressBar goalProgressBar;
    private CardView dateCard;

    private String currentStationUrl = "https://stream.live.vc.bbcmedia.co.uk/bbc_world_service";
    private String currentStationName = "BBC World Service";

    private BroadcastReceiver timerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(RadioService.TIMER_UPDATE)) {
                long millisPlayed = intent.getLongExtra("millis_played", 0);
                boolean isPlaying = intent.getBooleanExtra("is_playing", false);
                long dailyGoal = new PreferenceHelper(MainActivity.this).getDailyGoal();

                String stationUrl = intent.getStringExtra("station_url");
                String stationName = intent.getStringExtra("station_name");
                if (stationUrl != null && stationName != null) {
                    currentStationUrl = stationUrl;
                    currentStationName = stationName;
                    stationAdapter.setCurrentPlaying(stationUrl, isPlaying);
                    stationNameTop.setText(stationName);
                }

                String formattedTime = formatToShortTime(millisPlayed);
                totalDailyTime.setText(formattedTime + " / " + formatToShortTime(dailyGoal));
                int progressPercent = (int) ((millisPlayed * 100) / dailyGoal);
                goalProgressBar.setProgress(progressPercent);
                percentageText.setText("%" + progressPercent + " tamamlandı");

                int iconRes = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
                playIcon.setImageResource(iconRes);
            }
        }
    };

    private String formatToShortTime(long millis) {
        long hours = millis / 3600000;
        long minutes = (millis % 3600000) / 60000;
        long seconds = (millis % 60000) / 1000;
        return hours + "s " + minutes + "d " + seconds + "sn";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // View'ları bağla
        stationNameTop = findViewById(R.id.stationNameTop);
        stationRecyclerView = findViewById(R.id.stationRecyclerView);
        percentageText = findViewById(R.id.percentageText);
        playIcon = findViewById(R.id.playIcon);
        totalDailyTime = findViewById(R.id.totalDailyTime);
        goalProgressBar = findViewById(R.id.goalProgressBar);
        dateCard = findViewById(R.id.dateCard);
        CardView bbcCard = findViewById(R.id.bbcCard);

        // İstasyon listesini oluştur
        stationList = Arrays.asList(
                new Station("BBC World Service", "https://stream.live.vc.bbcmedia.co.uk/bbc_world_service"),
                new Station("NPR", "https://npr-ice.streamguys1.com/npr-mp3"),
                new Station("CBC Radio One", "https://cbc.m3u8") // Geçerli bir URL girin
        );

        stationRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        stationAdapter = new StationAdapter(stationList, station -> {
            Intent serviceIntent = new Intent(MainActivity.this, RadioService.class);
            serviceIntent.setAction(RadioService.ACTION_PLAY_STATION);
            serviceIntent.putExtra("station_url", station.getUrl());
            serviceIntent.putExtra("station_name", station.getName());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        });
        stationRecyclerView.setAdapter(stationAdapter);

        // System bars padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Ayarlar ikonu
        ImageView settingsIcon = findViewById(R.id.settingsIcon);
        settingsIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        requestNotificationPermission();

        // Takvim geçişi
        dateCard.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CalendarActivity.class);
            startActivity(intent);
        });

        // Üstteki büyük karta tıklanınca mevcut istasyonu toggle et
        bbcCard.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(this, RadioService.class);
            serviceIntent.setAction(RadioService.ACTION_TOGGLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        });

        loadTodayProgress();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(RadioService.TIMER_UPDATE);
        registerReceiver(timerReceiver, filter, Context.RECEIVER_EXPORTED);
        loadTodayProgress();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(timerReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void loadTodayProgress() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        long dailyGoal = new PreferenceHelper(this).getDailyGoal();

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            ListeningLog log = db.listeningDao().getLogByDate(today);

            if (log != null) {
                int progressPercent = (int) ((log.durationMillis * 100) / dailyGoal);

                runOnUiThread(() -> {
                    String formatted = formatToShortTime(log.durationMillis);
                    totalDailyTime.setText(formatted + " / " + formatToShortTime(dailyGoal));
                    goalProgressBar.setProgress(progressPercent);
                    percentageText.setText("%" + progressPercent + " tamamlandı");
                });
            }
        }).start();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    new AlertDialog.Builder(this)
                            .setTitle("Bildirim İzni Gerekli")
                            .setMessage("Radyo çalarken sizi bilgilendirebilmemiz için bildirim iznine ihtiyacımız var.")
                            .setPositiveButton("İzin Ver", (dialog, which) -> {
                                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
                            })
                            .setNegativeButton("Vazgeç", null)
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
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // İzin verildi
            } else {
                Toast.makeText(this, "Bildirim izni reddedildi. Radyo çalarken bildirim gösterilemeyecek.", Toast.LENGTH_LONG).show();
            }
        }
    }
}