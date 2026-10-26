package it.luca.sveglia.tts;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;

/**
 * Il file MP3 del briefing preparato prima della sveglia, più l'orario della sveglia
 * per cui è stato fatto: un file vecchio (sveglia di ieri, orario cambiato) non va letto.
 */
public final class BriefingCache {

    private static final String PREFS = "briefing_cache";
    private static final String KEY_ALARM_AT = "alarm_at";
    private static final String FILE_NAME = "briefing.mp3";
    // Tolleranza tra l'orario per cui è stato preparato e il momento in cui suona
    private static final long MAX_DISTANCE_MS = 30 * 60 * 1000L;

    private BriefingCache() {
    }

    public static File file(Context context) {
        return new File(context.getCacheDir(), FILE_NAME);
    }

    public static void markReady(Context context, long alarmAtMillis) {
        prefs(context).edit().putLong(KEY_ALARM_AT, alarmAtMillis).apply();
    }

    /** Il file da leggere adesso, oppure null se non c'è o non è per questa sveglia. */
    public static File validFile(Context context, long nowMillis) {
        File f = file(context);
        long alarmAt = prefs(context).getLong(KEY_ALARM_AT, 0);
        return f.isFile() && isFor(alarmAt, nowMillis) ? f : null;
    }

    public static void clear(Context context) {
        prefs(context).edit().remove(KEY_ALARM_AT).apply();
        //noinspection ResultOfMethodCallIgnored
        file(context).delete();
    }

    /** true se il briefing preparato per alarmAt va bene per una sveglia che suona a nowMillis. */
    static boolean isFor(long alarmAt, long nowMillis) {
        return alarmAt > 0 && Math.abs(nowMillis - alarmAt) <= MAX_DISTANCE_MS;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
