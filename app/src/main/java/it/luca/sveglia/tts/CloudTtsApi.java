package it.luca.sveglia.tts;

import com.google.gson.annotations.SerializedName;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

/** Endpoint REST di Google Cloud Text-to-Speech (v1 text:synthesize). */
interface CloudTtsApi {

    String BASE_URL = "https://texttospeech.googleapis.com/";

    /**
     * Gli header X-Android-* servono se la chiave è limitata alle app Android:
     * nelle chiamate REST fatte a mano non li aggiunge nessuno al posto nostro.
     */
    @POST("v1/text:synthesize")
    Call<SynthesizeResponse> synthesize(@Header("X-Goog-Api-Key") String apiKey,
                                        @Header("X-Android-Package") String packageName,
                                        @Header("X-Android-Cert") String certSha1,
                                        @Body SynthesizeRequest request);

    class SynthesizeRequest {
        final Input input;
        final Voice voice;
        final AudioConfig audioConfig;

        SynthesizeRequest(String text, String languageCode, String voiceName) {
            input = new Input(text);
            voice = new Voice(languageCode, voiceName);
            audioConfig = new AudioConfig("MP3");
        }
    }

    class Input {
        final String text;

        Input(String text) {
            this.text = text;
        }
    }

    class Voice {
        final String languageCode;
        final String name;

        Voice(String languageCode, String name) {
            this.languageCode = languageCode;
            this.name = name;
        }
    }

    class AudioConfig {
        final String audioEncoding;

        AudioConfig(String audioEncoding) {
            this.audioEncoding = audioEncoding;
        }
    }

    class SynthesizeResponse {
        /** Audio MP3 codificato in base64. */
        @SerializedName("audioContent")
        String audioContent;
    }
}
