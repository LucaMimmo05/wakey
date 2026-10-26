package it.luca.sveglia.tts;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import it.luca.sveglia.BuildConfig;
import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Trasforma un testo in un file MP3 con Google Cloud Text-to-Speech.
 * Le chiamate sono bloccanti: va usato fuori dal main thread (lo fa BriefingAudioWorker).
 */
public class CloudTtsClient {

    private static final String TAG = "CloudTtsClient";
    private static final long TIMEOUT_SECONDS = 20;

    private final Context context;
    private final CloudTtsApi api;

    public CloudTtsClient(Context context) {
        this.context = context.getApplicationContext();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
        api = new Retrofit.Builder()
                .baseUrl(CloudTtsApi.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(CloudTtsApi.class);
    }

    /** true se in local.properties è stata messa una chiave. */
    public static boolean hasApiKey() {
        return !BuildConfig.CLOUD_TTS_API_KEY.isEmpty();
    }

    /** Sintetizza il testo e lo salva in out. Ritorna false in caso di errore. */
    public boolean synthesizeToFile(String text, String voiceName, File out) {
        try {
            Response<CloudTtsApi.SynthesizeResponse> response = api.synthesize(
                    BuildConfig.CLOUD_TTS_API_KEY,
                    context.getPackageName(),
                    signingCertSha1(),
                    new CloudTtsApi.SynthesizeRequest(text, Voices.LANGUAGE, voiceName)).execute();
            if (!response.isSuccessful() || response.body() == null) {
                String error = response.errorBody() != null ? response.errorBody().string() : "";
                Log.w(TAG, "Cloud TTS ha risposto " + response.code() + ": " + error);
                return false;
            }
            byte[] mp3 = decodeAudio(response.body().audioContent);
            if (mp3 == null) {
                return false;
            }
            writeAtomically(mp3, out);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Sintesi vocale non riuscita", e);
            return false;
        }
    }

    /** Decodifica il campo audioContent (base64); null se vuoto o non valido. */
    static byte[] decodeAudio(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Scrive prima su un file temporaneo: chi legge out non trova mai un MP3 a metà. */
    private static void writeAtomically(byte[] data, File out) throws IOException {
        File tmp = new File(out.getParentFile(), out.getName() + ".tmp");
        try (FileOutputStream stream = new FileOutputStream(tmp)) {
            stream.write(data);
        }
        if (!tmp.renameTo(out)) {
            throw new IOException("Impossibile rinominare " + tmp);
        }
    }

    /** Impronta SHA-1 del certificato con cui è firmata l'app, come la vuole Google (esadecimale, senza ":"). */
    private String signingCertSha1() {
        try {
            PackageManager pm = context.getPackageManager();
            Signature[] signatures;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageInfo info = pm.getPackageInfo(context.getPackageName(),
                        PackageManager.GET_SIGNING_CERTIFICATES);
                signatures = info.signingInfo.getApkContentsSigners();
            } else {
                PackageInfo info = pm.getPackageInfo(context.getPackageName(),
                        PackageManager.GET_SIGNATURES);
                signatures = info.signatures;
            }
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(signatures[0].toByteArray());
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02X", b));
            }
            return hex.toString();
        } catch (Exception e) {
            Log.w(TAG, "Impronta del certificato non disponibile", e);
            return "";
        }
    }
}
