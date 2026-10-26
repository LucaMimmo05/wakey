package it.luca.sveglia.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import it.luca.sveglia.calendar.CalendarEvent;
import it.luca.sveglia.weather.WeatherReport;
import it.luca.sveglia.word.WordOfTheDay;

public class BriefingTextTest {

    @Test
    public void fullBriefing() {
        WeatherReport weather = new WeatherReport("Gallarate", "cielo sereno", 12, 18, 6, 10);
        WordOfTheDay.Word word = new WordOfTheDay.Word();
        word.parola = "garbo";
        word.significato = "la gentilezza nei modi";

        assertEquals("Buongiorno, sono le 7 e 5. Oggi a Gallarate cielo sereno, temperatura 12 gradi,"
                        + " massima 18, minima 6, probabilità di pioggia 10 per cento."
                        + " La parola del giorno è: garbo. Significa: la gentilezza nei modi.",
                BriefingText.build(7, 5, weather, word));
    }

    @Test
    public void withoutWeatherStillSaysTheTime() {
        assertEquals("Buongiorno, sono le 7. Meteo non disponibile.",
                BriefingText.build(7, 0, null, null));
    }

    @Test
    public void singularDegree() {
        WeatherReport weather = new WeatherReport("Gallarate", "neve", -1, 2, -4, 80);
        assertEquals("Buongiorno, è l'una e 30. Oggi a Gallarate neve, temperatura -1 grado,"
                        + " massima 2, minima -4, probabilità di pioggia 80 per cento.",
                BriefingText.build(1, 30, weather, null));
    }

    @Test
    public void calendarEvents() {
        assertEquals("Oggi non hai impegni in calendario.", BriefingText.events(Collections.emptyList()));
        assertEquals("Oggi hai un impegno: all'una, Pranzo.",
                BriefingText.events(Collections.singletonList(new CalendarEvent("Pranzo", false, 1, 0))));
        assertEquals("Oggi hai 2 impegni: tutto il giorno, Compleanno di Anna; alle 9 e 30, Dentista.",
                BriefingText.events(Arrays.asList(
                        new CalendarEvent("Compleanno di Anna", true, 0, 0),
                        new CalendarEvent("Dentista", false, 9, 30))));
    }

    @Test
    public void manyEventsAreSummarized() {
        CalendarEvent e = new CalendarEvent("X", false, 10, 0);
        assertEquals("Oggi hai 7 impegni: alle 10, X; alle 10, X; alle 10, X; alle 10, X; alle 10, X; e altri 2.",
                BriefingText.events(Arrays.asList(e, e, e, e, e, e, e)));
    }

    @Test
    public void skippedParts() {
        BriefingText.Parts parts = new BriefingText.Parts();
        parts.includeWeather = false;
        parts.events = Collections.emptyList();
        assertEquals("Buongiorno, sono le 7. Oggi non hai impegni in calendario.",
                BriefingText.build(7, 0, parts));
    }
}
