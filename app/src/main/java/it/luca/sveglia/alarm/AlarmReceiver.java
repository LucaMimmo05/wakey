package it.luca.sveglia.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import it.luca.sveglia.data.AlarmEntity;
import it.luca.sveglia.data.AlarmRepository;
import it.luca.sveglia.data.AppDatabase;
import it.luca.sveglia.service.AlarmService;

/**
 * Riceve lo scatto di una sveglia: avvia subito il foreground service (le sveglie di
 * setAlarmClock sono esenti dai limiti di avvio in background) e poi programma la
 * prossima occorrenza, oppure disattiva la sveglia se era singola.
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        long alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, 0);
        Context app = context.getApplicationContext();
        // goAsync: la lettura dal database non può stare sul main thread
        PendingResult pending = goAsync();
        executor.execute(() -> {
            try {
                AlarmEntity alarm = AppDatabase.get(app).alarmDao().getById(alarmId);
                // Sveglia cancellata o spenta nel frattempo: non deve suonare
                if (alarm == null || !alarm.enabled) {
                    return;
                }
                ContextCompat.startForegroundService(app,
                        AlarmService.ringIntent(app, alarm.id, alarm.soundUri, alarm.label));
                AlarmRepository.afterRingNow(app, alarm);
            } finally {
                pending.finish();
            }
        });
    }
}
