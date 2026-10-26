package it.luca.sveglia.weather;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Il posto di cui leggere il meteo: l'ultima posizione del telefono salvata
 * (se l'utente l'ha attivata) oppure Gallarate. Si salva quando l'app è aperta,
 * così il prefetch delle 7 di mattina non ha bisogno della posizione in background.
 */
public final class WeatherPlace {

    public static final String DEFAULT_CITY = "Gallarate";
    public static final double DEFAULT_LATITUDE = 45.66;
    public static final double DEFAULT_LONGITUDE = 8.79;

    private static final String PREFS = "weather_place";
    private static final String KEY_USE_DEVICE = "use_device_location";
    private static final String KEY_LAT = "lat";
    private static final String KEY_LON = "lon";
    private static final String KEY_CITY = "city";
    private static final String KEY_UPDATED = "updated_at";

    /** Nome della città, oppure null se il telefono non ha saputo dirlo. */
    public final String city;
    public final double latitude;
    public final double longitude;

    private WeatherPlace(String city, double latitude, double longitude) {
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /** Il posto da usare adesso. */
    public static WeatherPlace current(Context context) {
        SharedPreferences p = prefs(context);
        if (p.getBoolean(KEY_USE_DEVICE, false) && p.contains(KEY_LAT)) {
            return new WeatherPlace(p.getString(KEY_CITY, null),
                    Double.longBitsToDouble(p.getLong(KEY_LAT, 0)),
                    Double.longBitsToDouble(p.getLong(KEY_LON, 0)));
        }
        return new WeatherPlace(DEFAULT_CITY, DEFAULT_LATITUDE, DEFAULT_LONGITUDE);
    }

    public static boolean isUsingDeviceLocation(Context context) {
        return prefs(context).getBoolean(KEY_USE_DEVICE, false);
    }

    public static void setUseDeviceLocation(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_USE_DEVICE, enabled).apply();
    }

    /** Salva la posizione appena letta dal telefono. */
    public static void save(Context context, double latitude, double longitude, String city) {
        prefs(context).edit()
                .putLong(KEY_LAT, Double.doubleToRawLongBits(latitude))
                .putLong(KEY_LON, Double.doubleToRawLongBits(longitude))
                .putString(KEY_CITY, city)
                .putLong(KEY_UPDATED, System.currentTimeMillis())
                .apply();
    }

    /** Millisecondi dall'ultimo aggiornamento della posizione (molto grande se mai). */
    public static long ageMillis(Context context) {
        return System.currentTimeMillis() - prefs(context).getLong(KEY_UPDATED, 0);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
