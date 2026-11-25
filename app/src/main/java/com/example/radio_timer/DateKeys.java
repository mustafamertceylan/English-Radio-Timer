package com.example.radio_timer;

import java.util.Calendar;
import java.util.Locale;

/** Tarih anahtarları (Locale.US, cihaz saat dilimi). */
public final class DateKeys {
    private DateKeys() {}

    /** "yyyy-MM-dd". */
    public static String today() {
        Calendar c = Calendar.getInstance();
        return of(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    /** "yyyy-MM". */
    public static String currentMonth() {
        Calendar c = Calendar.getInstance();
        return monthKey(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1);
    }

    /** "yyyy-MM-dd". */
    public static String of(int year, int month1to12, int day) {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month1to12, day);
    }

    /** "yyyy-MM". */
    public static String monthKey(int year, int month1to12) {
        return String.format(Locale.US, "%04d-%02d", year, month1to12);
    }

    /** "2026-03-01" ⇒ "2026-02-28". */
    public static String previousDay(String dateKey) {
        int[] p = parse(dateKey);
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(p[0], p[1] - 1, p[2]);
        c.add(Calendar.DAY_OF_MONTH, -1);
        return of(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    /** {year, month1to12, day}. */
    public static int[] parse(String dateKey) {
        String[] parts = dateKey.split("-");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Geçersiz tarih anahtarı: " + dateKey);
        }
        return new int[]{
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
        };
    }
}
