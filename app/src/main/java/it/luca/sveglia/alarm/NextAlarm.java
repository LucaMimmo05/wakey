package it.luca.sveglia.alarm;

import java.time.LocalDateTime;

import it.luca.sveglia.util.Days;

/** Calcola quando deve suonare la prossima volta una sveglia. Java puro, testato a parte. */
public final class NextAlarm {

    private NextAlarm() {
    }

    /**
     * Primo istante strettamente successivo a "after" in cui cade hour:minute
     * in uno dei giorni della maschera (o in qualunque giorno se la maschera è 0).
     */
    public static LocalDateTime after(LocalDateTime after, int hour, int minute, int daysMask) {
        LocalDateTime candidate = after.toLocalDate().atTime(hour, minute);
        if (!candidate.isAfter(after)) {
            candidate = candidate.plusDays(1);
        }
        if (daysMask == 0) {
            return candidate;
        }
        // Al massimo 7 giorni di ricerca: la maschera ha almeno un giorno
        while (!Days.contains(daysMask, candidate.getDayOfWeek())) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }
}
