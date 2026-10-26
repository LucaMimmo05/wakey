package it.luca.sveglia.tts;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ForegroundInfo;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import it.luca.sveglia.R;
import it.luca.sveglia.alarm.AlarmPrefs;
import it.luca.sveglia.service.BriefingComposer;
import it.luca.sveglia.weather.WeatherReport;
import it.luca.sveglia.weather.WeatherPlace;
import it.luca.sveglia.weather.WeatherRepository;

/**
 * Prepara l'audio del briefing con la voce naturale prima che la sveglia suoni:
 * scarica il meteo, compone il testo con l'orario della sveglia e lo salva in MP3.
 * Se qualcosa va storto non è grave: alla sveglia si usa la voce di sistema.
 */
public class BriefingAudioWorker extends Worker {

    private static final String TAG = "BriefingAudioWorker";
    private static final String WORK_PREFIX = "briefing_audio_";
    private static final String KEY_ALARM_ID = "alarm_id";
    private static final String KEY_ALARM_AT = "alarm_at";
    private static final String CHANNEL_ID = "briefing_prefetch";
    private static final int NOTIFICATION_ID = 2;
    private static final int MAX_ATTEMPTS = 3;

    public BriefingAudioWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    /** Accoda la preparazione del briefing della sveglia alarmId che suona alle alarmAtMillis. */
    public static void enqueue(Context context, long alarmId, long alarmAtMillis) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BriefingAudioWorker.class)
                .setInputData(new Data.Builder()
                        .putLong(KEY_ALARM_ID, alarmId)
                        .putLong(KEY_ALARM_AT, alarmAtMillis)
                        .build())
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                // Expedited: parte subito anche con il telefono in standby; se la quota è finita, parte normale
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build();
        WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_PREFIX + alarmId, ExistingWorkPolicy.REPLACE, request);
    }

    public static void cancel(Context context, long alarmId) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_PREFIX + alarmId);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        if (!AlarmPrefs.isNaturalVoice(context) || !CloudTtsClient.hasApiKey()) {
            return Result.success();
        }
        long alarmId = getInputData().getLong(KEY_ALARM_ID, 0);
        long alarmAt = getInputData().getLong(KEY_ALARM_AT, 0);
        LocalDateTime when = LocalDateTime.ofInstant(Instant.ofEpochMilli(alarmAt), ZoneId.systemDefault());

        // Il testo dice l'orario della sveglia, non quello di adesso (10 minuti prima)
        WeatherReport weather = AlarmPrefs.isReadWeather(context) ? new WeatherRepository().fetchToday(WeatherPlace.current(context)) : null;
        String text = BriefingComposer.compose(context, when.getHour(), when.getMinute(),
                when.toLocalDate(), weather);

        File out = BriefingCache.file(context, alarmId);
        if (new CloudTtsClient(context).synthesizeToFile(text, AlarmPrefs.getVoice(context), out)) {
            BriefingCache.markReady(context, alarmId, alarmAt);
            return Result.success();
        }
        Log.w(TAG, "Briefing non preparato (tentativo " + (getRunAttemptCount() + 1) + ")");
        return getRunAttemptCount() + 1 < MAX_ATTEMPTS ? Result.retry() : Result.failure();
    }

    /** Solo prima di Android 12 il lavoro expedited gira come foreground service e serve una notifica. */
    @NonNull
    @Override
    public ForegroundInfo getForegroundInfo() {
        Context context = getApplicationContext();
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.channel_prefetch), NotificationManager.IMPORTANCE_MIN);
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle(context.getString(R.string.notification_prefetch))
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return new ForegroundInfo(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        }
        return new ForegroundInfo(NOTIFICATION_ID, notification);
    }
}
