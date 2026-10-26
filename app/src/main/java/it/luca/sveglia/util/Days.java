package it.luca.sveglia.util;

import java.time.DayOfWeek;

/**
 * Giorni di ripetizione salvati come maschera di bit: lunedì = bit 0 ... domenica = bit 6.
 * 0 significa "una volta sola".
 */
public final class Days {

    public static final int WEEKDAYS = 0b0011111;
    public static final int WEEKEND = 0b1100000;
    public static final int EVERY_DAY = 0b1111111;

    private static final String[] SHORT = {"lun", "mar", "mer", "gio", "ven", "sab", "dom"};

    private Days() {
    }

    public static int bit(DayOfWeek day) {
        return 1 << (day.getValue() - 1);
    }

    public static boolean contains(int mask, DayOfWeek day) {
        return (mask & bit(day)) != 0;
    }

    /** Descrizione per la lista: "Una volta", "Ogni giorno", "Giorni feriali", "Weekend" o "lun, mer, ven". */
    public static String describe(int mask) {
        switch (mask) {
            case 0:
                return "Una volta";
            case EVERY_DAY:
                return "Ogni giorno";
            case WEEKDAYS:
                return "Giorni feriali";
            case WEEKEND:
                return "Weekend";
            default:
                StringBuilder text = new StringBuilder();
                for (int i = 0; i < 7; i++) {
                    if ((mask & (1 << i)) != 0) {
                        if (text.length() > 0) {
                            text.append(", ");
                        }
                        text.append(SHORT[i]);
                    }
                }
                return text.toString();
        }
    }
}
