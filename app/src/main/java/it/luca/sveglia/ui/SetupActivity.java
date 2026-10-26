package it.luca.sveglia.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import it.luca.sveglia.R;

/**
 * Guida al primo avvio: porta l'utente nelle impostazioni che, su Xiaomi e Samsung,
 * impediscono al sistema di chiudere Wakey di notte. Si riapre dal menu.
 */
public class SetupActivity extends AppCompatActivity {

    private LinearLayout stepsView;
    private List<SetupSteps.Step> steps;

    public static Intent intent(Context context) {
        return new Intent(context, SetupActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        SetupSteps.markShown(this);
        stepsView = findViewById(R.id.setup_steps);
        steps = SetupSteps.forThisDevice(this);
        findViewById(R.id.button_done).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Al ritorno dalle impostazioni lo stato dei passaggi può essere cambiato
        showSteps();
    }

    private void showSteps() {
        stepsView.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < steps.size(); i++) {
            SetupSteps.Step step = steps.get(i);
            if (i > 0) {
                View divider = new View(this);
                divider.setBackgroundResource(R.drawable.bg_divider_inset);
                stepsView.addView(divider, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 1));
            }
            View row = inflater.inflate(R.layout.item_setup_step, stepsView, false);
            boolean done = SetupSteps.isDone(this, step);
            TextView badge = row.findViewById(R.id.step_badge);
            badge.setText(done ? "✓" : String.valueOf(i + 1));
            ((TextView) row.findViewById(R.id.step_title)).setText(step.titleRes);
            ((TextView) row.findViewById(R.id.step_text)).setText(step.descriptionRes);
            View open = row.findViewById(R.id.step_open);
            open.setOnClickListener(v -> open(step));
            row.setOnClickListener(v -> open(step));
            stepsView.addView(row);
        }
    }

    /** Apre la prima pagina di impostazioni che esiste su questo telefono. */
    private void open(SetupSteps.Step step) {
        for (Intent intent : step.intents) {
            try {
                startActivity(intent);
                SetupSteps.markOpened(this, step);
                return;
            } catch (ActivityNotFoundException | SecurityException e) {
                // Pagina non presente su questa versione: si prova la successiva
            }
        }
    }
}
