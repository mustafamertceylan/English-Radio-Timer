package com.example.radio_timer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Tüm Room okuma/yazmaları için tek paylaşımlı disk executor'ı. */
public final class AppExecutors {
    private static final ExecutorService DISK_IO = Executors.newSingleThreadExecutor();

    private AppExecutors() {}

    public static ExecutorService diskIO() {
        return DISK_IO;
    }
}
