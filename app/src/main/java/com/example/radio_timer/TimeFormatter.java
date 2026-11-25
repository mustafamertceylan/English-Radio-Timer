package com.example.radio_timer;

import java.util.Locale;

/** Süre biçimlendirme. Birim kısaltmaları Türkçe ve sınıf içinde sabittir. */
public final class TimeFormatter {
    private static final String UNIT_HOUR = "sa";
    private static final String UNIT_MINUTE = "dk";
    private static final String UNIT_SECOND = "sn";

    private TimeFormatter() {}

    /** "2 sa 5 dk 9 sn" | "5 dk 9 sn" | "9 sn"; 0 ⇒ "0 sn". */
    public static String formatShort(long millis) {
        long totalSec = Math.max(0, millis) / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) {
            sb.append(h).append(' ').append(UNIT_HOUR).append(' ')
              .append(m).append(' ').append(UNIT_MINUTE).append(' ');
        } else if (m > 0) {
            sb.append(m).append(' ').append(UNIT_MINUTE).append(' ');
        }
        sb.append(s).append(' ').append(UNIT_SECOND);
        return sb.toString();
    }

    /** Saniyesiz: "4 sa" | "2 sa 30 dk" | "45 dk" | 1 dk altı ⇒ "0 dk". */
    public static String formatGoal(long millis) {
        long totalMin = Math.max(0, millis) / 60_000L;
        long h = totalMin / 60;
        long m = totalMin % 60;
        if (h == 0) return m + " " + UNIT_MINUTE;
        if (m == 0) return h + " " + UNIT_HOUR;
        return h + " " + UNIT_HOUR + " " + m + " " + UNIT_MINUTE;
    }

    /** "02:05:09" (Locale.US). */
    public static String formatClock(long millis) {
        long totalSec = Math.max(0, millis) / 1000;
        return String.format(Locale.US, "%02d:%02d:%02d",
                totalSec / 3600, (totalSec % 3600) / 60, totalSec % 60);
    }
}
