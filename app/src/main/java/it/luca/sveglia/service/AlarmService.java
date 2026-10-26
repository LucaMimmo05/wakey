package it.luca.sveglia.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import java.util.Calendar;

import it.luca.sveglia.R;
import it.luca.sveglia.ui.RingingActivity;
import it.luca.sveglia.util.TimeFormat;

/**
 * Foreground service che fa suonare la sveglia e, dopo "Spegni", legge il briefing.
 * Resta in foreground finché la voce non ha finito, così il sistema non lo uccide a metà.
 */
public class AlarmService extends Service {

    public static final String ACTION_RING = "it.luca.sveglia.action.RING";
    public static final String ACTION_DISMISS = "it.luca.sveglia.action.DISMISS";
    /** Inviato all'interno dell'app per chiudere la schermata di squillo. */
    public static final String ACTION_RINGING_FINISHED = "it.luca.sveglia.action.RINGING_FINISHED";

    private static final String TAG = "AlarmService";
    private static final String CHANNEL_ID = "alarm";
    private static final int NOTIFICATION_ID = 1;
    // Limite di sicurezza per il wake lock: squillo + briefing non durano di più
    private static final long WAKE_LOCK_TIMEOUT_MS = 15 * 60 * 1000L;

    private MediaPlayer player;
    private BriefingSpeaker speaker;
    private PowerManager.WakeLock wakeLock;
    private boolean ringing;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_DISMISS.equals(action)) {
            dismiss();
        } else {
            ring();
        }
        return START_NOT_STICKY;
    }

    private void ring() {
        // startForeground va chiamato subito, altrimenti il sistema chiude il servizio
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildRingingNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        if (ringing) {
            return;
        }
        ringing = true;

        PowerManager pm = getSystemService(PowerManager.class);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Sveglia:alarm");
        wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS);

        // Il motore vocale si prepara mentre la sveglia suona
        speaker = new BriefingSpeaker(this);
        startSound();
    }

    private void dismiss() {
        if (!ringing) {
            stopEverything();
            return;
        }
        ringing = false;
        stopSound();
        sendBroadcast(new Intent(ACTION_RINGING_FINISHED).setPackage(getPackageName()));

        Calendar now = Calendar.getInstance();
        String text = getString(R.string.briefing_time,
                TimeFormat.spoken(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE)));
        speaker.speak(text, this::stopEverything);
    }

    private void startSound() {
        Uri uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
        if (uri == null) {
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        }
        try {
            player = new MediaPlayer();
            // USAGE_ALARM: usa il volume sveglia, suona anche con la suoneria bassa o in silenzioso
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            player.setDataSource(this, uri);
            player.setLooping(true);
            player.prepare();
            player.start();
        } catch (Exception e) {
            Log.e(TAG, "Impossibile riprodurre il suono della sveglia", e);
            stopSound();
        }
    }

    private void stopSound() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private void stopEverything() {
        releaseResources();
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void releaseResources() {
        ringing = false;
        stopSound();
        if (speaker != null) {
            speaker.shutdown();
            speaker = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Override
    public void onDestroy() {
        releaseResources();
        super.onDestroy();
    }

    private Notification buildRingingNotification() {
        createChannel();
        Intent fullScreen = new Intent(this, RingingActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        PendingIntent fullScreenPi = PendingIntent.getActivity(this, 0, fullScreen,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent dismissPi = PendingIntent.getService(this, 1,
                new Intent(this, AlarmService.class).setAction(ACTION_DISMISS),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle(getString(R.string.notification_ringing_title))
                .setContentText(getString(R.string.notification_ringing_text))
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setContentIntent(fullScreenPi)
                // Con schermo bloccato apre direttamente la schermata di squillo
                .setFullScreenIntent(fullScreenPi, true)
                .addAction(0, getString(R.string.dismiss), dismissPi)
                .build();
    }

    private void createChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                getString(R.string.channel_alarm), NotificationManager.IMPORTANCE_HIGH);
        // Il suono lo gestisce il MediaPlayer, non la notifica
        channel.setSound(null, null);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    /** Avvia lo spegnimento della sveglia (usato dalla schermata di squillo). */
    public static void requestDismiss(Context context) {
        context.startService(new Intent(context, AlarmService.class).setAction(ACTION_DISMISS));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
