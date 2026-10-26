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
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import java.io.File;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import it.luca.sveglia.R;
import it.luca.sveglia.alarm.AlarmPrefs;
import it.luca.sveglia.alarm.AlarmScheduler;
import it.luca.sveglia.tts.BriefingCache;
import it.luca.sveglia.ui.RingingActivity;
import it.luca.sveglia.weather.WeatherReport;
import it.luca.sveglia.weather.WeatherPlace;
import it.luca.sveglia.weather.WeatherRepository;
import it.luca.sveglia.util.TimeFormat;

/**
 * Foreground service che fa suonare la sveglia e legge il briefing mentre suona:
 * pochi secondi dopo l'inizio la musica si abbassa, la voce legge il briefing e poi
 * la musica torna a volume pieno finché l'utente non preme "Spegni", che ferma tutto.
 * La voce è quella naturale preparata prima da BriefingAudioWorker (file MP3), oppure,
 * se il file non c'è, quella di sistema. "Posticipa" la fa risuonare dopo qualche minuto,
 * senza ripetere il briefing.
 * Resta in foreground per tutto il tempo, così il sistema non lo uccide a metà.
 */
public class AlarmService extends Service {

    public static final String ACTION_RING = "it.luca.sveglia.action.RING";
    public static final String ACTION_DISMISS = "it.luca.sveglia.action.DISMISS";
    public static final String ACTION_SNOOZE = "it.luca.sveglia.action.SNOOZE";
    private static final String ACTION_CANCEL_SNOOZE = "it.luca.sveglia.action.CANCEL_SNOOZE";
    private static final String EXTRA_SNOOZED = "snoozed";
    private static final String EXTRA_ALARM_ID = "alarm_id";
    private static final String EXTRA_SOUND_URI = "sound_uri";
    private static final String EXTRA_LABEL = "label";
    /** Inviato all'interno dell'app per chiudere la schermata di squillo. */
    public static final String ACTION_RINGING_FINISHED = "it.luca.sveglia.action.RINGING_FINISHED";

    private static final String TAG = "AlarmService";
    private static final String CHANNEL_ID = "alarm";
    private static final String SNOOZE_CHANNEL_ID = "snooze";
    private static final int NOTIFICATION_ID = 1;
    private static final int SNOOZE_NOTIFICATION_ID = 3;
    // Limite di sicurezza per il wake lock: squillo + briefing non durano di più
    private static final long WAKE_LOCK_TIMEOUT_MS = 15 * 60 * 1000L;
    // Dopo quanto dall'inizio dello squillo parte la voce
    private static final long BRIEFING_DELAY_MS = 5000;
    // Attesa massima del meteo: se la rete è lenta il briefing parte senza
    private static final long WEATHER_WAIT_SECONDS = 6;
    // Volume della musica mentre parla la voce (1 = pieno)
    private static final float DUCKED_VOLUME = 0.15f;

    private MediaPlayer player;
    private MediaPlayer voicePlayer;
    private BriefingSpeaker speaker;
    private PowerManager.WakeLock wakeLock;
    private boolean ringing;
    private long alarmId;
    private String label;
    private String soundUri;
    private AudioFocusRequest focusRequest;

