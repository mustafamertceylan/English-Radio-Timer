package com.example.radio_timer;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private EditText goalInput;
    private Button saveButton;
    private PreferenceHelper preferenceHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // PreferenceHelper'ı başlat
        preferenceHelper = new PreferenceHelper(this);

        // View'ları bağla
        goalInput = findViewById(R.id.goalInput);
        saveButton = findViewById(R.id.saveButton);

        // Mevcut hedefi göster (varsayılan 4 saat)
        float currentGoalHours = preferenceHelper.getDailyGoalInHours();
        goalInput.setText(String.valueOf(currentGoalHours));

        // Geri butonu
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Kaydet butonu
        saveButton.setOnClickListener(v -> saveGoal());
    }

    private void saveGoal() {
        String inputText = goalInput.getText().toString().trim();
        if (inputText.isEmpty()) {
            Toast.makeText(this, "Lütfen bir hedef girin", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            float hours = Float.parseFloat(inputText);
            if (hours <= 0) {
                Toast.makeText(this, "Hedef 0'dan büyük olmalıdır", Toast.LENGTH_SHORT).show();
                return;
            }
            if (hours > 24) {
                Toast.makeText(this, "Hedef 24 saatten fazla olamaz", Toast.LENGTH_SHORT).show();
                return;
            }

            // Hedefi kaydet
            preferenceHelper.setDailyGoalInHours(hours);

            Toast.makeText(this, "Hedef kaydedildi: " + hours + " saat", Toast.LENGTH_SHORT).show();

            // Activity'yi kapat
            finish();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Geçersiz sayı formatı", Toast.LENGTH_SHORT).show();
        }
    }
}