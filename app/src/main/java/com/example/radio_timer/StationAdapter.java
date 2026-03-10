package com.example.radio_timer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StationAdapter extends RecyclerView.Adapter<StationAdapter.StationViewHolder> {

    private List<Station> stations;
    private OnStationClickListener listener;
    private String currentPlayingUrl;
    private boolean isPlaying;

    public interface OnStationClickListener {
        void onStationClick(Station station);
    }

    public StationAdapter(List<Station> stations, OnStationClickListener listener) {
        this.stations = stations;
        this.listener = listener;
    }

    public void setCurrentPlaying(String url, boolean playing) {
        currentPlayingUrl = url;
        isPlaying = playing;
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
        holder.stationTimer.setText(""); // İsterseniz buraya istasyonun anlık süresi eklenebilir

        // Çalma durumuna göre ikon belirle
        if (station.getUrl().equals(currentPlayingUrl) && isPlaying) {
            holder.stationPlayIcon.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            holder.stationPlayIcon.setImageResource(android.R.drawable.ic_media_play);
        }

        holder.itemView.setOnClickListener(v -> listener.onStationClick(station));
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    static class StationViewHolder extends RecyclerView.ViewHolder {
        ImageView stationPlayIcon;
        TextView stationName;
        TextView stationTimer;

        StationViewHolder(@NonNull View itemView) {
            super(itemView);
            stationPlayIcon = itemView.findViewById(R.id.stationPlayIcon);
            stationName = itemView.findViewById(R.id.stationName);
            stationTimer = itemView.findViewById(R.id.stationTimer);
        }
    }
}