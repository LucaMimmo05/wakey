package it.luca.sveglia.util;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Da Android 15 le app sono sempre a tutto schermo e la tastiera non ridimensiona più
 * la finestra: senza questo il campo di testo finirebbe sotto la tastiera.
 */
public final class KeyboardInsets {

    private KeyboardInsets() {
    }

    /** Padding = barre di sistema, e in basso il più alto tra barra di navigazione e tastiera. */
    public static void apply(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
