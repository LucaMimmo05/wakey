# Wakey

Sveglia Android che, appena suona, ti saluta e ti legge un breve **briefing** con voce naturale: ora, meteo, impegni del giorno e una parola italiana da imparare, con la musica abbassata sotto la voce.

App nativa in **Java**, pensata per funzionare in modo affidabile anche sui telefoni che chiudono le app in background (Samsung, Xiaomi/HyperOS).

## Funzionalità

- **Più sveglie** con giorni di ripetizione, nome e suono per ognuna (suoni di sistema o un brano dal telefono)
- **Briefing parlato** 3 secondi dopo l'inizio dello squillo:
  - ora ("Buongiorno, sono le 7 e 5")
  - meteo di oggi da [Open-Meteo](https://open-meteo.com) (cielo, temperatura, massima, minima, probabilità di pioggia) nella città in cui ti trovi: la posizione approssimativa viene letta quando apri l'app, senza bisogno del permesso "posizione sempre"
  - impegni di oggi dal calendario del telefono
  - parola del giorno con il suo significato
  - ogni parte si può attivare o disattivare
- **Voce naturale** con Google Cloud Text-to-Speech (voci Chirp 3 HD e Neural2): l'audio viene preparato 10 minuti prima della sveglia, così al risveglio parte senza attese. Senza rete o senza chiave si usa la voce di sistema.
- **Musica in ducking**: la musica si abbassa mentre parla la voce e poi riprende
- **Squillo in stile Samsung**: si spegne trascinando la ✕ fuori dal cerchio, **Posticipa** di 5/10/15 minuti
- **Selezione multipla** tenendo premuta una sveglia: attiva/disattiva, copia, elimina
- **Riprogrammazione automatica** dopo riavvio, aggiornamento dell'app o cambio di ora/fuso
- **Guida al primo avvio** per escludere Wakey dal risparmio batteria (con passaggi specifici per Xiaomi e Samsung)
- Interfaccia in stile **One UI**, tema chiaro e scuro

## Architettura

```
app/src/main/java/it/luca/sveglia/
  ui/        Activity: lista sveglie, editor (rotelle ore/minuti), squillo, impostazioni, guida
  alarm/     AlarmScheduler (setAlarmClock), AlarmReceiver, PrefetchReceiver, BootReceiver, NextAlarm
  service/   AlarmService (foreground service), BriefingSpeaker (TTS di sistema), BriefingText/Composer
  tts/       CloudTtsClient (REST), BriefingAudioWorker (WorkManager), BriefingCache, Voices
  weather/   WeatherApi (Retrofit), WeatherRepository, descrizioni dei codici WMO
  calendar/  CalendarReader (CalendarContract)
  word/      WordOfTheDay (elenco locale in res/raw)
  data/      Room: AlarmEntity, AlarmDao, AppDatabase, AlarmRepository
  util/      orari, giorni, testi relativi, produttore del telefono
```

Come funziona una sveglia:

1. `AlarmScheduler` programma la sveglia con `AlarmManager.setAlarmClock` (puntuale anche in Doze) e un secondo allarme `setExactAndAllowWhileIdle` 10 minuti prima.
2. Il secondo allarme accoda `BriefingAudioWorker` (WorkManager, expedited), che scarica il meteo, compone il testo e salva l'MP3 della voce naturale.
3. All'orario `AlarmReceiver` avvia `AlarmService` in foreground: musica sul canale sveglia, schermata a tutto schermo anche sopra la schermata di blocco.
4. Dopo 3 secondi la musica si abbassa e parte il briefing: l'MP3 se è stato preparato per questa sveglia, altrimenti la voce di sistema.
5. Le sveglie ripetute si riprogrammano da sole; quelle singole si disattivano.

## Stack

Java 17 · Android SDK 36 (minSdk 26) · XML Views + Material Components 3 · Room · WorkManager · Retrofit + Gson · Google Cloud Text-to-Speech · Open-Meteo · JUnit

## Compilare

Serve Android Studio (o solo l'Android SDK) e JDK 17+.

```bash
./gradlew assembleDebug        # APK in app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # test unitari
./gradlew installDebug         # installa sul telefono collegato
```

### Voce naturale (facoltativa)

1. Crea un progetto su Google Cloud, collega la fatturazione e abilita **Cloud Text-to-Speech API** (il primo milione di caratteri al mese è gratuito: un briefing al giorno ne usa circa 10.000).
2. Crea una chiave API limitata a Text-to-Speech e all'app Android (`it.luca.sveglia` + SHA-1 del certificato di firma).
3. Aggiungi la chiave a `local.properties`, che è escluso da git:

```properties
CLOUD_TTS_API_KEY=la-tua-chiave
```

Senza chiave l'app funziona comunque con la voce di sistema.

## Crediti

- Meteo: [Open-Meteo](https://open-meteo.com) (CC BY 4.0)
- Voce naturale: Google Cloud Text-to-Speech
