package it.luca.sveglia.ui;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.Calendar;

import it.luca.sveglia.R;
import it.luca.sveglia.alarm.AlarmScheduler;
import it.luca.sveglia.data.AlarmEntity;
import it.luca.sveglia.data.AlarmRepository;

/** Schermata principale: lista delle sveglie con interruttore, aggiunta e modifica. */
public class MainActivity extends AppCompatActivity {

    private View permissionCard;
    private TextView permissionText;
    private View permissionButton;

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_test_alarm) {
                addTestAlarm();
                return true;
            } else if (item.getItemId() == R.id.action_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });

        RecyclerView list = findViewById(R.id.alarm_list);
        TextView empty = findViewById(R.id.empty_text);
        AlarmAdapter adapter = new AlarmAdapter(
                alarm -> startActivity(AlarmEditActivity.intent(this, alarm.id)),
                (alarm, enabled) -> AlarmRepository.setEnabled(this, alarm, enabled));
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        AlarmRepository.observeAll(this).observe(this, alarms -> {
            adapter.submitList(alarms);
            empty.setVisibility(alarms.isEmpty() ? View.VISIBLE : View.GONE);
        });

        findViewById(R.id.fab_add).setOnClickListener(
                v -> startActivity(AlarmEditActivity.intent(this, 0)));

        permissionCard = findViewById(R.id.permission_card);
        permissionText = findViewById(R.id.permission_text);
        permissionButton = findViewById(R.id.permission_button);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermissions()) {
            // Le sveglie salvate senza permesso non erano state programmate: si recupera ora
            AlarmRepository.rescheduleAll(this);
        }
    }

    /** Sveglia singola tra un minuto, comoda per provare suono e briefing. */
    private void addTestAlarm() {
        Calendar at = Calendar.getInstance();
        at.add(Calendar.MINUTE, 1);
        AlarmEntity alarm = new AlarmEntity();
        alarm.hour = at.get(Calendar.HOUR_OF_DAY);
        alarm.minute = at.get(Calendar.MINUTE);
        alarm.label = getString(R.string.test_alarm_label);
        AlarmRepository.save(this, alarm);
    }

    /**
     * Controlla i permessi speciali che si concedono solo dalle impostazioni di sistema.
     * Se ne manca uno mostra un avviso con il bottone per aprire la pagina giusta.
     */
    private boolean checkPermissions() {
        if (!AlarmScheduler.canScheduleExact(this)) {
            showPermissionCard(R.string.permission_exact_alarm,
                    new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri()));
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                && !getSystemService(NotificationManager.class).canUseFullScreenIntent()) {
            showPermissionCard(R.string.permission_full_screen,
                    new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri()));
            return false;
        }
        permissionCard.setVisibility(View.GONE);
        return true;
    }

    private void showPermissionCard(int messageRes, Intent settingsIntent) {
        permissionText.setText(messageRes);
        permissionButton.setOnClickListener(v -> startActivity(settingsIntent));
        permissionCard.setVisibility(View.VISIBLE);
    }

    private Uri packageUri() {
        return Uri.fromParts("package", getPackageName(), null);
    }
}
