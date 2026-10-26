package it.luca.sveglia.ui;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import it.luca.sveglia.R;
import it.luca.sveglia.alarm.AlarmPrefs;
import it.luca.sveglia.tts.CloudTtsClient;
import it.luca.sveglia.tts.Voices;

/** Impostazioni generali: per ora la voce del briefing. */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        setupVoice();
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
}
