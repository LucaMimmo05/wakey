package it.luca.sveglia.calendar;

/** Un impegno di oggi letto dal calendario del telefono. */
public class CalendarEvent {

    public final String title;
    public final boolean allDay;
    public final int hour;
    public final int minute;

    public CalendarEvent(String title, boolean allDay, int hour, int minute) {
        this.title = title;
        this.allDay = allDay;
        this.hour = hour;
        this.minute = minute;
    }
}
