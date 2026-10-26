package it.luca.sveglia.alarm;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;

import it.luca.sveglia.tts.Voices;

/** Impostazioni generali: voce e contenuto del briefing, posticipo (il suono di ogni sveglia sta nel database). */
public final class AlarmPrefs {

    private static final String PREFS = "settings";
    private static final String KEY_NATURAL_VOICE = "natural_voice";
    private static final String KEY_VOICE = "voice";
    private static final String KEY_READ_WEATHER = "read_weather";
    private static final String KEY_READ_WORD = "read_word";
    private static final String KEY_READ_CALENDAR = "read_calendar";
    private static final String KEY_SPEECH_RATE = "speech_rate";
    private static final String KEY_SNOOZE_MINUTES = "snooze_minutes";

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

    public static boolean isReadWeather(Context context) {
        return prefs(context).getBoolean(KEY_READ_WEATHER, true);
    }

    public static void setReadWeather(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_READ_WEATHER, enabled).apply();
    }

    public static boolean isReadWord(Context context) {
        return prefs(context).getBoolean(KEY_READ_WORD, true);
    }

    public static void setReadWord(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_READ_WORD, enabled).apply();
    }

    /** Spento di default: serve il permesso di lettura del calendario. */
    public static boolean isReadCalendar(Context context) {
        return prefs(context).getBoolean(KEY_READ_CALENDAR, false);
    }

    public static void setReadCalendar(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_READ_CALENDAR, enabled).apply();
    }

    /** Velocità della voce: 1 = normale, 0.8 = più lenta, 1.2 = più veloce. */
    public static float getSpeechRate(Context context) {
        return prefs(context).getFloat(KEY_SPEECH_RATE, 1f);
    }

    public static void setSpeechRate(Context context, float rate) {
        prefs(context).edit().putFloat(KEY_SPEECH_RATE, rate).apply();
    }

    public static int getSnoozeMinutes(Context context) {
        return prefs(context).getInt(KEY_SNOOZE_MINUTES, 5);
    }

    public static void setSnoozeMinutes(Context context, int minutes) {
        prefs(context).edit().putInt(KEY_SNOOZE_MINUTES, minutes).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
