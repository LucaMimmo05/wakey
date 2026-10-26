package it.luca.sveglia.weather;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/** Previsioni di Open-Meteo (https://open-meteo.com), servizio gratuito senza chiave API. */
public interface WeatherApi {

    String BASE_URL = "https://api.open-meteo.com/";

    @GET("v1/forecast"
            + "?current=temperature_2m"
            + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max"
            + "&timezone=auto&forecast_days=1")
    Call<ForecastResponse> forecast(@Query("latitude") double latitude,
                                    @Query("longitude") double longitude);
}
