package it.luca.sveglia.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.DayOfWeek;

public class DaysTest {

    @Test
    public void describesCommonPatterns() {
        assertEquals("Una volta", Days.describe(0));
        assertEquals("Ogni giorno", Days.describe(Days.EVERY_DAY));
        assertEquals("Giorni feriali", Days.describe(Days.WEEKDAYS));
        assertEquals("Weekend", Days.describe(Days.WEEKEND));
        int mask = Days.bit(DayOfWeek.MONDAY) | Days.bit(DayOfWeek.WEDNESDAY) | Days.bit(DayOfWeek.SUNDAY);
        assertEquals("lun, mer, dom", Days.describe(mask));
    }

    @Test
    public void containsDay() {
        assertTrue(Days.contains(Days.WEEKDAYS, DayOfWeek.FRIDAY));
        assertFalse(Days.contains(Days.WEEKDAYS, DayOfWeek.SATURDAY));
    }
}
