package com.example.radio_timer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Takvim ay ızgarası (7 sütun, Pazartesi başlangıç). Hücreler {@link CalendarMath#monthCells}
 * çıktısıdır: null = boş hücre, aksi halde "yyyy-MM-dd".
 *
 * Boyama: isGoalReached ⇒ yeşil daire; geçmiş gün ve ulaşılmadı ⇒ kırmızı daire;
 * bugün ve ulaşılmadı ⇒ yalnız halka; gelecek ⇒ soluk metin.
 */
public class MonthGridAdapter extends RecyclerView.Adapter<MonthGridAdapter.DayViewHolder> {

    public interface OnDayClickListener {
        void onDayClick(String dateKey);
    }

    private final OnDayClickListener listener;
    private final String[] monthNames;

    private List<String> cells = new ArrayList<>();
    /** Kaydı olan günler ⇒ isGoalReached. */
    private Map<String, Boolean> goalByDate = Collections.emptyMap();
    private String todayKey = "";
    @Nullable private String selectedKey;

    public MonthGridAdapter(@NonNull Context context, @NonNull OnDayClickListener listener) {
        this.listener = listener;
        this.monthNames = context.getResources().getStringArray(R.array.calendar_month_names);
    }

    /** Yeni ayın hücrelerini gösterir; önceki ayın boyamasını temizler. */
    public void setMonth(@NonNull List<String> cells, @NonNull String todayKey) {
        this.cells = cells;
        this.todayKey = todayKey;
        this.goalByDate = Collections.emptyMap();
        notifyDataSetChanged();
    }

    public void setGoalStatus(@NonNull Map<String, Boolean> goalByDate) {
        this.goalByDate = goalByDate;
        notifyDataSetChanged();
    }

    public void setSelected(@Nullable String dateKey) {
        int old = cells.indexOf(selectedKey);
        selectedKey = dateKey;
        if (old >= 0) notifyItemChanged(old);
        int now = cells.indexOf(dateKey);
        if (now >= 0) notifyItemChanged(now);
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        String key = cells.get(position);
        Context ctx = holder.itemView.getContext();

        if (key == null) {
            holder.dayNumber.setText("");
            holder.dayNumber.setBackground(null);
            holder.itemView.setBackground(null);
            holder.itemView.setOnClickListener(null);
            holder.itemView.setClickable(false);
            holder.itemView.setFocusable(false);
            holder.itemView.setContentDescription(null);
            holder.itemView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            return;
        }

        int[] ymd = DateKeys.parse(key);
        holder.dayNumber.setText(String.valueOf(ymd[2]));
        holder.itemView.setContentDescription(ymd[2] + " " + monthNames[ymd[1] - 1] + " " + ymd[0]);
        holder.itemView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);

        int cmp = key.compareTo(todayKey);
        boolean isToday = cmp == 0;
        boolean isFuture = cmp > 0;
        Boolean reached = goalByDate.get(key);

        int textColor;
        if (Boolean.TRUE.equals(reached)) {
            holder.dayNumber.setBackgroundResource(R.drawable.circle_filled_green);
            textColor = R.color.on_accent;
        } else if (isToday) {
            holder.dayNumber.setBackgroundResource(R.drawable.bg_day_today);
            textColor = R.color.text_primary;
        } else if (reached != null && !isFuture) {
            holder.dayNumber.setBackgroundResource(R.drawable.circle_filled_red);
            textColor = R.color.on_accent;
        } else {
            holder.dayNumber.setBackground(null);
            textColor = isFuture ? R.color.text_tertiary : R.color.text_primary;
        }
        holder.dayNumber.setTextColor(ContextCompat.getColor(ctx, textColor));
        holder.dayNumber.setAlpha(isFuture ? 0.5f : 1f);

        boolean selected = key.equals(selectedKey);
        if (selected) {
            holder.itemView.setBackgroundResource(R.drawable.bg_day_selected);
        } else {
            holder.itemView.setBackground(null);
        }
        holder.itemView.setSelected(selected);

        holder.itemView.setClickable(true);
        holder.itemView.setFocusable(true);
        holder.itemView.setOnClickListener(v -> listener.onDayClick(key));
    }

    @Override
    public int getItemCount() {
        return cells.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        final TextView dayNumber;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            dayNumber = itemView.findViewById(R.id.dayNumber);
        }
    }
}
