package com.example.radio_timer;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.chip.ChipGroup;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private static final long MINUTE = 60_000L;
    private static final long HOUR = 3_600_000L;

    /** Chip id'leri ile hedef süreleri (ms) aynı sırada. */
    private static final int[] CHIP_IDS = {
            R.id.chip15m, R.id.chip30m, R.id.chip1h, R.id.chip2h, R.id.chip4h
    };
    private static final long[] CHIP_MILLIS = {
            15 * MINUTE, 30 * MINUTE, HOUR, 2 * HOUR, 4 * HOUR
    };

    private final DecimalFormat hoursFormat =
            new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));

    private EditText goalInput;
    private ChipGroup goalChipGroup;
    private PreferenceHelper preferenceHelper;
    /** Chip seçimi EditText'i doldururken TextWatcher'ın seçimi temizlemesini engeller. */
    private boolean updatingFromChip = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settingsRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        preferenceHelper = new PreferenceHelper(this);

        goalInput = findViewById(R.id.goalInput);
        goalChipGroup = findViewById(R.id.goalChipGroup);
        Button saveButton = findViewById(R.id.saveButton);

        // Mevcut hedefi gereksiz ondalık olmadan göster ("4", "2.5")
        long currentGoal = preferenceHelper.getDailyGoal();
        goalInput.setText(formatHours(currentGoal));
        checkChipFor(currentGoal);

        goalChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty() || updatingFromChip) return;
            int id = checkedIds.get(0);
            for (int i = 0; i < CHIP_IDS.length; i++) {
                if (CHIP_IDS[i] == id) {
                    updatingFromChip = true;
                    goalInput.setText(formatHours(CHIP_MILLIS[i]));
                    goalInput.setSelection(goalInput.getText().length());
                    updatingFromChip = false;
                    // K1: chip dokunuşu anında kaydeder, ekranı kapatmaz.
                    persistGoal(CHIP_MILLIS[i]);
                    break;
                }
            }
        });

        goalInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                if (updatingFromChip) return;
                Long millis = parseMillis(s.toString());
                if (millis == null) {
                    goalChipGroup.clearCheck();
                } else {
                    checkChipFor(millis);
                }
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        saveButton.setOnClickListener(v -> saveGoal());
    }

    private void checkChipFor(long millis) {
        for (int i = 0; i < CHIP_MILLIS.length; i++) {
            if (CHIP_MILLIS[i] == millis) {
                if (goalChipGroup.getCheckedChipId() != CHIP_IDS[i]) {
                    updatingFromChip = true;
                    goalChipGroup.check(CHIP_IDS[i]);
                    updatingFromChip = false;
                }
                return;
            }
        }
        goalChipGroup.clearCheck();
    }

    /** Gösterim: tam sayı ⇒ "4", ondalık ⇒ "2,5" / "0,25". */
    private String formatHours(long millis) {
        return hoursFormat.format(millis / (double) HOUR).replace('.', ',');
    }

    /** Saat cinsinden girişi ms'ye çevirir; virgül de kabul edilir. Geçersizse null. */
    private static Long parseMillis(String raw) {
        String text = raw.trim().replace(',', '.');
        if (text.isEmpty()) return null;
        try {
            double hours = Double.parseDouble(text);
            if (Double.isNaN(hours) || Double.isInfinite(hours)) return null;
            return Math.round(hours * HOUR);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void saveGoal() {
        String inputText = goalInput.getText().toString().trim();
        if (inputText.isEmpty()) {
            Toast.makeText(this, R.string.settings_error_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        Long millis = parseMillis(inputText);
        if (millis == null) {
            Toast.makeText(this, R.string.settings_error_format, Toast.LENGTH_SHORT).show();
            return;
        }
        if (millis < PreferenceHelper.MIN_GOAL_MILLIS) {
            Toast.makeText(this, R.string.settings_error_min, Toast.LENGTH_SHORT).show();
            return;
        }
        if (millis > PreferenceHelper.MAX_GOAL_MILLIS) {
            Toast.makeText(this, R.string.settings_error_max, Toast.LENGTH_SHORT).show();
            return;
        }

        persistGoal(millis);
        finish();
    }

    /** Hedefi kaydeder, bugünkü hedef bayrağını yeniden hesaplatır (Y1) ve Toast gösterir. */
    private void persistGoal(long millis) {
        preferenceHelper.setDailyGoal(millis);
        final Context appContext = getApplicationContext();
        final String today = DateKeys.today();
        AppExecutors.diskIO().execute(() -> StatsHelper.recomputeGoalFlag(appContext, today));
        Toast.makeText(this,
                getString(R.string.settings_saved, TimeFormatter.formatGoal(preferenceHelper.getDailyGoal())),
                Toast.LENGTH_SHORT).show();
    }
}
