package it.luca.sveglia.util;

import java.util.Locale;

/** Formattazione dell'orario, sia da mostrare sia da far pronunciare. */
public final class TimeFormat {

    private TimeFormat() {
    }

    /** "07:05" */
    public static String display(int hour, int minute) {
        return String.format(Locale.ITALIAN, "%02d:%02d", hour, minute);
    }

    /**
     * Frase che il TTS legge in modo naturale: "sono le 7 e 5", "è l'una e 30",
     * "è mezzanotte". Scrivere "07:05" farebbe leggere cose come "zero sette".
     */
    public static String spoken(int hour, int minute) {
        String base;
        if (hour == 0) {
            base = "è mezzanotte";
        } else if (hour == 1) {
            base = "è l'una";
        } else {
            base = "sono le " + hour;
        }
        return minute == 0 ? base : base + " e " + minute;
    }

    /** Orario di un impegno: "alle 9 e 30", "all'una", "a mezzanotte". */
    public static String at(int hour, int minute) {
        String base;
        if (hour == 0) {
            base = "a mezzanotte";
        } else if (hour == 1) {
            base = "all'una";
        } else {
            base = "alle " + hour;
        }
        return minute == 0 ? base : base + " e " + minute;
    }
}
