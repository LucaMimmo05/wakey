package it.luca.sveglia.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import it.luca.sveglia.data.AlarmRepository;

/**
 * Le sveglie programmate in AlarmManager si perdono al riavvio, e un cambio di ora
 * o fuso le sposterebbe: in questi casi le riprogramma tutte dal database.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                && !Intent.ACTION_TIME_CHANGED.equals(action)
                && !Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            return;
        }
        Context app = context.getApplicationContext();
        PendingResult pending = goAsync();
        executor.execute(() -> {
            try {
                AlarmRepository.rescheduleAllNow(app);
            } finally {
                pending.finish();
            }
        });
    }
}
