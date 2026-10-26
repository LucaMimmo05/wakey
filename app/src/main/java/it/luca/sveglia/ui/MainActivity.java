package it.luca.sveglia.ui;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.Calendar;

import it.luca.sveglia.R;
import it.luca.sveglia.alarm.AlarmScheduler;

/** Schermata principale: imposta o annulla l'unica sveglia (Tappa 1). */
public class MainActivity extends AppCompatActivity {

    private TextView statusText;

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.status_text);
        findViewById(R.id.button_in_one_minute).setOnClickListener(v -> scheduleInOneMinute());
        findViewById(R.id.button_pick_time).setOnClickListener(v -> showTimePicker());
        findViewById(R.id.button_cancel).setOnClickListener(v -> {
            AlarmScheduler.cancel(this);
            refreshStatus();
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void scheduleInOneMinute() {
        Calendar at = Calendar.getInstance();
        at.add(Calendar.MINUTE, 1);
        at.set(Calendar.SECOND, 0);
        at.set(Calendar.MILLISECOND, 0);
        schedule(at);
    }

    private void showTimePicker() {
        Calendar now = Calendar.getInstance();
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(now.get(Calendar.HOUR_OF_DAY))
                .setMinute(now.get(Calendar.MINUTE))
                .setTitleText(R.string.pick_time_title)
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            Calendar at = Calendar.getInstance();
            at.set(Calendar.HOUR_OF_DAY, picker.getHour());
            at.set(Calendar.MINUTE, picker.getMinute());
            at.set(Calendar.SECOND, 0);
            at.set(Calendar.MILLISECOND, 0);
            // Se l'orario di oggi è già passato, la sveglia è per domani
            if (!at.after(Calendar.getInstance())) {
                at.add(Calendar.DAY_OF_MONTH, 1);
            }
            schedule(at);
        });
        picker.show(getSupportFragmentManager(), "time_picker");
    }

    private void schedule(Calendar at) {
        if (!checkPermissions()) {
            return;
        }
        AlarmScheduler.schedule(this, at.getTimeInMillis());
        refreshStatus();
    }

    /**
     * Controlla i permessi speciali che si concedono solo dalle impostazioni di sistema.
     * Se ne manca uno spiega il motivo e apre la pagina giusta; ritorna false.
     */
    private boolean checkPermissions() {
        if (!AlarmScheduler.canScheduleExact(this)) {
            askForSetting(R.string.permission_exact_alarm,
                    new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri()));
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                && !getSystemService(NotificationManager.class).canUseFullScreenIntent()) {
            askForSetting(R.string.permission_full_screen,
                    new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri()));
            return false;
        }
        return true;
    }

    private void askForSetting(int messageRes, Intent settingsIntent) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.permission_title)
                .setMessage(messageRes)
                .setPositiveButton(R.string.open_settings, (d, w) -> startActivity(settingsIntent))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private Uri packageUri() {
        return Uri.fromParts("package", getPackageName(), null);
    }

    private void refreshStatus() {
        long at = AlarmScheduler.getScheduled(this);
        if (at == 0) {
            statusText.setText(R.string.no_alarm);
        } else {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(at);
            statusText.setText(getString(R.string.alarm_set_at, it.luca.sveglia.util.TimeFormat.display(
                    c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))));
        }
    }
}
