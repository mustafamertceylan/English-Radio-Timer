package com.example.radio_timer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Ay ızgarası hesapları (saf, birim testli). java.time kullanılmaz (minSdk 24).
 * Izgara Pazartesi başlangıçlıdır.
 */
public final class CalendarMath {
    private CalendarMath() {}

    public static int daysInMonth(int year, int month1to12) {
        GregorianCalendar c = new GregorianCalendar(year, month1to12 - 1, 1);
        return c.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    /**
     * Pazartesi başlangıçlı ızgara hücreleri: ayın 1'inden önceki boşluklar null, ardından
     * "yyyy-MM-dd" günler, son satır null ile 7'ye tamamlanır. Uzunluk 7'nin katı (28..42).
     */
    public static List<String> monthCells(int year, int month1to12) {
        GregorianCalendar c = new GregorianCalendar(year, month1to12 - 1, 1);
        // Calendar.MONDAY=2 ... SUNDAY=1  ⇒  Pazartesi=0 ... Pazar=6
        int leading = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        int days = c.getActualMaximum(Calendar.DAY_OF_MONTH);

        List<String> cells = new ArrayList<>(42);
        for (int i = 0; i < leading; i++) cells.add(null);
        for (int d = 1; d <= days; d++) cells.add(DateKeys.of(year, month1to12, d));
        while (cells.size() % 7 != 0) cells.add(null);
        return cells;
    }

    /** {year, month1to12}; delta negatif olabilir. */
    public static int[] addMonths(int year, int month1to12, int delta) {
        int index = year * 12 + (month1to12 - 1) + delta;
        int y = Math.floorDiv(index, 12);
        int m = Math.floorMod(index, 12) + 1;
        return new int[]{y, m};
    }

    /** {year, month1to12}, cihaz saat diliminde. */
    public static int[] currentYearMonth() {
        Calendar c = Calendar.getInstance();
        return new int[]{c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1};
    }
}
