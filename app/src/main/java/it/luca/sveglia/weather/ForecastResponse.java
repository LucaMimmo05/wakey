package it.luca.sveglia.weather;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Parte della risposta di Open-Meteo che ci serve. Le liste "daily" hanno un solo elemento (oggi). */
public class ForecastResponse {

    public Current current;
    public Daily daily;

    public static class Current {
        @SerializedName("temperature_2m")
        public Double temperature;
    }

    public static class Daily {
        @SerializedName("weather_code")
        public List<Integer> weatherCode;
        @SerializedName("temperature_2m_max")
        public List<Double> temperatureMax;
        @SerializedName("temperature_2m_min")
        public List<Double> temperatureMin;
        @SerializedName("precipitation_probability_max")
        public List<Integer> precipitationProbability;
    }
}
