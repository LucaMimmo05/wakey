package it.luca.sveglia.alarm;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;

import it.luca.sveglia.tts.Voices;

/** Impostazioni generali: suono predefinito e voce del briefing (il suono di ogni sveglia sta nel database). */
public final class AlarmPrefs {

    private static final String PREFS = "settings";
    private static final String KEY_NATURAL_VOICE = "natural_voice";
    private static final String KEY_VOICE = "voice";

    private AlarmPrefs() {
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
