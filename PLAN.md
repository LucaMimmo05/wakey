# Sveglia parlante (Android, Java)

## Obiettivo
App Android nativa che funziona come sveglia: all'orario impostato suona, e quando l'utente la spegne legge ad alta voce ora, meteo e (in seguito) eventi del calendario, con musica di sottofondo e **voce naturale**. Stile "briefing" di Bixby.

Target principale: telefono Xiaomi/Redmi (HyperOS/MIUI), che uccide aggressivamente i processi in background.

## Stack
- **Java** (11 o 17), Android Studio, minSdk 26, targetSdk ultimo stabile
- UI: **XML Views + Material Components** (Compose è pensato per Kotlin, in Java è scomodo)
- Sveglia: `AlarmManager.setAlarmClock`
- Voce di ripiego: `TextToSpeech` di sistema (`Locale.ITALIAN`)
- Voce naturale: **Google Cloud Text-to-Speech** (REST) con voce italiana neurale
- Meteo: Open-Meteo (nessuna chiave API) con Retrofit + Gson
- Musica: `MediaPlayer` o Media3/ExoPlayer
- Lavori in background: WorkManager
- Persistenza: Room (annotation processor, dalla tappa 5)
- Geolocalizzazione: `FusedLocationProvider` (opzionale, in alternativa città fissa)

## Struttura package
```
app/src/main/java/.../sveglia/
  ui/            Activity e Fragment (lista sveglie, editor, schermata squillo)
  alarm/         AlarmScheduler, AlarmReceiver, PrefetchReceiver, BootReceiver
  service/       AlarmService (foreground service), BriefingSpeaker
  weather/       WeatherApi (Retrofit), WeatherRepository
  tts/           CloudTtsClient, BriefingAudioWorker (WorkManager)
  data/          Room: AlarmEntity, AlarmDao, AppDatabase
  util/          permessi, formattazione orario
```

## Come lavorare (istruzioni per Claude Code)
- Procedi **una tappa alla volta**. Alla fine di ogni tappa: compila (`./gradlew assembleDebug`), esegui i test se esistono, riassumi cosa è stato fatto e **fermati** in attesa di conferma.
- Non aggiungere funzioni non richieste dalla tappa corrente.
- Solo Java, niente Kotlin. Codice semplice e commentato dove serve; niente librerie extra senza motivo.
- Testi dell'interfaccia in italiano, in `strings.xml`.
- Non inserire chiavi o dati personali nel codice né nel repository (vedi tappa 4).

---

