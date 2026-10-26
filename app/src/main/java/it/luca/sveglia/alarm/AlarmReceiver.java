package it.luca.sveglia.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

import it.luca.sveglia.service.AlarmService;

/**
 * Riceve lo scatto della sveglia e avvia subito il foreground service:
 * le sveglie di setAlarmClock sono esenti dai limiti di avvio in background.
 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        AlarmScheduler.clearSaved(context);
        Intent service = new Intent(context, AlarmService.class)
                .setAction(AlarmService.ACTION_RING);
        ContextCompat.startForegroundService(context, service);
    }
}
