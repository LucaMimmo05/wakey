package it.luca.sveglia.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AlarmDao {

    /** Lista per la schermata principale: si aggiorna da sola quando il database cambia. */
    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    LiveData<List<AlarmEntity>> observeAll();

    @Query("SELECT * FROM alarms WHERE enabled = 1")
    List<AlarmEntity> getEnabled();

    @Query("SELECT * FROM alarms WHERE id = :id")
    AlarmEntity getById(long id);

    @Insert
    long insert(AlarmEntity alarm);

    @Update
    void update(AlarmEntity alarm);

    @Delete
    void delete(AlarmEntity alarm);
}
