package it.luca.sveglia.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Calendar;

import it.luca.sveglia.R;
import it.luca.sveglia.service.AlarmService;
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
        setContentView(R.layout.activity_ringing);

        Calendar now = Calendar.getInstance();
        TextView time = findViewById(R.id.ringing_time);
        time.setText(TimeFormat.display(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE)));

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

    @Override
    protected void onDestroy() {
        unregisterReceiver(finishedReceiver);
        super.onDestroy();
    }
}
