package it.luca.sveglia.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.LocalDateTime;

public class RelativeTimeTest {

    // Lunedì 26 ottobre 2026, 22:48
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 26, 22, 48);

    @Test
    public void dayNames() {
        assertEquals("oggi", RelativeTime.day(NOW, NOW.plusMinutes(5)));
        assertEquals("domani", RelativeTime.day(NOW, LocalDateTime.of(2026, 10, 27, 7, 0)));
        assertEquals("mercoledì", RelativeTime.day(NOW, LocalDateTime.of(2026, 10, 28, 7, 0)));
    }

    @Test
    public void untilRoundsUpToTheMinute() {
        assertEquals("tra 8 h 12 min", RelativeTime.until(NOW, LocalDateTime.of(2026, 10, 27, 7, 0)));
        assertEquals("tra 2 min", RelativeTime.until(NOW.withSecond(30), NOW.plusMinutes(2)));
        assertEquals("tra 1 h", RelativeTime.until(NOW, NOW.plusHours(1)));
        assertEquals("tra meno di un minuto", RelativeTime.until(NOW, NOW));
    }

    @Test
    public void greetings() {
        assertEquals("Buongiorno", RelativeTime.greeting(7));
        assertEquals("Buonasera", RelativeTime.greeting(22));
    }
}
