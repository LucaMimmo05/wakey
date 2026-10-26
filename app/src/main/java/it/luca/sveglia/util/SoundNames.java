package it.luca.sveglia.util;

import android.content.Context;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;

/** Nome leggibile di un suono: nome del file per i documenti, titolo per i suoni di sistema. */
public final class SoundNames {

    private SoundNames() {
    }

    /** Nome del suono, oppure null se non è più leggibile (file cancellato, permesso perso). */
    public static String title(Context context, Uri uri) {
        try {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                try (Cursor c = context.getContentResolver().query(uri,
                        new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                    if (c != null && c.moveToFirst()) {
                        return c.getString(0);
                    }
                }
            } else {
                Ringtone ringtone = RingtoneManager.getRingtone(context, uri);
                if (ringtone != null) {
                    return ringtone.getTitle(context);
                }
            }
        } catch (Exception e) {
            // Alla sveglia si userà il suono predefinito
        }
        return null;
    }
}
