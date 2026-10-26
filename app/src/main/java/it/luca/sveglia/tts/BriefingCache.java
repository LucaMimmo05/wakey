package it.luca.sveglia.tts;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;

/**
 * I file MP3 del briefing preparati prima delle sveglie (uno per sveglia), più l'orario
 * per cui ognuno è stato fatto: un file vecchio (sveglia di ieri, orario cambiato) non va letto.
 */
public final class BriefingCache {

    private static final String PREFS = "briefing_cache";
    // Tolleranza tra l'orario per cui è stato preparato e il momento in cui suona
    private static final long MAX_DISTANCE_MS = 30 * 60 * 1000L;

    private BriefingCache() {
    }

    public static File file(Context context, long alarmId) {
        return new File(context.getCacheDir(), "briefing_" + alarmId + ".mp3");
    }

    public static void markReady(Context context, long alarmId, long alarmAtMillis) {
        prefs(context).edit().putLong(key(alarmId), alarmAtMillis).apply();
    }

    /** Il file da leggere adesso per questa sveglia, oppure null se non c'è o è vecchio. */
    public static File validFile(Context context, long alarmId, long nowMillis) {
        File f = file(context, alarmId);
        long alarmAt = prefs(context).getLong(key(alarmId), 0);
        return f.isFile() && isFor(alarmAt, nowMillis) ? f : null;
    }

    public static void clear(Context context, long alarmId) {
        prefs(context).edit().remove(key(alarmId)).apply();
        //noinspection ResultOfMethodCallIgnored
        file(context, alarmId).delete();
    }

    /** true se il briefing preparato per alarmAt va bene per una sveglia che suona a nowMillis. */
    static boolean isFor(long alarmAt, long nowMillis) {
        return alarmAt > 0 && Math.abs(nowMillis - alarmAt) <= MAX_DISTANCE_MS;
    }

    private static String key(long alarmId) {
        return "alarm_at_" + alarmId;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
