package it.luca.sveglia.alarm;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;

/** Preferenze della sveglia: per ora solo il suono scelto. */
public final class AlarmPrefs {

    private static final String PREFS = "settings";
    private static final String KEY_SOUND_URI = "sound_uri";

    private AlarmPrefs() {
    }

    /** Suono scelto dall'utente, oppure null per usare il suono sveglia di sistema. */
    public static Uri getSoundUri(Context context) {
        String uri = prefs(context).getString(KEY_SOUND_URI, null);
        return uri != null ? Uri.parse(uri) : null;
    }

    public static void setSoundUri(Context context, Uri uri) {
        prefs(context).edit()
                .putString(KEY_SOUND_URI, uri != null ? uri.toString() : null)
                .apply();
    }

    /** Suono predefinito del telefono per le sveglie (o la suoneria se non ce n'è uno). */
    public static Uri getDefaultSoundUri(Context context) {
        Uri uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM);
        return uri != null ? uri : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
