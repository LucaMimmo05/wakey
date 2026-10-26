package it.luca.sveglia.word;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import java.time.LocalDate;

public class WordOfTheDayTest {

    private static WordOfTheDay.Word[] words(int n) {
        WordOfTheDay.Word[] words = new WordOfTheDay.Word[n];
        for (int i = 0; i < n; i++) {
            words[i] = new WordOfTheDay.Word();
            words[i].parola = "p" + i;
        }
        return words;
    }

    @Test
    public void changesEveryDayAndWrapsAround() {
        WordOfTheDay.Word[] words = words(3);
        LocalDate day = LocalDate.of(2026, 10, 27);

        assertSame(WordOfTheDay.pick(words, day), WordOfTheDay.pick(words, day));
        assertNotSame(WordOfTheDay.pick(words, day), WordOfTheDay.pick(words, day.plusDays(1)));
        assertSame(WordOfTheDay.pick(words, day), WordOfTheDay.pick(words, day.plusDays(3)));
    }

    @Test
    public void emptyListGivesNull() {
        assertNull(WordOfTheDay.pick(new WordOfTheDay.Word[0], LocalDate.now()));
    }
}
