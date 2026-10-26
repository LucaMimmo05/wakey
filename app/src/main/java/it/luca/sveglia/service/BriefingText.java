package it.luca.sveglia.service;

import java.util.List;

import it.luca.sveglia.calendar.CalendarEvent;
import it.luca.sveglia.util.RelativeTime;
import it.luca.sveglia.util.TimeFormat;
import it.luca.sveglia.weather.WeatherReport;
import it.luca.sveglia.word.WordOfTheDay;

/**
 * Compone il testo del briefing da leggere ad alta voce.
 * È Java puro (niente Context) così si può verificare con un test.
 */
public final class BriefingText {

    // Oltre questo numero gli impegni si riassumono ("e altri 2")
    private static final int MAX_EVENTS = 5;

    /** Cosa leggere dopo l'orario. Le parti spente dall'utente vengono saltate. */
    public static class Parts {
        /** Nome dell'utente per il saluto, oppure null. */
        public String name;
        public boolean includeWeather = true;
        /** Meteo di oggi, oppure null se non disponibile (si dice "meteo non disponibile"). */
        public WeatherReport weather;
        /** Parola del giorno, oppure null per ometterla. */
        public WordOfTheDay.Word word;
        /** Impegni di oggi, oppure null per non parlare del calendario. */
        public List<CalendarEvent> events;
    }

    private BriefingText() {
    }

    /** Versione semplice: orario, meteo e parola del giorno. */
    public static String build(int hour, int minute, WeatherReport weather, WordOfTheDay.Word word) {
        Parts parts = new Parts();
        parts.weather = weather;
        parts.word = word;
        return build(hour, minute, parts);
    }

    public static String build(int hour, int minute, Parts parts) {
        // "Buongiorno Luca, sono le 7 e 5." (Buonasera se la sveglia suona di sera)
        StringBuilder text = new StringBuilder(RelativeTime.greeting(hour));
        if (parts.name != null && !parts.name.isEmpty()) {
            text.append(' ').append(parts.name);
        }
        text.append(", ").append(TimeFormat.spoken(hour, minute)).append(". ");

        if (parts.includeWeather) {
            WeatherReport weather = parts.weather;
            if (weather != null) {
                text.append(weather.city != null ? "Oggi a " + weather.city + " " : "Oggi ")
                        .append(weather.description)
                        .append(", temperatura ").append(degrees(weather.temperature))
                        .append(", massima ").append(weather.max)
                        .append(", minima ").append(weather.min)
                        .append(", probabilità di pioggia ").append(weather.rainChance).append(" per cento. ");
            } else {
                text.append("Meteo non disponibile. ");
            }
        }

        if (parts.events != null) {
            text.append(events(parts.events)).append(' ');
        }

        if (parts.word != null) {
            text.append("La parola del giorno è: ").append(parts.word.parola)
                    .append(". Significa: ").append(parts.word.significato).append('.');
        }
        return text.toString().trim();
    }

    /** "Oggi hai 2 impegni: alle 9 e 30, Dentista; alle 15, Riunione." */
    static String events(List<CalendarEvent> events) {
        if (events.isEmpty()) {
            return "Oggi non hai impegni in calendario.";
        }
        StringBuilder text = new StringBuilder(events.size() == 1
                ? "Oggi hai un impegno: " : "Oggi hai " + events.size() + " impegni: ");
        int shown = Math.min(events.size(), MAX_EVENTS);
        for (int i = 0; i < shown; i++) {
            CalendarEvent event = events.get(i);
            if (i > 0) {
                text.append("; ");
            }
            text.append(event.allDay ? "tutto il giorno" : TimeFormat.at(event.hour, event.minute))
                    .append(", ").append(event.title);
        }
        int others = events.size() - shown;
        if (others > 0) {
            text.append(others == 1 ? "; e un altro" : "; e altri " + others);
        }
        return text.append('.').toString();
    }

    /** "1 grado", "-1 grado", altrimenti "N gradi". */
    private static String degrees(int value) {
        return value + (Math.abs(value) == 1 ? " grado" : " gradi");
    }
}
