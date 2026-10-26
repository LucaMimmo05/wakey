package it.luca.sveglia.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TimeFormatTest {

    @Test
    public void spokenTime() {
        assertEquals("è mezzanotte e 15", TimeFormat.spoken(0, 15));
        assertEquals("è l'una", TimeFormat.spoken(1, 0));
        assertEquals("sono le 7 e 5", TimeFormat.spoken(7, 5));
        assertEquals("sono le 13 e 40", TimeFormat.spoken(13, 40));
    }

    @Test
    public void displayTime() {
        assertEquals("07:05", TimeFormat.display(7, 5));
    }
}
