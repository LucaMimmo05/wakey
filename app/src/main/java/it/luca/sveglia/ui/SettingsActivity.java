package it.luca.sveglia.ui;

import android.Manifest;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.Locale;

import it.luca.sveglia.R;
import it.luca.sveglia.alarm.AlarmPrefs;
import it.luca.sveglia.calendar.CalendarReader;
import it.luca.sveglia.tts.CloudTtsClient;
import it.luca.sveglia.tts.Voices;
import it.luca.sveglia.weather.DeviceLocation;
import it.luca.sveglia.weather.WeatherPlace;

/** Impostazioni: cosa legge il briefing, voce e velocità, durata del posticipo. */
public class SettingsActivity extends AppCompatActivity {

    private static final int[] SNOOZE_OPTIONS = {5, 10, 15};

    private MaterialSwitch readCalendar;
    private MaterialSwitch deviceLocation;
    private TextView placeText;

    private final ActivityResultLauncher<String> locationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    enableDeviceLocation();
                } else {
                    deviceLocation.setChecked(false);
                    Toast.makeText(this, R.string.location_denied, Toast.LENGTH_LONG).show();
                }
            });

    // Il calendario si legge solo con il permesso: se l'utente lo nega l'interruttore torna spento
    private final ActivityResultLauncher<String> calendarPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                AlarmPrefs.setReadCalendar(this, granted);
                readCalendar.setChecked(granted);
                if (!granted) {
                    Toast.makeText(this, R.string.calendar_denied, Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        setupContents();
        setupPlace();
        setupVoice();
        setupSpeechRate();
        setupSnooze();
    }

    private void setupContents() {
        MaterialSwitch weather = findViewById(R.id.switch_read_weather);
        weather.setChecked(AlarmPrefs.isReadWeather(this));
        weather.setOnCheckedChangeListener((b, checked) -> AlarmPrefs.setReadWeather(this, checked));

        MaterialSwitch word = findViewById(R.id.switch_read_word);
        word.setChecked(AlarmPrefs.isReadWord(this));
        word.setOnCheckedChangeListener((b, checked) -> AlarmPrefs.setReadWord(this, checked));

        readCalendar = findViewById(R.id.switch_read_calendar);
        readCalendar.setChecked(AlarmPrefs.isReadCalendar(this) && CalendarReader.hasPermission(this));
        readCalendar.setOnClickListener(v -> {
            if (!readCalendar.isChecked()) {
                AlarmPrefs.setReadCalendar(this, false);
            } else if (CalendarReader.hasPermission(this)) {
                AlarmPrefs.setReadCalendar(this, true);
            } else {
                calendarPermission.launch(Manifest.permission.READ_CALENDAR);
            }
        });
    }

    /** Meteo della posizione del telefono invece che di Gallarate. */
    private void setupPlace() {
        deviceLocation = findViewById(R.id.switch_device_location);
        placeText = findViewById(R.id.place_text);
        deviceLocation.setChecked(WeatherPlace.isUsingDeviceLocation(this) && DeviceLocation.hasPermission(this));
        showPlace();
        deviceLocation.setOnClickListener(v -> {
            if (!deviceLocation.isChecked()) {
                WeatherPlace.setUseDeviceLocation(this, false);
                showPlace();
            } else if (DeviceLocation.hasPermission(this)) {
                enableDeviceLocation();
            } else {
                locationPermission.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION);
            }
        });
    }

    private void enableDeviceLocation() {
        WeatherPlace.setUseDeviceLocation(this, true);
        placeText.setText(R.string.place_searching);
        DeviceLocation.update(this, place -> {
            if (place == null) {
                placeText.setText(R.string.place_unavailable);
            } else {
                showPlace();
            }
        });
    }

    private void showPlace() {
        WeatherPlace place = WeatherPlace.current(this);
        placeText.setText(place.city != null ? getString(R.string.place_current, place.city)
                : getString(R.string.place_unknown_city));
    }

    /** Interruttore voce naturale e scelta della voce; disattivati se manca la chiave API. */
    private void setupVoice() {
        boolean hasKey = CloudTtsClient.hasApiKey();
        MaterialSwitch naturalVoice = findViewById(R.id.switch_natural_voice);
        MaterialAutoCompleteTextView dropdown = findViewById(R.id.voice_dropdown);
        View voiceMenu = findViewById(R.id.voice_menu);

        naturalVoice.setEnabled(hasKey);
        naturalVoice.setChecked(hasKey && AlarmPrefs.isNaturalVoice(this));
        voiceMenu.setEnabled(naturalVoice.isChecked());
        findViewById(R.id.voice_key_missing).setVisibility(hasKey ? View.GONE : View.VISIBLE);
        naturalVoice.setOnCheckedChangeListener((button, checked) -> {
            AlarmPrefs.setNaturalVoice(this, checked);
            voiceMenu.setEnabled(checked);
        });

        dropdown.setSimpleItems(Voices.LABELS);
        dropdown.setText(Voices.labelOf(AlarmPrefs.getVoice(this)), false);
        dropdown.setOnItemClickListener((parent, view, position, id) ->
                AlarmPrefs.setVoice(this, Voices.NAMES[position]));
    }

    /** Velocità da 0.7 a 1.3: vale sia per la voce di sistema sia per quella naturale. */
    private void setupSpeechRate() {
        Slider slider = findViewById(R.id.speech_rate);
        TextView value = findViewById(R.id.speech_rate_value);
        // Arrotondato al passo dello slider, altrimenti Slider lancia un'eccezione
        float rate = Math.round(AlarmPrefs.getSpeechRate(this) * 10) / 10f;
        slider.setValue(Math.max(0.7f, Math.min(1.3f, rate)));
        value.setText(rateLabel(slider.getValue()));
        slider.addOnChangeListener((s, v, fromUser) -> {
            value.setText(rateLabel(v));
            AlarmPrefs.setSpeechRate(this, v);
        });
    }

    private static String rateLabel(float rate) {
        return String.format(Locale.ITALIAN, "%.1f×", rate);
    }

    private void setupSnooze() {
        MaterialAutoCompleteTextView dropdown = findViewById(R.id.snooze_dropdown);
        String[] labels = new String[SNOOZE_OPTIONS.length];
        for (int i = 0; i < SNOOZE_OPTIONS.length; i++) {
            labels[i] = getString(R.string.snooze_option, SNOOZE_OPTIONS[i]);
        }
        dropdown.setSimpleItems(labels);
        dropdown.setText(getString(R.string.snooze_option, AlarmPrefs.getSnoozeMinutes(this)), false);
        dropdown.setOnItemClickListener((parent, view, position, id) ->
                AlarmPrefs.setSnoozeMinutes(this, SNOOZE_OPTIONS[position]));
    }
}
