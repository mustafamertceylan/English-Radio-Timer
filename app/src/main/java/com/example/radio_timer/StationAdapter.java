package com.example.radio_timer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Objects;

public class StationAdapter extends RecyclerView.Adapter<StationAdapter.StationViewHolder> {

    private final List<Station> stations;
    private final OnStationClickListener listener;

    @Nullable private String currentUrl;
    private boolean isPlaying;
    @Nullable private String playerState;

    public interface OnStationClickListener {
        void onStationClick(Station station);
    }

    public StationAdapter(List<Station> stations, OnStationClickListener listener) {
        this.stations = stations;
        this.listener = listener;
    }

    /**
     * Servisten gelen duruma göre satırları günceller. Broadcast saniyede bir geldiği için
     * yalnızca değer değiştiğinde yeniden çizilir.
     *
     * @param url         aktif/son istasyon URL'si
     * @param playing     RadioService.EXTRA_IS_PLAYING
     * @param state       RadioService.STATE_* değerlerinden biri
     */
    public void setPlaybackState(@Nullable String url, boolean playing, @Nullable String state) {
        if (Objects.equals(currentUrl, url) && isPlaying == playing
                && Objects.equals(playerState, state)) {
            return;
        }
        currentUrl = url;
        isPlaying = playing;
        playerState = state;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_station, parent, false);
        return new StationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StationViewHolder holder, int position) {
        Station station = stations.get(position);
        holder.stationName.setText(station.getName());
        holder.stationAccent.setText(accentLabel(station.getAccent()));

        boolean isCurrent = station.getUrl().equals(currentUrl);
        boolean showPause = isCurrent && isPlaying;
        holder.stationPlayIcon.setImageResource(showPause
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play);
        holder.stationPlayIcon.setContentDescription(holder.itemView.getContext()
                .getString(showPause ? R.string.cd_pause : R.string.cd_play));

        boolean connecting = isCurrent && RadioService.STATE_CONNECTING.equals(playerState);
        boolean buffering = isCurrent && RadioService.STATE_BUFFERING.equals(playerState);
        boolean error = isCurrent && RadioService.STATE_ERROR.equals(playerState);
        Context ctx = holder.itemView.getContext();
        holder.stationTimer.setTextColor(ContextCompat.getColor(ctx, error ? R.color.error : R.color.accent));
        if (error) {
            holder.stationProgress.setVisibility(View.GONE);
            holder.stationTimer.setText(R.string.station_row_error);
        } else if (connecting || buffering) {
            holder.stationProgress.setVisibility(View.VISIBLE);
            holder.stationTimer.setText(connecting ? R.string.state_connecting : R.string.state_buffering);
        } else if (isCurrent && RadioService.STATE_PLAYING.equals(playerState)) {
            holder.stationProgress.setVisibility(View.GONE);
            holder.stationTimer.setText(R.string.state_playing);
        } else {
            holder.stationProgress.setVisibility(View.GONE);
            holder.stationTimer.setText("");
        }

        holder.itemView.setOnClickListener(v -> listener.onStationClick(station));
    }

    @StringRes
    private static int accentLabel(String accent) {
        if (Station.ACCENT_US.equals(accent)) return R.string.accent_us;
        if (Station.ACCENT_CA.equals(accent)) return R.string.accent_ca;
        return R.string.accent_uk;
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    static class StationViewHolder extends RecyclerView.ViewHolder {
        final ImageView stationPlayIcon;
        final TextView stationName;
        final TextView stationAccent;
        final TextView stationTimer;
        final ProgressBar stationProgress;

        StationViewHolder(@NonNull View itemView) {
            super(itemView);
            stationPlayIcon = itemView.findViewById(R.id.stationPlayIcon);
            stationName = itemView.findViewById(R.id.stationName);
            stationAccent = itemView.findViewById(R.id.stationAccent);
            stationTimer = itemView.findViewById(R.id.stationTimer);
            stationProgress = itemView.findViewById(R.id.stationProgress);
        }
    }
}
