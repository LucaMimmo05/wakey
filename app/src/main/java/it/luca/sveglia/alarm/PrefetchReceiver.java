package it.luca.sveglia.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import it.luca.sveglia.tts.BriefingAudioWorker;

/** Scatta 10 minuti prima di una sveglia e fa preparare l'audio del briefing. */
public class PrefetchReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        long alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, 0);
        long alarmAt = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_AT, 0);
        if (alarmId > 0 && alarmAt > 0) {
            BriefingAudioWorker.enqueue(context, alarmId, alarmAt);
        }
    }
}
