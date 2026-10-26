package it.luca.sveglia.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import it.luca.sveglia.ui.MainActivity;

/**
 * Programma e annulla la sveglia con AlarmManager.setAlarmClock, l'unico tipo di
 * allarme che il sistema rispetta al minuto anche in Doze e con l'app chiusa.
 * Per ora gestisce una sola sveglia; l'orario viene salvato nelle preferenze
 * solo per mostrarlo nella schermata principale.
 */
public final class AlarmScheduler {

    private static final String PREFS = "alarm";
    private static final String KEY_TRIGGER_AT = "trigger_at";
    private static final int REQUEST_ALARM = 1;
    private static final int REQUEST_SHOW = 2;

    private AlarmScheduler() {
    }

    /** true se l'app può programmare sveglie esatte (su Android 12+ va concesso dall'utente). */
    public static boolean canScheduleExact(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        return context.getSystemService(AlarmManager.class).canScheduleExactAlarms();
    }

    public static void schedule(Context context, long triggerAtMillis) {
        AlarmManager am = context.getSystemService(AlarmManager.class);
        // Intent mostrato quando l'utente tocca la sveglia nella barra di stato
        PendingIntent show = PendingIntent.getActivity(context, REQUEST_SHOW,
                new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerAtMillis, show), alarmIntent(context));
        prefs(context).edit().putLong(KEY_TRIGGER_AT, triggerAtMillis).apply();
    }

    public static void cancel(Context context) {
        context.getSystemService(AlarmManager.class).cancel(alarmIntent(context));
        clearSaved(context);
    }

    /** Orario della sveglia programmata, oppure 0 se non ce n'è. */
    public static long getScheduled(Context context) {
        long at = prefs(context).getLong(KEY_TRIGGER_AT, 0);
        return at > System.currentTimeMillis() ? at : 0;
    }

    static void clearSaved(Context context) {
        prefs(context).edit().remove(KEY_TRIGGER_AT).apply();
    }

    private static PendingIntent alarmIntent(Context context) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        return PendingIntent.getBroadcast(context, REQUEST_ALARM, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
