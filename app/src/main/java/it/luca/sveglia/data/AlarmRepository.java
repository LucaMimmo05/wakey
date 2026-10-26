package it.luca.sveglia.data;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import it.luca.sveglia.alarm.AlarmScheduler;

/**
 * Unico punto in cui le sveglie vengono modificate: ogni modifica al database
 * aggiorna anche la programmazione in AlarmManager, così le due cose non divergono.
 * Room non permette query sul main thread, quindi le scritture vanno su un executor.
 */
public final class AlarmRepository {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private AlarmRepository() {
    }

    public static LiveData<List<AlarmEntity>> observeAll(Context context) {
        return AppDatabase.get(context).alarmDao().observeAll();
    }

    /** Inserisce o aggiorna la sveglia e la riprogramma (o la annulla se disattivata). */
    public static void save(Context context, AlarmEntity alarm) {
        Context app = context.getApplicationContext();
        executor.execute(() -> {
            AlarmDao dao = AppDatabase.get(app).alarmDao();
            if (alarm.id == 0) {
                alarm.id = dao.insert(alarm);
            } else {
                dao.update(alarm);
            }
            AlarmScheduler.schedule(app, alarm);
        });
    }

    public static void setEnabled(Context context, AlarmEntity alarm, boolean enabled) {
        alarm.enabled = enabled;
        save(context, alarm);
    }

    /** Crea una copia attiva della sveglia (stesso orario, giorni, suono e nome). */
    public static void duplicate(Context context, AlarmEntity alarm) {
        AlarmEntity copy = new AlarmEntity();
        copy.hour = alarm.hour;
        copy.minute = alarm.minute;
        copy.days = alarm.days;
        copy.soundUri = alarm.soundUri;
        copy.label = alarm.label;
        copy.enabled = true;
        save(context, copy);
    }

    public static void delete(Context context, AlarmEntity alarm) {
        Context app = context.getApplicationContext();
        executor.execute(() -> {
            AlarmScheduler.cancel(app, alarm.id);
            AppDatabase.get(app).alarmDao().delete(alarm);
        });
    }

    /** Riprogramma tutte le sveglie attive (dopo un riavvio o un cambio di orario). */
    public static void rescheduleAll(Context context) {
        Context app = context.getApplicationContext();
        executor.execute(() -> rescheduleAllNow(app));
    }

    /** Versione bloccante, per chi è già fuori dal main thread. */
    public static void rescheduleAllNow(Context context) {
        for (AlarmEntity alarm : AppDatabase.get(context).alarmDao().getEnabled()) {
            AlarmScheduler.schedule(context, alarm);
        }
    }

    /**
     * Dopo che una sveglia ha suonato: se si ripete programma la prossima,
     * se era singola la disattiva. Bloccante.
     */
    public static void afterRingNow(Context context, AlarmEntity alarm) {
        if (alarm.isRepeating()) {
            // Si parte da un minuto dopo: se l'allarme è scattato con qualche millisecondo
            // di anticipo non deve riprogrammarsi per lo stesso minuto
            AlarmScheduler.schedule(context, alarm, System.currentTimeMillis() + 60_000);
        } else {
            alarm.enabled = false;
            AppDatabase.get(context).alarmDao().update(alarm);
        }
    }
}
