package com.example.radio_timer;

/** Değişmez istasyon modeli. */
public final class Station {
    public static final String ACCENT_UK = "UK";
    public static final String ACCENT_US = "US";
    public static final String ACCENT_CA = "CA";

    private final String name;
    private final String url;
    private final String accent;

    public Station(String name, String url, String accent) {
        this.name = name;
        this.url = url;
        this.accent = accent;
    }

    public String getName() { return name; }
    public String getUrl() { return url; }
    /** {@link #ACCENT_UK}, {@link #ACCENT_US} veya {@link #ACCENT_CA}. */
    public String getAccent() { return accent; }
}
