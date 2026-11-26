package com.example.radio_timer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DateKeysTest {

    @Test
    public void previousDay_sameMonth() {
        assertEquals("2026-03-14", DateKeys.previousDay("2026-03-15"));
    }

    @Test
    public void previousDay_monthBoundary() {
        assertEquals("2026-02-28", DateKeys.previousDay("2026-03-01"));
        assertEquals("2026-04-30", DateKeys.previousDay("2026-05-01"));
    }

    @Test
    public void previousDay_leapYear() {
        assertEquals("2024-02-29", DateKeys.previousDay("2024-03-01"));
    }

    @Test
    public void previousDay_yearBoundary() {
        assertEquals("2025-12-31", DateKeys.previousDay("2026-01-01"));
    }

    @Test
    public void of_padsWithZeros() {
        assertEquals("2026-03-05", DateKeys.of(2026, 3, 5));
        assertEquals("2026-03", DateKeys.monthKey(2026, 3));
    }

    @Test
    public void ofParse_roundTrip() {
        int[] parts = DateKeys.parse(DateKeys.of(2026, 11, 25));
        assertArrayEquals(new int[]{2026, 11, 25}, parts);
        assertEquals("2024-02-29", DateKeys.of(parts[0] - 2, 2, 29));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_invalid() {
        DateKeys.parse("2026-03");
    }

    @Test
    public void today_and_currentMonth_format() {
        assertTrue(DateKeys.today().matches("\\d{4}-\\d{2}-\\d{2}"));
        assertTrue(DateKeys.currentMonth().matches("\\d{4}-\\d{2}"));
        assertTrue(DateKeys.today().startsWith(DateKeys.currentMonth()));
    }
}
