package it.luca.sveglia.weather;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;

import org.junit.Test;

public class WeatherRepositoryTest {

    // Risposta reale di Open-Meteo, accorciata
    private static final String JSON = "{\"current\":{\"time\":\"2026-10-26T18:30\",\"temperature_2m\":12.3},"
            + "\"daily\":{\"time\":[\"2026-10-26\"],\"weather_code\":[3],\"temperature_2m_max\":[15.4],"
            + "\"temperature_2m_min\":[5.9],\"precipitation_probability_max\":[0]}}";

    @Test
    public void parsesOpenMeteoResponse() {
        ForecastResponse body = new Gson().fromJson(JSON, ForecastResponse.class);
        WeatherReport report = WeatherRepository.toReport("Gallarate", body);

        assertEquals("cielo coperto", report.description);
        assertEquals(12, report.temperature);
        assertEquals(15, report.max);
        assertEquals(6, report.min);
        assertEquals(0, report.rainChance);
    }

    @Test
    public void missingFieldsGiveNull() {
        ForecastResponse body = new Gson().fromJson("{\"current\":{}}", ForecastResponse.class);
        assertNull(WeatherRepository.toReport("Gallarate", body));
    }

    @Test
    public void unknownCodeHasFallback() {
        assertEquals("tempo variabile", WeatherDescriptions.describe(42));
    }
}
