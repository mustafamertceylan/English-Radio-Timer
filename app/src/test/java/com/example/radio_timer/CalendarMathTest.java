package com.example.radio_timer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class CalendarMathTest {

    private static int leadingNulls(List<String> cells) {
        int n = 0;
        while (n < cells.size() && cells.get(n) == null) n++;
        return n;
    }

    private static int trailingNulls(List<String> cells) {
        int n = 0;
        while (n < cells.size() && cells.get(cells.size() - 1 - n) == null) n++;
        return n;
    }

    @Test
    public void november2026_startsSunday_sixLeadingBlanks() {
        List<String> cells = CalendarMath.monthCells(2026, 11);
        assertEquals(42, cells.size());
        assertEquals(6, leadingNulls(cells));
        assertEquals("2026-11-01", cells.get(6));
        assertEquals("2026-11-30", cells.get(35));
        assertEquals(6, trailingNulls(cells));
    }

    @Test
    public void june2026_startsMonday_noLeadingBlanks() {
        List<String> cells = CalendarMath.monthCells(2026, 6);
        assertEquals(0, leadingNulls(cells));
        assertEquals("2026-06-01", cells.get(0));
        assertEquals(35, cells.size());
        assertEquals(5, trailingNulls(cells));
    }

    @Test
    public void february2026_exactlyFourWeeks() {
        // 1 Şubat 2026 Pazar ⇒ 6 boşluk + 28 gün + 1 = 35
        List<String> cells = CalendarMath.monthCells(2026, 2);
        assertEquals(28, CalendarMath.daysInMonth(2026, 2));
        assertEquals(6, leadingNulls(cells));
        assertEquals("2026-02-28", cells.get(33));
        assertNull(cells.get(34));
    }

    @Test
    public void february2021_startsMonday_28cells() {
        List<String> cells = CalendarMath.monthCells(2021, 2);
        assertEquals(28, cells.size());
        assertEquals("2021-02-01", cells.get(0));
        assertEquals("2021-02-28", cells.get(27));
    }

    @Test
    public void daysInMonth_leapAndNonLeap() {
        assertEquals(29, CalendarMath.daysInMonth(2024, 2));
        assertEquals(28, CalendarMath.daysInMonth(2026, 2));
        assertEquals(28, CalendarMath.daysInMonth(1900, 2));
        assertEquals(29, CalendarMath.daysInMonth(2000, 2));
        assertEquals(31, CalendarMath.daysInMonth(2026, 1));
        assertEquals(30, CalendarMath.daysInMonth(2026, 4));
        assertEquals(31, CalendarMath.daysInMonth(2026, 12));
    }

    @Test
    public void february2024_has29Days() {
        List<String> cells = CalendarMath.monthCells(2024, 2);
        assertTrue(cells.contains("2024-02-29"));
        assertEquals(3, leadingNulls(cells)); // 1 Şubat 2024 Perşembe
    }

    @Test
    public void lengthIsMultipleOfSeven_andDaysAreSequential() {
        for (int year = 2023; year <= 2027; year++) {
            for (int month = 1; month <= 12; month++) {
                List<String> cells = CalendarMath.monthCells(year, month);
                assertEquals(0, cells.size() % 7);
                assertTrue(cells.size() >= 28 && cells.size() <= 42);
                int lead = leadingNulls(cells);
                assertTrue(lead < 7);
                int days = CalendarMath.daysInMonth(year, month);
                for (int d = 1; d <= days; d++) {
                    assertEquals(DateKeys.of(year, month, d), cells.get(lead + d - 1));
                }
                assertTrue(trailingNulls(cells) < 7);
            }
        }
    }

    @Test
    public void addMonths_yearBoundaries() {
        assertArrayEquals(new int[]{2027, 1}, CalendarMath.addMonths(2026, 12, 1));
        assertArrayEquals(new int[]{2025, 12}, CalendarMath.addMonths(2026, 1, -1));
        assertArrayEquals(new int[]{2027, 12}, CalendarMath.addMonths(2026, 11, 13));
        assertArrayEquals(new int[]{2025, 10}, CalendarMath.addMonths(2026, 11, -13));
        assertArrayEquals(new int[]{2026, 11}, CalendarMath.addMonths(2026, 11, 0));
        assertArrayEquals(new int[]{2024, 1}, CalendarMath.addMonths(2026, 1, -24));
    }

    @Test
    public void currentYearMonth_isValid() {
        int[] ym = CalendarMath.currentYearMonth();
        assertEquals(2, ym.length);
        assertTrue(ym[1] >= 1 && ym[1] <= 12);
        assertEquals(DateKeys.currentMonth(), DateKeys.monthKey(ym[0], ym[1]));
    }
}
