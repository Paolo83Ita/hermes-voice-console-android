# Guida all'installazione — APK riparato GRAPH A4R (debug di qualifica)

Generato 2026-11-20T12:15:54Z.

1. Scarica `HERMES_VOICE_CONSOLE_A4R_REPAIRED_DEBUG.apk` (variante ARM64)
   e il file `A4R_REPAIRED_APK_SHA256.txt`.
2. (Facoltativo ma consigliato) Verifica l'impronta: il valore sha256 nel
   file deve corrispondere al file scaricato. Questa è la verifica P1 del
   mini-gate.
3. Apri il file APK dal file manager del telefono. Se il sistema chiede
   "consenti installazione da questa origine", consenti SOLO per questa
   volta.
4. L'app si installa come `Hermes Voice Console (A4R)` (pacchetto
   `com.hermestower.voiceconsole.a4rdebug`, versione
   0.4.0-alpha-grapha4r-merged-b-base, versionCode 13) ACCANTO alle app
   esistenti — nessuna sovrascrittura.
5. Alla prima apertura: onboarding → Tower Studio → "Scansiona il QR della
   Tower". Il permesso fotocamera è richiesto solo in quella schermata e
   serve solo per leggere il QR: nessun fotogramma esce dal telefono.
6. Il pairing segue il contratto Tower voice-console/v2: QR v2 → codice a
   6 cifre → frase a tre parole confermata sulla console Tower → stato
   salvato CIFRATO (AndroidKeyStore, AES-256-GCM).
