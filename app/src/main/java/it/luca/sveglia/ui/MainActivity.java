package it.luca.sveglia.ui;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import it.luca.sveglia.R;
import it.luca.sveglia.alarm.AlarmScheduler;
import it.luca.sveglia.data.AlarmEntity;
import it.luca.sveglia.data.AlarmRepository;
import it.luca.sveglia.util.RelativeTime;
import it.luca.sveglia.util.TimeFormat;

/**
 * Schermata principale: in alto la prossima sveglia su sfondo alba, sotto la lista
 * delle sveglie con interruttore, aggiunta e modifica.
 */
public class MainActivity extends AppCompatActivity {

    private static final long REFRESH_MS = 30_000;

    private View permissionCard;
    private TextView permissionText;
    private View permissionButton;
    private TextView greeting;
    private TextView heroOverline;
    private TextView heroTime;
    private TextView heroSubtitle;
    private List<AlarmEntity> alarms = Collections.emptyList();

    // Aggiorna "tra X h Y min" e il saluto mentre la schermata è aperta
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            updateHeader();
            handler.postDelayed(this, REFRESH_MS);
        }
    };

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        greeting = findViewById(R.id.greeting);
        heroOverline = findViewById(R.id.hero_overline);
        heroTime = findViewById(R.id.hero_time);
        heroSubtitle = findViewById(R.id.hero_subtitle);
        findViewById(R.id.button_menu).setOnClickListener(this::showMenu);

        RecyclerView listView = findViewById(R.id.alarm_list);
        TextView empty = findViewById(R.id.empty_text);
        AlarmAdapter adapter = new AlarmAdapter(
                alarm -> startActivity(AlarmEditActivity.intent(this, alarm.id)),
                (alarm, enabled) -> AlarmRepository.setEnabled(this, alarm, enabled));
        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.setAdapter(adapter);
        AlarmRepository.observeAll(this).observe(this, list -> {
            alarms = list;
            adapter.submitList(list);
            empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            updateHeader();
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
        handler.post(refresh);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(refresh);
        super.onPause();
    }

    private void showMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.inflate(R.menu.main);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_test_alarm) {
                addTestAlarm();
                return true;
            } else if (item.getItemId() == R.id.action_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });
        menu.show();
    }

    /** Saluto e riquadro con la prossima sveglia attiva (la più vicina nel tempo). */
    private void updateHeader() {
        long now = System.currentTimeMillis();
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime nowTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(now), zone);
        greeting.setText(RelativeTime.greeting(nowTime.getHour()));

        long next = Long.MAX_VALUE;
        for (AlarmEntity alarm : alarms) {
            if (alarm.enabled) {
                next = Math.min(next, AlarmScheduler.nextTrigger(alarm, now));
            }
        }
        if (next == Long.MAX_VALUE) {
            heroOverline.setVisibility(View.INVISIBLE);
            heroTime.setText(R.string.no_active_alarm);
            heroTime.setTextSize(40);
            heroSubtitle.setText(R.string.no_active_alarm_hint);
            return;
        }
        LocalDateTime when = LocalDateTime.ofInstant(Instant.ofEpochMilli(next), zone);
        heroOverline.setVisibility(View.VISIBLE);
        heroTime.setTextSize(72);
        heroTime.setText(TimeFormat.display(when.getHour(), when.getMinute()));
        heroSubtitle.setText(getString(R.string.hero_subtitle,
                RelativeTime.day(nowTime, when), RelativeTime.until(nowTime, when)));
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