    // Rete e lettura file fuori dal main thread
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Future<WeatherReport> weatherFuture;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_DISMISS.equals(action)) {
            dismiss();
        } else if (ACTION_SNOOZE.equals(action)) {
            snooze();
        } else if (ACTION_CANCEL_SNOOZE.equals(action)) {
            cancelSnooze(intent.getLongExtra(EXTRA_ALARM_ID, 0));
        } else if (intent != null) {
            ring(intent.getLongExtra(EXTRA_ALARM_ID, 0), intent.getStringExtra(EXTRA_SOUND_URI),
                    intent.getStringExtra(EXTRA_LABEL), intent.getBooleanExtra(EXTRA_SNOOZED, false));
        }
        return START_NOT_STICKY;
    }

    private void ring(long alarmId, String soundUri, String label, boolean snoozed) {
        // startForeground va chiamato subito, altrimenti il sistema chiude il servizio
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildRingingNotification(label),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        if (ringing) {
            return;
        }
        ringing = true;
        this.alarmId = alarmId;
        this.label = label;
        this.soundUri = soundUri;
        getSystemService(NotificationManager.class).cancel(SNOOZE_NOTIFICATION_ID);

        PowerManager pm = getSystemService(PowerManager.class);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Sveglia:alarm");
        wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS);

        requestAudioFocus();
        startSound(soundUri != null ? Uri.parse(soundUri) : null);
        if (snoozed) {
            // Dopo un posticipo suona e basta: il briefing è già stato letto
            return;
        }
        // Il motore vocale e il meteo si preparano mentre la sveglia inizia a suonare
        speaker = new BriefingSpeaker(this, AlarmPrefs.getSpeechRate(this));
        if (AlarmPrefs.isReadWeather(this)) {
            WeatherPlace place = WeatherPlace.current(this);
            weatherFuture = executor.submit(() -> new WeatherRepository().fetchToday(place));
        }
        scheduleBriefing(SystemClock.uptimeMillis() + BRIEFING_DELAY_MS);
    }

    /**
     * Prepara il testo (aspettando il meteo) e lo fa leggere all'orario indicato,
     * o appena è pronto se il meteo arriva dopo.
     */
    private void scheduleBriefing(long speakAtUptime) {
        executor.execute(() -> {
            WeatherReport weather = weatherFuture != null ? waitForWeather() : null;
            // L'orario detto è quello in cui parte la voce, pochi secondi dopo
            Calendar at = Calendar.getInstance();
            at.add(Calendar.MILLISECOND, (int) Math.max(0, speakAtUptime - SystemClock.uptimeMillis()));
            String text = BriefingComposer.compose(this, at.get(Calendar.HOUR_OF_DAY),
                    at.get(Calendar.MINUTE), LocalDate.now(), weather);
            mainHandler.postAtTime(() -> {
                // Se nel frattempo l'utente ha già spento la sveglia non si legge niente
                if (!ringing || speaker == null) {
                    return;
                }
                setVolume(DUCKED_VOLUME);
                // Finita la voce la sveglia continua a suonare a volume pieno
                Runnable restoreMusic = () -> setVolume(1f);
                File natural = AlarmPrefs.isNaturalVoice(this)
                        ? BriefingCache.validFile(this, alarmId, System.currentTimeMillis()) : null;
                if (natural == null || !playVoiceFile(natural, restoreMusic)) {
                    speaker.speak(text, restoreMusic);
                }
            }, speakAtUptime);
        });
    }

    /** "Posticipa": ferma tutto e riprogramma la stessa sveglia tra qualche minuto. */
    private void snooze() {
        if (!ringing) {
            stopEverything();
            return;
        }
        long at = System.currentTimeMillis() + AlarmPrefs.getSnoozeMinutes(this) * 60_000L;
        AlarmScheduler.scheduleSnooze(this, alarmId, at);
        showSnoozedNotification(at);
        sendBroadcast(new Intent(ACTION_RINGING_FINISHED).setPackage(getPackageName()));
        stopEverything();
    }

    private void cancelSnooze(long alarmId) {
        AlarmScheduler.cancelSnooze(this, alarmId);
        getSystemService(NotificationManager.class).cancel(SNOOZE_NOTIFICATION_ID);
        if (!ringing) {
            stopSelf();
        }
    }

    /** "Sveglia posticipata alle 07:05" con il bottone per annullare il posticipo. */
    private void showSnoozedNotification(long atMillis) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(SNOOZE_CHANNEL_ID,
                getString(R.string.channel_snooze), NotificationManager.IMPORTANCE_LOW));
        Calendar at = Calendar.getInstance();
        at.setTimeInMillis(atMillis);
        PendingIntent cancelPi = PendingIntent.getService(this, 3,
                new Intent(this, AlarmService.class).setAction(ACTION_CANCEL_SNOOZE)
                        .putExtra(EXTRA_ALARM_ID, alarmId),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        nm.notify(SNOOZE_NOTIFICATION_ID, new NotificationCompat.Builder(this, SNOOZE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle(getString(R.string.snoozed_until,
                        TimeFormat.display(at.get(Calendar.HOUR_OF_DAY), at.get(Calendar.MINUTE))))
                .setContentText(label)
                .setOngoing(true)
                .addAction(0, getString(R.string.snooze_cancel), cancelPi)
                .build());
    }

    /** "Spegni": ferma musica e voce insieme. */
    private void dismiss() {
        sendBroadcast(new Intent(ACTION_RINGING_FINISHED).setPackage(getPackageName()));
        stopEverything();
    }

    /** Meteo scaricato durante lo squillo, oppure null se non è arrivato in tempo. */
    private WeatherReport waitForWeather() {
        try {
            return weatherFuture.get(WEATHER_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.w(TAG, "Meteo non arrivato in tempo", e);
            return null;
        }
    }

    /** Suona il brano scelto; se non si riesce (file cancellato, permesso perso) usa il suono predefinito. */
    private void startSound(Uri chosen) {
        if (chosen != null && play(chosen)) {
            return;
        }
        if (!play(AlarmPrefs.getDefaultSoundUri(this))) {
            Log.e(TAG, "Impossibile riprodurre anche il suono predefinito");
        }
    }

    private boolean play(Uri uri) {
        try {
            player = new MediaPlayer();
            // USAGE_ALARM: usa il volume sveglia, suona anche con la suoneria bassa o in silenzioso
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            // Tiene sveglia la CPU finché suona, anche oltre il timeout del nostro wake lock
            player.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
            player.setDataSource(this, uri);
            player.setLooping(true);
            player.prepare();
            player.start();
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Impossibile riprodurre " + uri, e);
            stopSound();
            return false;
        }
    }

    /** Riproduce il briefing con voce naturale; false se il file non si riesce ad aprire. */
    private boolean playVoiceFile(File file, Runnable onDone) {
        try {
            voicePlayer = new MediaPlayer();
            voicePlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
            voicePlayer.setDataSource(file.getPath());
            voicePlayer.setOnCompletionListener(mp -> {
                stopVoice();
                onDone.run();
            });
            voicePlayer.setOnErrorListener((mp, what, extra) -> {
                stopVoice();
                onDone.run();
                return true;
            });
            voicePlayer.prepare();
            float rate = AlarmPrefs.getSpeechRate(this);
            if (rate != 1f) {
                // Stessa velocità scelta per la voce di sistema, senza cambiare il tono
                voicePlayer.setPlaybackParams(voicePlayer.getPlaybackParams().setSpeed(rate));
            }
            voicePlayer.start();
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Briefing con voce naturale non riproducibile, uso la voce di sistema", e);
            stopVoice();
            return false;
        }
    }

    private void stopVoice() {
        if (voicePlayer != null) {
            voicePlayer.release();
            voicePlayer = null;
        }
    }

    private void setVolume(float volume) {
        if (player != null) {
            player.setVolume(volume, volume);
        }
    }

    /**
     * Chiede il focus audio così le altre app (es. Spotify) si mettono in pausa.
     * La musica sotto la voce invece la abbassiamo noi con setVolume: voce e musica
     * sono entrambe nostre, quindi il ducking automatico del sistema non interviene.
     */
    private void requestAudioFocus() {
        focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .build();
        getSystemService(AudioManager.class).requestAudioFocus(focusRequest);
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
        stopVoice();
        if (speaker != null) {
            speaker.shutdown();
            speaker = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        if (focusRequest != null) {
            getSystemService(AudioManager.class).abandonAudioFocusRequest(focusRequest);
            focusRequest = null;
        }
    }

    @Override
    public void onDestroy() {
        releaseResources();
        mainHandler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
        super.onDestroy();
    }

    private Notification buildRingingNotification(String label) {
        createChannel();
        Intent fullScreen = RingingActivity.intent(this, label)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        PendingIntent fullScreenPi = PendingIntent.getActivity(this, 0, fullScreen,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent dismissPi = PendingIntent.getService(this, 1,
                new Intent(this, AlarmService.class).setAction(ACTION_DISMISS),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent snoozePi = PendingIntent.getService(this, 2,
                new Intent(this, AlarmService.class).setAction(ACTION_SNOOZE),
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
                .addAction(0, getString(R.string.snooze), snoozePi)
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

    /**
     * Intent per far suonare la sveglia alarmId con il suono indicato (null = predefinito).
     * snoozed = true per lo squillo dopo un posticipo, senza briefing.
     */
    public static Intent ringIntent(Context context, long alarmId, String soundUri, String label,
                                    boolean snoozed) {
        return new Intent(context, AlarmService.class)
                .setAction(ACTION_RING)
                .putExtra(EXTRA_ALARM_ID, alarmId)
                .putExtra(EXTRA_SOUND_URI, soundUri)
                .putExtra(EXTRA_LABEL, label)
                .putExtra(EXTRA_SNOOZED, snoozed);
    }

    /** Posticipa la sveglia che sta suonando (usato dalla schermata di squillo). */
    public static void requestSnooze(Context context) {
        context.startService(new Intent(context, AlarmService.class).setAction(ACTION_SNOOZE));
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
