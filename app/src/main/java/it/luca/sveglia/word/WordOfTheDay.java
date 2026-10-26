package it.luca.sveglia.word;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import it.luca.sveglia.R;

/**
 * Parola del giorno presa da un elenco locale (res/raw/parole_del_giorno.json),
 * così funziona anche senza rete. Ogni giorno si passa alla parola successiva.
 */
public final class WordOfTheDay {

    private static final String TAG = "WordOfTheDay";

    /** Una voce dell'elenco: i nomi dei campi corrispondono alle chiavi del JSON. */
    public static class Word {
        public String parola;
        public String significato;
    }

    private WordOfTheDay() {
    }

    /** Parola per la data indicata, oppure null se l'elenco non si può leggere. */
    public static Word forDate(Context context, LocalDate date) {
        try (Reader reader = new InputStreamReader(
                context.getResources().openRawResource(R.raw.parole_del_giorno),
                StandardCharsets.UTF_8)) {
            Word[] words = new Gson().fromJson(reader, Word[].class);
            return pick(words, date);
        } catch (Exception e) {
            Log.w(TAG, "Elenco delle parole non leggibile", e);
            return null;
        }
    }

    /** Sceglie la parola in base ai giorni trascorsi dal 1970: cambia a mezzanotte e ricomincia a fine elenco. */
    static Word pick(Word[] words, LocalDate date) {
        if (words == null || words.length == 0) {
            return null;
        }
        int index = (int) Math.floorMod(date.toEpochDay(), (long) words.length);
        return words[index];
    }
}
