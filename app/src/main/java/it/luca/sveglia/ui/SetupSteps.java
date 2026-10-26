package it.luca.sveglia.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;

import it.luca.sveglia.R;
import it.luca.sveglia.util.DeviceInfo;

/**
 * Passaggi per rendere la sveglia affidabile sui telefoni che chiudono le app
 * in background. Alcuni si possono verificare (batteria), gli altri si segnano
 * come fatti quando l'utente apre la pagina delle impostazioni.
 */
final class SetupSteps {

    private static final String PREFS = "setup";
    private static final String KEY_SHOWN = "shown";

    /** Un passaggio: testi, intent da aprire (con ripiego) e stato. */
    static final class Step {
        final String key;
        final int titleRes;
        final int descriptionRes;
        final List<Intent> intents;

        Step(String key, int titleRes, int descriptionRes, List<Intent> intents) {
            this.key = key;
            this.titleRes = titleRes;
            this.descriptionRes = descriptionRes;
            this.intents = intents;
        }
    }

    private SetupSteps() {
    }

    /** Passaggi adatti a questo telefono: la batteria per tutti, il resto per Xiaomi e Samsung. */
    static List<Step> forThisDevice(Context context) {
        List<Step> steps = new ArrayList<>();
        Intent appDetails = appDetails(context);

        List<Intent> battery = new ArrayList<>();
        battery.add(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri(context)));
        battery.add(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        steps.add(new Step("battery", R.string.setup_battery_title, R.string.setup_battery_text, battery));

        if (DeviceInfo.isXiaomi()) {
            List<Intent> autostart = new ArrayList<>();
            autostart.add(new Intent().setComponent(new ComponentName("com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity")));
            autostart.add(appDetails);
            steps.add(new Step("xiaomi_autostart", R.string.setup_autostart_title,
                    R.string.setup_autostart_text, autostart));

            List<Intent> lockScreen = new ArrayList<>();
            lockScreen.add(new Intent("miui.intent.action.APP_PERM_EDITOR")
                    .setComponent(new ComponentName("com.miui.securitycenter",
                            "com.miui.permcenter.permissions.PermissionsEditorActivity"))
                    .putExtra("extra_pkgname", context.getPackageName()));
            lockScreen.add(appDetails);
            steps.add(new Step("xiaomi_lockscreen", R.string.setup_lockscreen_title,
                    R.string.setup_lockscreen_text, lockScreen));
        }

        if (DeviceInfo.isSamsung()) {
            List<Intent> sleeping = new ArrayList<>();
            sleeping.add(appDetails);
            steps.add(new Step("samsung_sleeping", R.string.setup_samsung_title,
                    R.string.setup_samsung_text, sleeping));
        }
        return steps;
    }

    /** true se il passaggio risulta fatto: verificato davvero per la batteria, segnato per gli altri. */
    static boolean isDone(Context context, Step step) {
        if (step.key.equals("battery")) {
            return context.getSystemService(PowerManager.class)
                    .isIgnoringBatteryOptimizations(context.getPackageName());
        }
        return prefs(context).getBoolean(step.key, false);
    }

    static void markOpened(Context context, Step step) {
        prefs(context).edit().putBoolean(step.key, true).apply();
    }

    /** La guida si apre da sola solo al primo avvio. */
    static boolean wasShown(Context context) {
        return prefs(context).getBoolean(KEY_SHOWN, false);
    }

    static void markShown(Context context) {
        prefs(context).edit().putBoolean(KEY_SHOWN, true).apply();
    }

    private static Intent appDetails(Context context) {
        return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context));
    }

    private static Uri packageUri(Context context) {
        return Uri.fromParts("package", context.getPackageName(), null);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
