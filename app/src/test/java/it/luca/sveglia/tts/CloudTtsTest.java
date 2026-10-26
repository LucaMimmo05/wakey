package it.luca.sveglia.tts;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

public class CloudTtsTest {

    @Test
    public void requestHasTheShapeGoogleExpects() {
        String json = new Gson().toJson(
                new CloudTtsApi.SynthesizeRequest("Buongiorno", "it-IT", "it-IT-Chirp3-HD-Kore"));
        assertEquals("{\"input\":{\"text\":\"Buongiorno\"},"
                + "\"voice\":{\"languageCode\":\"it-IT\",\"name\":\"it-IT-Chirp3-HD-Kore\"},"
                + "\"audioConfig\":{\"audioEncoding\":\"MP3\"}}", json);
    }

    @Test
    public void decodesBase64Audio() {
        assertArrayEquals(new byte[]{'I', 'D', '3'}, CloudTtsClient.decodeAudio("SUQz"));
        assertNull(CloudTtsClient.decodeAudio(""));
        assertNull(CloudTtsClient.decodeAudio("non è base64!"));
    }

    @Test
    public void cachedBriefingOnlyForItsAlarm() {
        long alarm = 1_800_000_000_000L;
        assertTrue(BriefingCache.isFor(alarm, alarm + 3_000));
        assertTrue(BriefingCache.isFor(alarm, alarm - 60_000));
        // Sveglia di ieri o nessun file preparato
        assertFalse(BriefingCache.isFor(alarm, alarm + 24 * 60 * 60 * 1000L));
        assertFalse(BriefingCache.isFor(0, alarm));
    }

    @Test
    public void everyVoiceHasALabel() {
        assertEquals(Voices.NAMES.length, Voices.LABELS.length);
        assertEquals("Kore (donna, Chirp 3 HD)", Voices.labelOf(Voices.DEFAULT));
    }
}
