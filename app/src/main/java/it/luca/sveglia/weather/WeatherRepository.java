package it.luca.sveglia.weather;

import android.util.Log;

import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Scarica il meteo di oggi. fetch() è bloccante: va chiamato fuori dal main thread.
 * Per ora la città è fissa (Gallarate).
 */
public class WeatherRepository {

    private static final String TAG = "WeatherRepository";

    public static final String DEFAULT_CITY = "Gallarate";
    public static final double DEFAULT_LATITUDE = 45.66;
    public static final double DEFAULT_LONGITUDE = 8.79;

    // Timeout brevi: al risveglio è meglio dire "meteo non disponibile" che far aspettare
    private static final long TIMEOUT_SECONDS = 5;

    private final WeatherApi api;

    public WeatherRepository() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .callTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
        api = new Retrofit.Builder()
                .baseUrl(WeatherApi.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(WeatherApi.class);
    }

    /** Meteo di oggi per la città di default, oppure null se non è disponibile (niente rete, errore, ecc.). */
    public WeatherReport fetchToday() {
        try {
            Response<ForecastResponse> response =
                    api.forecast(DEFAULT_LATITUDE, DEFAULT_LONGITUDE).execute();
            if (!response.isSuccessful() || response.body() == null) {
                Log.w(TAG, "Risposta meteo non valida: " + response.code());
                return null;
            }
            return toReport(DEFAULT_CITY, response.body());
        } catch (Exception e) {
            Log.w(TAG, "Meteo non disponibile", e);
            return null;
        }
    }

    /** Converte la risposta di Open-Meteo; null se mancano dei campi. */
    static WeatherReport toReport(String city, ForecastResponse body) {
        ForecastResponse.Daily d = body.daily;
        if (body.current == null || body.current.temperature == null || d == null
                || isEmpty(d.weatherCode) || isEmpty(d.temperatureMax)
                || isEmpty(d.temperatureMin) || isEmpty(d.precipitationProbability)) {
            return null;
        }
        return new WeatherReport(city,
                WeatherDescriptions.describe(d.weatherCode.get(0)),
                (int) Math.round(body.current.temperature),
                (int) Math.round(d.temperatureMax.get(0)),
                (int) Math.round(d.temperatureMin.get(0)),
                d.precipitationProbability.get(0));
    }

    private static boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty() || list.get(0) == null;
    }
}
