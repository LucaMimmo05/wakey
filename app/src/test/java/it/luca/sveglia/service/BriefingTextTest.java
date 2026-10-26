package it.luca.sveglia.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

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
}
