package com.example.radio_timer;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** İstasyon listesinin tek kaynağı. */
public final class StationRepository {

    private static final List<Station> STATIONS;

    static {
        List<Station> list = new ArrayList<>();
        list.add(new Station("BBC World Service",
                "https://stream.live.vc.bbcmedia.co.uk/bbc_world_service", Station.ACCENT_UK));
        list.add(new Station("LBC News",
                "https://media-ssl.musicradio.com/LBCNewsUK", Station.ACCENT_UK));
        list.add(new Station("NPR News",
                "https://npr-ice.streamguys1.com/live.mp3", Station.ACCENT_US));
        list.add(new Station("WNYC 93.9 FM",
                "https://fm939.wnyc.org/wnycfm", Station.ACCENT_US));
        list.add(new Station("KEXP 90.3",
                "https://kexp-mp3-128.streamguys1.com/kexp128.mp3", Station.ACCENT_US));
        list.add(new Station("CBC Radio One",
                "https://cbcradiolive.akamaized.net/hls/live/2041057/ES_R1ETR/master.m3u8", Station.ACCENT_CA));
        STATIONS = Collections.unmodifiableList(list);
    }

    private StationRepository() {}

    /** Değiştirilemez liste. */
    public static List<Station> getStations() {
        return STATIONS;
    }

    /** BBC World Service. */
    public static Station getDefault() {
        return STATIONS.get(0);
    }

    @Nullable
    public static Station findByUrl(String url) {
        if (url == null) return null;
        for (Station s : STATIONS) {
            if (s.getUrl().equals(url)) return s;
        }
        return null;
    }
}
