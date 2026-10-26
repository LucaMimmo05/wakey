package it.luca.sveglia.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import it.luca.sveglia.data.AlarmEntity;
import it.luca.sveglia.tts.BriefingAudioWorker;
import it.luca.sveglia.tts.BriefingCache;
import it.luca.sveglia.ui.MainActivity;

/**
 * Programma e annulla le sveglie con AlarmManager.setAlarmClock, l'unico tipo di
 * allarme che il sistema rispetta al minuto anche in Doze e con l'app chiusa.
 * Insieme a ogni sveglia programma il prefetch del briefing con voce naturale, 10 minuti prima.
 * Ogni sveglia usa il proprio id come requestCode, così i PendingIntent non si sovrascrivono.
 */
public final class AlarmScheduler {

    static final String EXTRA_ALARM_ID = "alarm_id";
    static final String EXTRA_ALARM_AT = "alarm_at";
    static final String EXTRA_SNOOZED = "snoozed";
    // I posticipi usano requestCode separati da quelli delle sveglie
    private static final int SNOOZE_REQUEST_BASE = 1_000_000;
    private static final long PREFETCH_BEFORE_MS = 10 * 60 * 1000L;

    private AlarmScheduler() {
    }

    /** true se l'app può programmare sveglie esatte (su Android 12+ va concesso dall'utente). */
    public static boolean canScheduleExact(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        return context.getSystemService(AlarmManager.class).canScheduleExactAlarms();
    }

    /** Programma la prossima occorrenza della sveglia, o la annulla se è disattivata. */
    public static void schedule(Context context, AlarmEntity alarm) {
        schedule(context, alarm, System.currentTimeMillis());
    }

    /** Come schedule, cercando la prossima occorrenza dopo fromMillis. */
    public static void schedule(Context context, AlarmEntity alarm, long fromMillis) {
        if (!alarm.enabled) {
            cancel(context, alarm.id);
            return;
        }
        if (!canScheduleExact(context)) {
            // Senza permesso setAlarmClock lancerebbe SecurityException: la schermata principale lo chiede
            return;
        }
        long triggerAt = nextTrigger(alarm, fromMillis);
        AlarmManager am = context.getSystemService(AlarmManager.class);
        // Intent mostrato quando l'utente tocca la sveglia nella barra di stato
        PendingIntent show = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerAt, show),
                alarmIntent(context, alarm.id));
        schedulePrefetch(context, alarm.id, triggerAt);
    }

    /** Fa risuonare la sveglia alarmId all'orario indicato (dopo "Posticipa"). */
    public static void scheduleSnooze(Context context, long alarmId, long atMillis) {
        if (!canScheduleExact(context)) {
            return;
        }
        PendingIntent show = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        context.getSystemService(AlarmManager.class).setAlarmClock(
                new AlarmManager.AlarmClockInfo(atMillis, show), snoozeIntent(context, alarmId));
    }

    public static void cancelSnooze(Context context, long alarmId) {
        context.getSystemService(AlarmManager.class).cancel(snoozeIntent(context, alarmId));
    }

    public static void cancel(Context context, long alarmId) {
        AlarmManager am = context.getSystemService(AlarmManager.class);
        am.cancel(alarmIntent(context, alarmId));
        am.cancel(snoozeIntent(context, alarmId));
        am.cancel(prefetchIntent(context, alarmId, 0));
        BriefingAudioWorker.cancel(context, alarmId);
        BriefingCache.clear(context, alarmId);
    }

    /** Millisecondi della prossima occorrenza dopo fromMillis, nel fuso orario del telefono. */
    public static long nextTrigger(AlarmEntity alarm, long fromMillis) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime from = LocalDateTime.ofInstant(Instant.ofEpochMilli(fromMillis), zone);
        return NextAlarm.after(from, alarm.hour, alarm.minute, alarm.days)
                .atZone(zone).toInstant().toEpochMilli();
    }

    /**
     * Prepara l'audio del briefing 10 minuti prima della sveglia. Se mancano meno
     * di 10 minuti lo prepara subito.
     */
    private static void schedulePrefetch(Context context, long alarmId, long triggerAt) {
        BriefingCache.clear(context, alarmId);
        AlarmManager am = context.getSystemService(AlarmManager.class);
        long prefetchAt = triggerAt - PREFETCH_BEFORE_MS;
        if (prefetchAt <= System.currentTimeMillis()) {
            // Toglie un eventuale prefetch di un orario precedente, che riscriverebbe il file
            am.cancel(prefetchIntent(context, alarmId, 0));
            BriefingAudioWorker.enqueue(context, alarmId, triggerAt);
            return;
        }
        // AllowWhileIdle: scatta anche in Doze, a telefono bloccato da ore
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, prefetchAt,
                prefetchIntent(context, alarmId, triggerAt));
    }

    private static PendingIntent alarmIntent(Context context, long alarmId) {
        Intent intent = new Intent(context, AlarmReceiver.class)
                .putExtra(EXTRA_ALARM_ID, alarmId);
        return PendingIntent.getBroadcast(context, (int) alarmId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent snoozeIntent(Context context, long alarmId) {
        Intent intent = new Intent(context, AlarmReceiver.class)
                .putExtra(EXTRA_ALARM_ID, alarmId)
                .putExtra(EXTRA_SNOOZED, true);
        return PendingIntent.getBroadcast(context, SNOOZE_REQUEST_BASE + (int) alarmId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent prefetchIntent(Context context, long alarmId, long triggerAt) {
        Intent intent = new Intent(context, PrefetchReceiver.class)
                .putExtra(EXTRA_ALARM_ID, alarmId)
                .putExtra(EXTRA_ALARM_AT, triggerAt);
        return PendingIntent.getBroadcast(context, (int) alarmId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
