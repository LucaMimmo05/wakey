package it.luca.sveglia.weather;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.CancellationSignal;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Legge la posizione approssimativa del telefono (basta per il meteo) e il nome della città,
 * poi la salva in WeatherPlace. Va usata con l'app aperta: in background servirebbe
 * il permesso "posizione sempre", che qui non chiediamo.
 */
public final class DeviceLocation {

    public interface Callback {
        /** Chiamato sul main thread; place è null se la posizione non è disponibile. */
        void onResult(@Nullable WeatherPlace place);
    }

    private static final String TAG = "DeviceLocation";
    // Una posizione già nota più recente di così va bene senza chiederne una nuova
    private static final long MAX_CACHED_AGE_MS = 60 * 60 * 1000L;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private DeviceLocation() {
    }

    public static boolean hasPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Aggiorna la posizione salvata. Senza permesso o con la posizione spenta risponde null. */
    @SuppressWarnings("MissingPermission")
    public static void update(Context context, Callback callback) {
        Context app = context.getApplicationContext();
        if (!hasPermission(app)) {
            callback.onResult(null);
            return;
        }
        LocationManager lm = app.getSystemService(LocationManager.class);
        Location cached = lastKnown(lm);
        if (cached != null && System.currentTimeMillis() - cached.getTime() < MAX_CACHED_AGE_MS) {
            resolveCity(app, cached, callback);
            return;
        }
        String provider = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                ? LocationManager.NETWORK_PROVIDER : LocationManager.GPS_PROVIDER;
        if (!lm.isProviderEnabled(provider)) {
            // Localizzazione spenta: va bene anche una posizione vecchia
            if (cached != null) {
                resolveCity(app, cached, callback);
            } else {
                callback.onResult(null);
            }
            return;
        }
        LocationManagerCompat.getCurrentLocation(lm, provider, new CancellationSignal(),
                ContextCompat.getMainExecutor(app), location -> {
                    Location best = location != null ? location : cached;
                    if (best == null) {
                        callback.onResult(null);
                    } else {
                        resolveCity(app, best, callback);
                    }
                });
    }

    @SuppressWarnings("MissingPermission")
    @Nullable
    private static Location lastKnown(LocationManager lm) {
        Location best = null;
        for (String provider : lm.getProviders(true)) {
            Location l = lm.getLastKnownLocation(provider);
            if (l != null && (best == null || l.getTime() > best.getTime())) {
                best = l;
            }
        }
        return best;
    }

    /** Nome della città dalle coordinate (Geocoder di sistema), poi salva. Fuori dal main thread. */
    private static void resolveCity(Context context, Location location, Callback callback) {
        executor.execute(() -> {
            String city = null;
            if (Geocoder.isPresent()) {
                try {
                    @SuppressWarnings("deprecation")
                    List<Address> addresses = new Geocoder(context, Locale.ITALIAN)
                            .getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address a = addresses.get(0);
                        city = a.getLocality() != null ? a.getLocality() : a.getSubAdminArea();
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Nome della città non disponibile", e);
                }
            }
            WeatherPlace.save(context, location.getLatitude(), location.getLongitude(), city);
            WeatherPlace place = WeatherPlace.current(context);
            ContextCompat.getMainExecutor(context).execute(() -> callback.onResult(place));
        });
    }
}
