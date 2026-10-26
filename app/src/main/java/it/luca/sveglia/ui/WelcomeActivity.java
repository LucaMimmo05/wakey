package it.luca.sveglia.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ScrollView;

import androidx.appcompat.app.AppCompatActivity;

import it.luca.sveglia.R;
import it.luca.sveglia.alarm.AlarmPrefs;
import it.luca.sveglia.util.KeyboardInsets;

/**
 * Primo avvio: chiede il nome, così il briefing inizia con "Buongiorno Maria".
 * Poi passa alla guida per la batteria (se non è già stata vista).
 */
public class WelcomeActivity extends AppCompatActivity {

    private EditText nameView;
    private View continueButton;

    public static Intent intent(Context context) {
        return new Intent(context, WelcomeActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        KeyboardInsets.apply(findViewById(R.id.welcome_root));

        nameView = findViewById(R.id.welcome_name);
        // Quando si apre la tastiera si scorre in fondo, dove sta il campo
        ScrollView scroll = findViewById(R.id.welcome_scroll);
        scroll.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (nameView.hasFocus() && b < ob) {
                scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
            }
        });
        continueButton = findViewById(R.id.welcome_continue);
        continueButton.setEnabled(false);
        nameView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                continueButton.setEnabled(s.toString().trim().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        nameView.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE && continueButton.isEnabled()) {
                done(nameView.getText().toString());
                return true;
            }
            return false;
        });

        continueButton.setOnClickListener(v -> done(nameView.getText().toString()));
        // Saltando il briefing dice solo "Buongiorno": il nome si può mettere dopo nelle impostazioni
        findViewById(R.id.welcome_skip).setOnClickListener(v -> done(null));
    }

    private void done(String name) {
        AlarmPrefs.setUserName(this, name);
        if (!SetupSteps.wasShown(this)) {
            startActivity(SetupActivity.intent(this));
        }
        finish();
    }
}
