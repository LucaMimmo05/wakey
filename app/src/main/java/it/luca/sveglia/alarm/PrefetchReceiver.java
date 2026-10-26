package it.luca.sveglia.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import it.luca.sveglia.tts.BriefingAudioWorker;

/** Scatta 10 minuti prima della sveglia e fa preparare l'audio del briefing. */
public class PrefetchReceiver extends BroadcastReceiver {

    static final String EXTRA_ALARM_AT = "alarm_at";

    @Override
    public void onReceive(Context context, Intent intent) {
        long alarmAt = intent.getLongExtra(EXTRA_ALARM_AT, 0);
        if (alarmAt > 0) {
            BriefingAudioWorker.enqueue(context, alarmAt);
        }
    }
}
