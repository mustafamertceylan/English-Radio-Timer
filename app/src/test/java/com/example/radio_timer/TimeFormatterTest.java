package com.example.radio_timer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TimeFormatterTest {

    private static final long SEC = 1000L;
    private static final long MIN = 60 * SEC;
    private static final long HOUR = 60 * MIN;

    @Test
    public void formatShort_zero() {
        assertEquals("0 sn", TimeFormatter.formatShort(0));
    }

    @Test
    public void formatShort_negativeTreatedAsZero() {
        assertEquals("0 sn", TimeFormatter.formatShort(-5 * SEC));
    }

    @Test
    public void formatShort_59Seconds() {
        assertEquals("59 sn", TimeFormatter.formatShort(59 * SEC + 999));
    }

    @Test
    public void formatShort_oneMinute() {
        assertEquals("1 dk 0 sn", TimeFormatter.formatShort(MIN));
    }

    @Test
    public void formatShort_hourZeroMinutesFiveSeconds() {
        assertEquals("1 sa 0 dk 5 sn", TimeFormatter.formatShort(HOUR + 5 * SEC));
    }

    @Test
    public void formatShort_full() {
        assertEquals("2 sa 5 dk 9 sn", TimeFormatter.formatShort(2 * HOUR + 5 * MIN + 9 * SEC));
    }

    @Test
    public void formatGoal_lessThanMinute() {
        assertEquals("0 dk", TimeFormatter.formatGoal(59 * SEC));
    }

    @Test
    public void formatGoal_variants() {
        assertEquals("45 dk", TimeFormatter.formatGoal(45 * MIN));
        assertEquals("4 sa", TimeFormatter.formatGoal(4 * HOUR));
        assertEquals("2 sa 30 dk", TimeFormatter.formatGoal(2 * HOUR + 30 * MIN + 59 * SEC));
    }

    @Test
    public void formatClock() {
        assertEquals("00:00:00", TimeFormatter.formatClock(0));
        assertEquals("02:05:09", TimeFormatter.formatClock(2 * HOUR + 5 * MIN + 9 * SEC));
        assertEquals("25:00:00", TimeFormatter.formatClock(25 * HOUR));
    }
}
