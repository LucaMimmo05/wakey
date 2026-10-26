package it.luca.sveglia.ui;

import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.IntentCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;

import java.time.DayOfWeek;
import java.util.Calendar;
import java.util.concurrent.Executors;

import it.luca.sveglia.R;
import it.luca.sveglia.data.AlarmEntity;
import it.luca.sveglia.data.AlarmRepository;
import it.luca.sveglia.data.AppDatabase;
import it.luca.sveglia.util.Days;
import it.luca.sveglia.util.SoundNames;
import it.luca.sveglia.util.TimeFormat;

/** Creazione e modifica di una sveglia: orario, etichetta, giorni di ripetizione e suono. */
public class AlarmEditActivity extends AppCompatActivity {

    private static final String EXTRA_ALARM_ID = "alarm_id";
    private static final String[] DAY_LABELS = {"L", "M", "M", "G", "V", "S", "D"};

    private AlarmEntity alarm;
    private TextView timeView;
    private TextInputEditText labelView;
    private ChipGroup daysGroup;
    private TextView soundName;

    // Selettore dei suoni di sistema (suonerie, sveglie)
    private final ActivityResultLauncher<Intent> systemSoundPicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }
                Uri picked = IntentCompat.getParcelableExtra(result.getData(),
                        RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri.class);
                // "Predefinito" o nessuna scelta: si usa il suono sveglia del telefono
                boolean isDefault = picked == null || Settings.System.DEFAULT_ALARM_ALERT_URI.equals(picked);
                alarm.soundUri = isDefault ? null : picked.toString();
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
                alarm.soundUri = uri.toString();
                refreshSound();
            });

    /** alarmId = 0 per una sveglia nuova. */
    public static Intent intent(Context context, long alarmId) {
        return new Intent(context, AlarmEditActivity.class).putExtra(EXTRA_ALARM_ID, alarmId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_edit);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        timeView = findViewById(R.id.edit_time);
        labelView = findViewById(R.id.edit_label);
        daysGroup = findViewById(R.id.edit_days);
        soundName = findViewById(R.id.sound_name);
        for (int i = 0; i < 7; i++) {
            Chip chip = new Chip(this);
            chip.setText(DAY_LABELS[i]);
            chip.setCheckable(true);
            chip.setTag(DayOfWeek.of(i + 1));
            daysGroup.addView(chip);
        }

        timeView.setOnClickListener(v -> showTimePicker());
        findViewById(R.id.button_system_sound).setOnClickListener(v -> pickSystemSound());
        findViewById(R.id.button_file_sound).setOnClickListener(
                v -> fileSoundPicker.launch(new String[]{"audio/*"}));
        findViewById(R.id.button_save).setOnClickListener(v -> save());

        long id = getIntent().getLongExtra(EXTRA_ALARM_ID, 0);
        if (id == 0) {
            toolbar.setTitle(R.string.new_alarm);
            AlarmEntity fresh = new AlarmEntity();
            Calendar now = Calendar.getInstance();
            fresh.hour = now.get(Calendar.HOUR_OF_DAY);
            fresh.minute = now.get(Calendar.MINUTE);
            show(fresh);
        } else {
            toolbar.setTitle(R.string.edit_alarm);
            // Room non permette letture sul main thread
            Executors.newSingleThreadExecutor().execute(() -> {
                AlarmEntity loaded = AppDatabase.get(this).alarmDao().getById(id);
                runOnUiThread(() -> {
                    if (loaded == null) {
                        finish();
                    } else {
                        show(loaded);
                    }
                });
            });
        }
    }

    private void show(AlarmEntity loaded) {
        alarm = loaded;
        refreshTime();
        labelView.setText(alarm.label);
        for (int i = 0; i < daysGroup.getChildCount(); i++) {
            Chip chip = (Chip) daysGroup.getChildAt(i);
            chip.setChecked(Days.contains(alarm.days, (DayOfWeek) chip.getTag()));
        }
        refreshSound();
        View delete = findViewById(R.id.button_delete);
        if (alarm.id != 0) {
            delete.setVisibility(View.VISIBLE);
            delete.setOnClickListener(v -> {
                AlarmRepository.delete(this, alarm);
                finish();
            });
        }
    }

    private void showTimePicker() {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_24H)
                .setHour(alarm.hour)
                .setMinute(alarm.minute)
                .setTitleText(R.string.pick_time_title)
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            alarm.hour = picker.getHour();
            alarm.minute = picker.getMinute();
            refreshTime();
        });
        picker.show(getSupportFragmentManager(), "time_picker");
    }

    private void pickSystemSound() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, alarm.soundUri != null
                        ? Uri.parse(alarm.soundUri) : Settings.System.DEFAULT_ALARM_ALERT_URI);
        systemSoundPicker.launch(intent);
    }

    private void save() {
        CharSequence label = labelView.getText();
        alarm.label = TextUtils.isEmpty(label) ? null : label.toString().trim();
        int mask = 0;
        for (int i = 0; i < daysGroup.getChildCount(); i++) {
            Chip chip = (Chip) daysGroup.getChildAt(i);
            if (chip.isChecked()) {
                mask |= Days.bit((DayOfWeek) chip.getTag());
            }
        }
        alarm.days = mask;
        alarm.enabled = true;
        AlarmRepository.save(this, alarm);
        finish();
    }

    private void refreshTime() {
        timeView.setText(TimeFormat.display(alarm.hour, alarm.minute));
    }

    private void refreshSound() {
        if (alarm.soundUri == null) {
            soundName.setText(R.string.sound_default);
            return;
        }
        String title = SoundNames.title(this, Uri.parse(alarm.soundUri));
        soundName.setText(title != null ? title : getString(R.string.sound_unavailable));
    }
}
