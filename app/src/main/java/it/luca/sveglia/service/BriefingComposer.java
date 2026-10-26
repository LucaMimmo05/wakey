package it.luca.sveglia.service;

import android.content.Context;

import java.time.LocalDate;

import it.luca.sveglia.alarm.AlarmPrefs;
import it.luca.sveglia.calendar.CalendarReader;
import it.luca.sveglia.weather.WeatherReport;
import it.luca.sveglia.word.WordOfTheDay;

/**
 * Raccoglie le parti del briefing in base alle impostazioni (meteo, parola, calendario)
 * e compone il testo. Bloccante (legge file e calendario): va chiamato fuori dal main thread.
 */
public final class BriefingComposer {

    private BriefingComposer() {
    }

    /**
     * @param weather meteo già scaricato dal chiamante (null se non disponibile);
     *                ignorato se l'utente ha tolto il meteo dal briefing
     */
    public static String compose(Context context, int hour, int minute, LocalDate day, WeatherReport weather) {
        BriefingText.Parts parts = new BriefingText.Parts();
        parts.name = AlarmPrefs.getUserName(context);
        parts.includeWeather = AlarmPrefs.isReadWeather(context);
        parts.weather = weather;
        if (AlarmPrefs.isReadWord(context)) {
            parts.word = WordOfTheDay.forDate(context, day);
        }
        if (AlarmPrefs.isReadCalendar(context)) {
            // null (permesso revocato) = la parte del calendario viene saltata
            parts.events = CalendarReader.forDay(context, day);
        }
        return BriefingText.build(hour, minute, parts);
    }
}
