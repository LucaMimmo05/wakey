package it.luca.sveglia.data;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Una sveglia salvata nel database. */
@Entity(tableName = "alarms")
public class AlarmEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public int hour;
    public int minute;

    /** Giorni di ripetizione come maschera di bit (vedi Days); 0 = una volta sola. */
    public int days;

    public boolean enabled = true;

    /** Brano o suono scelto; null = suono sveglia predefinito del telefono. */
    @Nullable
    public String soundUri;

    @Nullable
    public String label;

    public boolean isRepeating() {
        return days != 0;
    }
}
