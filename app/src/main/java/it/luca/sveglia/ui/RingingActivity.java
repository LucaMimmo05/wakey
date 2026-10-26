package it.luca.sveglia.ui;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import it.luca.sveglia.R;
import it.luca.sveglia.alarm.AlarmPrefs;
import it.luca.sveglia.service.AlarmService;
import it.luca.sveglia.util.RelativeTime;
import it.luca.sveglia.util.TimeFormat;

/**
 * Schermata a tutto schermo mostrata mentre la sveglia suona, anche sopra
 * la schermata di blocco (showWhenLocked/turnScreenOn nel manifest).
 * Come sui Samsung la sveglia si spegne trascinando la X fuori dal cerchio:
 * un tocco accidentale non basta.
 */
public class RingingActivity extends AppCompatActivity {

    private static final String EXTRA_LABEL = "label";

    private View handle;
    private View fill;
    private View track;
    private ObjectAnimator pulse;
    private boolean dismissed;

    // Si chiude se la sveglia viene spenta da un'altra parte (es. dalla notifica)
    private final BroadcastReceiver finishedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    public static Intent intent(Context context, String label) {
        return new Intent(context, RingingActivity.class).putExtra(EXTRA_LABEL, label);
    }

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
        String label = getIntent().getStringExtra(EXTRA_LABEL);
        // Il nome della sveglia, oppure il saluto se non ne ha uno
        ((TextView) findViewById(R.id.ringing_label)).setText(
                TextUtils.isEmpty(label) ? RelativeTime.greeting(now.getHour()) : label);
        ((TextView) findViewById(R.id.ringing_time)).setText(
                TimeFormat.display(now.getHour(), now.getMinute()));
        ((TextView) findViewById(R.id.ringing_date)).setText(
                now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALIAN)));

        TextView snooze = findViewById(R.id.button_snooze);
        snooze.setText(getString(R.string.snooze_minutes, AlarmPrefs.getSnoozeMinutes(this)));
        snooze.setOnClickListener(v -> {
            if (!dismissed) {
                dismissed = true;
                AlarmService.requestSnooze(this);
                finish();
            }
        });

        handle = findViewById(R.id.dismiss_handle);
        fill = findViewById(R.id.dismiss_fill);
        track = findViewById(R.id.dismiss_track);
        pulse = startPulse(findViewById(R.id.dismiss_pulse));
        setupDrag();

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

    /**
     * La X segue il dito; il cerchio chiaro dietro si allarga fino al bordo esterno.
     * Rilasciata oltre il bordo spegne la sveglia, prima del bordo torna al centro.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupDrag() {
        handle.setOnTouchListener(new View.OnTouchListener() {
            private float startX;
            private float startY;
            private boolean beyond;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (dismissed) {
                    return true;
                }
                // Distanza da percorrere: dal centro fino al bordo del cerchio esterno
                float limit = (track.getWidth() - handle.getWidth()) / 2f;
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        startY = event.getRawY();
                        beyond = false;
                        handle.animate().cancel();
                        fill.animate().cancel();
                        pulse.pause();
                        return true;
                    case MotionEvent.ACTION_MOVE: {
                        float dx = event.getRawX() - startX;
                        float dy = event.getRawY() - startY;
                        float distance = (float) Math.hypot(dx, dy);
                        // La X può uscire di poco oltre il bordo, non oltre
                        float max = limit * 1.15f;
                        if (distance > max) {
                            dx = dx * max / distance;
                            dy = dy * max / distance;
                        }
                        handle.setTranslationX(dx);
                        handle.setTranslationY(dy);
                        float progress = Math.min(1f, distance / limit);
                        float fullScale = (float) track.getWidth() / fill.getWidth();
                        fill.setScaleX(1f + progress * (fullScale - 1f));
                        fill.setScaleY(1f + progress * (fullScale - 1f));
                        boolean nowBeyond = distance >= limit;
                        if (nowBeyond && !beyond) {
                            v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                        }
                        beyond = nowBeyond;
                        return true;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (beyond && event.getActionMasked() == MotionEvent.ACTION_UP) {
                            dismiss();
                        } else {
                            springBack();
                        }
                        return true;
                    default:
                        return false;
                }
            }
        });
        // Con TalkBack non si può trascinare: lì basta un tocco (il lettore annuncia la descrizione)
        handle.setOnClickListener(v -> {
            AccessibilityManager am = getSystemService(AccessibilityManager.class);
            if (am != null && am.isTouchExplorationEnabled()) {
                dismiss();
            }
        });
    }

    private void springBack() {
        handle.animate().translationX(0).translationY(0)
                .setInterpolator(new OvershootInterpolator(2f)).setDuration(350).start();
        fill.animate().scaleX(1f).scaleY(1f).setDuration(250).start();
        pulse.resume();
    }

    private void dismiss() {
        dismissed = true;
        AlarmService.requestDismiss(this);
        handle.animate().alpha(0f).setDuration(150).start();
        fill.animate().alpha(0f).setDuration(250).withEndAction(this::finish).start();
    }

    /** Anello che parte dalla X, si allarga e sfuma, all'infinito (come le sveglie Samsung). */
    private static ObjectAnimator startPulse(View view) {
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.9f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.9f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 0.9f, 0f));
        animator.setDuration(1800);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
        return animator;
    }

    @Override
    protected void onDestroy() {
        pulse.cancel();
        unregisterReceiver(finishedReceiver);
        super.onDestroy();
    }
}
