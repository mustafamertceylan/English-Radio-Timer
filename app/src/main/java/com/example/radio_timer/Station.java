package com.example.radio_timer;

public class Station {
    private String name;
    private String url;

    public Station(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() { return name; }
    public String getUrl() { return url; }
}