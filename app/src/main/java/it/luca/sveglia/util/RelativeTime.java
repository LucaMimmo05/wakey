package it.luca.sveglia.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Locale;

/** Testi per il riquadro "prossima sveglia": "domani · tra 9 h 12 min". */
public final class RelativeTime {

    private RelativeTime() {
    }

    /** "oggi", "domani" o il nome del giorno ("mercoledì") se è più avanti nella settimana. */
    public static String day(LocalDateTime now, LocalDateTime when) {
        long days = Duration.between(now.toLocalDate().atStartOfDay(), when.toLocalDate().atStartOfDay()).toDays();
        if (days == 0) {
            return "oggi";
        } else if (days == 1) {
            return "domani";
        }
        return when.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ITALIAN);
    }

    /** "tra 9 h 12 min", "tra 5 min", "tra 2 h", "tra meno di un minuto". */
    public static String until(LocalDateTime now, LocalDateTime when) {
        // Arrotondato al minuto per eccesso, come fanno le sveglie di sistema
        long minutes = (Duration.between(now, when).getSeconds() + 59) / 60;
        if (minutes <= 0) {
            return "tra meno di un minuto";
        }
        long hours = minutes / 60;
        long rest = minutes % 60;
        if (hours == 0) {
            return "tra " + rest + " min";
        }
        return rest == 0 ? "tra " + hours + " h" : "tra " + hours + " h " + rest + " min";
    }

    /** Durata per esteso, come nell'Orologio Samsung: "9 ore e 12 minuti", "1 ora", "5 minuti". */
    public static String durationLong(LocalDateTime now, LocalDateTime when) {
        long minutes = (Duration.between(now, when).getSeconds() + 59) / 60;
        if (minutes <= 0) {
            return "meno di un minuto";
        }
        long hours = minutes / 60;
        long rest = minutes % 60;
        String h = hours == 1 ? "1 ora" : hours + " ore";
        String m = rest == 1 ? "1 minuto" : rest + " minuti";
        if (hours == 0) {
            return m;
        }
        return rest == 0 ? h : h + " e " + m;
    }

    /** Saluto in base all'ora del giorno. */
    public static String greeting(int hour) {
        if (hour < 5) {
            return "Buonanotte";
        } else if (hour < 12) {
            return "Buongiorno";
        } else if (hour < 18) {
            return "Buon pomeriggio";
        }
        return "Buonasera";
    }
}
