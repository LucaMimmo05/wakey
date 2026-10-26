package it.luca.sveglia.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;

import com.google.android.material.color.MaterialColors;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.concurrent.Executors;

import it.luca.sveglia.R;
import it.luca.sveglia.alarm.AlarmScheduler;
import it.luca.sveglia.data.AlarmEntity;
import it.luca.sveglia.data.AlarmRepository;
import it.luca.sveglia.data.AppDatabase;
import it.luca.sveglia.util.Days;
import it.luca.sveglia.util.KeyboardInsets;
import it.luca.sveglia.util.RelativeTime;
import it.luca.sveglia.util.SoundNames;

/**
 * Creazione e modifica di una sveglia: orario con le rotelle (come nell'Orologio Samsung),
 * giorni di ripetizione, etichetta e suono.
 */
public class AlarmEditActivity extends AppCompatActivity {

    private static final String EXTRA_ALARM_ID = "alarm_id";
    private static final String[] DAY_LABELS = {"L", "M", "M", "G", "V", "S", "D"};

    private AlarmEntity alarm;
    private WheelPicker hourWheel;
    private WheelPicker minuteWheel;
    private TextView ringsIn;
    private EditText labelView;
    private final TextView[] dayViews = new TextView[7];
    private TextView daysSummary;
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
        KeyboardInsets.apply(findViewById(R.id.edit_root));

        findViewById(R.id.button_cancel).setOnClickListener(v -> finish());

        hourWheel = findViewById(R.id.wheel_hour);
        minuteWheel = findViewById(R.id.wheel_minute);
        hourWheel.setCount(24);
        minuteWheel.setCount(60);
        hourWheel.setOnValueChangeListener(v -> refreshRingsIn());
        minuteWheel.setOnValueChangeListener(v -> refreshRingsIn());
        ringsIn = findViewById(R.id.edit_rings_in);
        labelView = findViewById(R.id.edit_label);
        daysSummary = findViewById(R.id.edit_days_summary);
        soundName = findViewById(R.id.sound_name);
        buildDayToggles(findViewById(R.id.edit_days));

        findViewById(R.id.row_system_sound).setOnClickListener(v -> pickSystemSound());
        findViewById(R.id.row_file_sound).setOnClickListener(
                v -> fileSoundPicker.launch(new String[]{"audio/*"}));
        findViewById(R.id.button_save).setOnClickListener(v -> save());

        long id = getIntent().getLongExtra(EXTRA_ALARM_ID, 0);
        if (id == 0) {
            AlarmEntity fresh = new AlarmEntity();
            Calendar now = Calendar.getInstance();
            fresh.hour = now.get(Calendar.HOUR_OF_DAY);
            fresh.minute = now.get(Calendar.MINUTE);
            show(fresh);
        } else {
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

    /**
     * Sette cerchi L M M G V S D distribuiti su tutta la larghezza; un tocco li attiva/disattiva.
     * Come nel calendario Samsung il sabato è blu e la domenica rossa.
     */
    private void buildDayToggles(LinearLayout row) {
        int white = ContextCompat.getColor(this, android.R.color.white);
        int normal = MaterialColors.getColor(row, com.google.android.material.R.attr.colorOnSurface);
        int[] offColors = {normal, normal, normal, normal, normal,
                ContextCompat.getColor(this, R.color.ui_saturday),
                ContextCompat.getColor(this, R.color.ui_sunday)};
        for (int i = 0; i < 7; i++) {
            if (i > 0) {
                row.addView(new Space(this), new LinearLayout.LayoutParams(0, 1, 1f));
            }
            // Stile applicato come defStyleRes: cerchio, colori e dimensione del testo
            TextView day = new TextView(this, null, 0, R.style.Widget_Wakey_DayToggle);
            day.setText(DAY_LABELS[i]);
            day.setTextColor(new ColorStateList(
                    new int[][]{{android.R.attr.state_selected}, {}},
                    new int[]{white, offColors[i]}));
            day.setOnClickListener(v -> {
                v.setSelected(!v.isSelected());
                refreshDaysSummary();
                refreshRingsIn();
            });
            int size = getResources().getDimensionPixelSize(R.dimen.day_toggle_size);
            row.addView(day, new LinearLayout.LayoutParams(size, size));
            dayViews[i] = day;
        }
    }

    private void show(AlarmEntity loaded) {
        alarm = loaded;
        hourWheel.setValue(alarm.hour);
        minuteWheel.setValue(alarm.minute);
        labelView.setText(alarm.label);
        for (int i = 0; i < 7; i++) {
            dayViews[i].setSelected(Days.contains(alarm.days, DayOfWeek.of(i + 1)));
        }
        refreshDaysSummary();
        refreshRingsIn();
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
        alarm.hour = hourWheel.getValue();
        alarm.minute = minuteWheel.getValue();
        alarm.days = checkedDays();
        alarm.enabled = true;
        AlarmRepository.save(this, alarm);
        finish();
    }

    private int checkedDays() {
        int mask = 0;
        for (int i = 0; i < 7; i++) {
            if (dayViews[i].isSelected()) {
                mask |= Days.bit(DayOfWeek.of(i + 1));
            }
        }
        return mask;
    }

    private void refreshDaysSummary() {
        daysSummary.setText(Days.describe(checkedDays()));
    }

    /** "Suonerà domani, tra 8 h 12 min" con l'orario e i giorni scelti in questo momento. */
    private void refreshRingsIn() {
        if (alarm == null) {
            return;
        }
        AlarmEntity preview = new AlarmEntity();
        preview.hour = hourWheel.getValue();
        preview.minute = minuteWheel.getValue();
        preview.days = checkedDays();
        long now = System.currentTimeMillis();
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime nowTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(now), zone);
        LocalDateTime when = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(AlarmScheduler.nextTrigger(preview, now)), zone);
        ringsIn.setText(getString(R.string.rings_in,
                RelativeTime.day(nowTime, when), RelativeTime.until(nowTime, when)));
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
