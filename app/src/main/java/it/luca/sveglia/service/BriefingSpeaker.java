package it.luca.sveglia.service;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.Toast;

import java.util.Locale;

import it.luca.sveglia.R;

/**
 * Legge il briefing con la voce di sistema (TextToSpeech).
 * Va creato in anticipo (quando la sveglia inizia a suonare) così il motore
 * è già pronto quando l'utente preme "Spegni". Se speak() arriva prima che
 * l'inizializzazione sia finita, il testo viene tenuto da parte e letto appena possibile.
 */
public class BriefingSpeaker {

    private static final String TAG = "BriefingSpeaker";
    private static final String UTTERANCE_ID = "briefing";

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final TextToSpeech tts;

    private boolean ready;
    private boolean failed;
    private String pendingText;
    private Runnable onDone;

    public BriefingSpeaker(Context context) {
        this.context = context.getApplicationContext();
        tts = new TextToSpeech(this.context, this::onInit);
    }

    private void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "Inizializzazione TTS fallita: " + status);
            failed = true;
            finish();
            return;
        }
        int result = tts.setLanguage(Locale.ITALIAN);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Si prova comunque a parlare con la lingua di default, ma l'utente va avvisato
            Toast.makeText(context, R.string.tts_italian_missing, Toast.LENGTH_LONG).show();
        }
        // Stesso canale della sveglia: si sente anche con il volume multimediale a zero
        tts.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build());
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
            }

            @Override
            public void onDone(String utteranceId) {
                mainHandler.post(BriefingSpeaker.this::finish);
            }

            @Override
            public void onError(String utteranceId) {
                mainHandler.post(BriefingSpeaker.this::finish);
            }
        });
        ready = true;
        if (pendingText != null) {
            speakNow(pendingText);
            pendingText = null;
        }
    }

    /** Legge il testo e poi chiama onDone sul main thread (anche in caso di errore). */
    public void speak(String text, Runnable onDone) {
        this.onDone = onDone;
        if (failed) {
            finish();
        } else if (ready) {
            speakNow(text);
        } else {
            pendingText = text;
        }
    }

    private void speakNow(String text) {
        if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID) != TextToSpeech.SUCCESS) {
            finish();
        }
    }

    private void finish() {
        Runnable callback = onDone;
        onDone = null;
        if (callback != null) {
            callback.run();
        }
    }

    public void shutdown() {
        onDone = null;
        tts.stop();
        tts.shutdown();
    }
}
