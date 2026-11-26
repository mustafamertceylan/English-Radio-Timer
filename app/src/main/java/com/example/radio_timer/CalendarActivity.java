package com.example.radio_timer;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Kendi ay ızgarası (T4): Türkçe ay/gün adları, Pazartesi başlangıç, kütüphanesiz. */
public class CalendarActivity extends AppCompatActivity {

    private static final String STATE_YEAR = "cal_year";
    private static final String STATE_MONTH = "cal_month";
    private static final String STATE_SELECTED = "cal_selected";

    private TextView calendarHeaderTitle;
    private TextView monthlyTotalText;
    private TextView monthlySubtitle;
    private TextView selectedDayText;
    private MonthGridAdapter gridAdapter;
    private String[] monthNames;

    private int year;
    private int month; // 1..12
    @Nullable private String selectedKey;

    /** Ekranda gösterilen ayın anahtarı ("yyyy-MM"); geç gelen eski sonuçları elemek için. */
    private String displayedMonthKey;
    /** Seçili gün isteklerinin sırası; geç gelen eski sonuçları elemek için. */
    private int dayRequestSeq = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        monthNames = getResources().getStringArray(R.array.calendar_month_names);
        calendarHeaderTitle = findViewById(R.id.calendarHeaderTitle);
        monthlyTotalText = findViewById(R.id.monthlyTotalTime);
        monthlySubtitle = findViewById(R.id.monthlySubtitle);
        selectedDayText = findViewById(R.id.selectedDayText);

        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_YEAR)) {
            year = savedInstanceState.getInt(STATE_YEAR);
            month = savedInstanceState.getInt(STATE_MONTH);
            selectedKey = savedInstanceState.getString(STATE_SELECTED);
        } else {
            int[] ym = CalendarMath.currentYearMonth();
            year = ym[0];
            month = ym[1];
        }

        buildWeekdayHeader();

        RecyclerView grid = findViewById(R.id.calendarGrid);
        grid.setLayoutManager(new GridLayoutManager(this, 7));
        grid.setNestedScrollingEnabled(false);
        grid.setItemAnimator(null);
        gridAdapter = new MonthGridAdapter(this, this::onDayClicked);
        grid.setAdapter(gridAdapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnPrevMonth).setOnClickListener(v -> changeMonth(-1));
        findViewById(R.id.btnNextMonth).setOnClickListener(v -> changeMonth(1));
    }

    @Override
    protected void onResume() {
        super.onResume();
        showMonth();
        if (selectedKey != null) showDayDetail(selectedKey);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_YEAR, year);
        outState.putInt(STATE_MONTH, month);
        outState.putString(STATE_SELECTED, selectedKey);
    }

    private void buildWeekdayHeader() {
        LinearLayout header = findViewById(R.id.weekdayHeader);
        header.removeAllViews();
        int color = ContextCompat.getColor(this, R.color.text_secondary);
        for (String name : getResources().getStringArray(R.array.calendar_weekdays_short)) {
            TextView tv = new TextView(this);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            tv.setGravity(Gravity.CENTER);
            tv.setText(name);
            tv.setTextColor(color);
            tv.setTextSize(12);
            header.addView(tv);
        }
    }

    private void changeMonth(int delta) {
        int[] ym = CalendarMath.addMonths(year, month, delta);
        year = ym[0];
        month = ym[1];
        // Önceki ayın gün detayı yeni ayda kalmasın; bekleyen detay sorgusunu da geçersiz kıl.
        selectedKey = null;
        dayRequestSeq++;
        selectedDayText.setText(R.string.calendar_day_prompt);
        showMonth();
    }

    private void showMonth() {
        final String monthKey = DateKeys.monthKey(year, month);
        displayedMonthKey = monthKey;

        String monthName = monthNames[month - 1];
        calendarHeaderTitle.setText(getString(R.string.calendar_header, monthName, year));
        monthlySubtitle.setText(getString(R.string.calendar_month_subtitle, monthName));

        gridAdapter.setMonth(CalendarMath.monthCells(year, month), DateKeys.today());
        gridAdapter.setSelected(selectedKey);

        final Context appContext = getApplicationContext();
        AppExecutors.diskIO().execute(() -> {
            ListeningDao dao = AppDatabase.getInstance(appContext).listeningDao();
            List<ListeningLog> logs = dao.getLogsForMonth(monthKey);
            long monthlyTotalMillis = dao.getTotalTimeForMonth(monthKey);

            Map<String, Boolean> goalByDate = new HashMap<>();
            for (ListeningLog log : logs) {
                if (log.isGoalReached || log.durationMillis > 0) {
                    goalByDate.put(log.date, log.isGoalReached);
                }
            }

            runOnUiThread(() -> {
                if (isFinishing() || !monthKey.equals(displayedMonthKey)) return;
                monthlyTotalText.setText(getString(R.string.calendar_month_total,
                        TimeFormatter.formatGoal(monthlyTotalMillis)));
                gridAdapter.setGoalStatus(goalByDate);
            });
        });
    }

    private void onDayClicked(String dateKey) {
        selectedKey = dateKey;
        gridAdapter.setSelected(dateKey);
        showDayDetail(dateKey);
    }

    private void showDayDetail(@NonNull final String dateKey) {
        int[] ymd = DateKeys.parse(dateKey);
        final String label = ymd[2] + " " + monthNames[ymd[1] - 1] + " " + ymd[0];
        final int seq = ++dayRequestSeq;
        final Context appContext = getApplicationContext();
        AppExecutors.diskIO().execute(() -> {
            ListeningLog log = AppDatabase.getInstance(appContext).listeningDao().getLogByDate(dateKey);
            final long duration = log != null ? log.durationMillis : 0L;
            final boolean reached = log != null && log.isGoalReached;
            final boolean hasRecord = log != null && (duration > 0 || reached);
            final boolean isToday = dateKey.equals(DateKeys.today());
            runOnUiThread(() -> {
                if (isFinishing() || seq != dayRequestSeq) return;
                if (!hasRecord) {
                    selectedDayText.setText(getString(R.string.calendar_day_empty, label));
                    return;
                }
                String time = TimeFormatter.formatShort(duration);
                int res;
                if (reached) {
                    res = R.string.calendar_day_detail_reached;
                } else if (isToday) {
                    res = R.string.calendar_day_detail_today;
                } else {
                    res = R.string.calendar_day_detail_missed;
                }
                selectedDayText.setText(getString(res, label, time));
            });
        });
    }
}
