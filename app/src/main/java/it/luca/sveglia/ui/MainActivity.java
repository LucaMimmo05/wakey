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
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import it.luca.sveglia.R;
import it.luca.sveglia.alarm.AlarmScheduler;
import it.luca.sveglia.data.AlarmEntity;
import it.luca.sveglia.data.AlarmRepository;
import it.luca.sveglia.util.RelativeTime;

/**
 * Schermata principale in stile One UI: in alto un'intestazione grande con
 * "Sveglia tra 9 ore e 12 minuti" che si comprime scorrendo, sotto il gruppo delle sveglie.
 */
public class MainActivity extends AppCompatActivity {

    private static final long REFRESH_MS = 30_000;
    // Quota di schermo occupata dall'intestazione aperta, come nelle app Samsung
    private static final float HEADER_SCREEN_RATIO = 0.36f;
    private static final DateTimeFormatter HEADER_DATE =
            DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.ITALIAN);

    private View permissionCard;
    private TextView permissionText;
    private View permissionButton;
    private TextView headerTitle;
    private TextView headerSubtitle;
    private List<AlarmEntity> alarms = Collections.emptyList();

    // Aggiorna "tra X ore e Y minuti" mentre la schermata è aperta
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

        setupHeader();

        RecyclerView list = findViewById(R.id.alarm_list);
        View group = findViewById(R.id.alarm_group);
        TextView empty = findViewById(R.id.empty_text);
        AlarmAdapter adapter = new AlarmAdapter(
                alarm -> startActivity(AlarmEditActivity.intent(this, alarm.id)),
                (alarm, enabled) -> AlarmRepository.setEnabled(this, alarm, enabled));
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        list.addItemDecoration(new GroupDividers(this));
        AlarmRepository.observeAll(this).observe(this, items -> {
            alarms = items;
            adapter.submitList(items);
            group.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
            empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            updateHeader();
        });

        permissionCard = findViewById(R.id.permission_card);
        permissionText = findViewById(R.id.permission_text);
        permissionButton = findViewById(R.id.permission_button);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    /**
     * Intestazione alta circa un terzo dello schermo; mentre si comprime il testo grande
     * sfuma e compare il titolo piccolo nella barra, come in One UI.
     */
    private void setupHeader() {
        headerTitle = findViewById(R.id.header_title);
        headerSubtitle = findViewById(R.id.header_subtitle);
        View header = findViewById(R.id.header);
        ViewGroup.LayoutParams params = header.getLayoutParams();
        params.height = Math.round(getResources().getDisplayMetrics().heightPixels * HEADER_SCREEN_RATIO);
        header.setLayoutParams(params);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_add) {
                startActivity(AlarmEditActivity.intent(this, 0));
            } else if (id == R.id.action_test_alarm) {
                addTestAlarm();
            } else if (id == R.id.action_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            } else {
                return false;
            }
            return true;
        });

        AppBarLayout appBar = findViewById(R.id.appbar);
        appBar.addOnOffsetChangedListener((bar, offset) -> {
            float collapsed = -offset / (float) bar.getTotalScrollRange();
            header.setAlpha(1f - Math.min(1f, collapsed * 1.6f));
            // Titolo piccolo visibile solo a intestazione quasi chiusa
            toolbar.setTitle(collapsed > 0.8f ? getString(R.string.alarms_title) : "");
        });
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

    /** "Sveglia tra 9 ore e 12 minuti" + "mar 27 ott, 07:00" per la sveglia attiva più vicina. */
    private void updateHeader() {
        long now = System.currentTimeMillis();
        long next = Long.MAX_VALUE;
        for (AlarmEntity alarm : alarms) {
            if (alarm.enabled) {
                next = Math.min(next, AlarmScheduler.nextTrigger(alarm, now));
            }
        }
        if (next == Long.MAX_VALUE) {
            headerTitle.setText(R.string.header_none);
            headerSubtitle.setText(R.string.header_none_hint);
            return;
        }
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime nowTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(now), zone);
        LocalDateTime when = LocalDateTime.ofInstant(Instant.ofEpochMilli(next), zone);
        headerTitle.setText(getString(R.string.header_next, RelativeTime.durationLong(nowTime, when)));
        headerSubtitle.setText(when.format(HEADER_DATE));
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
