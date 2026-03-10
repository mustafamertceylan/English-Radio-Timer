package com.example.radio_timer;
import java.util.Date;
import java.util.Locale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.content.IntentFilter;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;

import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import android.app.AlertDialog;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private TextView goalPercentage;

    private TextView percentageText;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private ImageView playIcon;      // Üstteki yeşil kartın ikonu
    private ImageView playIconList;  // Alttaki istasyon listesinin ikonu
    private TextView totalDailyTime, bbcTimer;
    private ProgressBar goalProgressBar;
    private CardView dateCard;


    // Hedef süre: 4 saat (milisaniye cinsinden: 4 * 60 * 60 * 1000)

    private boolean isTimerRunning = false;

    // BBC World Service Canlı Yayın Linki
    private final String RADIO_URL = "http://stream.live.vc.bbcmedia.co.uk/bbc_world_service";
    private BroadcastReceiver timerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(RadioService.TIMER_UPDATE)) {
                long millisPlayed = intent.getLongExtra("millis_played", 0);
                boolean isPlaying = intent.getBooleanExtra("is_playing", false);
                long dailyGoal = new PreferenceHelper(MainActivity.this).getDailyGoal();

                String formattedTime = formatToShortTime(millisPlayed);
                totalDailyTime.setText(formattedTime + " / " + formatToShortTime(dailyGoal));
                bbcTimer.setText(formattedTime);

                // Yüzde hesapla ve güncelle
                int progressPercent = (int) ((millisPlayed * 100) / dailyGoal);
                goalProgressBar.setProgress(progressPercent);
                goalPercentage.setText("%" + progressPercent);
                percentageText.setText("%" + progressPercent + " tamamlandı");

                int iconRes = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
                playIcon.setImageResource(iconRes);
                playIconList.setImageResource(iconRes);
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
        percentageText = findViewById(R.id.percentageText);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Üst kısım (Şarj göstergesi) için boşluk ver
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            return insets;
        });
        goalPercentage = findViewById(R.id.percentageText);
        // Ayarlar ikonu
        ImageView settingsIcon = findViewById(R.id.settingsIcon);
        settingsIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        requestNotificationPermission();
        // 1. UI Elemanlarını Bağlama
        playIcon = findViewById(R.id.playIcon);
        playIconList = findViewById(R.id.playIconList);
        totalDailyTime = findViewById(R.id.totalDailyTime);
        bbcTimer = findViewById(R.id.bbcTimer);
        goalProgressBar = findViewById(R.id.goalProgressBar);
        dateCard = findViewById(R.id.dateCard);
        CardView bbcCard = findViewById(R.id.bbcCard);
        CardView bbcListItem = findViewById(R.id.bbcStationCard); // XML ID kontrolü

        // 2. Takvim Geçişi
        dateCard.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CalendarActivity.class);
            startActivity(intent);
        });

        // 3. Ortak Radyo Kontrol Görevi (Listener)
        View.OnClickListener toggleAction = v -> {
            Intent serviceIntent = new Intent(this, RadioService.class);
            serviceIntent.setAction("ACTION_TOGGLE");

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        };

        // 4. KRİTİK EKSİK: Görevi butonlara atıyoruz
        bbcCard.setOnClickListener(toggleAction);
        if (bbcListItem != null) {
            bbcListItem.setOnClickListener(toggleAction);
        }

        // 5. KRİTİK EKSİK: Veritabanından bugünün süresini yükle
        // Bu metodun MainActivity içinde tanımlı olduğundan emin ol!
        loadTodayProgress();
    }




    @Override
    protected void onResume() {
        super.onResume();
        // Servisten gelen güncellemeleri dinlemeye başla
        IntentFilter filter = new IntentFilter(RadioService.TIMER_UPDATE);
        registerReceiver(timerReceiver, filter, Context.RECEIVER_EXPORTED);

        // Uygulama açıldığında veritabanındaki eski veriyi yükle (Sıfırlanma çözümü)
        loadTodayProgress();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Uygulama arka plana geçince dinlemeyi bırak (Performans için)
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
                    bbcTimer.setText(formatted);
                    goalProgressBar.setProgress(progressPercent);
                    goalPercentage.setText("%" + progressPercent);
                    percentageText.setText("%" + progressPercent + " tamamlandı");
                });
            }
        }).start();
    }
    private void requestNotificationPermission() {
        // Sadece Android 13 (Tiramisu) ve üstünde izin iste
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Kullanıcıya neden ihtiyacımız olduğunu açıklamamız gerekiyor mu?
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    // Açıklama göster
                    new AlertDialog.Builder(this)
                            .setTitle("Bildirim İzni Gerekli")
                            .setMessage("Radyo çalarken sizi bilgilendirebilmemiz için bildirim iznine ihtiyacımız var.")
                            .setPositiveButton("İzin Ver", (dialog, which) -> {
                                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
                            })
                            .setNegativeButton("Vazgeç", null)
                            .show();
                } else {
                    // Direkt izin iste
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
                // İzin verildi, bir şey yapmaya gerek yok
            } else {
                // İzin verilmedi, kullanıcıya bildirim gösterilmeyeceğini söyle
                Toast.makeText(this, "Bildirim izni reddedildi. Radyo çalarken bildirim gösterilemeyecek.", Toast.LENGTH_LONG).show();
            }
        }
    }
}