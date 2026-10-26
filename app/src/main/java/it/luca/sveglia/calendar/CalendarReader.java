package it.luca.sveglia.calendar;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** Legge gli impegni di un giorno da tutti i calendari visibili del telefono. */
public final class CalendarReader {

    private static final String TAG = "CalendarReader";

    private static final String[] PROJECTION = {
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.ALL_DAY,
    };

    private CalendarReader() {
    }

    public static boolean hasPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Impegni del giorno in ordine di orario (quelli di tutto il giorno per primi),
     * oppure null se manca il permesso o il calendario non si può leggere.
     */
    public static List<CalendarEvent> forDay(Context context, LocalDate day) {
        if (!hasPermission(context)) {
            return null;
        }
        ZoneId zone = ZoneId.systemDefault();
        long start = day.atStartOfDay(zone).toInstant().toEpochMilli();
        long end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
        // Gli eventi di tutto il giorno sono salvati in UTC: con una finestra solo locale
        // si prenderebbe anche quello del giorno prima, quindi si filtrano dopo
        Uri.Builder uri = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(uri, start);
        ContentUris.appendId(uri, end);
        List<CalendarEvent> events = new ArrayList<>();
        try (Cursor c = context.getContentResolver().query(uri.build(), PROJECTION,
                CalendarContract.Instances.VISIBLE + " = 1", null,
                CalendarContract.Instances.ALL_DAY + " DESC, " + CalendarContract.Instances.BEGIN)) {
            if (c == null) {
                return null;
            }
            while (c.moveToNext()) {
                String title = c.getString(0);
                long begin = c.getLong(1);
                boolean allDay = c.getInt(2) == 1;
                if (allDay) {
                    LocalDate date = LocalDateTime.ofInstant(Instant.ofEpochMilli(begin), ZoneOffset.UTC).toLocalDate();
                    if (!date.equals(day)) {
                        continue;
                    }
                }
                LocalDateTime when = LocalDateTime.ofInstant(Instant.ofEpochMilli(begin), zone);
                events.add(new CalendarEvent(TextUtils.isEmpty(title) ? "Impegno senza titolo" : title,
                        allDay, when.getHour(), when.getMinute()));
            }
        } catch (Exception e) {
            Log.w(TAG, "Calendario non leggibile", e);
            return null;
        }
        return events;
    }
}
