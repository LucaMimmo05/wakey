package it.luca.sveglia.tts;

/**
 * Voci italiane di Google Cloud TTS proposte nelle impostazioni.
 * Elenco completo: https://docs.cloud.google.com/text-to-speech/docs/list-voices-and-types
 * Chirp 3: HD sono le più naturali; Neural2 costano un po' meno.
 */
public final class Voices {

    public static final String LANGUAGE = "it-IT";
    public static final String DEFAULT = "it-IT-Chirp3-HD-Kore";

    /** Nomi tecnici delle voci, nello stesso ordine di LABELS. */
    public static final String[] NAMES = {
            "it-IT-Chirp3-HD-Kore",
            "it-IT-Chirp3-HD-Aoede",
            "it-IT-Chirp3-HD-Charon",
            "it-IT-Chirp3-HD-Puck",
            "it-IT-Neural2-A",
            "it-IT-Neural2-F",
    };

    public static final String[] LABELS = {
            "Kore (donna, Chirp 3 HD)",
            "Aoede (donna, Chirp 3 HD)",
            "Charon (uomo, Chirp 3 HD)",
            "Puck (uomo, Chirp 3 HD)",
            "Neural2 A (donna)",
            "Neural2 F (uomo)",
    };

    private Voices() {
    }

    public static String labelOf(String name) {
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equals(name)) {
                return LABELS[i];
            }
        }
        return name;
    }
}
