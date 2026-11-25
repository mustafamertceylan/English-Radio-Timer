package com.example.radio_timer;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private static final Locale LOCALE_TR = new Locale("tr", "TR");

    private MaterialCalendarView calendarView;
    private TextView calendarHeaderTitle;
    private TextView monthlyTotalText;
    private TextView monthlySubtitle;
    private TextView selectedDayText;

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

        calendarView = findViewById(R.id.calendarView);
        calendarHeaderTitle = findViewById(R.id.calendarHeaderTitle);
        monthlyTotalText = findViewById(R.id.monthlyTotalTime);
        monthlySubtitle = findViewById(R.id.monthlySubtitle);
        selectedDayText = findViewById(R.id.selectedDayText);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        calendarView.setOnMonthChangedListener((widget, date) ->
                loadMonth(date.getYear(), date.getMonth()));
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            if (selected) showDayDetail(date);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        CalendarDay current = calendarView.getCurrentDate();
        loadMonth(current.getYear(), current.getMonth());
        CalendarDay selected = calendarView.getSelectedDate();
        if (selected != null) showDayDetail(selected);
    }

    /** @param month1to12 material-calendarview 2.x'te ay 1 tabanlıdır. */
    private void loadMonth(int year, int month1to12) {
        final String monthKey = DateKeys.monthKey(year, month1to12);
        displayedMonthKey = monthKey;

        String monthName = formatDate(year, month1to12, 1, "LLLL");
        calendarHeaderTitle.setText(formatDate(year, month1to12, 1, "LLLL yyyy"));
        monthlySubtitle.setText(getString(R.string.calendar_month_subtitle, monthName));

        final String today = DateKeys.today();
        final Context appContext = getApplicationContext();
        AppExecutors.diskIO().execute(() -> {
            ListeningDao dao = AppDatabase.getInstance(appContext).listeningDao();
            List<ListeningLog> logs = dao.getLogsForMonth(monthKey);
            long monthlyTotalMillis = dao.getTotalTimeForMonth(monthKey);

            List<CalendarDay> reachedDays = new ArrayList<>();
            List<CalendarDay> missedDays = new ArrayList<>();
            for (ListeningLog log : logs) {
                int[] ymd = DateKeys.parse(log.date);
                CalendarDay day = CalendarDay.from(ymd[0], ymd[1], ymd[2]);
                if (log.isGoalReached) {
                    reachedDays.add(day);
                } else if (!log.date.equals(today)) {
                    // Bugün henüz bitmedi: hedef dolmadıysa kırmızı boyanmaz (H13).
                    missedDays.add(day);
                }
            }

            runOnUiThread(() -> {
                if (isFinishing() || !monthKey.equals(displayedMonthKey)) return;
                monthlyTotalText.setText(getString(R.string.calendar_month_total,
                        TimeFormatter.formatGoal(monthlyTotalMillis)));
                calendarView.removeDecorators();
                int dayTextColor = ContextCompat.getColor(this, R.color.on_accent);
                calendarView.addDecorator(new FilledDayDecorator(
                        ContextCompat.getDrawable(this, R.drawable.circle_filled_green), dayTextColor, reachedDays));
                calendarView.addDecorator(new FilledDayDecorator(
                        ContextCompat.getDrawable(this, R.drawable.circle_filled_red), dayTextColor, missedDays));
            });
        });
    }

    private void showDayDetail(@NonNull CalendarDay day) {
        final String dateKey = DateKeys.of(day.getYear(), day.getMonth(), day.getDay());
        final String label = formatDate(day.getYear(), day.getMonth(), day.getDay(), "d MMMM yyyy");
        final int seq = ++dayRequestSeq;
        final Context appContext = getApplicationContext();
        AppExecutors.diskIO().execute(() -> {
            ListeningLog log = AppDatabase.getInstance(appContext).listeningDao().getLogByDate(dateKey);
            final long duration = log != null ? log.durationMillis : 0L;
            final boolean reached = log != null && log.isGoalReached;
            final boolean isToday = dateKey.equals(DateKeys.today());
            runOnUiThread(() -> {
                if (isFinishing() || seq != dayRequestSeq) return;
                if (log == null || (duration <= 0 && !reached)) {
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

    /** API 24 uyumlu java.text tarih biçimleme (Türkçe ay/gün adları). */
    private static String formatDate(int year, int month1to12, int day, String pattern) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(year, month1to12 - 1, day);
        return new SimpleDateFormat(pattern, LOCALE_TR).format(cal.getTime());
    }

    private static class FilledDayDecorator implements DayViewDecorator {
        private final Drawable drawable;
        private final int textColor;
        private final HashSet<CalendarDay> dates;

        FilledDayDecorator(Drawable drawable, int textColor, Collection<CalendarDay> dates) {
            this.drawable = drawable;
            this.textColor = textColor;
            this.dates = new HashSet<>(dates);
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return dates.contains(day);
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.setBackgroundDrawable(drawable);
            view.addSpan(new ForegroundColorSpan(textColor));
        }
    }
}
