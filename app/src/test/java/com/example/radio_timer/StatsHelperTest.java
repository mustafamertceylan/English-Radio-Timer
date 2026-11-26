package com.example.radio_timer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class StatsHelperTest {

    @Test
    public void progressPercent_goalZeroOrNegative() {
        assertEquals(0, StatsHelper.progressPercent(1000, 0));
        assertEquals(0, StatsHelper.progressPercent(1000, -1));
    }

    @Test
    public void progressPercent_bounds() {
        assertEquals(0, StatsHelper.progressPercent(0, 1000));
        assertEquals(50, StatsHelper.progressPercent(500, 1000));
        assertEquals(100, StatsHelper.progressPercent(1000, 1000));
        assertEquals(100, StatsHelper.progressPercent(5000, 1000));
    }

    @Test
    public void progressPercent_largeValuesNoOverflow() {
        assertEquals(100, StatsHelper.progressPercent(86_400_000L * 10, 86_400_000L));
    }

    @Test
    public void computeStreak_empty() {
        assertEquals(0, StatsHelper.computeStreak(Collections.<String>emptyList(), "2026-03-10"));
        assertEquals(0, StatsHelper.computeStreak(null, "2026-03-10"));
    }

    @Test
    public void computeStreak_includesToday() {
        assertEquals(3, StatsHelper.computeStreak(
                Arrays.asList("2026-03-10", "2026-03-09", "2026-03-08"), "2026-03-10"));
    }

    @Test
    public void computeStreak_onlyFromYesterday() {
        assertEquals(2, StatsHelper.computeStreak(
                Arrays.asList("2026-03-09", "2026-03-08"), "2026-03-10"));
    }

    @Test
    public void computeStreak_gapStopsCount() {
        assertEquals(2, StatsHelper.computeStreak(
                Arrays.asList("2026-03-10", "2026-03-09", "2026-03-07", "2026-03-06"), "2026-03-10"));
    }

    @Test
    public void computeStreak_lastReachedTwoDaysAgo() {
        assertEquals(0, StatsHelper.computeStreak(
                Collections.singletonList("2026-03-08"), "2026-03-10"));
    }

    @Test
    public void computeStreak_acrossMonthBoundary() {
        assertEquals(3, StatsHelper.computeStreak(
                Arrays.asList("2026-03-01", "2026-02-28", "2026-02-27"), "2026-03-01"));
    }
}
