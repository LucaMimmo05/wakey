package it.luca.sveglia.service;

import it.luca.sveglia.util.TimeFormat;
import it.luca.sveglia.weather.WeatherReport;
import it.luca.sveglia.word.WordOfTheDay;

/**
 * Compone il testo del briefing da leggere ad alta voce.
 * È Java puro (niente Context) così si può verificare con un test.
 */
public final class BriefingText {

    private BriefingText() {
    }

    /**
     * @param weather meteo di oggi, oppure null se non disponibile
     * @param word    parola del giorno, oppure null per ometterla
     */
    public static String build(int hour, int minute, WeatherReport weather, WordOfTheDay.Word word) {
        StringBuilder text = new StringBuilder("Buongiorno, ")
                .append(TimeFormat.spoken(hour, minute))
                .append(". ");

        if (weather != null) {
            text.append("Oggi a ").append(weather.city).append(' ').append(weather.description)
                    .append(", temperatura ").append(degrees(weather.temperature))
                    .append(", massima ").append(weather.max)
                    .append(", minima ").append(weather.min)
                    .append(", probabilità di pioggia ").append(weather.rainChance).append(" per cento. ");
        } else {
            text.append("Meteo non disponibile. ");
        }

        if (word != null) {
            text.append("La parola del giorno è: ").append(word.parola)
                    .append(". Significa: ").append(word.significato).append('.');
        }
        return text.toString().trim();
    }

    /** "1 grado", "-1 grado", altrimenti "N gradi". */
    private static String degrees(int value) {
        return value + (Math.abs(value) == 1 ? " grado" : " gradi");
    }
}
