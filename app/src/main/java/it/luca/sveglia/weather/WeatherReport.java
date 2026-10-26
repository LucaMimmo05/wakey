package it.luca.sveglia.weather;

/** Meteo di oggi già pronto per il briefing (temperature arrotondate al grado). */
public class WeatherReport {

    /** Nome della città, oppure null se sconosciuto (si dice solo "Oggi ..."). */
    public final String city;
    public final String description;
    public final int temperature;
    public final int max;
    public final int min;
    public final int rainChance;

    public WeatherReport(String city, String description, int temperature, int max, int min, int rainChance) {
        this.city = city;
        this.description = description;
        this.temperature = temperature;
        this.max = max;
        this.min = min;
        this.rainChance = rainChance;
    }
}
