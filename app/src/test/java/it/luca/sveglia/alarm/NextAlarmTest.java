package it.luca.sveglia.alarm;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.LocalDateTime;

import it.luca.sveglia.util.Days;

public class NextAlarmTest {

    // Lunedì 26 ottobre 2026
    private static final LocalDateTime MONDAY_8 = LocalDateTime.of(2026, 10, 26, 8, 0);

    @Test
    public void oneShotLaterToday() {
        assertEquals(LocalDateTime.of(2026, 10, 26, 9, 30), NextAlarm.after(MONDAY_8, 9, 30, 0));
    }

    @Test
    public void oneShotAlreadyPassedIsTomorrow() {
        assertEquals(LocalDateTime.of(2026, 10, 27, 7, 0), NextAlarm.after(MONDAY_8, 7, 0, 0));
    }

    @Test
    public void sameMinuteIsNotRepeated() {
        assertEquals(LocalDateTime.of(2026, 10, 27, 8, 0), NextAlarm.after(MONDAY_8, 8, 0, 0));
    }

    @Test
    public void weekdaysFromFridayEveningGoToMonday() {
        LocalDateTime friday = LocalDateTime.of(2026, 10, 30, 20, 0);
        assertEquals(LocalDateTime.of(2026, 11, 2, 7, 0), NextAlarm.after(friday, 7, 0, Days.WEEKDAYS));
    }

    @Test
    public void weekendFromMonday() {
        assertEquals(LocalDateTime.of(2026, 10, 31, 9, 0), NextAlarm.after(MONDAY_8, 9, 0, Days.WEEKEND));
    }
}
