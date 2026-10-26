package it.luca.sveglia.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import it.luca.sveglia.R;
import it.luca.sveglia.service.AlarmService;
import it.luca.sveglia.util.RelativeTime;
import it.luca.sveglia.util.TimeFormat;

/**
 * Schermata a tutto schermo mostrata mentre la sveglia suona, anche sopra
 * la schermata di blocco (showWhenLocked/turnScreenOn nel manifest).
 */
public class RingingActivity extends AppCompatActivity {

    // Si chiude se la sveglia viene spenta da un'altra parte (es. dalla notifica)
    private final BroadcastReceiver finishedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Su Android 8.0 gli attributi showWhenLocked/turnScreenOn del manifest non esistono ancora
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        setContentView(R.layout.activity_ringing);

        LocalDateTime now = LocalDateTime.now();
        TextView time = findViewById(R.id.ringing_time);
        time.setText(TimeFormat.display(now.getHour(), now.getMinute()));
        TextView greeting = findViewById(R.id.ringing_greeting);
        greeting.setText(RelativeTime.greeting(now.getHour()));
        TextView date = findViewById(R.id.ringing_date);
        date.setText(now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALIAN)));

        breathe(findViewById(R.id.ringing_pulse), 0.62f, 0f);

        findViewById(R.id.button_dismiss).setOnClickListener(v -> {
            AlarmService.requestDismiss(this);
            finish();
        });

        // Il tasto indietro non deve chiudere la schermata lasciando la sveglia a suonare
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
            }
        });

        ContextCompat.registerReceiver(this, finishedReceiver,
                new IntentFilter(AlarmService.ACTION_RINGING_FINISHED),
                ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    /** Anello che parte dal bottone, si allarga e sfuma, all'infinito (come le sveglie Samsung). */
    private static void breathe(View view, float fromScale, float endAlpha) {
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat(View.SCALE_X, fromScale, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, fromScale, 1f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 1f, endAlpha));
        animator.setDuration(1600);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(finishedReceiver);
        super.onDestroy();
    }
}
