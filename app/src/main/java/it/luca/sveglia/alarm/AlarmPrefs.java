package it.luca.sveglia.alarm;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;

import it.luca.sveglia.tts.Voices;

/** Preferenze della sveglia: suono e voce del briefing. */
public final class AlarmPrefs {

    private static final String PREFS = "settings";
    private static final String KEY_SOUND_URI = "sound_uri";
    private static final String KEY_NATURAL_VOICE = "natural_voice";
    private static final String KEY_VOICE = "voice";

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

    /** Voce naturale di Google Cloud (se c'è la chiave API) invece della voce di sistema. */
    public static boolean isNaturalVoice(Context context) {
        return prefs(context).getBoolean(KEY_NATURAL_VOICE, true);
    }

    public static void setNaturalVoice(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_NATURAL_VOICE, enabled).apply();
    }

    /** Nome della voce Google Cloud scelta (es. "it-IT-Chirp3-HD-Kore"). */
    public static String getVoice(Context context) {
        return prefs(context).getString(KEY_VOICE, Voices.DEFAULT);
    }

    public static void setVoice(Context context, String voiceName) {
        prefs(context).edit().putString(KEY_VOICE, voiceName).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
