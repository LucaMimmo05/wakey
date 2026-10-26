package it.luca.sveglia.ui;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.Calendar;

import it.luca.sveglia.R;
import it.luca.sveglia.alarm.AlarmPrefs;
import it.luca.sveglia.alarm.AlarmScheduler;

/** Schermata principale: imposta o annulla l'unica sveglia (Tappa 1). */
public class MainActivity extends AppCompatActivity {

    private TextView statusText;
    private TextView soundName;

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    // Selettore dei suoni di sistema (suonerie, sveglie)
    private final ActivityResultLauncher<Intent> systemSoundPicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }
                Uri picked = IntentCompat.getParcelableExtra(result.getData(),
                        RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri.class);
                // "Predefinito" o nessuna scelta: si torna al suono sveglia del telefono
                if (picked == null || Settings.System.DEFAULT_ALARM_ALERT_URI.equals(picked)) {
                    picked = null;
                }
                AlarmPrefs.setSoundUri(this, picked);
                refreshSound();
            });

    // Selettore di file audio (brani nella memoria del telefono, Drive, ecc.)
    private final ActivityResultLauncher<String[]> fileSoundPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) {
                    return;
                }
                // Senza permesso persistente il file non sarebbe più leggibile dopo un riavvio
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                AlarmPrefs.setSoundUri(this, uri);
                refreshSound();
            });

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

        soundName = findViewById(R.id.sound_name);
        findViewById(R.id.button_system_sound).setOnClickListener(v -> pickSystemSound());
        findViewById(R.id.button_file_sound).setOnClickListener(
                v -> fileSoundPicker.launch(new String[]{"audio/*"}));

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
        refreshSound();
    }

    private void pickSystemSound() {
        Uri current = AlarmPrefs.getSoundUri(this);
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                        current != null ? current : Settings.System.DEFAULT_ALARM_ALERT_URI);
        systemSoundPicker.launch(intent);
    }

    private void refreshSound() {
        Uri uri = AlarmPrefs.getSoundUri(this);
        soundName.setText(uri == null ? getString(R.string.sound_default) : soundTitle(uri));
    }

    /** Nome leggibile del suono: nome del file per i documenti, titolo per i suoni di sistema. */
    private String soundTitle(Uri uri) {
        try {
            if (DocumentsContract.isDocumentUri(this, uri)) {
                try (Cursor c = getContentResolver().query(uri,
                        new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                    if (c != null && c.moveToFirst()) {
                        return c.getString(0);
                    }
                }
            } else {
                Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
                if (ringtone != null) {
                    return ringtone.getTitle(this);
                }
            }
        } catch (Exception e) {
            // File cancellato o permesso perso: alla sveglia si userà il suono predefinito
        }
        return getString(R.string.sound_unavailable);
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