## Tappa 1 - Sveglia fissa che parla (MVP)
**Cosa fare**
- Creare il progetto con una sola schermata: bottone "Imposta sveglia tra 1 minuto" (per testare) e scelta dell'orario con `MaterialTimePicker`.
- `AlarmScheduler` che programma con `setAlarmClock`.
- `AlarmReceiver` che avvia `AlarmService` come foreground service con notifica.
- `AlarmService`: riproduce un suono sveglia in loop e mostra un'activity a schermo intero con bottone "Spegni".
- Al tap su "Spegni": ferma il suono e `BriefingSpeaker` dice "Buongiorno, sono le HH:mm" con la voce di sistema (per ora senza meteo).
- Permessi nel manifest: `SCHEDULE_EXACT_ALARM` (con richiesta all'utente su Android 12+), `POST_NOTIFICATIONS`, `USE_FULL_SCREEN_INTENT`, `FOREGROUND_SERVICE`, `WAKE_LOCK`.

**Criterio di accettazione**
La sveglia suona all'orario esatto anche con app chiusa e schermo bloccato; dopo "Spegni" il telefono pronuncia l'ora in italiano.

## Tappa 2 - Meteo parlato
**Cosa fare**
- `WeatherApi` (Retrofit) verso Open-Meteo: temperatura attuale, temperatura max/min del giorno, probabilità di pioggia, codice meteo.
- Mappare il `weathercode` in frasi italiane ("cielo sereno", "pioggia", ecc.).
- Città di default Gallarate (lat 45.66, lon 8.79), poi impostazione modificabile.
- Il briefing diventa: "Buongiorno, sono le HH:mm. Oggi a Gallarate [descrizione], temperatura X gradi, massima Y, minima Z, probabilità di pioggia W per cento."
- Gestire assenza di internet: briefing con sola ora e frase "meteo non disponibile".
- Chiamate di rete fuori dal main thread, con timeout breve.

**Criterio di accettazione**
Il briefing contiene dati meteo reali; senza rete non crasha e dice comunque l'ora.

## Tappa 3 - Musica di sottofondo
**Cosa fare**
- Scelta del suono/brano per la sveglia (file locale tramite selettore file, o suono di sistema).
- Durante il briefing vocale abbassare il volume della musica (`AudioFocus` con ducking), poi riportarlo.
- Opzione: musica che continua dopo il briefing, con bottone di stop.

**Criterio di accettazione**
La musica parte con la sveglia, si abbassa mentre parla la voce e riprende.

## Tappa 4 - Voce naturale (Google Cloud TTS)
**Idea**
Generare l'audio del briefing **prima** della sveglia, salvarlo in un file e riprodurlo subito quando l'utente spegne la sveglia. Così la voce è naturale e non ci sono attese al risveglio.

**Cosa fare**
- `CloudTtsClient`: chiamata REST `POST https://texttospeech.googleapis.com/v1/text:synthesize` con `languageCode: it-IT`, una voce neurale italiana (Neural2 o Chirp; verificare nella documentazione quali voci `it-IT` sono disponibili) e `audioEncoding: MP3`. La risposta contiene l'audio in base64 (`audioContent`): decodificarlo e salvarlo in `getCacheDir()/briefing.mp3`.
- `PrefetchReceiver`: programmare con `setExactAndAllowWhileIdle` un secondo allarme **10 minuti prima** della sveglia. Quando scatta, accoda un lavoro WorkManager (expedited) `BriefingAudioWorker`.
- `BriefingAudioWorker`: scarica il meteo, costruisce il testo del briefing, chiama `CloudTtsClient`, salva il file.
- Allo "Spegni": se `briefing.mp3` esiste ed è di oggi, riprodurlo con `MediaPlayer` (con ducking della musica). Altrimenti **ripiego** sulla voce di sistema `TextToSpeech`.
- Impostazione per scegliere la voce e un interruttore "voce naturale on/off".

**Chiave API (importante)**
- Salvare la chiave in `local.properties` (già escluso da git) e leggerla tramite `BuildConfig`. Mai nel codice o nel repository.
- Nella console Google Cloud limitare la chiave: solo l'API Text-to-Speech e, se possibile, restrizione per package name e firma SHA-1 dell'app.
- Impostare un budget/avviso di costo. Per una frase al giorno il consumo è minimo, ma conviene controllarlo.

**Criterio di accettazione**
Spegnendo la sveglia parte una voce italiana naturale con ora e meteo, senza attesa. Con internet spento durante il prefetch, parte comunque la voce di sistema.

## Tappa 5 - Più sveglie e persistenza
**Cosa fare**
- Room con `AlarmEntity` (id, ora, minuti, giorni della settimana, attiva, brano, etichetta).
- Schermata lista sveglie con switch attivo/disattivo, aggiunta, modifica, eliminazione.
- Ripetizione sui giorni scelti: dopo ogni suono riprogrammare la prossima (sveglia e relativo prefetch).
- `BootReceiver` (`BOOT_COMPLETED`) che riprogramma tutte le sveglie attive dopo il riavvio.

**Criterio di accettazione**
Sveglie multiple con giorni diversi, sopravvivono a riavvio e a chiusura dell'app.

## Tappa 6 - Compatibilità Xiaomi/HyperOS
**Cosa fare**
- Schermata guidata al primo avvio che porta l'utente a: esclusione dal risparmio batteria (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`), avvio automatico, e "mostra su schermata di blocco" nelle impostazioni dell'app.
- Rilevare il produttore (`Build.MANUFACTURER`) e mostrare le istruzioni solo su Xiaomi/Redmi/POCO.
- Verificare che anche il prefetch (allarme + WorkManager) parta con telefono bloccato.
- Test reale con telefono bloccato per diverse ore (sveglia notturna).

**Criterio di accettazione**
La sveglia e il briefing vocale funzionano in modo affidabile alle 7:00 dopo una notte con schermo spento.

## Tappa 7 - Extra (facoltativa)
- Eventi del giorno dal calendario (`READ_CALENDAR`) letti nel briefing.
- Impostazioni: cosa leggere (ora, meteo, calendario), velocità della voce.
- Snooze e spegnimento con gesto.
- Icona, tema chiaro/scuro, README per il portfolio.

---

## Rischi da tenere d'occhio
- Il processo viene ucciso dal sistema: la sveglia deve essere sempre programmata con `setAlarmClock` e il servizio deve partire in foreground subito.
- WorkManager non è puntuale al minuto: per questo il prefetch parte 10 minuti prima e c'è sempre il ripiego sulla voce di sistema.
- TTS di sistema non inizializzato o lingua italiana non installata: controllare `TextToSpeech.LANG_MISSING_DATA` e avvisare l'utente.
- Android 14+: richiede permessi espliciti per sveglie esatte e full-screen intent.
- Audio sul canale giusto: usare `AudioAttributes.USAGE_ALARM` così suona anche a volume suoneria basso.

## Primo comando da dare a Claude Code
> Leggi PLAN.md. Crea il progetto Android in Java (XML Views, Material Components) e implementa solo la Tappa 1. Compila, riassumi e fermati.
